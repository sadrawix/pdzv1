package com.example

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.theme.InterFontFamily
import kotlin.math.roundToInt

// Stitch Free Annotation Page Palette
private val StitchOutlineVariant = Color(0xFFE8BCB6)
private val StitchOutline = Color(0xFF936E69)
private val StitchOnSurface = Color(0xFF2A1614)
private val StitchOnSurfaceVariant = Color(0xFF5E3F3A)
private val StitchPrimary = Color(0xFFBC0003)
private val StitchSecondary = Color(0xFFAF2F22)
private val StitchTertiary = Color(0xFF0058BC)
private val StitchInverseSurface = Color(0xFF412B28)
private val StitchSurfaceContainerLow = Color(0xFFFFF0EE)
private val StitchSurfaceDim = Color(0xFFF6D2CD)

// Stitch Premium OCR Page Palette (`code_00c91e97.html` & `screen_9005060e.png`)
private val OcrSelectionBlue = Color(0xFF2563EB)
private val OcrSecondaryDashedBlue = Color(0xFF60A5FA)
private val OcrFigureDarkBg = Color(0xFF0D131F)
private val OcrEllipseCyan = Color(0xFF06B6D4)
private val OcrEllipseRed = Color(0xFFEF4444)
private val OcrEllipseAmber = Color(0xFFFBBF24)
private val OcrCenterSky = Color(0xFF38BDF8)

/**
 * Interactive PDF Page & Freehand Annotation Canvas (`InteractivePdfCanvas.kt`):
 * - Captures touch drag gestures (`pointerInput`) and normalizes coordinates to `[0.0f, 1.0f]`.
 * - Draws smooth quadratic Bezier paths over the PDF page content.
 * - Applies selected `Colour`, `Fill Colour`, `Thickness` (`1..50 pt`), and `Opacity` (`0..100%`)
 *   in real time when the Drawing tool is active.
 * - Renders highlight strokes with `BlendMode.Multiply` when the Text Highlight (`T`) tool is active
 *   so highlights blend transparently over black PDF text.
 */
@Composable
fun InteractivePdfCanvas(
    pageBitmap: Bitmap?,
    strokes: List<DrawingStroke>,
    activeTool: FreeAnnotationTool,
    activeStrokeColor: Int,
    activeFillColor: Int?,
    activeThicknessPt: Float,
    activeOpacity: Float,
    activeHighlightColor: Int,
    activeHighlightOpacity: Float,
    activeHighlightThicknessPt: Float,
    onStrokeCommitted: (List<Offset>) -> Unit,
    modifier: Modifier = Modifier,
    pageAspectRatio: Float = 0.77f
) {
    var liveNormalizedPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val effectiveAspectRatio = remember(pageBitmap, pageAspectRatio) {
        if (pageBitmap != null && !pageBitmap.isRecycled && pageBitmap.height > 0) {
            (pageBitmap.width.toFloat() / pageBitmap.height.toFloat()).coerceIn(0.55f, 1.85f)
        } else {
            pageAspectRatio.coerceIn(0.55f, 1.85f)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(6.dp))
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
            .border(1.dp, StitchOutlineVariant, RoundedCornerShape(6.dp))
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .pointerInput(
                activeTool,
                activeStrokeColor,
                activeFillColor,
                activeThicknessPt,
                activeOpacity,
                activeHighlightColor,
                activeHighlightOpacity
            ) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                        val normPt = NormalizedCoordinateMapper.canvasToNormalized(
                            startOffset,
                            canvasSize
                        )
                        liveNormalizedPoints = listOf(normPt)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                        val normPt = NormalizedCoordinateMapper.canvasToNormalized(
                            change.position,
                            canvasSize
                        )
                        liveNormalizedPoints = liveNormalizedPoints + normPt
                    },
                    onDragEnd = {
                        if (liveNormalizedPoints.size >= 2) {
                            onStrokeCommitted(liveNormalizedPoints)
                        }
                        liveNormalizedPoints = emptyList()
                    },
                    onDragCancel = {
                        liveNormalizedPoints = emptyList()
                    }
                )
            }
            .testTag("interactive_pdf_canvas")
    ) {
        // 1. Underlying PDF Page Content (render actual PDF page bitmap at 100% opacity without overlapping another PDF)
        if (pageBitmap != null && !pageBitmap.isRecycled) {
            Image(
                bitmap = remember(pageBitmap) { pageBitmap.asImageBitmap() },
                contentDescription = "PDF Page",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(effectiveAspectRatio),
                contentScale = ContentScale.FillBounds
            )
        } else {
            StitchGeometricOrbitPageContent(
                pageBitmap = null,
                activeTool = activeTool,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 2. Smooth Quadratic Bezier Stroke & Multiply-Blend Highlight Overlay
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            val canvasSize = Size(size.width, size.height)

            for (stroke in strokes) {
                val pixelPoints = stroke.toCanvasPoints(canvasSize)
                drawQuadraticBezierStroke(
                    points = pixelPoints,
                    strokeColor = Color(stroke.strokeColor),
                    fillColor = stroke.fillColor?.let { Color(it) },
                    thicknessPt = stroke.thicknessPt,
                    opacity = stroke.opacity,
                    isHighlight = stroke.isHighlight
                )
            }

            if (liveNormalizedPoints.size >= 2) {
                val livePixels = liveNormalizedPoints.map { pt ->
                    Offset(pt.x * size.width, pt.y * size.height)
                }
                val isHighlight = activeTool == FreeAnnotationTool.TEXT_HIGHLIGHT
                drawQuadraticBezierStroke(
                    points = livePixels,
                    strokeColor = Color(
                        if (isHighlight) activeHighlightColor else activeStrokeColor
                    ),
                    fillColor = if (isHighlight) null else activeFillColor?.let { Color(it) },
                    thicknessPt = if (isHighlight) activeHighlightThicknessPt else activeThicknessPt,
                    opacity = if (isHighlight) activeHighlightOpacity else activeOpacity,
                    isHighlight = isHighlight
                )
            }
        }
    }
}

