package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * High-performance PDF rendering engine backed by [android.graphics.pdf.PdfRenderer] and
 * [LruCache]<Int, Bitmap> for both high-resolution main viewport pages and fast miniature
 * thumbnails on [Dispatchers.IO].
 *
 * All native [PdfRenderer] page operations are synchronized with a [Mutex] to guarantee
 * only one [PdfRenderer.Page] is open at any moment.
 */
class PdfRenderEngine(
    highResCacheMaxKb: Int = calculateDefaultCacheSizeKb(fraction = 8),
    thumbnailCacheMaxKb: Int = calculateDefaultCacheSizeKb(fraction = 16)
) : Closeable {

    private val renderMutex = Mutex()

    private var currentFile: File? = null
    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var cachedPageCount: Int = 0

    /**
     * LruCache<Int, Bitmap> storing rendered high-resolution page bitmaps keyed by `pageIndex`.
     */
    private val highResPageCache: LruCache<Int, Bitmap> = object : LruCache<Int, Bitmap>(
        max(4 * 1024, highResCacheMaxKb)
    ) {
        override fun sizeOf(key: Int, value: Bitmap): Int {
            return max(1, value.byteCount / 1024)
        }
    }

    /**
     * LruCache<Int, Bitmap> storing fast miniature page thumbnails keyed by `pageIndex`.
     */
    private val thumbnailCache: LruCache<Int, Bitmap> = object : LruCache<Int, Bitmap>(
        max(2 * 1024, thumbnailCacheMaxKb)
    ) {
        override fun sizeOf(key: Int, value: Bitmap): Int {
            return max(1, value.byteCount / 1024)
        }
    }

    private val pageAspectRatios = mutableMapOf<Int, Float>()

    /**
     * Opens the specified [pdfFile] for rendering on [Dispatchers.IO] and returns its total page count.
     */
    suspend fun openDocument(pdfFile: File, forceReload: Boolean = false): Int =
        withContext(Dispatchers.IO) {
            renderMutex.withLock {
                if (!pdfFile.exists() || pdfFile.length() == 0L) {
                    closeInternalLocked()
                    return@withLock 0
                }

                if (!forceReload &&
                    currentFile?.absolutePath == pdfFile.absolutePath &&
                    pdfRenderer != null
                ) {
                    return@withLock cachedPageCount
                }

                closeInternalLocked()

                return@withLock try {
                    val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    currentFile = pdfFile
                    fileDescriptor = pfd
                    pdfRenderer = renderer
                    cachedPageCount = renderer.pageCount
                    cachedPageCount
                } catch (e: Exception) {
                    closeInternalLocked()
                    0
                }
            }
        }

    /**
     * Returns the total number of pages in the currently opened PDF document.
     */
    fun getPageCount(): Int = cachedPageCount

    /**
     * Renders a high-resolution [Bitmap] for [pageIndex] on [Dispatchers.IO], returning
     * the cached bitmap from [highResPageCache] if already rendered.
     */
    suspend fun renderPageHighRes(
        pageIndex: Int,
        targetWidthPx: Int = 1440
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (pageIndex < 0) return@withContext null

        highResPageCache.get(pageIndex)?.let { cached ->
            if (!cached.isRecycled) return@withContext cached
        }

        renderMutex.withLock {
            highResPageCache.get(pageIndex)?.let { cached ->
                if (!cached.isRecycled) return@withLock cached
            }

            val renderer = pdfRenderer ?: return@withLock null
            if (pageIndex !in 0 until renderer.pageCount) return@withLock null

            try {
                renderer.openPage(pageIndex).use { page ->
                    val pageWidthPt = max(1, page.width)
                    val pageHeightPt = max(1, page.height)
                    val widthOverHeight = pageWidthPt.toFloat() / pageHeightPt.toFloat()
                    pageAspectRatios[pageIndex] = widthOverHeight

                    val widthPx = targetWidthPx.coerceIn(480, 2560)
                    val heightPx = max(
                        320,
                        (widthPx.toFloat() * (pageHeightPt.toFloat() / pageWidthPt.toFloat())).roundToInt()
                    )

                    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)

                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )

                    highResPageCache.put(pageIndex, bitmap)
                    bitmap
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Renders a fast miniature thumbnail [Bitmap] for [pageIndex] on [Dispatchers.IO],
     * returning the cached bitmap from [thumbnailCache] if already rendered.
     */
    suspend fun renderThumbnail(
        pageIndex: Int,
        targetWidthPx: Int = 220
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (pageIndex < 0) return@withContext null

        thumbnailCache.get(pageIndex)?.let { cached ->
            if (!cached.isRecycled) return@withContext cached
        }

        renderMutex.withLock {
            thumbnailCache.get(pageIndex)?.let { cached ->
                if (!cached.isRecycled) return@withLock cached
            }

            val renderer = pdfRenderer ?: return@withLock null
            if (pageIndex !in 0 until renderer.pageCount) return@withLock null

            try {
                renderer.openPage(pageIndex).use { page ->
                    val pageWidthPt = max(1, page.width)
                    val pageHeightPt = max(1, page.height)
                    val widthOverHeight = pageWidthPt.toFloat() / pageHeightPt.toFloat()
                    pageAspectRatios[pageIndex] = widthOverHeight

                    val widthPx = targetWidthPx.coerceIn(96, 512)
                    val heightPx = max(
                        72,
                        (widthPx.toFloat() * (pageHeightPt.toFloat() / pageWidthPt.toFloat())).roundToInt()
                    )

                    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)

                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )

                    thumbnailCache.put(pageIndex, bitmap)
                    bitmap
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Returns the width-to-height aspect ratio of [pageIndex], defaulting to `1.32f` or `0.77f`.
     */
    fun getCachedPageAspectRatio(pageIndex: Int, defaultRatio: Float = 1.32f): Float {
        return pageAspectRatios[pageIndex] ?: defaultRatio
    }

    /**
     * Clears both the high-resolution and thumbnail LRU caches (for example after PDFBox saves edits).
     */
    fun invalidateCaches() {
        highResPageCache.evictAll()
        thumbnailCache.evictAll()
        pageAspectRatios.clear()
    }

    private fun closeInternalLocked() {
        highResPageCache.evictAll()
        thumbnailCache.evictAll()
        pageAspectRatios.clear()
        cachedPageCount = 0
        currentFile = null
        runCatching { pdfRenderer?.close() }
        runCatching { fileDescriptor?.close() }
        pdfRenderer = null
        fileDescriptor = null
    }

    override fun close() {
        highResPageCache.evictAll()
        thumbnailCache.evictAll()
        pageAspectRatios.clear()
        cachedPageCount = 0
        currentFile = null
        runCatching { pdfRenderer?.close() }
        runCatching { fileDescriptor?.close() }
        pdfRenderer = null
        fileDescriptor = null
    }

    companion object {
        private fun calculateDefaultCacheSizeKb(fraction: Int): Int {
            val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
            return max(4 * 1024, maxMemoryKb / fraction)
        }
    }
}
