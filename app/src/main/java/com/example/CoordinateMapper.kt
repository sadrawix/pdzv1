package com.example

import android.graphics.PointF
import android.graphics.Rect as AndroidRect
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/**
 * Represents a rectangle in PDFBox 72-DPI page points where the origin `(0, 0)` is at the
 * **bottom-left** corner of the PDF page.
 *
 * - [lowerLeftX]: X coordinate in points (`0..pageWidthPt`)
 * - [lowerLeftY]: Y coordinate of the bottom edge in points (`0..pageHeightPt`, bottom-left origin)
 * - [widthPt]: Width of the box in PDF points
 * - [heightPt]: Height of the box in PDF points
 * - [upperRightX]: Right edge in PDF points (`lowerLeftX + widthPt`)
 * - [upperRightY]: Top edge in PDF points (`lowerLeftY + heightPt`)
 */
data class PdfBoxRectPt(
    val lowerLeftX: Float,
    val lowerLeftY: Float,
    val widthPt: Float,
    val heightPt: Float
) {
    val upperRightX: Float get() = lowerLeftX + widthPt
    val upperRightY: Float get() = lowerLeftY + heightPt
}

/**
 * Bidirectional coordinate mapper between:
 * 1. **Normalized Page Space** (`0.0f..1.0f`, top-left origin `(0f, 0f)`),
 * 2. **Jetpack Compose Canvas Space** (`0..canvasWidthPx`, `0..canvasHeightPx`, top-left origin),
 * 3. **ML Kit / Android Bitmap Space** (`0..bitmapWidth`, `0..bitmapHeight`, top-left origin), and
 * 4. **PDFBox 72-DPI Page Point Space** (`0..pageWidthPt`, `0..pageHeightPt`, bottom-left origin).
 */
object CoordinateMapper {

    /**
     * Converts a single normalized point (`0f..1f`, top-left origin) into PDFBox page points
     * (`0..pageWidthPt`, `0..pageHeightPt`, bottom-left origin).
     */
    fun normalizedToPdfBoxPoint(
        normalizedPoint: Offset,
        pageWidthPt: Float,
        pageHeightPt: Float
    ): PointF {
        if (pageWidthPt <= 0f || pageHeightPt <= 0f) return PointF(0f, 0f)
        val clampedX = normalizedPoint.x.coerceIn(0f, 1f)
        val clampedY = normalizedPoint.y.coerceIn(0f, 1f)
        return PointF(
            clampedX * pageWidthPt,
            (1f - clampedY) * pageHeightPt
        )
    }

    /**
     * Converts a list of normalized points (`0f..1f`, top-left origin) into PDFBox page points
     * (`0..pageWidthPt`, `0..pageHeightPt`, bottom-left origin).
     */
    fun normalizedPointsToPdfBoxPoints(
        normalizedPoints: List<Offset>,
        pageWidthPt: Float,
        pageHeightPt: Float
    ): List<PointF> {
        if (pageWidthPt <= 0f || pageHeightPt <= 0f || normalizedPoints.isEmpty()) {
            return emptyList()
        }
        return normalizedPoints.map { pt ->
            normalizedToPdfBoxPoint(pt, pageWidthPt, pageHeightPt)
        }
    }

    /**
     * Converts a normalized bounding box (`left`, `top`, `right`, `bottom` in `0f..1f`,
     * top-left origin) into a [PdfBoxRectPt] suitable for `PDPageContentStream.addRect(x, y, w, h)`
     * in PDFBox's bottom-left origin coordinate system.
     */
    fun normalizedBoxToPdfBoxRect(
        normalizedBox: RectF,
        pageWidthPt: Float,
        pageHeightPt: Float
    ): PdfBoxRectPt {
        if (pageWidthPt <= 0f || pageHeightPt <= 0f) {
            return PdfBoxRectPt(0f, 0f, 0f, 0f)
        }
        val normLeft = normalizedBox.left.coerceIn(0f, 1f)
        val normTop = normalizedBox.top.coerceIn(0f, 1f)
        val normRight = normalizedBox.right.coerceIn(normLeft, 1f)
        val normBottom = normalizedBox.bottom.coerceIn(normTop, 1f)

        val lowerLeftX = normLeft * pageWidthPt
        val upperRightX = normRight * pageWidthPt
        val upperRightY = (1f - normTop) * pageHeightPt
        val lowerLeftY = (1f - normBottom) * pageHeightPt

        val widthPt = (upperRightX - lowerLeftX).coerceAtLeast(1f)
        val heightPt = (upperRightY - lowerLeftY).coerceAtLeast(1f)

        return PdfBoxRectPt(
            lowerLeftX = lowerLeftX,
            lowerLeftY = lowerLeftY,
            widthPt = widthPt,
            heightPt = heightPt
        )
    }

