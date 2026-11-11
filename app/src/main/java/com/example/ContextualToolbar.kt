package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private val ToolbarSurfaceWhite = Color(0xFFFFFFFF)
private val ToolbarBorderLight = Color(0xFFE2E8F0)
private val ToolbarDividerLight = Color(0xFFF1F5F9)
private val ToolbarIconDark = Color(0xFF1E293B)
private val ToolbarDeleteRed = Color(0xFFE11D48)

/**
 * Floating Contextual Toolbar (`ContextualToolbar.kt`) displayed when a user long-presses
 * any [EditableTextBlock] on the PDF Editor canvas.
 *
 * Displays 6 action icons from left to right inside a rounded white pill container with
 * subtle elevation shadow:
 * 1. **Cut** (Scissors icon)
 * 2. **Copy** (Double overlapping paper icon)
 * 3. **Delete** (Trash bin icon in red `#E11D48`)
 * 4. **Duplicate / Add** (Stacked squares with plus `+` icon)
 * 5. **Edit** (Pencil / Pen icon)
 * 6. **Eraser / Clear** (Angled eraser with dotted line icon)
 */
@Composable
fun ContextualToolbar(
    visible: Boolean = true,
    onCutClick: () -> Unit,
    onCopyClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDuplicateClick: () -> Unit,
    onEditClick: () -> Unit,
    onEraserClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f),
        modifier = modifier
    ) {
        Surface(
            color = ToolbarSurfaceWhite,
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 10.dp,
            tonalElevation = 2.dp,
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = ToolbarBorderLight,
                    shape = RoundedCornerShape(14.dp)
                )
                .testTag("contextual_text_block_toolbar")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // 1. Cut (Scissors icon)
                ContextualToolbarIconButton(
                    contentDescription = "Cut Text Block",
                    testTag = "contextual_toolbar_cut",
                    onClick = onCutClick
                ) {
                    ScissorsCutIcon(tint = ToolbarIconDark)
                }

                ContextualToolbarDivider()

                // 2. Copy (Double paper icon)
                ContextualToolbarIconButton(
                    contentDescription = "Copy Text Block",
                    testTag = "contextual_toolbar_copy",
                    onClick = onCopyClick
                ) {
                    DoublePaperCopyIcon(tint = ToolbarIconDark)
                }

                ContextualToolbarDivider()

                // 3. Delete (Trash bin icon in red)
                ContextualToolbarIconButton(
                    contentDescription = "Delete Text Block",
                    testTag = "contextual_toolbar_delete",
                    onClick = onDeleteClick
                ) {
                    RedTrashBinDeleteIcon(tint = ToolbarDeleteRed)
                }

                ContextualToolbarDivider()

                // 4. Duplicate / Add (Stacked squares with plus icon)
                ContextualToolbarIconButton(
                    contentDescription = "Duplicate Text Block",
                    testTag = "contextual_toolbar_duplicate",
                    onClick = onDuplicateClick
                ) {
                    StackedSquaresPlusDuplicateIcon(tint = ToolbarIconDark)
                }

                ContextualToolbarDivider()

                // 5. Edit (Pencil / Pen icon)
                ContextualToolbarIconButton(
                    contentDescription = "Edit Text Block",
                    testTag = "contextual_toolbar_edit",
                    onClick = onEditClick
                ) {
                    PencilEditIcon(tint = ToolbarIconDark)
                }

                ContextualToolbarDivider()

                // 6. Eraser / Clear (Eraser with dotted line icon)
                ContextualToolbarIconButton(
                    contentDescription = "Clear or Reset Text Block",
                    testTag = "contextual_toolbar_eraser",
                    onClick = onEraserClearClick
                ) {
                    EraserDottedLineIcon(tint = ToolbarIconDark)
                }
            }
        }
    }
}

@Composable
private fun ContextualToolbarIconButton(
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
    iconContent: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        iconContent()
    }
}

@Composable
private fun ContextualToolbarDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(22.dp)
            .background(ToolbarDividerLight)
    )
}

/**
 * 1. Cut Icon — Scissors with two finger rings and crossing blades.
 */
@Composable
private fun ScissorsCutIcon(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val strokePx = 1.7.dp.toPx()

        // Top-left ring
        drawCircle(
            color = tint,
            radius = w * 0.14f,
            center = Offset(w * 0.26f, h * 0.28f),
            style = Stroke(width = strokePx)
        )
        // Bottom-left ring
        drawCircle(
            color = tint,
            radius = w * 0.14f,
            center = Offset(w * 0.26f, h * 0.72f),
            style = Stroke(width = strokePx)
        )
        // Blade from top-left ring to bottom-right tip
        drawLine(
            color = tint,
            start = Offset(w * 0.37f, h * 0.35f),
            end = Offset(w * 0.84f, h * 0.78f),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
        // Blade from bottom-left ring to top-right tip
        drawLine(
            color = tint,
            start = Offset(w * 0.37f, h * 0.65f),
            end = Offset(w * 0.84f, h * 0.22f),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
    }
}

/**
 * 2. Copy Icon — Double overlapping paper sheets.
 */
@Composable
private fun DoublePaperCopyIcon(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val strokePx = 1.7.dp.toPx()

        // Back sheet (top-left L-shape border)
        val backPath = Path().apply {
            moveTo(w * 0.64f, h * 0.24f)
            lineTo(w * 0.64f, h * 0.16f)
            lineTo(w * 0.20f, h * 0.16f)
            lineTo(w * 0.20f, h * 0.68f)
            lineTo(w * 0.30f, h * 0.68f)
        }
        drawPath(
            path = backPath,
            color = tint,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Front sheet (rounded rect at bottom-right)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.34f, h * 0.28f),
            size = Size(w * 0.48f, h * 0.56f),
            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
            style = Stroke(width = strokePx)
        )
    }
}

