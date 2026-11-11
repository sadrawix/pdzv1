package com.example

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.font.PDFont
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font
import com.tom_roush.pdfbox.pdmodel.graphics.blend.BlendMode
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Production PDFBox-Android save & flattening engine (`com.tom-roush:pdfbox-android`).
 *
 * Responsibilities:
 * 1. Initializes `PDFBoxResourceLoader.init(context)` safely on Android.
 * 2. Loads the target [PDDocument] and opens a [PDPageContentStream] in [PDPageContentStream.AppendMode.APPEND].
 * 3. **Flattens Freehand Drawings & Highlights**:
 *    - Maps normalized `[0f..1f]` points to 72-DPI PDFBox page points via [CoordinateMapper].
 *    - Applies [PDExtendedGraphicsState] for stroke/fill opacity (`CA` / `ca`) and uses
 *      [BlendMode.MULTIPLY] for highlighter strokes so highlights blend transparently over black text.
 * 4. **Flattens Modified OCR Text Blocks**:
 *    - For every [EditableTextBlock] with `isModified == true`, draws a solid white filled rectangle
 *      over its `normalizedBox` to erase the original scanned text, then renders the edited
 *      multi-line text using the chosen TrueType font from `assets/fonts/` (with standard PDFBox
 *      Type-1 font fallback), font size, RGB color, alignment (`LEFT`, `CENTER`, `RIGHT`), and
 *      line spacing multiplier (`1`, `1.15`, `1.5`, `2`).
 */
