package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.Paint
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.InterFontFamily
import kotlin.math.roundToInt

// Exact Stitch AI Premium OCR Editor Palette (`code_00c91e97.html` & `screen_9005060e.png`)
private val StitchCanvasSlateBg = Color(0xFFE2E8F0)
private val StitchHeaderBorder = Color(0xFFE2E8F0)
private val StitchSaveRed = Color(0xFFDC2626)
private val StitchInspectorPurple = Color(0xFF3B00B3)
private val StitchAccentPurple = Color(0xFF3800B0)
private val StitchActiveBlueBg = Color(0xFFDBEAFE)
private val StitchActiveBlueTint = Color(0xFF1D4ED8)
private val StitchSelectionBlue = Color(0xFF2563EB)
private val StitchHighlightOrange = Color(0xFFD97706)
private val StitchSlate900 = Color(0xFF0F172A)
private val StitchSlate800 = Color(0xFF1E293B)
private val StitchSlate700 = Color(0xFF334155)
private val StitchSlate600 = Color(0xFF475569)
private val StitchSlate500 = Color(0xFF64748B)
private val StitchSlate400 = Color(0xFF94A3B8)
private val StitchSlate200 = Color(0xFFE2E8F0)
private val StitchSlate100 = Color(0xFFF1F5F9)
private val StitchSlate50 = Color(0xFFF8FAFC)

// Dark Text Colour Inspector Palette (matching uploaded Text Colour PALETTE & CUSTOM screenshots)
private val TextColorDarkHeader = Color(0xFF080808)
private val TextColorDarkSurface = Color(0xFF141414)
private val TextColorTabBarBg = Color(0xFF383838)
private val TextColorCoralAccent = Color(0xFFF26B5B)
private val TextColorInactiveGray = Color(0xFFB5B5B5)
private val TextColorTrackBlack = Color(0xFF050505)

/**
 * Entry point for the PDF Editor Screen:
 * - When launched in [EditorLaunchMode.FREE_ANNOTATION], renders [FreeAnnotationEditor] with
 *   the Stitch AI Free Plan Annotation UI (`InteractivePdfCanvas` + purple `#3B00B3` BottomSheets).
 * - When launched in [EditorLaunchMode.PREMIUM_OCR_TEXT_EDIT], renders [PremiumOcrTextEditorScreen]
 *   with the Stitch AI Premium OCR & Direct Text Editor UI.
 */
@Composable
fun PdfEditorScreen(
    documentId: Long,
    initialPageIndex: Int,
    launchMode: EditorLaunchMode,
    repository: PdfRepository,
    subscriptionManager: SubscriptionManager,
    onNavigateBack: () -> Unit
) {
    val factory = remember(documentId, initialPageIndex, launchMode, repository, subscriptionManager) {
        PdfEditorViewModel.Factory(
            documentId = documentId,
            initialPageIndex = initialPageIndex,
            initialLaunchMode = launchMode,
            repository = repository,
            subscriptionManager = subscriptionManager
        )
    }

    val editorViewModel: PdfEditorViewModel = viewModel(
        key = "editor_${documentId}_${initialPageIndex}_${launchMode.name}",
        factory = factory
    )

    if (launchMode == EditorLaunchMode.FREE_ANNOTATION) {
        FreeAnnotationEditor(
            viewModel = editorViewModel,
            onNavigateBack = onNavigateBack
        )
    } else {
        PremiumOcrEditor(
            viewModel = editorViewModel,
            onNavigateBack = onNavigateBack
        )
    }
}

/**
 * Public entry point for the Stitch AI Premium OCR & Direct Text Editor Screen.
 */
@Composable
fun PremiumOcrEditor(
    viewModel: PdfEditorViewModel,
    onNavigateBack: () -> Unit
) {
    PremiumOcrTextEditorScreen(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack
    )
}

/**
 * Stitch AI Premium OCR & Direct Text Editor Screen (`code_00c91e97.html` & `screen_9005060e.png`):
 * - Top bar with Back arrow, filename (`fractal_v2.pdf`), Undo/Redo, and red `"✓ Save"` pill button.
 * - Dashed blue bounding boxes around detected PDF text blocks with the top-left blue resize slider.
 * - Keyboard formatting bar with `TT`, `AA`, Color circle, `B`, `I`, Highlight, Alignment, Drawing, and Line Spacing.
 * - Purple `#3B00B3` BottomSheets for Font, Size, Line Spacing, Text Colour (Palette/Custom), Drawing, and Highlight.
 */