/**
 * 3. Delete Icon — Trash bin in red (`#E11D48`) with lid, handle, and vertical ribs.
 */
@Composable
private fun RedTrashBinDeleteIcon(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val strokePx = 1.7.dp.toPx()

        // Top lid bar
        drawLine(
            color = tint,
            start = Offset(w * 0.18f, h * 0.26f),
            end = Offset(w * 0.82f, h * 0.26f),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
        // Top handle arch
        val handlePath = Path().apply {
            moveTo(w * 0.38f, h * 0.26f)
            lineTo(w * 0.38f, h * 0.16f)
            lineTo(w * 0.62f, h * 0.16f)
            lineTo(w * 0.62f, h * 0.26f)
        }
        drawPath(
            path = handlePath,
            color = tint,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Bin body
        val bodyPath = Path().apply {
            moveTo(w * 0.26f, h * 0.28f)
            lineTo(w * 0.30f, h * 0.84f)
            lineTo(w * 0.70f, h * 0.84f)
            lineTo(w * 0.74f, h * 0.28f)
        }
        drawPath(
            path = bodyPath,
            color = tint,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Two inner vertical lines
        drawLine(
            color = tint,
            start = Offset(w * 0.43f, h * 0.42f),
            end = Offset(w * 0.43f, h * 0.70f),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.57f, h * 0.42f),
            end = Offset(w * 0.57f, h * 0.70f),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
    }
}

/**
 * 4. Duplicate / Add Icon — Stacked squares with a `+` plus symbol inside the front square.
 */
@Composable
private fun StackedSquaresPlusDuplicateIcon(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val strokePx = 1.7.dp.toPx()

        // Back square (top-left L-border)
        val backPath = Path().apply {
            moveTo(w * 0.66f, h * 0.24f)
            lineTo(w * 0.66f, h * 0.16f)
            lineTo(w * 0.16f, h * 0.16f)
            lineTo(w * 0.16f, h * 0.66f)
            lineTo(w * 0.26f, h * 0.66f)
        }
        drawPath(
            path = backPath,
            color = tint,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Front square at bottom-right
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.30f, h * 0.30f),
            size = Size(w * 0.54f, h * 0.54f),
            cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
            style = Stroke(width = strokePx)
        )

        // Plus '+' inside front square
        val cx = w * 0.57f
        val cy = h * 0.57f
        val arm = w * 0.13f
        drawLine(
            color = tint,
            start = Offset(cx - arm, cy),
            end = Offset(cx + arm, cy),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(cx, cy - arm),
            end = Offset(cx, cy + arm),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
    }
}

/**
 * 5. Edit Icon — Pencil / Pen icon with writing baseline.
 */
@Composable
private fun PencilEditIcon(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val strokePx = 1.7.dp.toPx()

        val pencilPath = Path().apply {
            moveTo(w * 0.68f, h * 0.18f)
            lineTo(w * 0.82f, h * 0.32f)
            lineTo(w * 0.36f, h * 0.78f)
            lineTo(w * 0.18f, h * 0.82f)
            lineTo(w * 0.22f, h * 0.64f)
            close()
        }
        drawPath(
            path = pencilPath,
            color = tint,
            style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Collar line near top-right of pencil
        drawLine(
            color = tint,
            start = Offset(w * 0.58f, h * 0.28f),
            end = Offset(w * 0.72f, h * 0.42f),
            strokeWidth = strokePx,
            cap = StrokeCap.Round
        )
    }
}

/**
 * 6. Eraser / Clear Icon — Angled two-part block eraser with a dotted line underneath.
 */
@Composable
private fun EraserDottedLineIcon(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        val strokePx = 1.7.dp.toPx()

        // Angled eraser body
        rotate(degrees = -35f, pivot = Offset(w * 0.46f, h * 0.46f)) {
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.22f, h * 0.30f),
                size = Size(w * 0.48f, h * 0.30f),
                cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx()),
                style = Stroke(width = strokePx)
            )
            // Band dividing the eraser
            drawLine(
                color = tint,
                start = Offset(w * 0.42f, h * 0.30f),
                end = Offset(w * 0.42f, h * 0.60f),
                strokeWidth = strokePx
            )
        }

        // Dotted baseline at bottom-right
        drawLine(
            color = tint,
            start = Offset(w * 0.38f, h * 0.82f),
            end = Offset(w * 0.86f, h * 0.82f),
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(2.2.dp.toPx(), 2.6.dp.toPx()),
                0f
            )
        )
    }
}