class PdfBoxSaveEngine(
    context: Context
) {
    private val appContext: Context = context.applicationContext

    init {
        ensurePdfBoxInitialized(appContext)
    }

    /**
     * Flattens all [strokes], [deletedRedactions], and modified [textBlocks] onto [pageIndex] of
     * [pdfFile] and saves the updated PDF document in-place on [Dispatchers.IO].
     */
    suspend fun flattenAndSavePageEdits(
        pdfFile: File,
        pageIndex: Int,
        strokes: List<DrawingStroke>,
        textBlocks: List<EditableTextBlock>,
        deletedRedactions: List<DeletedRedaction> = emptyList()
    ): Boolean = withContext(Dispatchers.IO) {
        ensurePdfBoxInitialized(appContext)
        if (!pdfFile.exists()) return@withContext false

        val modifiedTextBlocks = textBlocks.filter { it.isModified }
        if (strokes.isEmpty() && modifiedTextBlocks.isEmpty() && deletedRedactions.isEmpty()) {
            return@withContext true
        }

        runCatching {
            PDDocument.load(pdfFile).use { pdDoc ->
                if (pageIndex !in 0 until pdDoc.numberOfPages) return@withContext false
                val page = pdDoc.getPage(pageIndex)
                val mediaBox = page.mediaBox
                val pageWidthPt = mediaBox.width
                val pageHeightPt = mediaBox.height

                // Cache loaded TrueType fonts per save pass so we don't embed duplicates
                val loadedFontCache = mutableMapOf<String, PDFont>()

                PDPageContentStream(
                    pdDoc,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
                ).use { stream ->
                    // -------------------------------------------------------------------------
                    // 0. Erase Original Baked-In OCR Text Regions Using Sampled Natural Background RGB
                    // -------------------------------------------------------------------------
                    if (deletedRedactions.isNotEmpty()) {
                        for (redaction in deletedRedactions) {
                            val bg = redaction.ambientBackgroundColor
                            if (bg == AndroidColor.TRANSPARENT || AndroidColor.alpha(bg) == 0) continue
                            stream.setNonStrokingColor(
                                AndroidColor.red(bg),
                                AndroidColor.green(bg),
                                AndroidColor.blue(bg)
                            )
                            val pdfRect = CoordinateMapper.normalizedBoxToPdfBoxRect(
                                normalizedBox = redaction.normalizedBox,
                                pageWidthPt = pageWidthPt,
                                pageHeightPt = pageHeightPt
                            )
                            stream.addRect(
                                pdfRect.lowerLeftX.coerceAtLeast(0f),
                                pdfRect.lowerLeftY.coerceAtLeast(0f),
                                pdfRect.widthPt,
                                pdfRect.heightPt
                            )
                            stream.fill()
                        }
                    }

                    // -------------------------------------------------------------------------
                    // 1. Flatten Modified OCR Text Blocks (Clean Vector Text Glyphs Only)
                    // -------------------------------------------------------------------------
                    if (modifiedTextBlocks.isNotEmpty()) {
                        for (block in modifiedTextBlocks) {
                            flattenModifiedTextBlock(
                                pdDoc = pdDoc,
                                stream = stream,
                                block = block,
                                pageWidthPt = pageWidthPt,
                                pageHeightPt = pageHeightPt,
                                fontCache = loadedFontCache
                            )
                        }
                    }

                    // -------------------------------------------------------------------------
                    // 2. Flatten Freehand Drawings & Multiply-Blended Text Highlights
                    // -------------------------------------------------------------------------
                    for (stroke in strokes) {
                        flattenDrawingOrHighlightStroke(
                            stream = stream,
                            stroke = stroke,
                            pageWidthPt = pageWidthPt,
                            pageHeightPt = pageHeightPt
                        )
                    }
                }

                pdDoc.save(pdfFile)
            }
            true
        }.getOrDefault(false)
    }

    /**
     * Writes a single [DrawingStroke] (freehand ink path or highlighter stroke) into [stream]
     * using [PDExtendedGraphicsState] for opacity and [BlendMode.MULTIPLY] for highlights.
     */
    private fun flattenDrawingOrHighlightStroke(
        stream: PDPageContentStream,
        stroke: DrawingStroke,
        pageWidthPt: Float,
        pageHeightPt: Float
    ) {
        val pdfPoints: List<PointF> = CoordinateMapper.normalizedPointsToPdfBoxPoints(
            normalizedPoints = stroke.normalizedPoints,
            pageWidthPt = pageWidthPt,
            pageHeightPt = pageHeightPt
        )
        if (pdfPoints.size < 2) return

        val clampedOpacity = stroke.opacity.coerceIn(0.05f, 1.0f)

        stream.saveGraphicsState()
        try {
            val extState = PDExtendedGraphicsState().apply {
                strokingAlphaConstant = clampedOpacity
                nonStrokingAlphaConstant = if (stroke.fillColor != null) {
                    (clampedOpacity * 0.45f).coerceIn(0.08f, 0.85f)
                } else {
                    clampedOpacity
                }
                blendMode = if (stroke.isHighlight) {
                    BlendMode.MULTIPLY
                } else {
                    BlendMode.NORMAL
                }
            }
            stream.setGraphicsStateParameters(extState)

            // Optional fill colour for closed freehand drawing shapes
            val fillColorInt = stroke.fillColor
            if (!stroke.isHighlight && fillColorInt != null && pdfPoints.size >= 3) {
                stream.setNonStrokingColor(
                    AndroidColor.red(fillColorInt),
                    AndroidColor.green(fillColorInt),
                    AndroidColor.blue(fillColorInt)
                )
                appendSmoothPdfPath(stream, pdfPoints)
                stream.closePath()
                stream.fill()
            }

            // Stroke path with round cap (1) for freehand drawing or square cap (2) for highlight
            stream.setStrokingColor(
                AndroidColor.red(stroke.strokeColor),
                AndroidColor.green(stroke.strokeColor),
                AndroidColor.blue(stroke.strokeColor)
            )
            stream.setLineWidth(stroke.thicknessPt.coerceIn(1f, 60f))
            stream.setLineCapStyle(if (stroke.isHighlight) 2 else 1)
            stream.setLineJoinStyle(1)

            appendSmoothPdfPath(stream, pdfPoints)
            stream.stroke()
        } finally {
            stream.restoreGraphicsState()
        }
    }

    /**
     * Appends a smooth cubic/quadratic Bezier path through [pts] in PDFBox page coordinates.
     */
    private fun appendSmoothPdfPath(
        stream: PDPageContentStream,
        pts: List<PointF>
    ) {
        if (pts.isEmpty()) return
        stream.moveTo(pts.first().x, pts.first().y)
        if (pts.size == 2) {
            stream.lineTo(pts[1].x, pts[1].y)
            return
        }

        for (i in 1 until pts.size - 1) {
            val current = pts[i]
            val next = pts[i + 1]
            val midX = (current.x + next.x) / 2f
            val midY = (current.y + next.y) / 2f
            stream.curveTo1(current.x, current.y, midX, midY)
        }

        val secondLast = pts[pts.size - 2]
        val last = pts.last()
        stream.curveTo1(secondLast.x, secondLast.y, last.x, last.y)
    }

    /**
     * Renders the edited multi-line text over `block.normalizedBox`.
     * When `block.isBackgroundTransparent` is `true`, no solid white/colored rectangle is drawn
     * behind the text so the underlying PDF content shows through.
     */
    private fun flattenModifiedTextBlock(
        pdDoc: PDDocument,
        stream: PDPageContentStream,
        block: EditableTextBlock,
        pageWidthPt: Float,
        pageHeightPt: Float,
        fontCache: MutableMap<String, PDFont>
    ) {
        val pdfRect: PdfBoxRectPt = CoordinateMapper.normalizedBoxToPdfBoxRect(
            normalizedBox = block.normalizedBox,
            pageWidthPt = pageWidthPt,
            pageHeightPt = pageHeightPt
        )

        // 1. Only draw a crisp flat background rectangle if `block.isTransparentBackground` is false
        //    AND an explicit non-transparent `backgroundColor` was set (zero bleed/feathering).
        if (
            !block.isTransparentBackground &&
            !block.isTransparent &&
            block.backgroundColor != AndroidColor.TRANSPARENT
        ) {
            val bgColorInt = block.backgroundColor
            stream.setNonStrokingColor(
                AndroidColor.red(bgColorInt),
                AndroidColor.green(bgColorInt),
                AndroidColor.blue(bgColorInt)
            )
            stream.addRect(
                pdfRect.lowerLeftX.coerceAtLeast(0f),
                pdfRect.lowerLeftY.coerceAtLeast(0f),
                pdfRect.widthPt,
                pdfRect.heightPt
            )
            stream.fill()
        }

        // 2. Resolve TTF font from assets/fonts/ or fallback to standard PDType1Font
        val font: PDFont = resolvePdfFontWithAssetTtf(
            pdDoc = pdDoc,
            fontName = block.fontName,
            isBold = block.isBold,
            isItalic = block.isItalic,
            fontCache = fontCache
        )

        val pdfScale = (pageWidthPt / 360f).coerceIn(1.0f, 3.0f)
        val fontSizePt = (block.fontSizeSp * pdfScale).coerceIn(8f, 96f)
        val lineHeightPt = fontSizePt * block.lineSpacing.coerceIn(1.0f, 2.5f)

        // 3. Set RGB text color
        stream.setNonStrokingColor(
            AndroidColor.red(block.textColor),
            AndroidColor.green(block.textColor),
            AndroidColor.blue(block.textColor)
        )

        // 4. Break text into lines (wrapping long lines to fit within pdfRect.widthPt)
        val wrappedLines = wrapTextToPdfBoxWidth(
            rawText = block.currentText,
            font = font,
            fontSizePt = fontSizePt,
            maxWidthPt = (pdfRect.widthPt - 8f).coerceAtLeast(40f)
        )

        // Start first line baseline near the top of the bounding box
        var baselineY = pdfRect.upperRightY - fontSizePt * 1.05f

        for (line in wrappedLines) {
            if (baselineY < pdfRect.lowerLeftY - fontSizePt * 0.5f) break

            val sanitizedLine = sanitizeForPdfFont(line)
            if (sanitizedLine.isNotEmpty()) {
                val lineWidthPt = measurePdfStringWidthPt(font, sanitizedLine, fontSizePt)
                val startX = when (block.alignment) {
                    Paint.Align.CENTER ->
                        (pdfRect.lowerLeftX + (pdfRect.widthPt - lineWidthPt) / 2f)
                            .coerceAtLeast(pdfRect.lowerLeftX + 2f)
                    Paint.Align.RIGHT ->
                        (pdfRect.upperRightX - lineWidthPt - 4f)
                            .coerceAtLeast(pdfRect.lowerLeftX + 2f)
                    else -> pdfRect.lowerLeftX + 4f
                }

                stream.beginText()
                stream.setFont(font, fontSizePt)
                stream.newLineAtOffset(startX, baselineY)
                runCatching {
                    stream.showText(sanitizedLine)
                }.onFailure {
                    // Fallback to standard Helvetica if a custom glyph wasn't present in the TTF
                    val fallbackFont = EditableTextBlock.resolvePdfBoxFont(
                        fontName = block.fontName,
                        isBold = block.isBold,
                        isItalic = block.isItalic
                    )
                    stream.setFont(fallbackFont, fontSizePt)
                    stream.showText(sanitizedLine.replace(Regex("[^\\x20-\\x7E]"), " "))
                }
                stream.endText()
            }

            baselineY -= lineHeightPt
        }
    }

    /**
     * Loads the requested TrueType font from `assets/fonts/` using [PDType0Font.load], or falls
     * back to [EditableTextBlock.resolvePdfBoxFont] when a standard Type-1 font (`Courier`,
     * `Times-Roman`, or bold/italic variants) is selected.
     */
    private fun resolvePdfFontWithAssetTtf(
        pdDoc: PDDocument,
        fontName: String,
        isBold: Boolean,
        isItalic: Boolean,
        fontCache: MutableMap<String, PDFont>
    ): PDFont {
        val assetFileName: String? = when (fontName) {
            EditableTextBlock.FONT_CARROIS_GOTHIC_SC -> "fonts/carrois_gothic_sc.ttf"
            EditableTextBlock.FONT_CUTIVE_MONO -> "fonts/cutive_mono.ttf"
            EditableTextBlock.FONT_DANCING_SCRIPT -> "fonts/dancing_script.ttf"
            EditableTextBlock.FONT_ROBOTO -> if (!isBold && !isItalic) "fonts/roboto.ttf" else null
            EditableTextBlock.FONT_HELVETICA -> if (!isBold && !isItalic) "fonts/inter.ttf" else null
            else -> null
        }

        if (assetFileName != null) {
            fontCache[assetFileName]?.let { return it }
            val loadedTtf = runCatching {
                appContext.assets.open(assetFileName).use { inputStream ->
                    PDType0Font.load(pdDoc, inputStream)
                }
            }.getOrNull()

            if (loadedTtf != null) {
                fontCache[assetFileName] = loadedTtf
                return loadedTtf
            }
        }

        return EditableTextBlock.resolvePdfBoxFont(
            fontName = fontName,
            isBold = isBold,
            isItalic = isItalic
        )
    }

    /**
     * Wraps each paragraph line of [rawText] so that it fits within [maxWidthPt] when rendered
     * with [font] at [fontSizePt].
     */
    private fun wrapTextToPdfBoxWidth(
        rawText: String,
        font: PDFont,
        fontSizePt: Float,
        maxWidthPt: Float
    ): List<String> {
        val result = mutableListOf<String>()
        for (paragraph in rawText.lines()) {
            val trimmed = paragraph.trim()
            if (trimmed.isEmpty()) {
                result.add("")
                continue
            }
            val words = trimmed.split(Regex("\\s+"))
            var currentLine = StringBuilder()
            for (word in words) {
                val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                val candidateWidth = measurePdfStringWidthPt(font, candidate, fontSizePt)
                if (candidateWidth <= maxWidthPt || currentLine.isEmpty()) {
                    currentLine = StringBuilder(candidate)
                } else {
                    result.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                result.add(currentLine.toString())
            }
        }
        return result
    }

    private fun measurePdfStringWidthPt(
        font: PDFont,
        text: String,
        fontSizePt: Float
    ): Float {
        val sanitized = sanitizeForPdfFont(text)
        return runCatching {
            (font.getStringWidth(sanitized) / 1000f) * fontSizePt
        }.getOrDefault(sanitized.length * fontSizePt * 0.52f)
    }

    private fun sanitizeForPdfFont(text: String): String {
        return text
            .replace('\u2013', '-')
            .replace('\u2014', '-')
            .replace('\u2018', '\'')
            .replace('\u2019', '\'')
            .replace('\u201C', '"')
            .replace('\u201D', '"')
            .replace(Regex("[^\\x20-\\x7E]"), " ")
    }

    companion object {
        @Synchronized
        fun ensurePdfBoxInitialized(context: Context) {
            if (!PDFBoxResourceLoader.isReady()) {
                PDFBoxResourceLoader.init(context.applicationContext)
            }
        }
    }
}