@Composable
fun PremiumOcrTextEditorScreen(
    viewModel: PdfEditorViewModel,
    onNavigateBack: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    val document by viewModel.document.collectAsStateWithLifecycle()
    val pageBitmap by viewModel.pageBitmap.collectAsStateWithLifecycle()
    val deletedRedactions by viewModel.deletedRedactions.collectAsStateWithLifecycle()
    val pageAspectRatio by viewModel.pageAspectRatio.collectAsStateWithLifecycle()
    val textBlocks by viewModel.textBlocks.collectAsStateWithLifecycle()
    val selectedTextBlockId by viewModel.selectedTextBlockId.collectAsStateWithLifecycle()
    val isToolbarVisible by viewModel.isToolbarVisible.collectAsStateWithLifecycle()
    val isInlineEditingText by viewModel.isInlineEditingText.collectAsStateWithLifecycle()
    val activeOcrSheet by viewModel.activeOcrSheet.collectAsStateWithLifecycle()
    val textColorTab by viewModel.textColorTab.collectAsStateWithLifecycle()
    val customColorInputMode by viewModel.customColorInputMode.collectAsStateWithLifecycle()
    val recentlyUsedTextColors by viewModel.recentlyUsedTextColors.collectAsStateWithLifecycle()

    val strokes by viewModel.strokes.collectAsStateWithLifecycle()
    val drawingStrokeColor by viewModel.drawingStrokeColor.collectAsStateWithLifecycle()
    val drawingFillColor by viewModel.drawingFillColor.collectAsStateWithLifecycle()
    val drawingThicknessPt by viewModel.drawingThicknessPt.collectAsStateWithLifecycle()
    val drawingOpacity by viewModel.drawingOpacity.collectAsStateWithLifecycle()
    val highlightColor by viewModel.highlightColor.collectAsStateWithLifecycle()
    val highlightOpacity by viewModel.highlightOpacity.collectAsStateWithLifecycle()
    val highlightThicknessPt by viewModel.highlightThicknessPt.collectAsStateWithLifecycle()

    val undoStack by viewModel.undoStack.collectAsStateWithLifecycle()
    val redoStack by viewModel.redoStack.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val copyToAndroidClipboard: (String) -> Unit = remember(context) {
        { textToCopy ->
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("PDF Text Block", textToCopy))
        }
    }

    val selectedBlock = remember(textBlocks, selectedTextBlockId) {
        textBlocks.firstOrNull { it.id == selectedTextBlockId } ?: textBlocks.firstOrNull()
    }

    val displayFileName = remember(document) {
        document?.name ?: "Document.pdf"
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .testTag("premium_ocr_editor_screen"),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = StitchCanvasSlateBg,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            StitchPremiumOcrTopBar(
                fileName = displayFileName,
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty(),
                isSaving = isSaving,
                onBackClick = onNavigateBack,
                onUndoClick = { viewModel.undo() },
                onRedoClick = { viewModel.redo() },
                onSaveClick = {
                    viewModel.saveAnnotationsToPdf(onSaved = onNavigateBack)
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Purple #3B00B3 / Dark Text Colour Modular Bottom Sheet Inspector (Font, Size,
                // Line Spacing, Text Colour Palette/Custom, Drawing, or Text Highlight)
                AnimatedVisibility(
                    visible = activeOcrSheet != OcrInspectorSheetType.NONE,
                    enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                    exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
                ) {
                    StitchPremiumPurpleBottomSheet(
                        sheetType = activeOcrSheet,
                        selectedBlock = selectedBlock,
                        textColorTab = textColorTab,
                        customColorInputMode = customColorInputMode,
                        recentlyUsedTextColors = recentlyUsedTextColors,
                        drawingStrokeColor = drawingStrokeColor,
                        drawingFillColor = drawingFillColor,
                        drawingThicknessPt = drawingThicknessPt,
                        drawingOpacity = drawingOpacity,
                        highlightColor = highlightColor,
                        highlightOpacity = highlightOpacity,
                        onCloseSheet = { viewModel.closeOcrSheet() },
                        onSelectFont = { viewModel.setSelectedBlockFont(it) },
                        onSelectFontSize = { viewModel.setSelectedBlockFontSize(it) },
                        onSelectLineSpacing = { viewModel.setSelectedBlockLineSpacing(it) },
                        onSelectTextColorTab = { viewModel.setTextColorTab(it) },
                        onSelectCustomColorMode = { viewModel.setCustomColorInputMode(it) },
                        onSelectTextColor = { colorInt, recordInRecents ->
                            viewModel.setSelectedBlockTextColor(colorInt, recordInRecents)
                        },
                        onToggleBackgroundTransparent = {
                            viewModel.setSelectedBlockBackgroundTransparent(
                                isTransparent = true,
                                backgroundColor = AndroidColor.TRANSPARENT
                            )
                        },
                        onSelectDrawingStrokeColor = { viewModel.setDrawingStrokeColor(it) },
                        onSelectDrawingFillColor = { viewModel.setDrawingFillColor(it) },
                        onSelectDrawingThickness = { viewModel.setDrawingThicknessPt(it) },
                        onSelectDrawingOpacityPercent = { viewModel.setDrawingOpacityPercent(it) },
                        onSelectHighlightColor = { viewModel.setHighlightColor(it) },
                        onSelectHighlightOpacityPercent = { viewModel.setHighlightOpacityPercent(it) }
                    )
                }

                // Bottom Keyboard Formatting Toolbar (`TT`, `AA`, Color circle, `B`, `I`,
                // Highlight, Alignment, Drawing scribble, Line Spacing)
                StitchOcrFormattingToolbar(
                    selectedBlock = selectedBlock,
                    activeSheet = activeOcrSheet,
                    onFontClick = { viewModel.toggleOcrSheet(OcrInspectorSheetType.FONT) },
                    onSizeClick = { viewModel.toggleOcrSheet(OcrInspectorSheetType.SIZE) },
                    onColorClick = { viewModel.toggleOcrSheet(OcrInspectorSheetType.TEXT_COLOR) },
                    onBoldClick = { viewModel.toggleSelectedBlockBold() },
                    onItalicClick = { viewModel.toggleSelectedBlockItalic() },
                    onHighlightClick = { viewModel.toggleOcrSheet(OcrInspectorSheetType.HIGHLIGHT) },
                    onAlignmentClick = { viewModel.cycleOrSetSelectedBlockAlignment() },
                    onDrawingClick = { viewModel.toggleOcrSheet(OcrInspectorSheetType.DRAWING) },
                    onLineSpacingClick = { viewModel.toggleOcrSheet(OcrInspectorSheetType.LINE_SPACING) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(StitchCanvasSlateBg)
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = StitchAccentPurple)
                }
            } else {
                val isDrawingOrHighlight = activeOcrSheet == OcrInspectorSheetType.DRAWING ||
                    activeOcrSheet == OcrInspectorSheetType.HIGHLIGHT
                val isHighlightTool = activeOcrSheet == OcrInspectorSheetType.HIGHLIGHT

                PremiumOcrInteractiveCanvas(
                    pageBitmap = pageBitmap,
                    textBlocks = textBlocks,
                    selectedTextBlockId = selectedTextBlockId,
                    isInlineEditingText = isInlineEditingText,
                    strokes = strokes,
                    isDrawingOrHighlightMode = isDrawingOrHighlight,
                    isHighlightTool = isHighlightTool,
                    activeStrokeColor = drawingStrokeColor,
                    activeFillColor = drawingFillColor,
                    activeThicknessPt = drawingThicknessPt,
                    activeOpacity = drawingOpacity,
                    activeHighlightColor = highlightColor,
                    activeHighlightOpacity = highlightOpacity,
                    activeHighlightThicknessPt = highlightThicknessPt,
                    onSelectTextBlock = { blockId ->
                        viewModel.selectTextBlock(blockId, startInlineEditing = true)
                    },
                    onUpdateSelectedBlockText = { newText ->
                        viewModel.updateSelectedBlockText(newText)
                    },
                    onUpdateTextBlockText = { blockId, newText ->
                        viewModel.updateTextBlockText(blockId, newText)
                    },
                    onStartMoveTextBlock = { blockId ->
                        viewModel.onStartMoveTextBlock(blockId)
                    },
                    onMoveTextBlock = { blockId, dNormX, dNormY ->
                        viewModel.moveTextBlock(blockId, dNormX, dNormY)
                    },
                    onEndMoveTextBlock = { blockId ->
                        viewModel.commitMoveTextBlock(blockId)
                    },
                    onResizeSelectedBlockFromTopLeft = { dNormX, dNormY ->
                        viewModel.resizeSelectedBlockFromTopLeft(dNormX, dNormY)
                    },
                    onStrokeCommitted = { normalizedPts ->
                        viewModel.commitNormalizedStroke(normalizedPts)
                    },
                    deletedRedactions = deletedRedactions,
                    isToolbarVisible = isToolbarVisible,
                    onLongPressTextBlock = { blockId ->
                        viewModel.onLongPressTextBlock(blockId)
                    },
                    onDismissToolbar = {
                        viewModel.dismissContextualToolbar()
                        viewModel.setInlineEditingText(false)
                    },
                    onCutBlock = {
                        viewModel.cutSelectedTextBlock(onCopyToClipboard = copyToAndroidClipboard)
                    },
                    onCopyBlock = {
                        viewModel.copySelectedTextBlock(onCopyToClipboard = copyToAndroidClipboard)
                    },
                    onDeleteBlock = {
                        viewModel.deleteSelectedBlock()
                    },
                    onDuplicateBlock = {
                        viewModel.duplicateSelectedTextBlock()
                    },
                    onEditBlock = {
                        viewModel.editSelectedTextBlock()
                    },
                    onEraserClearBlock = {
                        viewModel.clearOrResetSelectedTextBlock()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(pageAspectRatio.coerceIn(0.55f, 1.85f))
                )
            }
        }
    }
}