/**
 * Interactive PDF Canvas for **Premium OCR & Direct Text Edit Mode** (`code_00c91e97.html` & `screen_9005060e.png`):
 * - Draws dashed blue bounding boxes (`PathEffect.dashPathEffect`) around every detected [EditableTextBlock].
 * - When a dashed box is selected:
 *   - Displays the 4 corner squares (`10.dp` white with `#2563EB` border) and the floating top-left
 *     blue circular resize handle (`24.dp` `#2563EB` circle with white hand/resize icon).
 *   - Dragging the top-left blue resize handle resizes the normalized bounding box in real time.
 *   - Opens an inline editable text field (`BasicTextField`) right inside the bounding box.
 * - When a text block has `isModified = true`:
 *   - Draws a solid white background rect on the `Canvas` over the original scanned text area and
 *     renders the new text using the selected Font (`Carrois Gothic SC`, `Courier`, `Cutive Mono`,
 *     `Dancing Script`, `Droid Sans Mono`, `Helvetica`, `Roboto`), Size (`14..72`), Text Colour
 *     (`Palette` or `Custom` hex/RGB), Bold/Italic, Alignment, and Line Spacing (`1`, `1.15`, `1.5`, `2`).
 */
@Composable
fun PremiumOcrInteractiveCanvas(
    pageBitmap: Bitmap?,
    textBlocks: List<EditableTextBlock>,
    selectedTextBlockId: String?,
    isInlineEditingText: Boolean,
    strokes: List<DrawingStroke>,
    isDrawingOrHighlightMode: Boolean,
    isHighlightTool: Boolean,
    activeStrokeColor: Int,
    activeFillColor: Int?,
    activeThicknessPt: Float,
    activeOpacity: Float,
    activeHighlightColor: Int,
    activeHighlightOpacity: Float,
    activeHighlightThicknessPt: Float,
    onSelectTextBlock: (String) -> Unit,
    onUpdateSelectedBlockText: (String) -> Unit,
    onResizeSelectedBlockFromTopLeft: (Float, Float) -> Unit,
    onStrokeCommitted: (List<Offset>) -> Unit,
    modifier: Modifier = Modifier,
    onUpdateTextBlockText: ((String, String) -> Unit)? = null,
    onStartMoveTextBlock: (String) -> Unit = {},
    onMoveTextBlock: (String, Float, Float) -> Unit = { _, _, _ -> },
    onEndMoveTextBlock: (String) -> Unit = {},
    deletedRedactions: List<DeletedRedaction> = emptyList(),
    isToolbarVisible: Boolean = false,
    onLongPressTextBlock: (String) -> Unit = {},
    onDismissToolbar: () -> Unit = {},
    onCutBlock: () -> Unit = {},
    onCopyBlock: () -> Unit = {},
    onDeleteBlock: () -> Unit = {},
    onDuplicateBlock: () -> Unit = {},
    onEditBlock: () -> Unit = {},
    onEraserClearBlock: () -> Unit = {}
) {
    var liveNormalizedPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var activeDraggingBlockId by remember { mutableStateOf<String?>(null) }
    val hapticFeedback = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .pointerInput(isToolbarVisible, isInlineEditingText) {
                detectTapGestures(
                    onTap = {
                        onDismissToolbar()
                    }
                )
            }
            .then(
                if (isDrawingOrHighlightMode) {
                    Modifier.pointerInput(
                        isHighlightTool,
                        activeStrokeColor,
                        activeFillColor,
                        activeThicknessPt,
                        activeOpacity,
                        activeHighlightColor,
                        activeHighlightOpacity
                    ) {
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                onDismissToolbar()
                                val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                                val normPt = NormalizedCoordinateMapper.canvasToNormalized(
                                    startOffset,
                                    canvasSize
                                )
                                liveNormalizedPoints = listOf(normPt)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val canvasSize = Size(size.width.toFloat(), size.height.toFloat())
                                val normPt = NormalizedCoordinateMapper.canvasToNormalized(
                                    change.position,
                                    canvasSize
                                )
                                liveNormalizedPoints = liveNormalizedPoints + normPt
                            },
                            onDragEnd = {
                                if (liveNormalizedPoints.size >= 2) {
                                    onStrokeCommitted(liveNormalizedPoints)
                                }
                                liveNormalizedPoints = emptyList()
                            },
                            onDragCancel = {
                                liveNormalizedPoints = emptyList()
                            }
                        )
                    }
                } else {
                    // Canvas-level hit-test fallback for dragging any EditableTextBlock when touch
                    // starts inside its bounding box
                    Modifier.pointerInput(textBlocks, selectedTextBlockId) {
                        var hitBlockId: String? = null
                        detectDragGestures(
                            onDragStart = { startOffset ->
                                val cSize = Size(size.width.toFloat(), size.height.toFloat())
                                val hitBlock = textBlocks
                                    .sortedByDescending { it.id == selectedTextBlockId }
                                    .firstOrNull { block ->
                                        block.toCanvasRect(cSize).inflate(12f).contains(startOffset)
                                    }
                                hitBlockId = hitBlock?.id
                                if (hitBlock != null) {
                                    activeDraggingBlockId = hitBlock.id
                                    onStartMoveTextBlock(hitBlock.id)
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            },
                            onDrag = { change, dragAmount ->
                                val targetId = hitBlockId ?: return@detectDragGestures
                                change.consume()
                                val cSize = Size(size.width.toFloat(), size.height.toFloat())
                                val normDelta = CoordinateMapper.pixelDeltaToNormalizedDelta(
                                    deltaPx = dragAmount,
                                    canvasSize = cSize
                                )
                                onMoveTextBlock(targetId, normDelta.x, normDelta.y)
                            },
                            onDragEnd = {
                                val targetId = hitBlockId
                                hitBlockId = null
                                activeDraggingBlockId = null
                                if (targetId != null) {
                                    onEndMoveTextBlock(targetId)
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                            onDragCancel = {
                                val targetId = hitBlockId
                                hitBlockId = null
                                activeDraggingBlockId = null
                                if (targetId != null) {
                                    onEndMoveTextBlock(targetId)
                                }
                            }
                        )
                    }
                }
            )
            .testTag("premium_ocr_interactive_canvas")
    ) {
        val canvasWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val canvasHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val canvasSize = Size(canvasWidthPx, canvasHeightPx)
        val density = LocalDensity.current

        // 1. Underlying PDF Page Content (render actual PDF page bitmap at 100% opacity without overlapping another PDF)
        if (pageBitmap != null && !pageBitmap.isRecycled) {
            Image(
                bitmap = remember(pageBitmap, deletedRedactions) { pageBitmap.asImageBitmap() },
                contentDescription = "PDF Page",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        } else {
            StitchPremiumOcrDocumentBackground(
                pageBitmap = null,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Canvas layer for:
        //    - Solid white mask rect over any modified or actively inline-edited text block
        //    - Dashed blue bounding boxes (`PathEffect.dashPathEffect`) around every detected OCR text block
        //    - Freehand drawing & highlight strokes
        Canvas(
            modifier = Modifier.matchParentSize()
        ) {
            val currentCanvasSize = Size(size.width, size.height)

            // When pageBitmap is null (fallback vector preview), render deletedRedactions using
            // a crisp flat solid fill with zero feathering or edge expansion.
            if (pageBitmap == null || pageBitmap.isRecycled) {
                for (redaction in deletedRedactions) {
                    val redactedPx = CoordinateMapper.normalizedBoxToCanvasRect(
                        redaction.normalizedBox,
                        currentCanvasSize
                    )
                    drawRect(
                        color = Color(redaction.ambientBackgroundColor),
                        topLeft = Offset(redactedPx.left, redactedPx.top),
                        size = Size(redactedPx.width, redactedPx.height),
                        style = Fill
                    )
                }
            }

            // Render ONLY:
            // 1. Crisp flat background (only if not transparent, zero feathering)
            // 2. Pure text glyphs with clean anti-aliasing (rendered in isolated Compose layer below)
            // 3. Dashed blue wireframe stroke (no glow, no outer shadow)
            // 4. Solid corner handles
            for (block in textBlocks) {
                val rectPx: Rect = block.toCanvasRect(currentCanvasSize)
                val isSelected = block.id == selectedTextBlockId

                // 1. Crisp flat background (only if not transparent; if transparent, Color(0x00000000) — no drawing call at all)
                if (
                    !block.isTransparentBackground &&
                    !block.isTransparent &&
                    block.backgroundColor != android.graphics.Color.TRANSPARENT
                ) {
                    drawRect(
                        color = Color(block.backgroundColor),
                        topLeft = Offset(rectPx.left, rectPx.top),
                        size = Size(rectPx.width, rectPx.height),
                        style = Fill
                    )
                }

                // 3. Dashed blue wireframe stroke (no glow, no outer shadow)
                val dashEffect = if (isSelected) {
                    PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f)
                } else {
                    PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()), 0f)
                }

                drawRect(
                    color = if (isSelected) OcrSelectionBlue else OcrSecondaryDashedBlue,
                    topLeft = Offset(rectPx.left, rectPx.top),
                    size = Size(rectPx.width, rectPx.height),
                    style = Stroke(
                        width = if (isSelected) 2.dp.toPx() else 1.2.dp.toPx(),
                        pathEffect = dashEffect
                    )
                )

                // 4. Solid corner handles on the selected dashed blue bounding box
                if (isSelected) {
                    val handleSizePx = 10.dp.toPx()
                    val halfHandle = handleSizePx / 2f
                    val corners = listOf(
                        Offset(rectPx.left, rectPx.top),
                        Offset(rectPx.right, rectPx.top),
                        Offset(rectPx.left, rectPx.bottom),
                        Offset(rectPx.right, rectPx.bottom)
                    )
                    corners.forEach { corner ->
                        drawRect(
                            color = Color.White,
                            topLeft = Offset(corner.x - halfHandle, corner.y - halfHandle),
                            size = Size(handleSizePx, handleSizePx),
                            style = Fill
                        )
                        drawRect(
                            color = OcrSelectionBlue,
                            topLeft = Offset(corner.x - halfHandle, corner.y - halfHandle),
                            size = Size(handleSizePx, handleSizePx),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                }
            }

            // Draw committed freehand & highlight strokes
            for (stroke in strokes) {
                val pixelPoints = stroke.toCanvasPoints(currentCanvasSize)
                drawQuadraticBezierStroke(
                    points = pixelPoints,
                    strokeColor = Color(stroke.strokeColor),
                    fillColor = stroke.fillColor?.let { Color(it) },
                    thicknessPt = stroke.thicknessPt,
                    opacity = stroke.opacity,
                    isHighlight = stroke.isHighlight
                )
            }

            // Draw live drag stroke if user is drawing/highlighting
            if (liveNormalizedPoints.size >= 2) {
                val livePixels = liveNormalizedPoints.map { pt ->
                    Offset(pt.x * size.width, pt.y * size.height)
                }
                drawQuadraticBezierStroke(
                    points = livePixels,
                    strokeColor = Color(
                        if (isHighlightTool) activeHighlightColor else activeStrokeColor
                    ),
                    fillColor = if (isHighlightTool) null else activeFillColor?.let { Color(it) },
                    thicknessPt = if (isHighlightTool) activeHighlightThicknessPt else activeThicknessPt,
                    opacity = if (isHighlightTool) activeHighlightOpacity else activeOpacity,
                    isHighlight = isHighlightTool
                )
            }
        }

        // 3. Interactive OCR Text Block Layer (Tap to edit, Long-press for ContextualToolbar,
        //    Drag to Move/Reposition anywhere on the page, Top-Left Handle to Resize)
        textBlocks.forEach { block ->
            key(block.id) {
                val rectPx = block.toCanvasRect(canvasSize)
                val isSelected = block.id == selectedTextBlockId
                val isDraggingThisBlock = block.id == activeDraggingBlockId
                val widthDp = with(density) { rectPx.width.toDp() }
                val heightDp = with(density) { rectPx.height.toDp() }

                val composeFontFamily = remember(block.fontName) {
                    EditableTextBlock.resolveComposeFontFamily(block.fontName)
                }
                val textAlign = when (block.alignment) {
                    Paint.Align.LEFT -> TextAlign.Start
                    Paint.Align.CENTER -> TextAlign.Center
                    Paint.Align.RIGHT -> TextAlign.End
                }

                val boxAlignment = when (block.alignment) {
                    Paint.Align.LEFT -> Alignment.CenterStart
                    Paint.Align.CENTER -> Alignment.Center
                    Paint.Align.RIGHT -> Alignment.CenterEnd
                }
                val focusRequester = remember(block.id) { FocusRequester() }

                LaunchedEffect(isSelected, isInlineEditingText) {
                    if (isSelected && isInlineEditingText) {
                        runCatching { focusRequester.requestFocus() }
                    }
                }

                val blockZIndex = when {
                    isDraggingThisBlock -> 20f
                    isSelected -> 10f
                    else -> 1f
                }

                Box(
                    modifier = Modifier
                        .zIndex(blockZIndex)
                        .offset {
                            IntOffset(
                                x = rectPx.left.roundToInt(),
                                y = rectPx.top.roundToInt()
                            )
                        }
                        .size(
                            width = widthDp.coerceAtLeast(36.dp),
                            height = heightDp.coerceAtLeast(20.dp)
                        )
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                            renderEffect = null
                            shadowElevation = 0f
                        }
                        .background(Color(0x00000000))
                        .then(
                            if (!(isSelected && isInlineEditingText)) {
                                Modifier.pointerInput(block.id) {
                                    detectTapGestures(
                                        onTap = {
                                            onSelectTextBlock(block.id)
                                        },
                                        onLongPress = {
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onLongPressTextBlock(block.id)
                                        }
                                    )
                                }
                            } else {
                                Modifier
                            }
                        )
                        .pointerInput(block.id, canvasWidthPx, canvasHeightPx) {
                            detectDragGestures(
                                onDragStart = {
                                    activeDraggingBlockId = block.id
                                    onStartMoveTextBlock(block.id)
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val normDelta = CoordinateMapper.pixelDeltaToNormalizedDelta(
                                        deltaPx = dragAmount,
                                        canvasSize = Size(canvasWidthPx, canvasHeightPx)
                                    )
                                    onMoveTextBlock(block.id, normDelta.x, normDelta.y)
                                },
                                onDragEnd = {
                                    activeDraggingBlockId = null
                                    onEndMoveTextBlock(block.id)
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragCancel = {
                                    activeDraggingBlockId = null
                                    onEndMoveTextBlock(block.id)
                                }
                            )
                        }
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                        .testTag("ocr_text_block_${block.id}"),
                    contentAlignment = boxAlignment
                ) {
                    if (isSelected && isInlineEditingText) {
                        BasicTextField(
                            value = block.currentText,
                            onValueChange = { updatedText ->
                                if (onUpdateTextBlockText != null) {
                                    onUpdateTextBlockText(block.id, updatedText)
                                } else {
                                    onUpdateSelectedBlockText(updatedText)
                                }
                            },
                            textStyle = TextStyle(
                                fontFamily = composeFontFamily,
                                fontSize = block.fontSizeSp.sp,
                                fontWeight = if (block.isBold) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (block.isItalic) FontStyle.Italic else FontStyle.Normal,
                                color = Color(block.textColor),
                                textAlign = textAlign,
                                lineHeight = (block.fontSizeSp * block.lineSpacing).sp
                            ),
                            cursorBrush = SolidColor(OcrSelectionBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                                .testTag("ocr_inline_text_field")
                        )
                    } else if (!block.isOriginalBakedIn || block.isModified || pageBitmap == null || pageBitmap.isRecycled) {
                        Text(
                            text = block.currentText,
                            fontFamily = composeFontFamily,
                            fontSize = block.fontSizeSp.sp,
                            fontWeight = if (block.isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (block.isItalic) FontStyle.Italic else FontStyle.Normal,
                            color = Color(block.textColor),
                            textAlign = textAlign,
                            lineHeight = (block.fontSizeSp * block.lineSpacing).sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Floating Top-Left Blue Circular Resize Slider Handle (`w-6 h-6 bg-blue-600 rounded-full`)
                // shown on the currently selected dashed blue bounding box (also supports long-press to open toolbar)
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .zIndex(25f)
                            .offset {
                                IntOffset(
                                    x = (rectPx.left - 12.dp.toPx()).roundToInt(),
                                    y = (rectPx.top - 12.dp.toPx()).roundToInt()
                                )
                            }
                            .size(26.dp)
                            .shadow(elevation = 4.dp, shape = CircleShape)
                            .clip(CircleShape)
                            .background(OcrSelectionBlue)
                            .border(1.5.dp, Color.White, CircleShape)
                            .pointerInput(block.id) {
                                detectTapGestures(
                                    onLongPress = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onLongPressTextBlock(block.id)
                                    }
                                )
                            }
                            .pointerInput(block.id, canvasWidthPx, canvasHeightPx) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val dNormX = dragAmount.x / canvasWidthPx
                                    val dNormY = dragAmount.y / canvasHeightPx
                                    onResizeSelectedBlockFromTopLeft(dNormX, dNormY)
                                }
                            }
                            .testTag("ocr_top_left_resize_handle"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Exact white pointer-hand / resize vector icon from Stitch `code_00c91e97.html`
                        Canvas(modifier = Modifier.size(14.dp)) {
                            val w = size.width
                            val h = size.height
                            val path = Path().apply {
                                moveTo(w * 0.36f, h * 0.45f)
                                lineTo(w * 0.36f, h * 0.20f)
                                cubicTo(w * 0.36f, h * 0.12f, w * 0.48f, h * 0.12f, w * 0.48f, h * 0.20f)
                                lineTo(w * 0.48f, h * 0.42f)
                                lineTo(w * 0.72f, h * 0.46f)
                                cubicTo(w * 0.82f, h * 0.48f, w * 0.85f, h * 0.58f, w * 0.82f, h * 0.68f)
                                lineTo(w * 0.76f, h * 0.84f)
                                cubicTo(w * 0.72f, h * 0.90f, w * 0.65f, h * 0.92f, w * 0.55f, h * 0.92f)
                                lineTo(w * 0.38f, h * 0.92f)
                                cubicTo(w * 0.26f, h * 0.92f, w * 0.18f, h * 0.82f, w * 0.18f, h * 0.70f)
                                lineTo(w * 0.18f, h * 0.52f)
                                cubicTo(w * 0.18f, h * 0.46f, w * 0.28f, h * 0.45f, w * 0.36f, h * 0.45f)
                                close()
                            }
                            drawPath(
                                path = path,
                                color = Color.White,
                                style = Stroke(
                                    width = 1.4.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }
                }
            }
        }

        // 4. Floating Long-Press Contextual Toolbar Pill positioned dynamically above (or below)
        //    the selected text block's bounding box coordinates on the screen and following it in real time as it moves
        val activeSelectedBlock = textBlocks.firstOrNull { it.id == selectedTextBlockId }
        if (isToolbarVisible && activeSelectedBlock != null) {
            val selectedRectPx = activeSelectedBlock.toCanvasRect(canvasSize)
            val toolbarWidthPx = with(density) { 276.dp.toPx() }
            val toolbarHeightPx = with(density) { 48.dp.toPx() }
            val verticalGapPx = with(density) { 10.dp.toPx() }
            val horizontalMarginPx = with(density) { 8.dp.toPx() }

            // Center the floating pill horizontally over the bounding box and clamp within canvas bounds
            val desiredLeftPx = selectedRectPx.center.x - (toolbarWidthPx / 2f)
            val maxLeftPx = (canvasWidthPx - toolbarWidthPx - horizontalMarginPx).coerceAtLeast(horizontalMarginPx)
            val clampedLeftPx = desiredLeftPx.coerceIn(horizontalMarginPx, maxLeftPx)

            // Position just above the bounding box, or below if near the top edge of the page
            val aboveTopPx = selectedRectPx.top - toolbarHeightPx - verticalGapPx
            val belowTopPx = selectedRectPx.bottom + verticalGapPx
            val resolvedTopPx = if (aboveTopPx >= horizontalMarginPx) {
                aboveTopPx
            } else {
                belowTopPx.coerceAtMost((canvasHeightPx - toolbarHeightPx - horizontalMarginPx).coerceAtLeast(horizontalMarginPx))
            }

            Box(
                modifier = Modifier
                    .zIndex(30f)
                    .offset {
                        IntOffset(
                            x = clampedLeftPx.roundToInt(),
                            y = resolvedTopPx.roundToInt()
                        )
                    }
            ) {
                ContextualToolbar(
                    onCutClick = onCutBlock,
                    onCopyClick = onCopyBlock,
                    onDeleteClick = onDeleteBlock,
                    onDuplicateClick = onDuplicateBlock,
                    onEditClick = onEditBlock,
                    onEraserClearClick = onEraserClearBlock
                )
            }
        }
    }
}

/**
 * Renders the static non-editable portions of the Stitch Premium OCR Document page (`code_00c91e97.html`):
 * - Top header: `"ANALYZING FINANCIAL MARKETS USING FRACTAL GEOMETRY"`
 * - Center dark `#0D131F` rounded box (`144.dp x 112.dp`) with 4 rotated concentric colored ellipses
 *   (`#2563EB` dashed, `#06B6D4`, `#EF4444`, `#FBBF24`), `#38BDF8` center circle, and `"Fig. 1.0"`
 * - Bottom highlighted paragraph: `"The Golden Ratio (1.618) recurs consistently..."`
 */
@Composable
private fun StitchPremiumOcrDocumentBackground(
    pageBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        val totalHeight = maxHeight

        if (pageBitmap != null && !pageBitmap.isRecycled) {
            Image(
                bitmap = remember(pageBitmap) { pageBitmap.asImageBitmap() },
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.03f },
                contentScale = ContentScale.Crop
            )
        }

        // 1. Top Document Sub-Header (`text-[10px] tracking-widest uppercase text-slate-500 font-semibold`)
        Text(
            text = "ANALYZING FINANCIAL MARKETS USING FRACTAL GEOMETRY",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 9.5.sp,
            letterSpacing = 0.9.sp,
            color = Color(0xFF64748B),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        )

        // 2. Center Dark `#0D131F` Concentric Ellipses Illustration (`w-36 h-28 bg-[#0D131F] rounded-xl`)
        // Positioned between the top primary OCR box (0.085..0.305) and bottom Soul Esprit box (0.635..0.775)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = totalHeight * 0.335f)
                .width(148.dp)
                .height(110.dp)
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(OcrFigureDarkBg),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f

                rotate(degrees = -15f, pivot = Offset(cx, cy)) {
                    // Outer dashed blue ellipse (`rx=55, ry=32, stroke=#2563EB, stroke-dasharray=3 2`)
                    drawOval(
                        color = OcrSelectionBlue,
                        topLeft = Offset(cx - size.width * 0.39f, cy - size.height * 0.30f),
                        size = Size(size.width * 0.78f, size.height * 0.60f),
                        style = Stroke(
                            width = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(3.dp.toPx(), 2.dp.toPx())
                            )
                        )
                    )

                    // Second cyan ellipse (`rx=42, ry=24, stroke=#06B6D4`)
                    drawOval(
                        color = OcrEllipseCyan,
                        topLeft = Offset(cx - size.width * 0.30f, cy - size.height * 0.22f),
                        size = Size(size.width * 0.60f, size.height * 0.44f),
                        style = Stroke(width = 1.3.dp.toPx())
                    )

                    // Third red ellipse (`rx=30, ry=16, stroke=#EF4444`)
                    drawOval(
                        color = OcrEllipseRed,
                        topLeft = Offset(cx - size.width * 0.21f, cy - size.height * 0.15f),
                        size = Size(size.width * 0.42f, size.height * 0.30f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Fourth amber ellipse (`rx=18, ry=9, stroke=#FBBF24`)
                    drawOval(
                        color = OcrEllipseAmber,
                        topLeft = Offset(cx - size.width * 0.13f, cy - size.height * 0.085f),
                        size = Size(size.width * 0.26f, size.height * 0.17f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Center sky-blue core (`r=4, fill=#38BDF8`)
                    drawCircle(
                        color = OcrCenterSky,
                        radius = 4.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }

            Text(
                text = "Fig. 1.0",
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = Color(0xFF94A3B8),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 8.dp, bottom = 5.dp)
            )
        }

        // 3. Bottom Highlighted Text Snippet (`bg-yellow-300/60 px-1 rounded text-slate-900 font-medium`)
        val goldenRatioText = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    background = Color(0x99FDE047),
                    color = Color(0xFF0F172A),
                    fontWeight = FontWeight.Medium
                )
            ) {
                append("The Golden Ratio (1.618)")
            }
            withStyle(
                SpanStyle(
                    color = Color(0xFF475569)
                )
            ) {
                append(" recurs consistently in financial price retracements and time-cycle extensions across multi-timeframe charts.")
            }
        }

        Text(
            text = goldenRatioText,
            fontFamily = InterFontFamily,
            fontSize = 10.5.sp,
            lineHeight = 14.5.sp,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/**
 * Builds a smooth quadratic Bezier [Path] from a list of pixel [Offset] points and renders
 * optional fill + stroke (using [BlendMode.Multiply] when [isHighlight] is true).
 */
private fun DrawScope.drawQuadraticBezierStroke(
    points: List<Offset>,
    strokeColor: Color,
    fillColor: Color?,
    thicknessPt: Float,
    opacity: Float,
    isHighlight: Boolean
) {
    if (points.isEmpty()) return

    val strokeWidthPx = (thicknessPt * density).coerceAtLeast(2f)
    val clampedAlpha = opacity.coerceIn(0f, 1f)
    val blendMode = if (isHighlight) BlendMode.Multiply else BlendMode.SrcOver

    if (points.size == 1) {
        drawCircle(
            color = strokeColor.copy(alpha = clampedAlpha),
            radius = strokeWidthPx / 2f,
            center = points.first(),
            blendMode = blendMode
        )
        return
    }

    val bezierPath = buildSmoothQuadraticBezierPath(points)

    if (!isHighlight && fillColor != null) {
        val closedFillPath = Path().apply {
            addPath(bezierPath)
            close()
        }
        drawPath(
            path = closedFillPath,
            color = fillColor.copy(alpha = (clampedAlpha * 0.45f).coerceIn(0.08f, 0.85f)),
            style = Fill
        )
    }

    drawPath(
        path = bezierPath,
        color = strokeColor.copy(alpha = clampedAlpha),
        style = Stroke(
            width = strokeWidthPx,
            cap = if (isHighlight) StrokeCap.Square else StrokeCap.Round,
            join = StrokeJoin.Round
        ),
        blendMode = blendMode
    )
}

/**
 * Constructs a smooth quadratic Bezier curve through [points] using midpoint control knots.
 */
fun buildSmoothQuadraticBezierPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path

    path.moveTo(points.first().x, points.first().y)
    if (points.size == 2) {
        path.lineTo(points[1].x, points[1].y)
        return path
    }

    for (i in 1 until points.size - 1) {
        val current = points[i]
        val next = points[i + 1]
        val midX = (current.x + next.x) / 2f
        val midY = (current.y + next.y) / 2f
        path.quadraticTo(current.x, current.y, midX, midY)
    }

    val secondLast = points[points.size - 2]
    val last = points.last()
    path.quadraticTo(secondLast.x, secondLast.y, last.x, last.y)
    return path
}

/**
 * Renders the exact Stitch Free Plan Editor page layout (`code_95ca715a.html` & `screen_ec775298.png`).
 */
@Composable
private fun StitchGeometricOrbitPageContent(
    pageBitmap: Bitmap?,
    activeTool: FreeAnnotationTool,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.White)
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        if (pageBitmap != null && !pageBitmap.isRecycled) {
            Image(
                bitmap = remember(pageBitmap) { pageBitmap.asImageBitmap() },
                contentDescription = null,
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = 0.04f },
                contentScale = ContentScale.Crop
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "FRACTAL TRADING",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                letterSpacing = 1.1.sp,
                color = StitchOnSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Analyzing Financial Markets using Fractal Geometry and the Golden Ratio",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 10.5.sp,
                lineHeight = 14.sp,
                color = StitchOnSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(1.5.dp)
                    .background(StitchPrimary)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Soul Esprit – Servant of the Lord Jesus Christ",
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                fontSize = 9.5.sp,
                color = StitchOutline,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(StitchSurfaceContainerLow.copy(alpha = 0.45f))
                    .border(
                        width = 1.dp,
                        color = StitchOutlineVariant.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(top = 12.dp, bottom = 6.dp, start = 10.dp, end = 10.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .width(190.dp)
                            .height(132.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cx = size.width / 2f
                            val cy = size.height / 2f

                            rotate(degrees = 12f, pivot = Offset(cx, cy)) {
                                drawOval(
                                    color = StitchOutlineVariant.copy(alpha = 0.85f),
                                    topLeft = Offset(size.width * 0.06f, size.height * 0.08f),
                                    size = Size(size.width * 0.88f, size.height * 0.84f),
                                    style = Stroke(width = 1.2.dp.toPx())
                                )
                            }

                            rotate(degrees = -6f, pivot = Offset(cx, cy)) {
                                drawOval(
                                    color = StitchOutline,
                                    topLeft = Offset(size.width * 0.16f, size.height * 0.16f),
                                    size = Size(size.width * 0.68f, size.height * 0.68f),
                                    style = Stroke(
                                        width = 1.2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(3.dp.toPx(), 3.dp.toPx())
                                        )
                                    )
                                )
                            }

                            drawCircle(
                                color = StitchInverseSurface,
                                radius = 20.dp.toPx(),
                                center = Offset(cx, cy)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 6.dp.toPx(),
                                center = Offset(cx, cy)
                            )

                            drawCircle(
                                color = StitchSecondary,
                                radius = 8.dp.toPx(),
                                center = Offset(size.width * 0.77f, size.height * 0.24f)
                            )

                            drawCircle(
                                color = StitchTertiary,
                                radius = 5.dp.toPx(),
                                center = Offset(size.width * 0.26f, size.height * 0.73f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Fig. 1.0 Geometric Orbit Dynamic",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = StitchOutline
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                    ) {
                        drawLine(
                            color = StitchPrimary.copy(alpha = 0.5f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "[α = 1.618]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 7.5.sp,
                            color = StitchPrimary
                        )
                        Text(
                            text = if (activeTool == FreeAnnotationTool.TEXT_HIGHLIGHT) {
                                "Active Layer: Text Highlight"
                            } else {
                                "Active Layer: Pen Stroke"
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 7.5.sp,
                            color = StitchPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(StitchSurfaceDim.copy(alpha = 0.65f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(StitchSurfaceDim.copy(alpha = 0.65f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.80f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(StitchSurfaceDim.copy(alpha = 0.65f))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
