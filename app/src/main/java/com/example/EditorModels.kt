package com.example

import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import java.util.UUID

/**
 * Room Entity representing a scanned, imported, or edited PDF document tracked by PDF Z Scanner.
 */
@Entity(
    tableName = "pdf_documents",
    indices = [
        Index(value = ["filePath"], unique = true),
        Index(value = ["lastOpenedAt"]),
        Index(value = ["isDownloaded"]),
        Index(value = ["isOpened"])
    ]
)
data class PdfDocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val filePath: String,
    val thumbnailPath: String? = null,
    val pageCount: Int = 1,
    val lastOpenedAt: Long = System.currentTimeMillis(),
    val isDownloaded: Boolean = false,
    val isOpened: Boolean = false
)

/**
 * Represents a freehand ink drawing or highlighter stroke on a PDF page.
 *
 * All [normalizedPoints] are stored in normalized page space (`0.0f..1.0f` for both X and Y,
 * with origin `(0f, 0f)` at the top-left of the page) so they map losslessly between any
 * Compose Canvas size and PDFBox 72-DPI page points.
 */
data class DrawingStroke(
    val id: String = UUID.randomUUID().toString(),
    val normalizedPoints: List<Offset>,
    val strokeColor: Int = Color.RED,
    val fillColor: Int? = null,
    val thicknessPt: Float = 5f,
    val opacity: Float = 1.0f,
    val isHighlight: Boolean = false,
    val pageIndex: Int = 0
) {
    /**
     * Converts normalized points (`0.0f..1.0f`) to Compose Canvas pixel coordinates.
     */
    fun toCanvasPoints(canvasSize: Size): List<Offset> {
        if (canvasSize.width <= 0f || canvasSize.height <= 0f) return emptyList()
        return normalizedPoints.map { pt ->
            CoordinateMapper.normalizedToCanvasPoint(pt, canvasSize)
        }
    }

    /**
     * Converts normalized points (`0.0f..1.0f`, top-left origin) to PDFBox 72-DPI page
     * points (`0..pageWidthPt`, `0..pageHeightPt`, bottom-left origin).
     */
    fun toPdfBoxPoints(pageWidthPt: Float, pageHeightPt: Float): List<PointF> {
        return CoordinateMapper.normalizedPointsToPdfBoxPoints(
            normalizedPoints = normalizedPoints,
            pageWidthPt = pageWidthPt,
            pageHeightPt = pageHeightPt
        )
    }
}

/**
 * Represents an OCR-recognized or user-created editable text block on a PDF page.
 *
 * [normalizedBox] has coordinates (`left`, `top`, `right`, `bottom`) in `[0.0f, 1.0f]`
 * relative to the page's width and height (top-left origin).
 */