// =============================================================================================
// 1. TOP APP BAR (`code_00c91e97.html` & `screen_9005060e.png`)
// =============================================================================================

@Composable
private fun StitchPremiumOcrTopBar(
    fileName: String,
    canUndo: Boolean,
    canRedo: Boolean,
    isSaving: Boolean,
    onBackClick: () -> Unit,
    onUndoClick: () -> Unit,
    onRedoClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Back arrow + Filename (`fractal_v2.pdf`)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("ocr_back_button")
                    ) {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            val w = size.width
                            val h = size.height
                            // Left arrow `M19 12H5 M12 19l-7-7 7-7`
                            drawLine(
                                color = StitchSlate700,
                                start = Offset(w * 0.82f, h * 0.50f),
                                end = Offset(w * 0.18f, h * 0.50f),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            val head = Path().apply {
                                moveTo(w * 0.48f, h * 0.80f)
                                lineTo(w * 0.18f, h * 0.50f)
                                lineTo(w * 0.48f, h * 0.20f)
                            }
                            drawPath(
                                path = head,
                                color = StitchSlate700,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    Text(
                        text = fileName,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.5.sp,
                        color = StitchSlate800,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Right: Undo + Redo + Red "✓ Save" pill button (`#DC2626`)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Undo Button (`M3 7v6h6 M21 17a9 9 0 0 0-9-9 9 9 0 0 0-6 2.3L3 13`)
                    IconButton(
                        onClick = onUndoClick,
                        enabled = canUndo,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("ocr_undo_button")
                    ) {
                        val tint = if (canUndo) StitchSlate600 else StitchSlate400.copy(alpha = 0.5f)
                        Canvas(modifier = Modifier.size(19.dp)) {
                            val w = size.width
                            val h = size.height
                            val path = Path().apply {
                                moveTo(w * 0.14f, h * 0.28f)
                                lineTo(w * 0.14f, h * 0.54f)
                                lineTo(w * 0.40f, h * 0.54f)
                                moveTo(w * 0.16f, h * 0.52f)
                                cubicTo(w * 0.32f, h * 0.28f, w * 0.72f, h * 0.28f, w * 0.86f, h * 0.56f)
                                cubicTo(w * 0.90f, h * 0.65f, w * 0.88f, h * 0.74f, w * 0.82f, h * 0.80f)
                            }
                            drawPath(
                                path = path,
                                color = tint,
                                style = Stroke(
                                    width = 1.9.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // Redo Button (`M21 7v6h-6 M3 17a9 9 0 0 1 9-9 9 9 0 0 1 6 2.3l3 2.7`)
                    IconButton(
                        onClick = onRedoClick,
                        enabled = canRedo,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("ocr_redo_button")
                    ) {
                        val tint = if (canRedo) StitchSlate600 else StitchSlate400.copy(alpha = 0.5f)
                        Canvas(modifier = Modifier.size(19.dp)) {
                            val w = size.width
                            val h = size.height
                            val path = Path().apply {
                                moveTo(w * 0.86f, h * 0.28f)
                                lineTo(w * 0.86f, h * 0.54f)
                                lineTo(w * 0.60f, h * 0.54f)
                                moveTo(w * 0.84f, h * 0.52f)
                                cubicTo(w * 0.68f, h * 0.28f, w * 0.28f, h * 0.28f, w * 0.14f, h * 0.56f)
                                cubicTo(w * 0.10f, h * 0.65f, w * 0.12f, h * 0.74f, w * 0.18f, h * 0.80f)
                            }
                            drawPath(
                                path = path,
                                color = tint,
                                style = Stroke(
                                    width = 1.9.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Red "✓ Save" pill button (`bg-[#DC2626] text-white text-xs font-semibold px-3.5 py-2 rounded-full`)
                    Button(
                        onClick = onSaveClick,
                        enabled = !isSaving,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchSaveRed,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(50))
                            .testTag("ocr_save_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isSaving) "Saving..." else "Save",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(StitchHeaderBorder)
            )
        }
    }
}

// =============================================================================================
// 2. BOTTOM KEYBOARD FORMATTING TOOLBAR (`code_00c91e97.html` & `screen_9005060e.png`)
// =============================================================================================

@Composable
private fun StitchOcrFormattingToolbar(
    selectedBlock: EditableTextBlock?,
    activeSheet: OcrInspectorSheetType,
    onFontClick: () -> Unit,
    onSizeClick: () -> Unit,
    onColorClick: () -> Unit,
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onHighlightClick: () -> Unit,
    onAlignmentClick: () -> Unit,
    onDrawingClick: () -> Unit,
    onLineSpacingClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ocr_formatting_toolbar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(StitchSlate200)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. TT (Font Family)
                val isFontActive = activeSheet == OcrInspectorSheetType.FONT
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFontActive) StitchActiveBlueBg else Color.Transparent)
                        .clickable(onClick = onFontClick)
                        .testTag("toolbar_font_tt_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TT",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        letterSpacing = (-0.5).sp,
                        color = if (isFontActive) StitchActiveBlueTint else StitchSlate700
                    )
                }

                // 2. AA (Font Size)
                val isSizeActive = activeSheet == OcrInspectorSheetType.SIZE
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSizeActive) StitchActiveBlueBg else Color.Transparent)
                        .clickable(onClick = onSizeClick)
                        .testTag("toolbar_size_aa_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "AA",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isSizeActive) StitchActiveBlueTint else StitchSlate700
                    )
                }

                // 3. Solid Color Circle (Text Colour Palette/Custom)
                val isColorActive = activeSheet == OcrInspectorSheetType.TEXT_COLOR
                val activeTextColor = Color(selectedBlock?.textColor ?: AndroidColor.parseColor("#0F172A"))
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isColorActive) StitchActiveBlueBg else Color.Transparent)
                        .clickable(onClick = onColorClick)
                        .testTag("toolbar_text_color_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .shadow(elevation = 1.dp, shape = CircleShape)
                            .clip(CircleShape)
                            .background(activeTextColor)
                            .border(2.dp, StitchSlate200, CircleShape)
                    )
                }

                // 4. B (Bold)
                val isBold = selectedBlock?.isBold == true
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isBold) StitchSlate100 else Color.Transparent)
                        .clickable(onClick = onBoldClick)
                        .testTag("toolbar_bold_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "B",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = if (isBold) StitchAccentPurple else StitchSlate800
                    )
                }

                // 5. I (Italic Serif)
                val isItalic = selectedBlock?.isItalic == true
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isItalic) StitchSlate100 else Color.Transparent)
                        .clickable(onClick = onItalicClick)
                        .testTag("toolbar_italic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "I",
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = if (isItalic) StitchAccentPurple else StitchSlate700
                    )
                }

                // 6. Text Highlight Marker Icon (`text-amber-600`)
                val isHighlightActive = activeSheet == OcrInspectorSheetType.HIGHLIGHT
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isHighlightActive) Color(0xFFFEF3C7) else Color.Transparent)
                        .clickable(onClick = onHighlightClick)
                        .testTag("toolbar_highlight_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val w = size.width
                        val h = size.height
                        val marker = Path().apply {
                            moveTo(w * 0.38f, h * 0.22f)
                            lineTo(w * 0.78f, h * 0.62f)
                            lineTo(w * 0.50f, h * 0.90f)
                            lineTo(w * 0.10f, h * 0.50f)
                            close()
                        }
                        drawPath(
                            path = marker,
                            color = StitchHighlightOrange,
                            style = Stroke(
                                width = 1.8.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        drawLine(
                            color = StitchHighlightOrange,
                            start = Offset(w * 0.12f, h * 0.92f),
                            end = Offset(w * 0.55f, h * 0.92f),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // 7. Alignment Icon (Active Blue Rounded Box `bg-blue-100 text-blue-700`)
                val currentAlign = selectedBlock?.alignment ?: Paint.Align.CENTER
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchActiveBlueBg)
                        .clickable(onClick = onAlignmentClick)
                        .testTag("toolbar_alignment_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val w = size.width
                        val h = size.height
                        val strokePx = 2.dp.toPx()

                        // Line 1 (Full width)
                        drawLine(
                            color = StitchActiveBlueTint,
                            start = Offset(w * 0.12f, h * 0.22f),
                            end = Offset(w * 0.88f, h * 0.22f),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                        // Line 2 (Aligned according to LEFT / CENTER / RIGHT)
                        val (l2Start, l2End) = when (currentAlign) {
                            Paint.Align.LEFT -> 0.12f to 0.66f
                            Paint.Align.CENTER -> 0.22f to 0.78f
                            Paint.Align.RIGHT -> 0.34f to 0.88f
                        }
                        drawLine(
                            color = StitchActiveBlueTint,
                            start = Offset(w * l2Start, h * 0.48f),
                            end = Offset(w * l2End, h * 0.48f),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                        // Line 3 (Full width)
                        drawLine(
                            color = StitchActiveBlueTint,
                            start = Offset(w * 0.12f, h * 0.74f),
                            end = Offset(w * 0.88f, h * 0.74f),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // 8. Drawing Scribble Loop Icon (`text-[#3800B0]`)
                val isDrawingActive = activeSheet == OcrInspectorSheetType.DRAWING
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDrawingActive) Color(0xFFF3E8FF) else Color.Transparent)
                        .clickable(onClick = onDrawingClick)
                        .testTag("toolbar_drawing_button"),
                    contentAlignment = Alignment.Center
                ) {
                    StitchLoopScribbleIcon(
                        color = StitchAccentPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 9. Line Spacing / Formatting Sliders Icon
                val isSpacingActive = activeSheet == OcrInspectorSheetType.LINE_SPACING
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSpacingActive) StitchActiveBlueBg else Color.Transparent)
                        .clickable(onClick = onLineSpacingClick)
                        .testTag("toolbar_line_spacing_button"),
                    contentAlignment = Alignment.Center
                ) {
                    val sliderTint = if (isSpacingActive) StitchActiveBlueTint else StitchSlate600
                    Canvas(modifier = Modifier.size(18.dp)) {
                        val w = size.width
                        val h = size.height
                        val strokePx = 1.8.dp.toPx()

                        // Top horizontal track + knob
                        drawLine(
                            color = sliderTint,
                            start = Offset(w * 0.15f, h * 0.25f),
                            end = Offset(w * 0.85f, h * 0.25f),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.6.dp.toPx(),
                            center = Offset(w * 0.36f, h * 0.25f)
                        )
                        drawCircle(
                            color = sliderTint,
                            radius = 2.6.dp.toPx(),
                            center = Offset(w * 0.36f, h * 0.25f),
                            style = Stroke(width = strokePx)
                        )

                        // Middle horizontal track + knob
                        drawLine(
                            color = sliderTint,
                            start = Offset(w * 0.15f, h * 0.52f),
                            end = Offset(w * 0.85f, h * 0.52f),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.6.dp.toPx(),
                            center = Offset(w * 0.66f, h * 0.52f)
                        )
                        drawCircle(
                            color = sliderTint,
                            radius = 2.6.dp.toPx(),
                            center = Offset(w * 0.66f, h * 0.52f),
                            style = Stroke(width = strokePx)
                        )

                        // Bottom horizontal track + knob
                        drawLine(
                            color = sliderTint,
                            start = Offset(w * 0.15f, h * 0.79f),
                            end = Offset(w * 0.85f, h * 0.79f),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.6.dp.toPx(),
                            center = Offset(w * 0.44f, h * 0.79f)
                        )
                        drawCircle(
                            color = sliderTint,
                            radius = 2.6.dp.toPx(),
                            center = Offset(w * 0.44f, h * 0.79f),
                            style = Stroke(width = strokePx)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Exact cursive loop scribble icon from the Stitch Drawing header & toolbar (`M3 17c3-6 6-6 9 0s6 6 9-4...`).
 */
@Composable
private fun StitchLoopScribbleIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.72f)
            cubicTo(w * 0.25f, h * 0.30f, w * 0.58f, h * 0.28f, w * 0.62f, h * 0.55f)
            cubicTo(w * 0.65f, h * 0.78f, w * 0.34f, h * 0.82f, w * 0.38f, h * 0.54f)
            cubicTo(w * 0.42f, h * 0.30f, w * 0.74f, h * 0.45f, w * 0.88f, h * 0.76f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

// =============================================================================================
// 3. PURPLE `#3B00B3` MODULAR BOTTOM SHEETS (Font, Size, Line Spacing, Text Colour, Drawing, Highlight)
// =============================================================================================

@Composable
private fun StitchPremiumPurpleBottomSheet(
    sheetType: OcrInspectorSheetType,
    selectedBlock: EditableTextBlock?,
    textColorTab: TextColorTab,
    customColorInputMode: CustomColorInputMode,
    recentlyUsedTextColors: List<Int>,
    drawingStrokeColor: Int,
    drawingFillColor: Int?,
    drawingThicknessPt: Float,
    drawingOpacity: Float,
    highlightColor: Int,
    highlightOpacity: Float,
    onCloseSheet: () -> Unit,
    onSelectFont: (String) -> Unit,
    onSelectFontSize: (Float) -> Unit,
    onSelectLineSpacing: (Float) -> Unit,
    onSelectTextColorTab: (TextColorTab) -> Unit,
    onSelectCustomColorMode: (CustomColorInputMode) -> Unit,
    onSelectTextColor: (Int, Boolean) -> Unit,
    onToggleBackgroundTransparent: () -> Unit,
    onSelectDrawingStrokeColor: (Int) -> Unit,
    onSelectDrawingFillColor: (Int?) -> Unit,
    onSelectDrawingThickness: (Float) -> Unit,
    onSelectDrawingOpacityPercent: (Float) -> Unit,
    onSelectHighlightColor: (Int) -> Unit,
    onSelectHighlightOpacityPercent: (Float) -> Unit
) {
    val sheetTitle = when (sheetType) {
        OcrInspectorSheetType.DRAWING -> "Drawing"
        OcrInspectorSheetType.FONT -> "Font"
        OcrInspectorSheetType.SIZE -> "Size"
        OcrInspectorSheetType.TEXT_COLOR -> "Text Colour"
        OcrInspectorSheetType.LINE_SPACING -> "Line Spacing"
        OcrInspectorSheetType.HIGHLIGHT -> "Text Highlight"
        OcrInspectorSheetType.NONE -> ""
    }

    val isTextColorSheet = sheetType == OcrInspectorSheetType.TEXT_COLOR

    Surface(
        shape = RoundedCornerShape(
            topStart = if (isTextColorSheet) 16.dp else 24.dp,
            topEnd = if (isTextColorSheet) 16.dp else 24.dp
        ),
        color = if (isTextColorSheet) TextColorDarkSurface else Color.White,
        shadowElevation = 18.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("premium_purple_bottom_sheet")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Sheet Header: Dark `#080808` with Back Arrow + "Text Colour" + Close 'X' for TEXT_COLOR,
            // or Purple `#3B00B3` Header for the other inspector sheets
            if (isTextColorSheet) {
                TextColourTopHeader(
                    title = "Text Colour",
                    onBackClick = onCloseSheet,
                    onCloseClick = onCloseSheet
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(StitchInspectorPurple)
                        .padding(horizontal = 20.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (sheetType == OcrInspectorSheetType.DRAWING) {
                            StitchLoopScribbleIcon(
                                color = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = sheetTitle,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color.White,
                            letterSpacing = 0.2.sp
                        )
                    }

                    IconButton(
                        onClick = onCloseSheet,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("close_purple_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close $sheetTitle Inspector",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Sheet Body Content
            when (sheetType) {
                OcrInspectorSheetType.DRAWING -> {
                    StitchPremiumDrawingSheetBody(
                        strokeColor = drawingStrokeColor,
                        fillColor = drawingFillColor,
                        thicknessPt = drawingThicknessPt,
                        opacityFraction = drawingOpacity,
                        onSelectStrokeColor = onSelectDrawingStrokeColor,
                        onSelectFillColor = onSelectDrawingFillColor,
                        onThicknessChanged = onSelectDrawingThickness,
                        onOpacityPercentChanged = onSelectDrawingOpacityPercent
                    )
                }
                OcrInspectorSheetType.FONT -> {
                    StitchPremiumFontSheetBody(
                        selectedFont = selectedBlock?.fontName ?: EditableTextBlock.FONT_ROBOTO,
                        onSelectFont = onSelectFont
                    )
                }
                OcrInspectorSheetType.SIZE -> {
                    StitchPremiumSizeSheetBody(
                        selectedSizeSp = selectedBlock?.fontSizeSp ?: 20f,
                        onSelectFontSize = onSelectFontSize
                    )
                }
                OcrInspectorSheetType.LINE_SPACING -> {
                    StitchPremiumLineSpacingSheetBody(
                        selectedLineSpacing = selectedBlock?.lineSpacing ?: 1.15f,
                        onSelectLineSpacing = onSelectLineSpacing
                    )
                }
                OcrInspectorSheetType.TEXT_COLOR -> {
                    TextColourEditorContent(
                        selectedColorInt = selectedBlock?.textColor ?: AndroidColor.parseColor("#000000"),
                        activeTab = textColorTab,
                        customColorInputMode = customColorInputMode,
                        recentlyUsedColors = recentlyUsedTextColors,
                        onTabSelected = onSelectTextColorTab,
                        onCustomColorModeSelected = onSelectCustomColorMode,
                        onColorSelected = onSelectTextColor,
                        allowTransparentSelection = true,
                        isTransparentSelected = selectedBlock?.isTransparentBackground ?: true,
                        onTransparentSelected = onToggleBackgroundTransparent
                    )
                }
                OcrInspectorSheetType.HIGHLIGHT -> {
                    StitchPremiumHighlightSheetBody(
                        highlightColor = highlightColor,
                        highlightOpacity = highlightOpacity,
                        onSelectHighlightColor = onSelectHighlightColor,
                        onSelectHighlightOpacityPercent = onSelectHighlightOpacityPercent
                    )
                }
                OcrInspectorSheetType.NONE -> Unit
            }
        }
    }
}

/**
 * 1. Stitch Premium `Drawing` Sheet Body (`code_00c91e97.html` & `screen_9005060e.png`):
 * - `Stroke Preview` card (`#F8FAFC`) with `"Stroke Preview"` + `"5 pt"` and inner white card with sine wave.
 * - `Stroke Colour` + `"Tap to select"` + 7 color swatches + 8th dashed custom color picker button.
 * - `Fill Colour` + `"None selected"` + red slash None circle + 6 pastel fill circles.
 * - `Thickness` + `"5 pt"` in `#3800B0` + purple `#3800B0` slider (`1..50 pt`) + `Opacity` slider.
 */
@Composable
private fun StitchPremiumDrawingSheetBody(
    strokeColor: Int,
    fillColor: Int?,
    thicknessPt: Float,
    opacityFraction: Float,
    onSelectStrokeColor: (Int) -> Unit,
    onSelectFillColor: (Int?) -> Unit,
    onThicknessChanged: (Float) -> Unit,
    onOpacityPercentChanged: (Float) -> Unit
) {
    var showCustomStrokeColors by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 320.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Stroke Preview Container (`bg-slate-50 border border-slate-200/80 rounded-2xl p-3`)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(StitchSlate50)
                .border(1.dp, StitchSlate200.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stroke Preview",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = StitchSlate400
                )
                Text(
                    text = "${thicknessPt.roundToInt()} pt",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = StitchSlate700
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // White inner wave card (`h-11 bg-white rounded-xl border border-slate-100`)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .border(1.dp, StitchSlate100, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .padding(horizontal = 14.dp)
                ) {
                    val w = size.width
                    val h = size.height
                    val cy = h / 2f
                    val wavePath = Path().apply {
                        moveTo(w * 0.02f, cy + h * 0.14f)
                        cubicTo(
                            w * 0.30f,
                            cy - h * 0.42f,
                            w * 0.65f,
                            cy + h * 0.52f,
                            w * 0.98f,
                            cy + h * 0.05f
                        )
                    }
                    drawPath(
                        path = wavePath,
                        color = Color(strokeColor).copy(alpha = opacityFraction.coerceIn(0.08f, 1f)),
                        style = Stroke(
                            width = (thicknessPt * 1.1f).coerceIn(2f, 26f).dp.toPx() * 0.38f,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
        }

        // Stroke Colour Swatch Row
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stroke Colour",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = StitchSlate800
                )
                Text(
                    text = "Tap to select",
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    color = StitchSlate400
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PdfEditorViewModel.PREMIUM_STROKE_SWATCHES.forEach { swatchInt ->
                    val isSelected = swatchInt == strokeColor
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { onSelectStrokeColor(swatchInt) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .border(2.dp, Color(swatchInt), CircleShape)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 26.dp else 30.dp)
                                .shadow(elevation = 1.dp, shape = CircleShape)
                                .clip(CircleShape)
                                .background(Color(swatchInt))
                        )
                    }
                }

                // 8th Dashed Custom Color Picker Button
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable { showCustomStrokeColors = !showCustomStrokeColors },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = StitchSlate400,
                            style = Stroke(
                                width = 1.4.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()))
                            )
                        )
                    }
                    Text(
                        text = "+",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = StitchSlate500
                    )
                }
            }

            AnimatedVisibility(visible = showCustomStrokeColors) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PdfEditorViewModel.DRAWING_STROKE_PALETTE.forEach { extraColor ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(extraColor))
                                .border(
                                    width = if (extraColor == strokeColor) 2.dp else 1.dp,
                                    color = if (extraColor == strokeColor) StitchSlate900 else Color.White,
                                    shape = CircleShape
                                )
                                .clickable { onSelectStrokeColor(extraColor) }
                        )
                    }
                }
            }
        }

        // Fill Colour Swatch Row
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Fill Colour",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = StitchSlate800
                )
                Text(
                    text = if (fillColor == null) {
                        "None selected"
                    } else {
                        String.format("#%06X", 0xFFFFFF and fillColor)
                    },
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    color = StitchSlate400
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Red diagonal-slash "None" button (`border-2 border-red-500`)
                val isNoneSelected = fillColor == null
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(
                            width = if (isNoneSelected) 2.dp else 1.2.dp,
                            color = if (isNoneSelected) Color(0xFFEF4444) else StitchSlate400,
                            shape = CircleShape
                        )
                        .clickable { onSelectFillColor(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawLine(
                            color = Color(0xFFEF4444),
                            start = Offset(size.width * 0.22f, size.height * 0.22f),
                            end = Offset(size.width * 0.78f, size.height * 0.78f),
                            strokeWidth = 1.8.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                PdfEditorViewModel.PREMIUM_FILL_SWATCHES.forEach { fillSwatch ->
                    val isSelected = fillColor == fillSwatch
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(fillSwatch))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) StitchAccentPurple else StitchSlate200,
                                shape = CircleShape
                            )
                            .clickable { onSelectFillColor(fillSwatch) }
                    )
                }
            }
        }

        // Thickness Slider (`1 pt` to `50 pt`, default `5 pt`)
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Thickness",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = StitchSlate800
                )
                Text(
                    text = "${thicknessPt.roundToInt()} pt",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = StitchAccentPurple
                )
            }
            Slider(
                value = thicknessPt,
                onValueChange = onThicknessChanged,
                valueRange = 1f..50f,
                colors = SliderDefaults.colors(
                    thumbColor = StitchAccentPurple,
                    activeTrackColor = StitchAccentPurple,
                    inactiveTrackColor = StitchSlate200
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("premium_drawing_thickness_slider")
            )
        }

        // Opacity Slider (`0%` to `100%`)
        val opacityPercent = (opacityFraction * 100f).roundToInt().coerceIn(0, 100)
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Opacity",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = StitchSlate800
                )
                Text(
                    text = "$opacityPercent %",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = StitchAccentPurple
                )
            }
            Slider(
                value = opacityFraction * 100f,
                onValueChange = onOpacityPercentChanged,
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = StitchAccentPurple,
                    activeTrackColor = StitchAccentPurple,
                    inactiveTrackColor = StitchSlate200
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("premium_drawing_opacity_slider")
            )
        }
    }
}

