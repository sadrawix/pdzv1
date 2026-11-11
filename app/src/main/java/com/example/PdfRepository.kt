package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Filter tabs for the PDF Z Scanner home screen document list.
 */
enum class DocumentFilter {
    ALL_RECENT,
    DOWNLOADS,
    OPENED
}

/**
 * Launch modes when transitioning from the PDF Viewer Screen to the PDF Editor Screen.
 */
enum class EditorLaunchMode {
    FREE_ANNOTATION,
    PREMIUM_OCR_TEXT_EDIT
}

/**
 * Result of inspecting and rendering the first page of a PDF file via [PdfRenderer].
 */
data class PdfMetadataAndThumbnail(
    val thumbnailPath: String?,
    val pageCount: Int
)

/**
 * Repository managing PDF files on disk, first-page cover thumbnail generation via
 * [android.graphics.pdf.PdfRenderer], and reactive Room database persistence via [PdfDao].
 */
class PdfRepository(
    private val context: Context,
    private val pdfDao: PdfDao = PdfDatabase.getInstance(context).pdfDao()
) {
    val appContext: Context
        get() = context.applicationContext

    val saveEngine: PdfBoxSaveEngine by lazy {
        PdfBoxSaveEngine(context.applicationContext)
    }

    private val documentsDir: File by lazy {
        File(context.filesDir, "pdfs").apply { mkdirs() }
    }

    private val thumbnailsDir: File by lazy {
        File(context.filesDir, "thumbnails").apply { mkdirs() }
    }

    init {
        PdfBoxSaveEngine.ensurePdfBoxInitialized(context.applicationContext)
    }

    val recentDocuments: Flow<List<PdfDocumentEntity>> = pdfDao.getAllRecentDocuments()
    val downloadedDocuments: Flow<List<PdfDocumentEntity>> = pdfDao.getDownloadedDocuments()
    val openedDocuments: Flow<List<PdfDocumentEntity>> = pdfDao.getOpenedDocuments()

    fun getDocumentsByFilter(filter: DocumentFilter): Flow<List<PdfDocumentEntity>> {
        return when (filter) {
            DocumentFilter.ALL_RECENT -> recentDocuments
            DocumentFilter.DOWNLOADS -> downloadedDocuments
            DocumentFilter.OPENED -> openedDocuments
        }
    }

    fun searchDocuments(query: String): Flow<List<PdfDocumentEntity>> {
        val trimmed = query.trim()
        return if (trimmed.isEmpty()) {
            recentDocuments
        } else {
            pdfDao.getDocumentsBySearchQuery(trimmed)
        }
    }

    suspend fun getDocumentById(id: Long): PdfDocumentEntity? = withContext(Dispatchers.IO) {
        pdfDao.getDocumentById(id)
    }

    suspend fun markAsOpened(id: Long) = withContext(Dispatchers.IO) {
        pdfDao.markDocumentAsOpened(id, System.currentTimeMillis())
    }

    suspend fun importPdfFromUri(
        uri: Uri,
        isDownloaded: Boolean = true,
        isOpened: Boolean = false
    ): PdfDocumentEntity? = withContext(Dispatchers.IO) {
        runCatching {
            val displayName = resolveDisplayName(uri) ?: "Document_${System.currentTimeMillis()}.pdf"
            val sanitizedName = sanitizeFileName(
                if (displayName.endsWith(".pdf", ignoreCase = true)) displayName else "$displayName.pdf"
            )
            val destFile = File(documentsDir, "${System.currentTimeMillis()}_$sanitizedName")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext null

            registerPdfFile(
                pdfFile = destFile,
                displayName = ensurePdfExtension(displayName),
                isDownloaded = isDownloaded,
                isOpened = isOpened
            )
        }.getOrNull()
    }

    suspend fun saveScannedDocument(
        pdfUri: Uri?,
        jpegPageUris: List<Uri> = emptyList()
    ): PdfDocumentEntity? = withContext(Dispatchers.IO) {
        val timestampLabel = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "Scan_$timestampLabel.pdf"
        val destFile = File(documentsDir, fileName)

        if (pdfUri != null) {
            val copied = runCatching {
                context.contentResolver.openInputStream(pdfUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                    true
                } ?: false
            }.getOrDefault(false)

            if (copied && destFile.exists() && destFile.length() > 0L) {
                return@withContext registerPdfFile(
                    pdfFile = destFile,
                    displayName = fileName,
                    isDownloaded = true,
                    isOpened = true
                )
            }
        }

        if (jpegPageUris.isNotEmpty()) {
            val built = runCatching {
                PDDocument().use { document ->
                    for (jpegUri in jpegPageUris) {
                        context.contentResolver.openInputStream(jpegUri)?.use { inputStream ->
                            val pdImage = JPEGFactory.createFromStream(document, inputStream)
                            val pageRect = PDRectangle.LETTER
                            val page = PDPage(pageRect)
                            document.addPage(page)

                            val imgW = pdImage.width.toFloat().coerceAtLeast(1f)
                            val imgH = pdImage.height.toFloat().coerceAtLeast(1f)
                            val scale = min(pageRect.width / imgW, pageRect.height / imgH)
                            val drawW = imgW * scale
                            val drawH = imgH * scale
                            val offsetX = (pageRect.width - drawW) / 2f
                            val offsetY = (pageRect.height - drawH) / 2f

                            PDPageContentStream(document, page).use { contentStream ->
                                contentStream.drawImage(pdImage, offsetX, offsetY, drawW, drawH)
                            }
                        }
                    }
                    if (document.numberOfPages > 0) {
                        document.save(destFile)
                        true
                    } else {
                        false
                    }
                }
            }.getOrDefault(false)

            if (built && destFile.exists() && destFile.length() > 0L) {
                return@withContext registerPdfFile(
                    pdfFile = destFile,
                    displayName = fileName,
                    isDownloaded = true,
                    isOpened = true
                )
            }
        }

        null
    }

    suspend fun registerPdfFile(
        pdfFile: File,
        displayName: String = pdfFile.name,
        isDownloaded: Boolean = false,
        isOpened: Boolean = false
    ): PdfDocumentEntity = withContext(Dispatchers.IO) {
        val existing = pdfDao.getDocumentByFilePath(pdfFile.absolutePath)
        val metadata = generateFirstPageThumbnail(pdfFile)
        val now = System.currentTimeMillis()
        val normalizedName = ensurePdfExtension(displayName.ifBlank { "Scanned_Document.pdf" })

        if (existing != null) {
            val updated = existing.copy(
                name = normalizedName,
                thumbnailPath = metadata.thumbnailPath ?: existing.thumbnailPath,
                pageCount = max(1, metadata.pageCount),
                lastOpenedAt = now,
                isDownloaded = existing.isDownloaded || isDownloaded,
                isOpened = existing.isOpened || isOpened
            )
            pdfDao.updateDocument(updated)
            updated
        } else {
            val entity = PdfDocumentEntity(
                name = normalizedName,
                filePath = pdfFile.absolutePath,
                thumbnailPath = metadata.thumbnailPath,
                pageCount = max(1, metadata.pageCount),
                lastOpenedAt = now,
                isDownloaded = isDownloaded,
                isOpened = isOpened
            )
            val newId = pdfDao.insertDocument(entity)
            entity.copy(id = newId)
        }
    }

    suspend fun refreshThumbnailAndMetadata(documentId: Long): PdfDocumentEntity? =
        withContext(Dispatchers.IO) {
            val doc = pdfDao.getDocumentById(documentId) ?: return@withContext null
            val pdfFile = File(doc.filePath)
            if (!pdfFile.exists()) return@withContext doc

            val metadata = generateFirstPageThumbnail(pdfFile, forceRegenerate = true)
            val updated = doc.copy(
                thumbnailPath = metadata.thumbnailPath ?: doc.thumbnailPath,
                pageCount = max(1, metadata.pageCount),
                lastOpenedAt = System.currentTimeMillis()
            )
            pdfDao.updateDocument(updated)
            updated
        }

    suspend fun generateFirstPageThumbnail(
        pdfFile: File,
        targetWidthPx: Int = 540,
        forceRegenerate: Boolean = false
    ): PdfMetadataAndThumbnail = withContext(Dispatchers.IO) {
        if (!pdfFile.exists() || pdfFile.length() == 0L) {
            return@withContext PdfMetadataAndThumbnail(thumbnailPath = null, pageCount = 0)
        }

        val hashKey = sha256Short("${pdfFile.absolutePath}_${pdfFile.lastModified()}_${pdfFile.length()}")
        val thumbnailFile = File(thumbnailsDir, "thumb_$hashKey.png")

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount

            if (pageCount <= 0) {
                return@withContext PdfMetadataAndThumbnail(thumbnailPath = null, pageCount = 0)
            }

            if (thumbnailFile.exists() && thumbnailFile.length() > 0L && !forceRegenerate) {
                return@withContext PdfMetadataAndThumbnail(
                    thumbnailPath = thumbnailFile.absolutePath,
                    pageCount = pageCount
                )
            }

            renderer.openPage(0).use { firstPage ->
                val pageWidth = max(1, firstPage.width)
                val pageHeight = max(1, firstPage.height)
                val aspectRatio = pageHeight.toFloat() / pageWidth.toFloat()
                val widthPx = max(120, targetWidthPx)
                val heightPx = max(160, (widthPx * aspectRatio).roundToInt())

                val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)

                firstPage.render(
                    bitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )

                FileOutputStream(thumbnailFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 92, out)
                    out.flush()
                }
                bitmap.recycle()
            }

            PdfMetadataAndThumbnail(
                thumbnailPath = thumbnailFile.absolutePath,
                pageCount = pageCount
            )
        } catch (e: Exception) {
            PdfMetadataAndThumbnail(thumbnailPath = null, pageCount = 1)
        } finally {
            runCatching { renderer?.close() }
            runCatching { pfd?.close() }
        }
    }

    val trashedDocuments: StateFlow<List<PdfDocumentEntity>>
        get() = TrashBinStore.trashedDocuments

    suspend fun renameDocument(id: Long, newName: String) = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isNotEmpty()) {
            pdfDao.renameDocument(id, ensurePdfExtension(trimmed))
        }
    }

    /**
     * Moves [document] into the Trash Bin (`Delete -> Bin` in the Files tab) while keeping its
     * underlying PDF and thumbnail files intact so it can be restored or permanently deleted.
     */
    suspend fun moveToTrashBin(document: PdfDocumentEntity) = withContext(Dispatchers.IO) {
        pdfDao.deleteDocument(document)
        TrashBinStore.addTrashed(document)
    }

    /**
     * Restores a trashed [document] from the Trash Bin back into the active Room database.
     */
    suspend fun restoreFromTrashBin(document: PdfDocumentEntity): PdfDocumentEntity =
        withContext(Dispatchers.IO) {
            TrashBinStore.removeTrashed(document.filePath)
            val restored = document.copy(
                id = 0L,
                lastOpenedAt = System.currentTimeMillis()
            )
            val newId = pdfDao.insertDocument(restored)
            restored.copy(id = newId)
        }

    /**
     * Permanently deletes a single [document] from the Trash Bin and removes its PDF and
     * thumbnail files from internal storage.
     */
    suspend fun permanentlyDeleteFromTrashBin(document: PdfDocumentEntity) =
        withContext(Dispatchers.IO) {
            TrashBinStore.removeTrashed(document.filePath)
            deletePhysicalFiles(document)
        }

    /**
     * Empties all documents from the Trash Bin and removes their files from internal storage.
     */
    suspend fun emptyTrashBin() = withContext(Dispatchers.IO) {
        val allTrashed = TrashBinStore.clearAll()
        allTrashed.forEach { doc ->
            deletePhysicalFiles(doc)
        }
    }

    suspend fun deleteDocument(document: PdfDocumentEntity) = withContext(Dispatchers.IO) {
        moveToTrashBin(document)
    }

    private fun deletePhysicalFiles(document: PdfDocumentEntity) {
        runCatching {
            val pdfFile = File(document.filePath)
            if (pdfFile.exists() && pdfFile.absolutePath.startsWith(context.filesDir.absolutePath)) {
                pdfFile.delete()
            }
            document.thumbnailPath?.let { thumbPath ->
                val thumbFile = File(thumbPath)
                if (thumbFile.exists()) {
                    thumbFile.delete()
                }
            }
        }
    }

    /**
     * Creates a multi-page sample PDF document using PDFBox-Android (72-DPI coordinates).
     * When `coverStyle == 2` (`fractal_trading(2).pdf`), generates 7 landscape book-spread pages
     * (`792 x 600 pt` aspect `1.32:1`) matching the 7 spreads in the Stitch PDF Viewer design!
     */
    suspend fun createSamplePdfDocument(
        title: String = "Scanned_Document.pdf",
        isDownloaded: Boolean = true,
        isOpened: Boolean = true,
        coverStyle: Int = 0
    ): PdfDocumentEntity = withContext(Dispatchers.IO) {
        val displayTitle = ensurePdfExtension(title)
        val sanitized = sanitizeFileName(displayTitle)
        val outFile = File(documentsDir, "${System.currentTimeMillis()}_$sanitized")

        PDDocument().use { document ->
            if (coverStyle == 2) {
                buildFractalTradingBookSpreads(document)
            } else {
                val page1 = PDPage(PDRectangle.LETTER)
                document.addPage(page1)

                PDPageContentStream(document, page1).use { stream ->
                    if (coverStyle == 1) {
                        // Style 1: Welcome Guide cover
                        stream.setNonStrokingColor(43, 43, 78)
                        stream.addRect(0f, 440f, 612f, 352f)
                        stream.fill()

                        stream.setNonStrokingColor(84, 70, 115)
                        stream.addRect(0f, 200f, 612f, 240f)
                        stream.fill()

                        stream.setNonStrokingColor(235, 90, 102)
                        stream.addRect(0f, 0f, 612f, 200f)
                        stream.fill()

                        stream.setNonStrokingColor(255, 255, 255)
                        stream.addRect(236f, 520f, 140f, 140f)
                        stream.fill()

                        stream.setNonStrokingColor(229, 50, 45)
                        stream.beginText()
                        stream.setFont(PDType1Font.HELVETICA_BOLD, 42f)
                        stream.newLineAtOffset(262f, 575f)
                        stream.showText("PDF Z")
                        stream.endText()

                        stream.setNonStrokingColor(255, 255, 255)
                        stream.beginText()
                        stream.setFont(PDType1Font.HELVETICA_BOLD, 34f)
                        stream.newLineAtOffset(175f, 265f)
                        stream.showText("Welcome Guide")
                        stream.endText()

                        stream.setNonStrokingColor(252, 213, 206)
                        stream.beginText()
                        stream.setFont(PDType1Font.HELVETICA, 18f)
                        stream.newLineAtOffset(162f, 228f)
                        stream.showText("PDF tools for productive people")
                        stream.endText()

                        stream.setNonStrokingColor(255, 255, 255)
                        stream.beginText()
                        stream.setFont(PDType1Font.HELVETICA, 14f)
                        stream.newLineAtOffset(245f, 48f)
                        stream.showText("www.ilovepdf.com")
                        stream.endText()
                    } else {
                        // Style 3 / Default: Clean PDF Document with Red PDF Badge & OCR Text
                        stream.setNonStrokingColor(229, 50, 45)
                        stream.addRect(236f, 470f, 140f, 52f)
                        stream.fill()

                        stream.beginText()
                        stream.setNonStrokingColor(255, 255, 255)
                        stream.setFont(PDType1Font.HELVETICA_BOLD, 28f)
                        stream.newLineAtOffset(278f, 486f)
                        stream.showText("PDF")
                        stream.endText()

                        stream.setNonStrokingColor(229, 50, 45)
                        stream.addRect(226f, 415f, 160f, 10f)
                        stream.fill()
                        stream.addRect(226f, 392f, 160f, 10f)
                        stream.fill()
                        stream.addRect(246f, 369f, 120f, 10f)
                        stream.fill()

                        stream.beginText()
                        stream.setNonStrokingColor(15, 23, 42)
                        stream.setFont(PDType1Font.HELVETICA_BOLD, 16f)
                        stream.newLineAtOffset(64f, 280f)
                        stream.showText("Document Summary & OCR Test Page")
                        stream.endText()

                        val lines = listOf(
                            "Document Name: $displayTitle",
                            "Resolution: 72-DPI Native Vector Coordinates (612 x 792 pt)",
                            "Free Plan: View, Scan, Freehand Draw, Text Highlighting",
                            "Premium Plan: ML Kit OCR v2, Direct Text Edit, Typography & TTS"
                        )
                        var y = 248f
                        for (line in lines) {
                            stream.beginText()
                            stream.setNonStrokingColor(51, 65, 85)
                            stream.setFont(PDType1Font.HELVETICA, 12f)
                            stream.newLineAtOffset(64f, y)
                            stream.showText(line)
                            stream.endText()
                            y -= 24f
                        }
                    }
                }

                // Page 2: Detailed Editable & OCR-Ready Text Page
                val page2 = PDPage(PDRectangle.LETTER)
                document.addPage(page2)
                PDPageContentStream(document, page2).use { stream ->
                    stream.beginText()
                    stream.setNonStrokingColor(15, 23, 42)
                    stream.setFont(PDType1Font.HELVETICA_BOLD, 18f)
                    stream.newLineAtOffset(48f, 724f)
                    stream.showText("PDF Z Scanner — Editable OCR & Annotation Workspace")
                    stream.endText()

                    val page2Lines = listOf(
                        "1. Normalized Coordinate Mapping (0.0f to 1.0f)",
                        "All freehand strokes, highlighter markers, and OCR text blocks are stored",
                        "in normalized coordinates so they scale cleanly across any screen density",
                        "and map directly onto 72-DPI PDFBox page points (612 x 792 pt).",
                        "",
                        "2. Free Plan vs. Premium Plan Capabilities",
                        "Free Plan: Multi-page PDF viewing, ML Kit Document Scanning, Freehand",
                        "Ink Drawing, and Multiply-Blend Text Highlighting.",
                        "Premium Plan: ML Kit Text Recognition v2 (OCR), Direct In-Place Text",
                        "Block Editing, Font/Size/Line Spacing Customization, and Text-to-Speech."
                    )
                    var y = 682f
                    for (line in page2Lines) {
                        if (line.isNotEmpty()) {
                            stream.beginText()
                            val isHeader = line.startsWith("1.") || line.startsWith("2.")
                            stream.setFont(
                                if (isHeader) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA,
                                if (isHeader) 13.5f else 12f
                            )
                            stream.setNonStrokingColor(
                                if (isHeader) 229 else 30,
                                if (isHeader) 50 else 41,
                                if (isHeader) 45 else 59
                            )
                            stream.newLineAtOffset(48f, y)
                            stream.showText(line)
                            stream.endText()
                        }
                        y -= 24f
                    }
                }
            }

            document.save(outFile)
        }

        registerPdfFile(
            pdfFile = outFile,
            displayName = displayTitle,
            isDownloaded = isDownloaded,
            isOpened = isOpened
        )
    }

    /**
     * Generates the 7 1.32:1 book-spread pages (`792 x 600 pt`) for `fractal_trading(2).pdf`
     * matching the Stitch PDF Viewer screen and its 7 bottom thumbnail spreads.
     */
    private fun buildFractalTradingBookSpreads(document: PDDocument) {
        val spreadRect = PDRectangle(792f, 600f)

        // Spread 1 (Pages 1-2): Dark Fractal Horn Cover on Left + FRACTAL TRADING Title Page on Right
        val spread1 = PDPage(spreadRect)
        document.addPage(spread1)
        PDPageContentStream(document, spread1).use { s ->
            // Left dark panel
            s.setNonStrokingColor(10, 11, 14)
            s.addRect(22f, 22f, 362f, 556f)
            s.fill()

            // Glowing concentric spiral fractal circles on left page
            s.setStrokingColor(148, 163, 184)
            s.setLineWidth(1.5f)
            val spiralCenters = listOf(
                Triple(230f, 240f, 82f),
                Triple(205f, 285f, 58f),
                Triple(170f, 345f, 44f),
                Triple(135f, 405f, 30f),
                Triple(105f, 455f, 20f),
                Triple(82f, 492f, 12f)
            )
            for ((cx, cy, r) in spiralCenters) {
                s.setNonStrokingColor(51, 65, 85)
                s.addRect(cx - r, cy - r * 0.8f, r * 1.8f, r * 1.6f)
                s.stroke()
            }

            // Center spine divider
            s.setStrokingColor(203, 213, 225)
            s.setLineWidth(1f)
            s.moveTo(396f, 0f)
            s.lineTo(396f, 600f)
            s.stroke()

            // Right page: FRACTAL TRADING Title & OCR-ready text
            s.setNonStrokingColor(180, 180, 185)
            s.beginText()
            s.setFont(PDType1Font.TIMES_ROMAN, 9.5f)
            s.newLineAtOffset(432f, 535f)
            s.showText("Analyzing Financial Markets using Fractal Geometry and the Golden Ratio")
            s.endText()

            s.setNonStrokingColor(15, 23, 42)
            s.beginText()
            s.setFont(PDType1Font.HELVETICA_BOLD, 24f)
            s.newLineAtOffset(458f, 480f)
            s.showText("FRACTAL TRADING")
            s.endText()

            // Dotted underline
            s.setStrokingColor(64, 64, 64)
            s.setLineWidth(2f)
            var dotX = 430f
            while (dotX < 755f) {
                s.moveTo(dotX, 464f)
                s.lineTo(dotX + 3f, 464f)
                s.stroke()
                dotX += 7f
            }

            val subLines = listOf(
                "Analyzing Financial Markets",
                "using Fractal Geometry",
                "and the Golden Ratio"
            )
            var sy = 436f
            for (line in subLines) {
                s.beginText()
                s.setNonStrokingColor(38, 38, 38)
                s.setFont(PDType1Font.TIMES_ROMAN, 14f)
                s.newLineAtOffset(575f, sy)
                s.showText(line)
                s.endText()
                sy -= 18f
            }

            // Author block
            s.beginText()
            s.setNonStrokingColor(17, 24, 39)
            s.setFont(PDType1Font.TIMES_BOLD, 20f)
            s.newLineAtOffset(546f, 260f)
            s.showText("Soul Esprit")
            s.endText()

            s.beginText()
            s.setNonStrokingColor(82, 82, 91)
            s.setFont(PDType1Font.TIMES_ITALIC, 11.5f)
            s.newLineAtOffset(515f, 238f)
            s.showText("Servant of the Lord Jesus Christ")
            s.endText()

            // Publisher footer
            s.beginText()
            s.setNonStrokingColor(38, 38, 38)
            s.setFont(PDType1Font.TIMES_ROMAN, 12.5f)
            s.newLineAtOffset(532f, 48f)
            s.showText("NarrowGate Publishing")
            s.endText()
        }

        // Spreads 2 to 7 matching the 6 remaining thumbnails in the Stitch footer
        val spreadConfigs = listOf(
            "Spread 3-4: Harmonic Price Waves & Golden Ratio Projections" to
                "Fibonacci retracement vectors (0.382, 0.618, 0.786) across multi-timeframe charts.",
            "Spread 5-6: Self-Similar Market Structure & Equilibrium Zones" to
                "Identifying institutional liquidity pools and fractal swing highs/lows.",
            "Spread 7-8: Quantitative Correlation Matrix & Volatility Ratios" to
                "Empirical distribution of impulse vs. corrective wave durations.",
            "Spread 9-10: Algorithmic Execution & Risk Management Protocol" to
                "Position sizing calibrated to structural invalidation thresholds.",
            "Spread 11-12: Elliott Wave & Mandelbrot Multifractal Synthesis" to
                "Combining non-linear time compression with harmonic price expansion.",
            "Spread 13-14: Epilogue & Appendix Summary" to
                "Complete index of geometric ratios, chart templates, and trading rules."
        )

        spreadConfigs.forEachIndexed { idx, (heading, summary) ->
            val page = PDPage(spreadRect)
            document.addPage(page)
            PDPageContentStream(document, page).use { s ->
                // Center spine line
                s.setStrokingColor(203, 213, 225)
                s.setLineWidth(1f)
                s.moveTo(396f, 0f)
                s.lineTo(396f, 600f)
                s.stroke()

                // Left Page Header & Content
                s.setNonStrokingColor(15, 23, 42)
                s.beginText()
                s.setFont(PDType1Font.HELVETICA_BOLD, 15f)
                s.newLineAtOffset(42f, 540f)
                s.showText(heading)
                s.endText()

                s.setNonStrokingColor(51, 65, 85)
                s.beginText()
                s.setFont(PDType1Font.TIMES_ROMAN, 13f)
                s.newLineAtOffset(42f, 505f)
                s.showText(summary)
                s.endText()

                // Draw chart / table graphics matching the thumbnail style
                s.setStrokingColor(71, 85, 105)
                s.setLineWidth(2f)
                s.moveTo(48f, 220f)
                s.lineTo(120f, 340f)
                s.lineTo(190f, 260f)
                s.lineTo(270f, 410f)
                s.lineTo(350f, 240f)
                s.stroke()

                // Right Page OCR-ready paragraphs
                s.setNonStrokingColor(15, 23, 42)
                s.beginText()
                s.setFont(PDType1Font.HELVETICA_BOLD, 14f)
                s.newLineAtOffset(432f, 540f)
                s.showText("Chapter ${idx + 2} — Analytical Notes")
                s.endText()

                val rightLines = listOf(
                    "Financial markets operate as non-linear feedback loops",
                    "where self-similar geometric structures repeat across",
                    "every observable timeframe from 1-minute to monthly bars.",
                    "",
                    "Use PDF Z Scanner's Freehand Drawing and Highlighting",
                    "tools to annotate key breakout structures, or unlock",
                    "Premium OCR to edit these text blocks directly in place."
                )
                var ry = 498f
                for (line in rightLines) {
                    if (line.isNotEmpty()) {
                        s.beginText()
                        s.setNonStrokingColor(38, 38, 38)
                        s.setFont(PDType1Font.TIMES_ROMAN, 12.5f)
                        s.newLineAtOffset(432f, ry)
                        s.showText(line)
                        s.endText()
                    }
                    ry -= 22f
                }
            }
        }
    }

    /**
     * Seeds initial sample documents matching the Stitch Home Screen cards if the database
     * is empty on first launch, and upgrades `fractal_trading(2).pdf` to the 7-spread book if needed.
     */
    suspend fun ensureSeedDocumentsIfEmpty() = withContext(Dispatchers.IO) {
        if (pdfDao.getDocumentCount() == 0) {
            createSamplePdfDocument(
                title = "fractal_trading(1).pdf",
                isDownloaded = true,
                isOpened = false,
                coverStyle = 3
            )
            kotlinx.coroutines.delay(15)
            createSamplePdfDocument(
                title = "fractal_trading(2).pdf",
                isDownloaded = true,
                isOpened = true,
                coverStyle = 2
            )
            kotlinx.coroutines.delay(15)
            createSamplePdfDocument(
                title = "User guide.pdf",
                isDownloaded = true,
                isOpened = true,
                coverStyle = 1
            )
        }
    }

    private fun ensurePdfExtension(name: String): String {
        val trimmed = name.trim()
        return if (trimmed.endsWith(".pdf", ignoreCase = true)) trimmed else "$trimmed.pdf"
    }

    private fun resolveDisplayName(uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) {
                            return cursor.getString(idx)
                        }
                    }
                }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun sanitizeFileName(raw: String): String {
        return raw.replace(Regex("[^a-zA-Z0-9._()\\- ]"), "_")
            .trim()
            .replace(Regex("\\s+"), "_")
            .take(64)
            .ifBlank { "Document.pdf" }
    }

    private fun sha256Short(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.take(10).joinToString("") { "%02x".format(it) }
    }
}

/**
 * Shared reactive Trash Bin store tracking documents moved to the Bin (`Delete -> Bin` in Files tab).
 */
object TrashBinStore {
    private val _trashedDocuments = MutableStateFlow<List<PdfDocumentEntity>>(emptyList())
    val trashedDocuments: StateFlow<List<PdfDocumentEntity>> = _trashedDocuments.asStateFlow()

    fun addTrashed(document: PdfDocumentEntity) {
        _trashedDocuments.update { current ->
            val withoutDuplicate = current.filterNot { it.filePath == document.filePath }
            listOf(document.copy(lastOpenedAt = System.currentTimeMillis())) + withoutDuplicate
        }
    }

    fun removeTrashed(filePath: String) {
        _trashedDocuments.update { current ->
            current.filterNot { it.filePath == filePath }
        }
    }

    fun clearAll(): List<PdfDocumentEntity> {
        val snapshot = _trashedDocuments.value
        _trashedDocuments.value = emptyList()
        return snapshot
    }
}