data class EditableTextBlock(
    val id: String = UUID.randomUUID().toString(),
    val originalText: String,
    val currentText: String = originalText,
    val normalizedBox: RectF,
    val originalNormalizedBox: RectF = RectF(normalizedBox),
    val fontName: String = FONT_ROBOTO,
    val fontSizeSp: Float = 18f,
    val textColor: Int = Color.parseColor("#0F172A"),
    val backgroundColor: Int = Color.TRANSPARENT,
    val isTransparentBackground: Boolean = true,
    val isTransparent: Boolean = isTransparentBackground,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val alignment: Paint.Align = Paint.Align.CENTER,
    val lineSpacing: Float = 1.15f,
    val isModified: Boolean = false,
    val isOriginalBakedIn: Boolean = true,
    val pageIndex: Int = 0
) {
    val isBackgroundTransparent: Boolean
        get() = isTransparentBackground || isTransparent || backgroundColor == Color.TRANSPARENT
    /**
     * Maps the normalized bounding box (`0.0f..1.0f`) to a Compose [Rect] in pixels.
     */
    fun toCanvasRect(canvasSize: Size): Rect {
        return CoordinateMapper.normalizedBoxToCanvasRect(normalizedBox, canvasSize)
    }

    /**
     * Maps the normalized bounding box (`0.0f..1.0f`, top-left origin) to PDFBox 72-DPI
     * page coordinates (`lowerLeftX`, `lowerLeftY`, `upperRightX`, `upperRightY` in bottom-left origin).
     */
    fun toPdfBoxRect(pageWidthPt: Float, pageHeightPt: Float): RectF {
        val boxPt = CoordinateMapper.normalizedBoxToPdfBoxRect(
            normalizedBox = normalizedBox,
            pageWidthPt = pageWidthPt,
            pageHeightPt = pageHeightPt
        )
        return RectF(
            boxPt.lowerLeftX,
            boxPt.lowerLeftY,
            boxPt.upperRightX,
            boxPt.upperRightY
        )
    }

    companion object {
        const val FONT_CARROIS_GOTHIC_SC = "Carrois Gothic SC"
        const val FONT_COURIER = "Courier"
        const val FONT_CUTIVE_MONO = "Cutive Mono"
        const val FONT_DANCING_SCRIPT = "Dancing Script"
        const val FONT_DROID_SANS_MONO = "Droid Sans Mono"
        const val FONT_HELVETICA = "Helvetica"
        const val FONT_ROBOTO = "Roboto"
        const val FONT_TIMES_ROMAN = "Times-Roman"

        val AVAILABLE_FONTS: List<String> = listOf(
            FONT_CARROIS_GOTHIC_SC,
            FONT_COURIER,
            FONT_CUTIVE_MONO,
            FONT_DANCING_SCRIPT,
            FONT_DROID_SANS_MONO,
            FONT_HELVETICA,
            FONT_ROBOTO
        )

        val LINE_SPACING_OPTIONS: List<Float> = listOf(1.0f, 1.15f, 1.5f, 2.0f)

        fun resolveComposeFontFamily(fontName: String): FontFamily {
            return when (fontName) {
                FONT_CARROIS_GOTHIC_SC -> FontFamily(Font(R.font.carrois_gothic_sc, FontWeight.Normal))
                FONT_COURIER -> FontFamily.Monospace
                FONT_CUTIVE_MONO -> FontFamily(Font(R.font.cutive_mono, FontWeight.Normal))
                FONT_DANCING_SCRIPT -> FontFamily(Font(R.font.dancing_script, FontWeight.Normal))
                FONT_DROID_SANS_MONO -> FontFamily.Monospace
                FONT_HELVETICA -> FontFamily(Font(R.font.inter, FontWeight.Normal))
                FONT_ROBOTO -> FontFamily(Font(R.font.roboto, FontWeight.Normal))
                FONT_TIMES_ROMAN -> FontFamily.Serif
                else -> FontFamily(Font(R.font.roboto, FontWeight.Normal))
            }
        }

        fun resolvePdfBoxFont(
            fontName: String,
            isBold: Boolean,
            isItalic: Boolean
        ): PDType1Font {
            return when (fontName) {
                FONT_COURIER, FONT_CUTIVE_MONO, FONT_DROID_SANS_MONO -> when {
                    isBold && isItalic -> PDType1Font.COURIER_BOLD_OBLIQUE
                    isBold -> PDType1Font.COURIER_BOLD
                    isItalic -> PDType1Font.COURIER_OBLIQUE
                    else -> PDType1Font.COURIER
                }
                FONT_DANCING_SCRIPT, FONT_TIMES_ROMAN -> when {
                    isBold && isItalic -> PDType1Font.TIMES_BOLD_ITALIC
                    isBold -> PDType1Font.TIMES_BOLD
                    isItalic -> PDType1Font.TIMES_ITALIC
                    else -> PDType1Font.TIMES_ROMAN
                }
                else -> when {
                    isBold && isItalic -> PDType1Font.HELVETICA_BOLD_OBLIQUE
                    isBold -> PDType1Font.HELVETICA_BOLD
                    isItalic -> PDType1Font.HELVETICA_OBLIQUE
                    else -> PDType1Font.HELVETICA
                }
            }
        }
    }
}

/**
 * Represents a deleted original baked-in OCR text region along with its dynamically sampled
 * [ambientBackgroundColor] so both the in-memory [android.graphics.Bitmap] and [PdfBoxSaveEngine]
 * seamlessly restore the underlying background without leaving a harsh white patch.
 */
