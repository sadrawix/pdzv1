package com.example

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Optical Character Recognition (OCR) & Positioned Text Extraction engine powered by
 * Google ML Kit Text Recognition v2 (`TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)`)
 * and PDFBox `TextPosition` coordinate extraction.
 *
 * Maps every detected text block into accurate normalized `[0.0f, 1.0f]` page coordinates
 * (`List<EditableTextBlock>`) along with sampled paper background color and ink color so
 * in-place editing erases the exact underlying text region without any duplication.
 */
class OcrScannerEngine(
    private val recognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )
) : Closeable {

    private data class PositionedSpan(
        val text: String,
        val leftPt: Float,
        val topPt: Float,
        val rightPt: Float,
        val bottomPt: Float,
        val baselineY: Float,
        val fontSizePt: Float,
        val isBold: Boolean,
        val isItalic: Boolean
    )

    private data class PositionedLine(
        var text: String,
        var leftPt: Float,
        var topPt: Float,
        var rightPt: Float,
        var bottomPt: Float,
        var baselineY: Float,
        var fontSizePt: Float,
        var isBold: Boolean,
        var isItalic: Boolean
    )

    /**
     * Extracts all [EditableTextBlock]s from [pageBitmap] and/or [fallbackPdfFile] on [Dispatchers.IO],
     * normalizing every bounding box into exact `[0.0f, 1.0f]` page coordinates.
     */
    suspend fun extractTextBlocks(
        pageBitmap: Bitmap?,
        pageIndex: Int = 0,
        fallbackPdfFile: File? = null
    ): List<EditableTextBlock> = withContext(Dispatchers.IO) {
        // 1. If the PDF file has embedded positioned text streams, extract exact 72-DPI
        //    character/line bounding boxes via PDFBox TextPosition so coordinates are 100% exact.
        if (fallbackPdfFile != null && fallbackPdfFile.exists()) {
            val pdfBoxPositionedBlocks = extractPositionedPdfBoxBlocks(
                pdfFile = fallbackPdfFile,
                pageIndex = pageIndex,
                pageBitmap = pageBitmap
            )
            if (pdfBoxPositionedBlocks.isNotEmpty()) {
                return@withContext pdfBoxPositionedBlocks
            }
        }

        // 2. For scanned/raster image pages without embedded PDF text, run ML Kit Text Recognition v2
        //    on the rendered page bitmap.
        val mlKitBlocks = extractMlKitVisionBlocks(
            pageBitmap = pageBitmap,
            pageIndex = pageIndex
        )
        if (mlKitBlocks.isNotEmpty()) {
            return@withContext mlKitBlocks
        }

        emptyList()
    }

    /**
     * Extracts exact normalized `[0.0f, 1.0f]` bounding boxes for every text block on [pageIndex]
     * using PDFBox's [PDFTextStripper] and [TextPosition] glyph coordinates.
     */
    private fun extractPositionedPdfBoxBlocks(
        pdfFile: File,
        pageIndex: Int,
        pageBitmap: Bitmap?
    ): List<EditableTextBlock> {
        return runCatching {
            PDDocument.load(pdfFile).use { pdDoc ->
                if (pageIndex !in 0 until pdDoc.numberOfPages) {
                    return@use emptyList()
                }
                val pdPage = pdDoc.getPage(pageIndex)
                val cropBox = pdPage.cropBox ?: pdPage.mediaBox
                val pageWidthPt = cropBox.width.coerceAtLeast(1f)
                val pageHeightPt = cropBox.height.coerceAtLeast(1f)

                val rawSpans = mutableListOf<PositionedSpan>()

                val stripper = object : PDFTextStripper() {
                    override fun writeString(
                        text: String?,
                        textPositions: MutableList<TextPosition>?
                    ) {
                        val cleanText = text?.trim().orEmpty()
                        val positions = textPositions?.filter { !it.unicode.isNullOrBlank() }.orEmpty()
                        if (cleanText.isEmpty() || positions.isEmpty()) return

                        val minX = positions.minOf { it.xDirAdj }
                        val maxX = positions.maxOf { it.xDirAdj + it.widthDirAdj.coerceAtLeast(2f) }
                        val avgBaselineY = positions.map { it.yDirAdj }.average().toFloat()
                        val maxFontPt = positions.maxOf {
                            max(it.fontSizeInPt, it.heightDir)
                        }.coerceIn(8f, 72f)

                        val minTop = positions.minOf {
                            it.yDirAdj - max(it.heightDir, maxFontPt * 0.92f)
                        }
                        val maxBottom = positions.maxOf {
                            it.yDirAdj + max(maxFontPt * 0.30f, 3f)
                        }

                        val fontNameLower = positions.firstOrNull()?.font?.name?.lowercase().orEmpty()
                        val isBold = fontNameLower.contains("bold") || fontNameLower.contains("black")
                        val isItalic = fontNameLower.contains("italic") || fontNameLower.contains("oblique")

                        rawSpans.add(
                            PositionedSpan(
                                text = cleanText,
                                leftPt = minX,
                                topPt = minTop,
                                rightPt = maxX,
                                bottomPt = maxBottom,
                                baselineY = avgBaselineY,
                                fontSizePt = maxFontPt,
                                isBold = isBold,
                                isItalic = isItalic
                            )
                        )
                    }
                }.apply {
                    sortByPosition = true
                    startPage = pageIndex + 1
                    endPage = pageIndex + 1
                }

                // Trigger PDFTextStripper to populate rawSpans
                stripper.getText(pdDoc)

                if (rawSpans.isEmpty()) {
                    return@use emptyList()
                }

                // Step A: Merge spans that sit on the same horizontal baseline into lines
                val lines = mutableListOf<PositionedLine>()
                for (span in rawSpans) {
                    val lastLine = lines.lastOrNull()
                    val sameBaseline = lastLine != null &&
                        abs(span.baselineY - lastLine.baselineY) <= max(lastLine.fontSizePt, span.fontSizePt) * 0.45f &&
                        span.leftPt >= lastLine.leftPt - 8f &&
                        (span.leftPt - lastLine.rightPt) <= max(lastLine.fontSizePt, span.fontSizePt) * 3.0f

                    if (sameBaseline && lastLine != null) {
                        val needsSpace = !lastLine.text.endsWith(" ") && !span.text.startsWith(" ")
                        lastLine.text = if (needsSpace) "${lastLine.text} ${span.text}" else "${lastLine.text}${span.text}"
                        lastLine.leftPt = min(lastLine.leftPt, span.leftPt)
                        lastLine.topPt = min(lastLine.topPt, span.topPt)
                        lastLine.rightPt = max(lastLine.rightPt, span.rightPt)
                        lastLine.bottomPt = max(lastLine.bottomPt, span.bottomPt)
                        lastLine.fontSizePt = max(lastLine.fontSizePt, span.fontSizePt)
                        lastLine.isBold = lastLine.isBold || span.isBold
                        lastLine.isItalic = lastLine.isItalic || span.isItalic
                    } else {
                        lines.add(
                            PositionedLine(
                                text = span.text,
                                leftPt = span.leftPt,
                                topPt = span.topPt,
                                rightPt = span.rightPt,
                                bottomPt = span.bottomPt,
                                baselineY = span.baselineY,
                                fontSizePt = span.fontSizePt,
                                isBold = span.isBold,
                                isItalic = span.isItalic
                            )
                        )
                    }
                }

                // Step B: Merge consecutive lines that belong to the same paragraph block
                // (same font size, same style, similar horizontal alignment, tight vertical gap)
                val groupedBlocks = mutableListOf<PositionedLine>()
                for (line in lines) {
                    val prev = groupedBlocks.lastOrNull()
                    val lineCountInPrev = prev?.text?.count { it == '\n' }?.plus(1) ?: 0
                    val verticalGap = if (prev != null) line.topPt - prev.bottomPt else Float.MAX_VALUE
                    val prevCenter = if (prev != null) (prev.leftPt + prev.rightPt) / 2f else 0f
                    val lineCenter = (line.leftPt + line.rightPt) / 2f

                    val canMergeParagraph = prev != null &&
                        lineCountInPrev < 4 &&
                        abs(prev.fontSizePt - line.fontSizePt) <= 1.5f &&
                        prev.isBold == line.isBold &&
                        prev.isItalic == line.isItalic &&
                        verticalGap >= -4f &&
                        verticalGap <= prev.fontSizePt * 0.95f &&
                        (abs(prev.leftPt - line.leftPt) <= 18f || abs(prevCenter - lineCenter) <= 24f)

                    if (canMergeParagraph && prev != null) {
                        prev.text = "${prev.text}\n${line.text}"
                        prev.leftPt = min(prev.leftPt, line.leftPt)
                        prev.topPt = min(prev.topPt, line.topPt)
                        prev.rightPt = max(prev.rightPt, line.rightPt)
                        prev.bottomPt = max(prev.bottomPt, line.bottomPt)
                    } else {
                        groupedBlocks.add(line)
                    }
                }

                // Step C: Convert each positioned block to normalized [0.0f, 1.0f] coordinates
                // with padding so erasing completely covers all glyph ascenders/descenders
                val padXPt = 5f
                val padYPt = 4f
                val visualScale = 360f / pageWidthPt

                groupedBlocks.map { block ->
                    val normLeft = ((block.leftPt - padXPt) / pageWidthPt).coerceIn(0.01f, 0.96f)
                    val normTop = ((block.topPt - padYPt) / pageHeightPt).coerceIn(0.01f, 0.96f)
                    val normRight = ((block.rightPt + padXPt) / pageWidthPt).coerceIn(normLeft + 0.06f, 0.99f)
                    val normBottom = ((block.bottomPt + padYPt) / pageHeightPt).coerceIn(normTop + 0.035f, 0.99f)
                    val normRect = RectF(normLeft, normTop, normRight, normBottom)

                    val pixelRect = if (pageBitmap != null && !pageBitmap.isRecycled) {
                        Rect(
                            (normLeft * pageBitmap.width).roundToInt(),
                            (normTop * pageBitmap.height).roundToInt(),
                            (normRight * pageBitmap.width).roundToInt(),
                            (normBottom * pageBitmap.height).roundToInt()
                        )
                    } else {
                        null
                    }

                    val sampledBgColor = if (pageBitmap != null && pixelRect != null) {
                        samplePaperBackgroundColor(pageBitmap, pixelRect)
                    } else {
                        Color.WHITE
                    }

                    val sampledTextColor = if (pageBitmap != null && pixelRect != null) {
                        sampleForegroundInkColor(pageBitmap, pixelRect, sampledBgColor)
                    } else {
                        Color.parseColor("#0F172A")
                    }

                    // Scale 72-DPI font size to visual canvas sp so inline editing matches 1:1
                    val visualFontSizeSp = (block.fontSizePt * visualScale).coerceIn(11f, 36f)

                    EditableTextBlock(
                        id = UUID.randomUUID().toString(),
                        originalText = block.text,
                        currentText = block.text,
                        normalizedBox = normRect,
                        fontName = EditableTextBlock.FONT_ROBOTO,
                        fontSizeSp = visualFontSizeSp,
                        textColor = sampledTextColor,
                        backgroundColor = Color.TRANSPARENT,
                        isTransparentBackground = true,
                        isTransparent = true,
                        isBold = block.isBold,
                        isItalic = block.isItalic,
                        alignment = Paint.Align.LEFT,
                        lineSpacing = 1.15f,
                        isModified = false,
                        pageIndex = pageIndex
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Runs Google ML Kit Text Recognition v2 on [pageBitmap] for camera-scanned or image-only PDFs.
     */
    private suspend fun extractMlKitVisionBlocks(
        pageBitmap: Bitmap?,
        pageIndex: Int
    ): List<EditableTextBlock> {
        val extracted = mutableListOf<EditableTextBlock>()
        if (pageBitmap == null || pageBitmap.isRecycled || pageBitmap.width <= 0 || pageBitmap.height <= 0) {
            return extracted
        }

        runCatching {
            val inputImage = InputImage.fromBitmap(pageBitmap, 0)
            val visionText = recognizer.process(inputImage).await()
            val bmpWidth = pageBitmap.width
            val bmpHeight = pageBitmap.height

            for (textBlock in visionText.textBlocks) {
                val rawText = textBlock.text.trim()
                val rect = textBlock.boundingBox
                if (rawText.isEmpty() || rect == null) continue

                val rawNorm = NormalizedCoordinateMapper.bitmapRectToNormalized(
                    rect = rect,
                    bitmapWidth = bmpWidth,
                    bitmapHeight = bmpHeight
                )
                // Expand slightly so the solid background rectangle covers all glyph edges cleanly
                val paddedNorm = RectF(
                    (rawNorm.left - 0.006f).coerceIn(0.01f, 0.96f),
                    (rawNorm.top - 0.006f).coerceIn(0.01f, 0.96f),
                    (rawNorm.right + 0.006f).coerceIn(rawNorm.left + 0.05f, 0.99f),
                    (rawNorm.bottom + 0.006f).coerceIn(rawNorm.top + 0.035f, 0.99f)
                )

                val lineCount = textBlock.lines.size.coerceAtLeast(1)
                val boxHeightSp = (paddedNorm.height() * 440f).coerceAtLeast(14f)
                val estimatedSizeSp = (boxHeightSp / (lineCount * 1.35f)).coerceIn(11f, 36f)
                val isAllCapsHeading = rawText == rawText.uppercase() && rawText.length >= 4
                val sampledBgColor = samplePaperBackgroundColor(pageBitmap, rect)
                val sampledInkColor = sampleForegroundInkColor(pageBitmap, rect, sampledBgColor)

                extracted.add(
                    EditableTextBlock(
                        id = UUID.randomUUID().toString(),
                        originalText = rawText,
                        currentText = rawText,
                        normalizedBox = paddedNorm,
                        fontName = EditableTextBlock.FONT_ROBOTO,
                        fontSizeSp = estimatedSizeSp,
                        textColor = sampledInkColor,
                        backgroundColor = Color.TRANSPARENT,
                        isTransparentBackground = true,
                        isTransparent = true,
                        isBold = isAllCapsHeading,
                        isItalic = false,
                        alignment = Paint.Align.LEFT,
                        lineSpacing = 1.15f,
                        isModified = false,
                        pageIndex = pageIndex
                    )
                )
            }
        }
        return extracted
    }

    /**
     * Samples pixels inside [rect] and along its immediate horizontal margins on [bitmap] using a
     * quantized color histogram to find the **dominant (mode) background color** — never mistaking
     * white text ink on colored/dark banners for a white background.
     */
    private fun samplePaperBackgroundColor(bitmap: Bitmap, rect: Rect): Int {
        return runCatching {
            val w = bitmap.width
            val h = bitmap.height
            if (w <= 0 || h <= 0) return Color.WHITE

            val left = rect.left.coerceIn(0, w - 1)
            val right = rect.right.coerceIn(left, w - 1)
            val top = rect.top.coerceIn(0, h - 1)
            val bottom = rect.bottom.coerceIn(top, h - 1)

            val counts = HashMap<Int, Int>(64)
            val sumR = HashMap<Int, Int>(64)
            val sumG = HashMap<Int, Int>(64)
            val sumB = HashMap<Int, Int>(64)

            fun addPixel(pixel: Int, weight: Int) {
                val r = Color.red(pixel)
                val g = Color.green(pixel)
                val b = Color.blue(pixel)
                val key = ((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)
                counts[key] = (counts[key] ?: 0) + weight
                sumR[key] = (sumR[key] ?: 0) + r * weight
                sumG[key] = (sumG[key] ?: 0) + g * weight
                sumB[key] = (sumB[key] ?: 0) + b * weight
            }

            val regionW = (right - left + 1).coerceAtLeast(1)
            val regionH = (bottom - top + 1).coerceAtLeast(1)
            val stepX = (regionW / 16).coerceAtLeast(1)
            val stepY = (regionH / 8).coerceAtLeast(1)

            for (dy in 0 until regionH step stepY) {
                val sy = (top + dy).coerceIn(0, h - 1)
                val isTopOrBottomRing = dy <= 2 || dy >= regionH - 3
                for (dx in 0 until regionW step stepX) {
                    val sx = (left + dx).coerceIn(0, w - 1)
                    val isLeftOrRightRing = dx <= 2 || dx >= regionW - 3
                    val weight = if (isTopOrBottomRing || isLeftOrRightRing) 3 else 1
                    addPixel(bitmap.getPixel(sx, sy), weight = weight)
                }
            }

            var bestKey = -1
            var bestWeight = -1
            for ((k, weight) in counts) {
                if (weight > bestWeight) {
                    bestWeight = weight
                    bestKey = k
                }
            }

            if (bestKey != -1 && bestWeight > 0) {
                Color.rgb(
                    (sumR[bestKey] ?: 255) / bestWeight,
                    (sumG[bestKey] ?: 255) / bestWeight,
                    (sumB[bestKey] ?: 255) / bestWeight
                )
            } else {
                Color.WHITE
            }
        }.getOrDefault(Color.WHITE)
    }

    /**
     * Samples pixels inside [rect] on [bitmap] to find the highest-contrast foreground ink color
     * relative to [backgroundColor].
     */
    private fun sampleForegroundInkColor(bitmap: Bitmap, rect: Rect, backgroundColor: Int): Int {
        return runCatching {
            val w = bitmap.width
            val h = bitmap.height
            if (w <= 0 || h <= 0) return Color.parseColor("#0F172A")

            val bgR = Color.red(backgroundColor)
            val bgG = Color.green(backgroundColor)
            val bgB = Color.blue(backgroundColor)

            val left = rect.left.coerceIn(0, w - 1)
            val right = rect.right.coerceIn(left, w - 1)
            val top = rect.top.coerceIn(0, h - 1)
            val bottom = rect.bottom.coerceIn(top, h - 1)

            val stepX = ((right - left) / 12).coerceAtLeast(1)
            val stepY = ((bottom - top) / 6).coerceAtLeast(1)

            var maxDistSq = -1
            var bestColor = Color.parseColor("#0F172A")

            var y = top
            while (y <= bottom) {
                var x = left
                while (x <= right) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    val dr = r - bgR
                    val dg = g - bgG
                    val db = b - bgB
                    val distSq = dr * dr + dg * dg + db * db
                    if (distSq > maxDistSq) {
                        maxDistSq = distSq
                        bestColor = Color.rgb(r, g, b)
                    }
                    x += stepX
                }
                y += stepY
            }

            if (maxDistSq >= 1800) bestColor else Color.parseColor("#0F172A")
        }.getOrDefault(Color.parseColor("#0F172A"))
    }

    override fun close() {
        runCatching { recognizer.close() }
    }
}