/**
 * 2. Stitch Premium `Font` Sheet Body:
 * Displays all 7 fonts (`Carrois Gothic SC`, `Courier`, `Cutive Mono`, `Dancing Script`,
 * `Droid Sans Mono`, `Helvetica`, `Roboto`), each rendered in its own typeface.
 */
@Composable
private fun StitchPremiumFontSheetBody(
    selectedFont: String,
    onSelectFont: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 280.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        EditableTextBlock.AVAILABLE_FONTS.forEach { fontName ->
            val isSelected = fontName == selectedFont
            val fontFamily = remember(fontName) {
                EditableTextBlock.resolveComposeFontFamily(fontName)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Color(0xFFF3E8FF) else Color.Transparent)
                    .clickable { onSelectFont(fontName) }
                    .padding(horizontal = 14.dp, vertical = 11.dp)
                    .testTag("font_option_${fontName.lowercase().replace(' ', '_')}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = fontName,
                    fontFamily = fontFamily,
                    fontSize = 15.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) StitchInspectorPurple else StitchSlate800
                )

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected Font",
                        tint = StitchInspectorPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * 3. Stitch Premium `Size` Sheet Body (`14` to `72` sp).
 */
@Composable
private fun StitchPremiumSizeSheetBody(
    selectedSizeSp: Float,
    onSelectFontSize: (Float) -> Unit
) {
    val presetSizes = remember { listOf(14, 16, 18, 20, 24, 28, 32, 36, 48, 60, 72) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Font Size (14 – 72 pt)",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = StitchSlate800
            )
            Text(
                text = "${selectedSizeSp.roundToInt()} pt",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = StitchInspectorPurple
            )
        }

        Slider(
            value = selectedSizeSp.coerceIn(14f, 72f),
            onValueChange = onSelectFontSize,
            valueRange = 14f..72f,
            colors = SliderDefaults.colors(
                thumbColor = StitchInspectorPurple,
                activeTrackColor = StitchInspectorPurple,
                inactiveTrackColor = StitchSlate200
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ocr_font_size_slider")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presetSizes.forEach { preset ->
                val isSelected = selectedSizeSp.roundToInt() == preset
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) StitchInspectorPurple else StitchSlate100)
                        .clickable { onSelectFontSize(preset.toFloat()) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("font_size_chip_$preset"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$preset",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (isSelected) Color.White else StitchSlate700
                    )
                }
            }
        }
    }
}