data class DeletedRedaction(
    val blockId: String,
    val normalizedBox: RectF,
    val ambientBackgroundColor: Int
)

/**
 * Sealed command hierarchy for the Undo/Redo stack in the PDF Editor.
 * Every mutation to strokes or text blocks is captured as a reversible [EditorAction].
 */
sealed interface EditorAction {
    val pageIndex: Int

    fun invert(): EditorAction

    data class AddStroke(
        override val pageIndex: Int,
        val stroke: DrawingStroke
    ) : EditorAction {
        override fun invert(): EditorAction = RemoveStroke(pageIndex = pageIndex, stroke = stroke)
    }

    data class RemoveStroke(
        override val pageIndex: Int,
        val stroke: DrawingStroke
    ) : EditorAction {
        override fun invert(): EditorAction = AddStroke(pageIndex = pageIndex, stroke = stroke)
    }

    data class ClearStrokes(
        override val pageIndex: Int,
        val removedStrokes: List<DrawingStroke>
    ) : EditorAction {
        override fun invert(): EditorAction = RestoreStrokes(
            pageIndex = pageIndex,
            restoredStrokes = removedStrokes
        )
    }

    data class RestoreStrokes(
        override val pageIndex: Int,
        val restoredStrokes: List<DrawingStroke>
    ) : EditorAction {
        override fun invert(): EditorAction = ClearStrokes(
            pageIndex = pageIndex,
            removedStrokes = restoredStrokes
        )
    }

    data class UpdateTextBlock(
        override val pageIndex: Int,
        val previousBlock: EditableTextBlock,
        val updatedBlock: EditableTextBlock
    ) : EditorAction {
        override fun invert(): EditorAction = UpdateTextBlock(
            pageIndex = pageIndex,
            previousBlock = updatedBlock,
            updatedBlock = previousBlock
        )
    }

    data class AddTextBlock(
        override val pageIndex: Int,
        val block: EditableTextBlock
    ) : EditorAction {
        override fun invert(): EditorAction = RemoveTextBlock(pageIndex = pageIndex, block = block)
    }

    data class RemoveTextBlock(
        override val pageIndex: Int,
        val block: EditableTextBlock
    ) : EditorAction {
        override fun invert(): EditorAction = AddTextBlock(pageIndex = pageIndex, block = block)
    }

    data class BatchAction(
        override val pageIndex: Int,
        val actions: List<EditorAction>
    ) : EditorAction {
        override fun invert(): EditorAction = BatchAction(
            pageIndex = pageIndex,
            actions = actions.asReversed().map { it.invert() }
        )
    }
}

/**
 * Utility object for normalizing and denormalizing coordinates between Compose Canvas pixels,
 * ML Kit Image bitmaps, and PDFBox 72-DPI page points.
 */
object NormalizedCoordinateMapper {

    fun canvasToNormalized(point: Offset, canvasSize: Size): Offset {
        if (canvasSize.width <= 0f || canvasSize.height <= 0f) return Offset.Zero
        return Offset(
            x = (point.x / canvasSize.width).coerceIn(0f, 1f),
            y = (point.y / canvasSize.height).coerceIn(0f, 1f)
        )
    }

    fun bitmapRectToNormalized(
        rect: android.graphics.Rect,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): RectF {
        if (bitmapWidth <= 0 || bitmapHeight <= 0) return RectF(0f, 0f, 0f, 0f)
        val w = bitmapWidth.toFloat()
        val h = bitmapHeight.toFloat()
        return RectF(
            (rect.left / w).coerceIn(0f, 1f),
            (rect.top / h).coerceIn(0f, 1f),
            (rect.right / w).coerceIn(0f, 1f),
            (rect.bottom / h).coerceIn(0f, 1f)
        )
    }

    fun thicknessPtToCanvasPx(
        thicknessPt: Float,
        canvasWidthPx: Float,
        pageWidthPt: Float = 612f
    ): Float {
        if (canvasWidthPx <= 0f || pageWidthPt <= 0f) return thicknessPt
        return (thicknessPt / pageWidthPt) * canvasWidthPx
    }
}
