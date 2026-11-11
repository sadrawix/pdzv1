package com.example

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.InterFontFamily
import kotlin.math.roundToInt

// Exact Stitch Free Plan Annotation Screen Colors (`screen_ec775298.png` & `code_95ca715a.html`)
private val StitchActiveToolGreen = Color(0xFF2E9B68)
private val StitchInactiveToolPeach = Color(0xFFFFDAD5)
private val StitchOnSurface = Color(0xFF2A1614)
private val StitchOnSurfaceVariant = Color(0xFF5E3F3A)
private val StitchOutline = Color(0xFF936E69)
private val StitchOutlineVariant = Color(0xFFE8BCB6)
private val StitchWorkspaceBg = Color(0xFFF1F3F5)
private val StitchPurpleSheetHeader = Color(0xFF3B00B3)
private val StitchSliderThumbRed = Color(0xFFE71610)
private val StitchSliderTrackPeach = Color(0xFFFFDAD5)
private val StitchRowDivider = Color(0xFFE2E8F0)

/**
 * Stitch AI Free Plan Annotation Screen (`FreeAnnotationEditor.kt`):
 * - Top toolbar with `'T'` Text Highlight icon, green `#2E9B68` active Drawing fountain-pen icon,
 *   active color indicator circle, Undo/Redo arrows, clear `'X'` button, and `:::` handle.
 * - Middle scrollable PDF page preview area containing [InteractivePdfCanvas].
 * - Purple `#3B00B3` BottomSheets for `"Drawing"` (live sine-wave preview curve, `Colour`,
 *   `Fill Colour` with checkerboard pattern, `Thickness` 1..50 pt slider, `Opacity` 0..100% slider)
 *   and `"Text Highlight"` (5x2 color grid + `Opacity` slider).
 */