/**
 * 4. Stitch Premium `Line Spacing` Sheet Body (`1`, `1.15`, `1.5`, `2`).
 */
@Composable
private fun StitchPremiumLineSpacingSheetBody(
    selectedLineSpacing: Float,
    onSelectLineSpacing: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Select Line Spacing Multiplier",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            color = StitchSlate600
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EditableTextBlock.LINE_SPACING_OPTIONS.forEach { option ->
                val isSelected = kotlin.math.abs(selectedLineSpacing - option) < 0.03f
                val label = if (option == 1.0f || option == 2.0f) {
                    option.toInt().toString()
                } else {
                    option.toString()
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) StitchInspectorPurple else StitchSlate100)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) StitchInspectorPurple else StitchSlate200,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectLineSpacing(option) }
                        .testTag("line_spacing_option_$label"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isSelected) Color.White else StitchSlate800
                    )
                }
            }
        }
    }
}

// Text Colour UI components (`TextColourTopHeader`, `TextColourEditorContent`, `TextColourBottomSheet`)
// are defined in `TextColourBottomSheet.kt`.

/**
 * 6. Stitch Premium `Text Highlight` Sheet Body (5x2 Color Grid + Highlight Opacity Slider).
 */
@Composable
private fun StitchPremiumHighlightSheetBody(
    highlightColor: Int,
    highlightOpacity: Float,
    onSelectHighlightColor: (Int) -> Unit,
    onSelectHighlightOpacityPercent: (Float) -> Unit
) {
    val rows = PdfEditorViewModel.HIGHLIGHT_PALETTE_5X2.chunked(5)
    val opacityPercent = (highlightOpacity * 100f).roundToInt().coerceIn(0, 100)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowColors.forEach { swatchInt ->
                    val isSelected = swatchInt == highlightColor
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(swatchInt))
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) StitchSlate900 else StitchSlate200,
                                shape = CircleShape
                            )
                            .clickable { onSelectHighlightColor(swatchInt) }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Opacity",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = StitchSlate800
            )
            Text(
                text = "$opacityPercent %",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = StitchInspectorPurple
            )
        }

        Slider(
            value = highlightOpacity * 100f,
            onValueChange = onSelectHighlightOpacityPercent,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = StitchInspectorPurple,
                activeTrackColor = StitchInspectorPurple,
                inactiveTrackColor = StitchSlate200
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