    /**
     * Converts a Compose Canvas pixel coordinate into normalized page space (`0f..1f`, top-left origin).
     */
    fun canvasToNormalizedPoint(
        canvasPoint: Offset,
        canvasSize: Size
    ): Offset {
        if (canvasSize.width <= 0f || canvasSize.height <= 0f) return Offset.Zero
        return Offset(
            x = (canvasPoint.x / canvasSize.width).coerceIn(0f, 1f),
            y = (canvasPoint.y / canvasSize.height).coerceIn(0f, 1f)
        )
    }

    /**
     * Converts a normalized point (`0f..1f`, top-left origin) into Compose Canvas pixel coordinates.
     */
    fun normalizedToCanvasPoint(
        normalizedPoint: Offset,
        canvasSize: Size
    ): Offset {
        if (canvasSize.width <= 0f || canvasSize.height <= 0f) return Offset.Zero
        return Offset(
            x = normalizedPoint.x.coerceIn(0f, 1f) * canvasSize.width,
            y = normalizedPoint.y.coerceIn(0f, 1f) * canvasSize.height
        )
    }

    /**
     * Converts a normalized bounding box (`0f..1f`, top-left origin) into a Compose [Rect] in pixels.
     */
    fun normalizedBoxToCanvasRect(
        normalizedBox: RectF,
        canvasSize: Size
    ): Rect {
        return Rect(
            left = normalizedBox.left.coerceIn(0f, 1f) * canvasSize.width,
            top = normalizedBox.top.coerceIn(0f, 1f) * canvasSize.height,
            right = normalizedBox.right.coerceIn(0f, 1f) * canvasSize.width,
            bottom = normalizedBox.bottom.coerceIn(0f, 1f) * canvasSize.height
        )
    }

    /**
     * Converts an ML Kit / Android [AndroidRect] in bitmap pixels into a normalized [RectF] (`0f..1f`).
     */
    fun bitmapRectToNormalizedBox(
        rect: AndroidRect,
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

    /**
     * Converts a normalized bounding box (`0.0f..1.0f`) into a pixel [RectF] for an in-memory
     * [android.graphics.Bitmap] of dimensions [bitmapWidth] x [bitmapHeight], including optional
     * pixel [bleedPx] padding to ensure complete glyph erasure.
     */
    fun toScreenRect(
        normalizedBox: RectF,
        bitmapWidth: Int,
        bitmapHeight: Int,
        bleedPx: Float = 4f
    ): RectF {
        if (bitmapWidth <= 0 || bitmapHeight <= 0) return RectF(0f, 0f, 0f, 0f)
        val w = bitmapWidth.toFloat()
        val h = bitmapHeight.toFloat()
        return RectF(
            (normalizedBox.left.coerceIn(0f, 1f) * w - bleedPx).coerceAtLeast(0f),
            (normalizedBox.top.coerceIn(0f, 1f) * h - bleedPx).coerceAtLeast(0f),
            (normalizedBox.right.coerceIn(0f, 1f) * w + bleedPx).coerceAtMost(w),
            (normalizedBox.bottom.coerceIn(0f, 1f) * h + bleedPx).coerceAtMost(h)
        )
    }

    /**
     * Converts a drag delta in screen/canvas pixels (`dragAmount.x`, `dragAmount.y`) into
     * normalized page coordinate deltas (`-1.0f..1.0f`) relative to [canvasSize].
     */
    fun pixelDeltaToNormalizedDelta(
        deltaPx: Offset,
        canvasSize: Size
    ): Offset {
        if (canvasSize.width <= 0f || canvasSize.height <= 0f) return Offset.Zero
        return Offset(
            x = deltaPx.x / canvasSize.width,
            y = deltaPx.y / canvasSize.height
        )
    }

    /**
     * Shifts [normalizedBox] by ([deltaNormalizedX], [deltaNormalizedY]) while preserving its
     * width and height and clamping strictly inside the PDF page bounds
     * (`0.0f <= left < right <= 1.0f` and `0.0f <= top < bottom <= 1.0f`).
     */
    fun shiftAndClampNormalizedBox(
        normalizedBox: RectF,
        deltaNormalizedX: Float,
        deltaNormalizedY: Float
    ): RectF {
        val boxWidth = (normalizedBox.right - normalizedBox.left).coerceIn(0.02f, 1.0f)
        val boxHeight = (normalizedBox.bottom - normalizedBox.top).coerceIn(0.02f, 1.0f)

        val maxLeft = (1.0f - boxWidth).coerceAtLeast(0.0f)
        val maxTop = (1.0f - boxHeight).coerceAtLeast(0.0f)

        val newLeft = (normalizedBox.left + deltaNormalizedX).coerceIn(0.0f, maxLeft)
        val newTop = (normalizedBox.top + deltaNormalizedY).coerceIn(0.0f, maxTop)
        val newRight = (newLeft + boxWidth).coerceIn(newLeft + 0.01f, 1.0f)
        val newBottom = (newTop + boxHeight).coerceIn(newTop + 0.01f, 1.0f)

        return RectF(newLeft, newTop, newRight, newBottom)
    }
}