@Composable
fun FreeAnnotationEditor(
    viewModel: PdfEditorViewModel,
    onNavigateBack: () -> Unit
) {
    val pageBitmap by viewModel.pageBitmap.collectAsStateWithLifecycle()
    val pageAspectRatio by viewModel.pageAspectRatio.collectAsStateWithLifecycle()
    val strokes by viewModel.strokes.collectAsStateWithLifecycle()
    val activeTool by viewModel.activeTool.collectAsStateWithLifecycle()
    val isBottomSheetExpanded by viewModel.isBottomSheetExpanded.collectAsStateWithLifecycle()
    val activeColorPicker by viewModel.activeColorPicker.collectAsStateWithLifecycle()

    BackHandler {
        if (strokes.isNotEmpty()) {
            viewModel.saveAnnotationsToPdf(onSaved = onNavigateBack)
        } else {
            onNavigateBack()
        }
    }

    val drawingStrokeColor by viewModel.drawingStrokeColor.collectAsStateWithLifecycle()
    val drawingFillColor by viewModel.drawingFillColor.collectAsStateWithLifecycle()
    val drawingThicknessPt by viewModel.drawingThicknessPt.collectAsStateWithLifecycle()
    val drawingOpacity by viewModel.drawingOpacity.collectAsStateWithLifecycle()

    val highlightColor by viewModel.highlightColor.collectAsStateWithLifecycle()
    val highlightOpacity by viewModel.highlightOpacity.collectAsStateWithLifecycle()
    val highlightThicknessPt by viewModel.highlightThicknessPt.collectAsStateWithLifecycle()

    val undoStack by viewModel.undoStack.collectAsStateWithLifecycle()
    val redoStack by viewModel.redoStack.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val textColorTab by viewModel.textColorTab.collectAsStateWithLifecycle()
    val customColorInputMode by viewModel.customColorInputMode.collectAsStateWithLifecycle()
    val recentlyUsedTextColors by viewModel.recentlyUsedTextColors.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    val topIndicatorColor = if (activeTool == FreeAnnotationTool.TEXT_HIGHLIGHT) {
        highlightColor
    } else {
        drawingStrokeColor
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("free_annotation_editor_screen"),
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 1. Top Annotation Toolbar (`nav`)
                StitchFreeAnnotationTopToolbar(
                    activeTool = activeTool,
                    activeColorInt = topIndicatorColor,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty(),
                    hasStrokes = strokes.isNotEmpty(),
                    onSelectTool = { tool -> viewModel.selectTool(tool) },
                    onColorIndicatorClick = {
                        viewModel.setBottomSheetExpanded(true)
                        if (activeTool == FreeAnnotationTool.DRAWING) {
                            viewModel.toggleColorPicker(ActiveColorPickerType.STROKE_COLOR)
                        }
                    },
                    onUndoClick = { viewModel.undo() },
                    onRedoClick = { viewModel.redo() },
                    onClearOrCloseClick = {
                        if (strokes.isNotEmpty()) {
                            viewModel.clearAllStrokes()
                        } else {
                            onNavigateBack()
                        }
                    },
                    onSaveAndExitClick = {
                        viewModel.saveAnnotationsToPdf(onSaved = onNavigateBack)
                    },
                    onToggleSheetClick = {
                        viewModel.setBottomSheetExpanded(!isBottomSheetExpanded)
                    },
                    onExitWithoutSavingClick = onNavigateBack
                )

                // 2. Middle PDF Document Preview Area (`main class="flex-1 bg-[#F1F3F5] px-4 py-4"`)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(StitchWorkspaceBg)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    InteractivePdfCanvas(
                        pageBitmap = pageBitmap,
                        strokes = strokes,
                        activeTool = activeTool,
                        activeStrokeColor = drawingStrokeColor,
                        activeFillColor = drawingFillColor,
                        activeThicknessPt = drawingThicknessPt,
                        activeOpacity = drawingOpacity,
                        activeHighlightColor = highlightColor,
                        activeHighlightOpacity = highlightOpacity,
                        activeHighlightThicknessPt = highlightThicknessPt,
                        onStrokeCommitted = { normalizedPts ->
                            viewModel.commitNormalizedStroke(normalizedPts)
                        },
                        modifier = Modifier.widthIn(max = 420.dp),
                        pageAspectRatio = pageAspectRatio
                    )
                }

                // 3. Purple `#3B00B3` BottomSheet Inspector ("Drawing" or "Text Highlight")
                AnimatedVisibility(
                    visible = isBottomSheetExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    if (activeTool == FreeAnnotationTool.DRAWING) {
                        StitchDrawingInspectorBottomSheet(
                            strokeColor = drawingStrokeColor,
                            fillColor = drawingFillColor,
                            thicknessPt = drawingThicknessPt,
                            opacityFraction = drawingOpacity,
                            onColourRowClick = {
                                viewModel.toggleColorPicker(ActiveColorPickerType.STROKE_COLOR)
                            },
                            onFillColourRowClick = {
                                viewModel.toggleColorPicker(ActiveColorPickerType.FILL_COLOR)
                            },
                            onThicknessChanged = { viewModel.setDrawingThicknessPt(it) },
                            onOpacityPercentChanged = { viewModel.setDrawingOpacityPercent(it) },
                            onCloseSheet = { viewModel.setBottomSheetExpanded(false) }
                        )
                    } else {
                        StitchTextHighlightBottomSheet(
                            selectedColor = highlightColor,
                            opacityFraction = highlightOpacity,
                            onSelectColor = { viewModel.setHighlightColor(it) },
                            onOpacityPercentChanged = { viewModel.setHighlightOpacityPercent(it) },
                            onCloseSheet = { viewModel.setBottomSheetExpanded(false) }
                        )
                    }
                }
            }

            // 4. Interactive "Text Colour" / "Fill Colour" Bottom Sheet (`PALETTE` & `CUSTOM` tabs)
            if (activeColorPicker != ActiveColorPickerType.NONE) {
                val isFillPicker = activeColorPicker == ActiveColorPickerType.FILL_COLOR
                val activePickerColor = if (isFillPicker) {
                    drawingFillColor ?: drawingStrokeColor
                } else {
                    drawingStrokeColor
                }
                TextColourBottomSheet(
                    selectedColorInt = activePickerColor,
                    activeTab = textColorTab,
                    customColorInputMode = customColorInputMode,
                    recentlyUsedColors = recentlyUsedTextColors,
                    onTabSelected = { viewModel.setTextColorTab(it) },
                    onCustomColorModeSelected = { viewModel.setCustomColorInputMode(it) },
                    onColorSelected = { colorInt, recordInRecents ->
                        if (isFillPicker) {
                            viewModel.setDrawingFillColor(
                                color = colorInt,
                                closePicker = false,
                                recordInRecents = recordInRecents
                            )
                        } else {
                            viewModel.setDrawingStrokeColor(
                                color = colorInt,
                                closePicker = false,
                                recordInRecents = recordInRecents
                            )
                        }
                    },
                    onBackClick = { viewModel.closeColorPicker() },
                    onCloseClick = { viewModel.closeColorPicker() },
                    title = if (isFillPicker) "Fill Colour" else "Text Colour",
                    allowTransparentSelection = isFillPicker,
                    isTransparentSelected = isFillPicker && drawingFillColor == null,
                    onTransparentSelected = {
                        viewModel.setDrawingFillColor(
                            color = null,
                            closePicker = false,
                            recordInRecents = false
                        )
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

/**
 * Exact Stitch Top Annotation Toolbar (`nav`):
 * - Left:
 *   - `'T'` Text Highlight button (`36.dp x 36.dp`, `rounded-lg`) with `▼` at bottom-right
 *   - Fountain Pen Drawing button (`36.dp x 36.dp`, `rounded-lg`, `#2E9B68` green when active) with `▼` at bottom-right
 * - Right:
 *   - Active Color Indicator circle (`28.dp`, `1.dp` dark border, `2.dp` padding, colored inner circle)
 *   - Undo / Redo combined cluster with `▼`
 *   - Clear (`X`) circle button (`28.dp`, `1.dp` dark border)
 *   - Drag handle / Overflow (`:::` 3x2 dots)
 */
@Composable
private fun StitchFreeAnnotationTopToolbar(
    activeTool: FreeAnnotationTool,
    activeColorInt: Int,
    canUndo: Boolean,
    canRedo: Boolean,
    hasStrokes: Boolean,
    onSelectTool: (FreeAnnotationTool) -> Unit,
    onColorIndicatorClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onClearOrCloseClick: () -> Unit,
    onSaveAndExitClick: () -> Unit,
    onToggleSheetClick: () -> Unit,
    onExitWithoutSavingClick: () -> Unit
) {
    var showHandleMenu by remember { mutableStateOf(false) }

    Surface(
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Tool Group (`flex items-center space-x-2`)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Text Highlight ('T') Tool Button
                    val isHighlightActive = activeTool == FreeAnnotationTool.TEXT_HIGHLIGHT
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(1.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isHighlightActive) StitchActiveToolGreen else StitchInactiveToolPeach
                            )
                            .clickable { onSelectTool(FreeAnnotationTool.TEXT_HIGHLIGHT) }
                            .testTag("tool_highlight_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "T",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isHighlightActive) Color.White else StitchOnSurfaceVariant
                        )
                        Text(
                            text = "▼",
                            fontSize = 7.sp,
                            color = if (isHighlightActive) Color.White else StitchOnSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 3.dp, bottom = 2.dp)
                        )
                    }

                    // 2. Freehand Drawing Tool Button (Green `#2E9B68` when active)
                    val isDrawingActive = activeTool == FreeAnnotationTool.DRAWING
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .shadow(1.dp, RoundedCornerShape(8.dp))
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDrawingActive) StitchActiveToolGreen else StitchInactiveToolPeach
                            )
                            .clickable { onSelectTool(FreeAnnotationTool.DRAWING) }
                            .testTag("tool_drawing_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        val iconTint = if (isDrawingActive) Color.White else StitchOnSurfaceVariant
                        // Fountain Pen Nib + Marker Icon matching `code_95ca715a.html` & `screen_ec775298.png`
                        Canvas(modifier = Modifier.size(20.dp)) {
                            val s = size.width / 24f
                            // Left vertical nib/cap shape: M7 14c1.66 0 3-1.34 3-3V3h-2v8c0 .55-.45 1-1 1s-1-.45-1-1V3H4v8c0 1.66 1.34 3 3 3z
                            val capPath = Path().apply {
                                moveTo(4f * s, 3f * s)
                                lineTo(6f * s, 3f * s)
                                lineTo(6f * s, 11f * s)
                                quadraticTo(7f * s, 12.2f * s, 8f * s, 11f * s)
                                lineTo(8f * s, 3f * s)
                                lineTo(10f * s, 3f * s)
                                lineTo(10f * s, 11f * s)
                                quadraticTo(10f * s, 14f * s, 7f * s, 14f * s)
                                quadraticTo(4f * s, 14f * s, 4f * s, 11f * s)
                                close()
                            }
                            drawPath(path = capPath, color = iconTint)

                            // Diagonal pencil body: M13.29 8.46L4 17.75V21h3.25l9.29-9.29-3.25-3.25z
                            val bodyPath = Path().apply {
                                moveTo(13.3f * s, 8.5f * s)
                                lineTo(4f * s, 17.75f * s)
                                lineTo(4f * s, 21f * s)
                                lineTo(7.25f * s, 21f * s)
                                lineTo(16.5f * s, 11.7f * s)
                                close()
                            }
                            drawPath(path = bodyPath, color = iconTint)

                            // Top-right pencil tip cap
                            val tipPath = Path().apply {
                                moveTo(14.7f * s, 7.05f * s)
                                lineTo(17.9f * s, 3.85f * s)
                                lineTo(20.7f * s, 6.7f * s)
                                lineTo(17.5f * s, 9.9f * s)
                                close()
                            }
                            drawPath(path = tipPath, color = iconTint)
                        }

                        Text(
                            text = "▼",
                            fontSize = 7.sp,
                            color = iconTint,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 3.dp, bottom = 2.dp)
                        )
                    }
                }

                // Right Action Tools (`flex items-center space-x-2.5`)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Active Color Indicator Circle (`w-7 h-7 rounded-full p-[2px] border border-on-surface`)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .border(1.2.dp, StitchOnSurface, CircleShape)
                            .clickable(onClick = onColorIndicatorClick)
                            .padding(2.5.dp)
                            .testTag("top_color_indicator"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(activeColorInt))
                        )
                    }

                    // 2. Undo / Redo Combined Cluster
                    Box(
                        modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = canUndo, onClick = onUndoClick)
                                    .testTag("editor_undo_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Undo",
                                    tint = if (canUndo) {
                                        StitchOnSurface
                                    } else {
                                        StitchOutline.copy(alpha = 0.4f)
                                    },
                                    modifier = Modifier.size(19.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .clickable(enabled = canRedo, onClick = onRedoClick)
                                    .testTag("editor_redo_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Redo,
                                    contentDescription = "Redo",
                                    tint = if (canRedo) {
                                        StitchOnSurface
                                    } else {
                                        StitchOutline.copy(alpha = 0.45f)
                                    },
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }

                        Text(
                            text = "▼",
                            fontSize = 6.5.sp,
                            color = StitchOnSurfaceVariant,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(top = 14.dp)
                        )
                    }

                    // 3. Clear / Close ('X') Button (`w-7 h-7 rounded-full border border-on-surface`)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .border(1.2.dp, StitchOnSurface, CircleShape)
                            .clickable(onClick = onClearOrCloseClick)
                            .testTag("editor_clear_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (hasStrokes) "Clear strokes" else "Close editor",
                            tint = StitchOnSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // 4. Drag Handle / Overflow (`:::` 3x2 dots)
                    Box {
                        Box(
                            modifier = Modifier
                                .size(width = 24.dp, height = 28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { showHandleMenu = true }
                                .testTag("editor_drag_handle_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(width = 16.dp, height = 22.dp)) {
                                val sx = size.width / 16f
                                val sy = size.height / 24f
                                val r = 1.8f * sx
                                val ys = listOf(5f, 12f, 19f)
                                val xs = listOf(4f, 12f)
                                for (y in ys) {
                                    for (x in xs) {
                                        drawCircle(
                                            color = StitchOutline,
                                            radius = r,
                                            center = Offset(x * sx, y * sy)
                                        )
                                    }
                                }
                            }
                        }

                        DropdownMenu(
                            expanded = showHandleMenu,
                            onDismissRequest = { showHandleMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Save & Apply to PDF") },
                                onClick = {
                                    showHandleMenu = false
                                    onSaveAndExitClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear All Strokes on Page") },
                                onClick = {
                                    showHandleMenu = false
                                    onClearOrCloseClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Toggle Inspector Panel") },
                                onClick = {
                                    showHandleMenu = false
                                    onToggleSheetClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Back to PDF Viewer") },
                                onClick = {
                                    showHandleMenu = false
                                    onExitWithoutSavingClick()
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                thickness = 1.dp,
                color = StitchOutlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}

/**
 * Purple `#3B00B3` BottomSheet for the "Drawing" Tool (`screen_ec775298.png`):
 * 1. Purple `#3B00B3` header bar with "Drawing" title and 'X' close button
 * 2. Live Sine-Wave Preview Box updating dynamically with `strokeColor`, `fillColor`,
 *    `thicknessPt` (`1..50 pt`), and `opacityFraction` (`0..100%`)
 * 3. `Colour` row with circular color dot + `chevron_right`
 * 4. `Fill Colour` row with checkerboard transparency circle (or fill color) + `chevron_right`
 * 5. `Thickness` slider (`1 pt` to `50 pt`, default `5 pt`) with underlined `"5 pt"` indicator
 * 6. `Opacity` slider (`0%` to `100%`, default `100%`) with underlined `"100 %"` indicator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StitchDrawingInspectorBottomSheet(
    strokeColor: Int,
    fillColor: Int?,
    thicknessPt: Float,
    opacityFraction: Float,
    onColourRowClick: () -> Unit,
    onFillColourRowClick: () -> Unit,
    onThicknessChanged: (Float) -> Unit,
    onOpacityPercentChanged: (Float) -> Unit,
    onCloseSheet: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        shadowElevation = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("drawing_inspector_bottom_sheet")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Purple `#3B00B3` Header Bar (`screen_ec775298.png`)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(StitchPurpleSheetHeader)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Drawing",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                IconButton(
                    onClick = onCloseSheet,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(28.dp)
                        .testTag("close_drawing_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Drawing Panel",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // 2. Stroke Live Dynamic Sine-Wave Preview Box (`M 10 30 Q 90 -2 170 30 T 330 30`)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("drawing_sine_wave_preview")
                ) {
                    val sx = size.width / 340f
                    val sy = size.height / 60f

                    val sinePath = Path().apply {
                        moveTo(10f * sx, 30f * sy)
                        quadraticTo(90f * sx, -2f * sy, 170f * sx, 30f * sy)
                        quadraticTo(250f * sx, 62f * sy, 330f * sx, 30f * sy)
                    }

                    val clampedAlpha = opacityFraction.coerceIn(0f, 1f)

                    if (fillColor != null) {
                        drawPath(
                            path = sinePath,
                            color = Color(fillColor).copy(
                                alpha = (clampedAlpha * 0.45f).coerceIn(0.08f, 0.85f)
                            ),
                            style = Fill
                        )
                    }

                    val previewStrokePx = (thicknessPt * density).coerceIn(2f, size.height * 0.72f)
                    drawPath(
                        path = sinePath,
                        color = Color(strokeColor).copy(alpha = clampedAlpha),
                        style = Stroke(
                            width = previewStrokePx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = StitchRowDivider)

            // 3. Settings Rows Form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Row 1: Colour
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onColourRowClick)
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                        .testTag("drawing_colour_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Colour",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        color = StitchOnSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .shadow(1.dp, CircleShape)
                                .clip(CircleShape)
                                .background(Color(strokeColor))
                                .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Select Colour",
                            tint = StitchOutline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = StitchRowDivider)

                // Row 2: Fill Colour (Checkerboard pattern when null)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onFillColourRowClick)
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                        .testTag("drawing_fill_colour_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Fill Colour",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        color = StitchOnSurfaceVariant
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CheckerboardOrColorCircle(
                            colorInt = fillColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Select Fill Colour",
                            tint = StitchOutline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(thickness = 1.dp, color = StitchRowDivider)

                // Row 3: Thickness Slider (1 pt to 50 pt, default 5 pt)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Thickness",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        color = StitchOnSurfaceVariant,
                        modifier = Modifier.width(76.dp)
                    )

                    Slider(
                        value = thicknessPt,
                        onValueChange = onThicknessChanged,
                        valueRange = 1f..50f,
                        colors = SliderDefaults.colors(
                            thumbColor = StitchSliderThumbRed,
                            activeTrackColor = StitchSliderTrackPeach,
                            inactiveTrackColor = StitchSliderTrackPeach
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("drawing_thickness_slider")
                    )

                    UnderlinedValueBadge(
                        text = "${thicknessPt.roundToInt()} pt",
                        minWidthDp = 38,
                        testTag = "thickness_value_text"
                    )
                }

                HorizontalDivider(thickness = 1.dp, color = StitchRowDivider)

                // Row 4: Opacity Slider (0% to 100%, default 100%)
                val opacityPercent = (opacityFraction * 100f).roundToInt().coerceIn(0, 100)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Opacity",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        color = StitchOnSurfaceVariant,
                        modifier = Modifier.width(76.dp)
                    )

                    Slider(
                        value = opacityFraction * 100f,
                        onValueChange = onOpacityPercentChanged,
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = StitchSliderThumbRed,
                            activeTrackColor = StitchSliderTrackPeach,
                            inactiveTrackColor = StitchSliderTrackPeach
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("drawing_opacity_slider")
                    )

                    UnderlinedValueBadge(
                        text = "$opacityPercent %",
                        minWidthDp = 44,
                        testTag = "opacity_value_text"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Purple `#3B00B3` BottomSheet for the "Text Highlight" (`T`) Tool:
 * - Purple `#3B00B3` header bar with "Text Highlight" title and 'X' close button
 * - 5x2 Color Grid (2 rows of 5 highlighter colors)
 * - `Opacity` slider (`0%` to `100%`) with underlined percentage badge
 */
@Composable
private fun StitchTextHighlightBottomSheet(
    selectedColor: Int,
    opacityFraction: Float,
    onSelectColor: (Int) -> Unit,
    onOpacityPercentChanged: (Float) -> Unit,
    onCloseSheet: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
        shadowElevation = 14.dp,
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("text_highlight_bottom_sheet")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. Purple `#3B00B3` Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(StitchPurpleSheetHeader)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Text Highlight",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                IconButton(
                    onClick = onCloseSheet,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(28.dp)
                        .testTag("close_highlight_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Text Highlight Panel",
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // 2. 5x2 Highlighter Color Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val rows = PdfEditorViewModel.HIGHLIGHT_PALETTE_5X2.chunked(5)
                rows.forEachIndexed { rowIdx, rowColors ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowColors.forEachIndexed { colIdx, colorInt ->
                            val isSelected = selectedColor == colorInt
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier
                                                .border(2.5.dp, StitchPurpleSheetHeader, CircleShape)
                                                .padding(3.dp)
                                        } else {
                                            Modifier
                                                .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                                        }
                                    )
                                    .clickable { onSelectColor(colorInt) }
                                    .testTag("highlight_color_${rowIdx * 5 + colIdx}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color(colorInt))
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 1.dp, color = StitchRowDivider)

            // 3. Opacity Slider (0% to 100%)
            val opacityPercent = (opacityFraction * 100f).roundToInt().coerceIn(0, 100)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Opacity",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    color = StitchOnSurfaceVariant,
                    modifier = Modifier.width(76.dp)
                )

                Slider(
                    value = opacityFraction * 100f,
                    onValueChange = onOpacityPercentChanged,
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = StitchSliderThumbRed,
                        activeTrackColor = StitchSliderTrackPeach,
                        inactiveTrackColor = StitchSliderTrackPeach
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("highlight_opacity_slider")
                )

                UnderlinedValueBadge(
                    text = "$opacityPercent %",
                    minWidthDp = 44,
                    testTag = "highlight_opacity_value_text"
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Renders the circular checkerboard pattern (`checkerboard-pattern` in `code_95ca715a.html`)
 * when [colorInt] is `null`, or a solid color circle when a Fill Colour is chosen.
 */
@Composable
private fun CheckerboardOrColorCircle(
    colorInt: Int?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(1.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, Color(0xFFCBD5E1), CircleShape)
    ) {
        if (colorInt != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(colorInt))
            )
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellSize = 4.dp.toPx()
                val cols = (size.width / cellSize).toInt() + 1
                val rows = (size.height / cellSize).toInt() + 1
                val gray = Color(0xFFCBD5E1)
                for (r in 0..rows) {
                    for (c in 0..cols) {
                        if ((r + c) % 2 == 0) {
                            drawRect(
                                color = gray,
                                topLeft = Offset(c * cellSize, r * cellSize),
                                size = Size(cellSize, cellSize)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Right-aligned underlined value indicator (`5 pt`, `100 %`) matching `border-b-2 border-on-surface`.
 */
@Composable
private fun UnderlinedValueBadge(
    text: String,
    minWidthDp: Int,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = minWidthDp.dp)
            .testTag(testTag)
    ) {
        Text(
            text = text,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = StitchOnSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .width(minWidthDp.dp)
                .height(2.dp)
                .background(StitchOnSurface)
        )
    }
}

/**
 * Interactive Color Palette Popover (`palettePickerModal` in `code_95ca715a.html`)
 * supporting both Stroke Colour (`Colour`) and Fill Colour (`Fill Colour` with a transparent
 * checkerboard option).
 */
@Composable
private fun StitchColorPalettePopover(
    pickerType: ActiveColorPickerType,
    selectedStrokeColor: Int,
    selectedFillColor: Int?,
    onSelectStrokeColor: (Int) -> Unit,
    onSelectFillColor: (Int?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 390.dp)
            .testTag("color_palette_popover"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StitchOutlineVariant, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (pickerType == ActiveColorPickerType.FILL_COLOR) {
                        "Select Fill Colour"
                    } else {
                        "Select Stroke Colour"
                    },
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = StitchOnSurface
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close palette",
                        tint = StitchOutline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val colors = PdfEditorViewModel.DRAWING_STROKE_PALETTE
            if (pickerType == ActiveColorPickerType.FILL_COLOR) {
                // Row 1: Checkerboard "None" + first 5 colors
                val firstRow = listOf<Int?>(null) + colors.take(5)
                val secondRow = colors.drop(5).take(6)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    firstRow.forEach { cInt ->
                        val isSelected = selectedFillColor == cInt
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .border(2.dp, StitchSliderThumbRed, CircleShape)
                                            .padding(2.dp)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onSelectFillColor(cInt) }
                        ) {
                            CheckerboardOrColorCircle(
                                colorInt = cInt,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    secondRow.forEach { cInt ->
                        val isSelected = selectedFillColor == cInt
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier
                                            .border(2.dp, StitchSliderThumbRed, CircleShape)
                                            .padding(2.dp)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onSelectFillColor(cInt) }
                        ) {
                            CheckerboardOrColorCircle(
                                colorInt = cInt,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            } else {
                // 6x2 Grid of Stroke Colours
                colors.chunked(6).forEachIndexed { rIdx, rowColors ->
                    if (rIdx > 0) Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowColors.forEach { cInt ->
                            val isSelected = selectedStrokeColor == cInt
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) {
                                            Modifier
                                                .border(2.dp, StitchSliderThumbRed, CircleShape)
                                                .padding(2.5.dp)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { onSelectStrokeColor(cInt) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(Color(cInt))
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
