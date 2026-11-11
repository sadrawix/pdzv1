package com.example

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Active Free Plan annotation tool in the Top Toolbar.
 */
enum class FreeAnnotationTool {
    DRAWING,
    TEXT_HIGHLIGHT
}

/**
 * Active color picker popover inside the Free Drawing Bottom Sheet.
 */
enum class ActiveColorPickerType {
    NONE,
    STROKE_COLOR,
    FILL_COLOR
}

/**
 * Active purple `#3B00B3` BottomSheet inspector in the Premium OCR & Direct Text Editor.
 */
enum class OcrInspectorSheetType {
    NONE,
    DRAWING,
    FONT,
    SIZE,
    TEXT_COLOR,
    LINE_SPACING,
    HIGHLIGHT
}

/**
 * Active tab inside the "Text Colour" BottomSheet (`PALETTE` vs `CUSTOM`).
 */
enum class TextColorTab {
    PALETTE,
    CUSTOM
}

/**
 * Active color input mode inside the "CUSTOM" tab of the "Text Colour" sheet (`Hex`, `HSL`, `RGB`).
 */
enum class CustomColorInputMode {
    HEX,
    HSL,
    RGB
}

/**
 * ViewModel for both the Free Plan Annotation Editor (`FreeAnnotationEditor.kt`) and the
 * Premium OCR & Direct Text Editor (`PdfEditorScreen.kt`), connected to [PdfRepository],
 * [PdfRenderEngine], [OcrScannerEngine], and the [EditorAction] Undo/Redo command stack.
 */
class PdfEditorViewModel(
    private val documentId: Long,
    private val initialPageIndex: Int,
    private val initialLaunchMode: EditorLaunchMode,
    private val repository: PdfRepository,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    private val renderEngine = PdfRenderEngine()
    private val ocrScannerEngine = OcrScannerEngine()

    private val _document = MutableStateFlow<PdfDocumentEntity?>(null)
    val document: StateFlow<PdfDocumentEntity?> = _document.asStateFlow()

    private val _pageIndex = MutableStateFlow(initialPageIndex)
    val pageIndex: StateFlow<Int> = _pageIndex.asStateFlow()

    private var originalPageBitmap: Bitmap? = null

    private val _pageBitmap = MutableStateFlow<Bitmap?>(null)
    val pageBitmap: StateFlow<Bitmap?> = _pageBitmap.asStateFlow()

    // Deleted OCR text block redactions with dynamically sampled ambient background colors for PDFBox save
    private val _deletedRedactions = MutableStateFlow<List<DeletedRedaction>>(emptyList())
    val deletedRedactions: StateFlow<List<DeletedRedaction>> = _deletedRedactions.asStateFlow()

    private val _pageAspectRatio = MutableStateFlow(0.77f)
    val pageAspectRatio: StateFlow<Float> = _pageAspectRatio.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _launchMode = MutableStateFlow(initialLaunchMode)
    val launchMode: StateFlow<EditorLaunchMode> = _launchMode.asStateFlow()

    // Active Free Tool: DRAWING (default active in Stitch screenshot) or TEXT_HIGHLIGHT ('T')
    private val _activeTool = MutableStateFlow(FreeAnnotationTool.DRAWING)
    val activeTool: StateFlow<FreeAnnotationTool> = _activeTool.asStateFlow()

    // Free Editor bottom sheet visibility (true by default so the purple #3B00B3 inspector is shown)
    private val _isBottomSheetExpanded = MutableStateFlow(true)
    val isBottomSheetExpanded: StateFlow<Boolean> = _isBottomSheetExpanded.asStateFlow()

    private val _activeColorPicker = MutableStateFlow(ActiveColorPickerType.NONE)
    val activeColorPicker: StateFlow<ActiveColorPickerType> = _activeColorPicker.asStateFlow()

    // Drawing Tool Properties
    private val _drawingStrokeColor = MutableStateFlow(
        if (initialLaunchMode == EditorLaunchMode.PREMIUM_OCR_TEXT_EDIT) {
            PREMIUM_DRAWING_BLUE
        } else {
            DEFAULT_DRAWING_BLUE
        }
    )
    val drawingStrokeColor: StateFlow<Int> = _drawingStrokeColor.asStateFlow()

    private val _drawingFillColor = MutableStateFlow<Int?>(null)
    val drawingFillColor: StateFlow<Int?> = _drawingFillColor.asStateFlow()

    // Thickness slider: 1 pt to 50 pt, default 5 pt
    private val _drawingThicknessPt = MutableStateFlow(5f)
    val drawingThicknessPt: StateFlow<Float> = _drawingThicknessPt.asStateFlow()

    // Opacity slider: 0% to 100%, default 100% (1.0f)
    private val _drawingOpacity = MutableStateFlow(1.0f)
    val drawingOpacity: StateFlow<Float> = _drawingOpacity.asStateFlow()

    // Text Highlight ('T') Tool Properties (5x2 color grid + Opacity slider)
    private val _highlightColor = MutableStateFlow(HIGHLIGHT_PALETTE_5X2.first())
    val highlightColor: StateFlow<Int> = _highlightColor.asStateFlow()

    private val _highlightOpacity = MutableStateFlow(0.50f)
    val highlightOpacity: StateFlow<Float> = _highlightOpacity.asStateFlow()

    private val _highlightThicknessPt = MutableStateFlow(16f)
    val highlightThicknessPt: StateFlow<Float> = _highlightThicknessPt.asStateFlow()

    // Committed normalized strokes on the current page
    private val _strokes = MutableStateFlow<List<DrawingStroke>>(emptyList())
    val strokes: StateFlow<List<DrawingStroke>> = _strokes.asStateFlow()

    // Editable OCR text blocks for Premium OCR & Direct Text Edit mode
    private val _textBlocks = MutableStateFlow<List<EditableTextBlock>>(emptyList())
    val textBlocks: StateFlow<List<EditableTextBlock>> = _textBlocks.asStateFlow()

    // Currently selected OCR text block ID (dashed blue box with top-left blue resize handle)
    private val _selectedTextBlockId = MutableStateFlow<String?>(null)
    val selectedTextBlockId: StateFlow<String?> = _selectedTextBlockId.asStateFlow()
    val selectedBlockId: StateFlow<String?> get() = selectedTextBlockId

    // Whether the floating long-press ContextualToolbar pill is visible above/below the selected block
    private val _isToolbarVisible = MutableStateFlow(false)
    val isToolbarVisible: StateFlow<Boolean> = _isToolbarVisible.asStateFlow()

    // Whether the inline text field for the selected OCR text block is focused/open
    private val _isInlineEditingText = MutableStateFlow(false)
    val isInlineEditingText: StateFlow<Boolean> = _isInlineEditingText.asStateFlow()

    // Active purple #3B00B3 BottomSheet in Premium OCR Editor (defaults to DRAWING per Stitch upload)
    private val _activeOcrSheet = MutableStateFlow(OcrInspectorSheetType.DRAWING)
    val activeOcrSheet: StateFlow<OcrInspectorSheetType> = _activeOcrSheet.asStateFlow()

    // Active tab inside the Text Colour BottomSheet (PALETTE vs CUSTOM)
    private val _textColorTab = MutableStateFlow(TextColorTab.PALETTE)
    val textColorTab: StateFlow<TextColorTab> = _textColorTab.asStateFlow()

    // Active input mode inside the Text Colour CUSTOM tab (Hex, HSL, RGB — defaults to HSL)
    private val _customColorInputMode = MutableStateFlow(CustomColorInputMode.HSL)
    val customColorInputMode: StateFlow<CustomColorInputMode> = _customColorInputMode.asStateFlow()

    // Recently used text colours shown at the top of the PALETTE tab
    private val _recentlyUsedTextColors = MutableStateFlow(INITIAL_RECENT_TEXT_COLORS)
    val recentlyUsedTextColors: StateFlow<List<Int>> = _recentlyUsedTextColors.asStateFlow()

    // Undo / Redo Command Stacks
    private val _undoStack = MutableStateFlow<List<EditorAction>>(emptyList())
    val undoStack: StateFlow<List<EditorAction>> = _undoStack.asStateFlow()

    private val _redoStack = MutableStateFlow<List<EditorAction>>(emptyList())
    val redoStack: StateFlow<List<EditorAction>> = _redoStack.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val isPremiumUser: StateFlow<Boolean> = subscriptionManager.isPremiumUser

    init {
        loadDocumentAndPage()
    }

    private fun loadDocumentAndPage() {
        viewModelScope.launch {
            _isLoading.value = true
            val doc = repository.getDocumentById(documentId)
            _document.value = doc

            if (doc != null) {
                val file = File(doc.filePath)
                val count = renderEngine.openDocument(file)
                if (count > 0) {
                    val validPage = _pageIndex.value.coerceIn(0, count - 1)
                    _pageIndex.value = validPage
                    val bmp = renderEngine.renderPageHighRes(validPage, targetWidthPx = 1440)
                    originalPageBitmap = bmp?.copy(Bitmap.Config.ARGB_8888, false)
                    _pageBitmap.value = bmp?.copy(Bitmap.Config.ARGB_8888, true) ?: bmp
                    _deletedRedactions.value = emptyList()
                    if (bmp != null && bmp.height > 0) {
                        _pageAspectRatio.value = bmp.width.toFloat() / bmp.height.toFloat()
                    }

                    // Load cached OCR text blocks or run OcrScannerEngine on the page
                    val cachedOcr = OcrSessionCache.getBlocks(documentId, validPage)
                    if (!cachedOcr.isNullOrEmpty()) {
                        val normalizedCached = cachedOcr.map { block ->
                            block.copy(
                                backgroundColor = AndroidColor.TRANSPARENT,
                                isTransparentBackground = true,
                                isTransparent = true
                            )
                        }
                        _textBlocks.value = normalizedCached
                        val cachedRedactions = OcrSessionCache.getRedactions(documentId, validPage)
                        if (cachedRedactions.isNotEmpty()) {
                            _deletedRedactions.value = cachedRedactions
                            rebuildPageBitmapWithRedactions(cachedRedactions)
                        }
                    } else if (_launchMode.value == EditorLaunchMode.PREMIUM_OCR_TEXT_EDIT) {
                        val extractedBlocks = ocrScannerEngine.extractTextBlocks(
                            pageBitmap = bmp,
                            pageIndex = validPage,
                            fallbackPdfFile = file
                        ).map { block ->
                            block.copy(
                                backgroundColor = AndroidColor.TRANSPARENT,
                                isTransparentBackground = true,
                                isTransparent = true
                            )
                        }
                        _textBlocks.value = extractedBlocks
                        OcrSessionCache.putBlocks(documentId, validPage, extractedBlocks)
                        OcrSessionCache.putRedactions(documentId, validPage, emptyList())
                    }
                }
            }

            _isLoading.value = false
        }
    }

    /**
     * Switches between the Freehand Drawing tool (`FreeAnnotationTool.DRAWING`) and
     * the Text Highlight tool (`FreeAnnotationTool.TEXT_HIGHLIGHT`), automatically
     * opening the corresponding purple `#3B00B3` BottomSheet.
     */
    fun selectTool(tool: FreeAnnotationTool) {
        if (_activeTool.value == tool) {
            _isBottomSheetExpanded.update { !it }
        } else {
            _activeTool.value = tool
            _isBottomSheetExpanded.value = true
        }
        _activeColorPicker.value = ActiveColorPickerType.NONE
    }

    fun setBottomSheetExpanded(expanded: Boolean) {
        _isBottomSheetExpanded.value = expanded
        if (!expanded) {
            _activeColorPicker.value = ActiveColorPickerType.NONE
        }
    }

    fun toggleColorPicker(type: ActiveColorPickerType) {
        _activeColorPicker.update { current ->
            if (current == type) ActiveColorPickerType.NONE else type
        }
    }

    fun closeColorPicker() {
        _activeColorPicker.value = ActiveColorPickerType.NONE
    }

    // -------------------------------------------------------------------------------------
    // Premium OCR & Direct Text Block Selection, Long-Press Toolbar, Inline Editing & Formatting
    // -------------------------------------------------------------------------------------

    /**
     * Selects a dashed blue OCR text block on the canvas and transforms that exact block in place
     * into an active inline editable text field (`isModified = true`), erasing its original
     * baked-in text once (if `isOriginalBakedIn == true`) using the natural background color
     * sampled from the page Bitmap without spawning any duplicate overlay or white patch.
     */
    fun selectTextBlock(blockId: String, startInlineEditing: Boolean = true) {
        val wasAlreadySelected = _selectedTextBlockId.value == blockId
        _selectedTextBlockId.value = blockId
        _isToolbarVisible.value = false
        if (startInlineEditing) {
            _isInlineEditingText.value = true
            val targetBlock = _textBlocks.value.firstOrNull { it.id == blockId }
            if (targetBlock != null) {
                redactOriginalBakedInBlockOnce(targetBlock)
            }
            _textBlocks.update { list ->
                list.map { block ->
                    if (block.id == blockId) {
                        block.copy(
                            isModified = true,
                            isOriginalBakedIn = false,
                            backgroundColor = AndroidColor.TRANSPARENT,
                            isTransparentBackground = true,
                            isTransparent = true
                        )
                    } else {
                        block
                    }
                }
            }
            OcrSessionCache.putBlocks(documentId, _pageIndex.value, _textBlocks.value)
            // When inline editing starts, collapse the tall Drawing inspector if open so the
            // formatting toolbar sits cleanly above the keyboard or active formatting sheet
            if (!wasAlreadySelected && _activeOcrSheet.value == OcrInspectorSheetType.DRAWING) {
                _activeOcrSheet.value = OcrInspectorSheetType.NONE
            }
        }
    }

    /**
     * Triggered when the user long-presses an [EditableTextBlock] on the PDF canvas:
     * selects the block, highlights its active border, exits inline keyboard focus so the
     * floating [ContextualToolbar] pill is clearly visible above/below the bounding box,
     * and sets [isToolbarVisible] to `true`.
     */
    fun onLongPressTextBlock(blockId: String) {
        _selectedTextBlockId.value = blockId
        _isInlineEditingText.value = false
        _isToolbarVisible.value = true
    }

    /**
     * Dismisses the floating long-press [ContextualToolbar] when tapping outside the bounding box
     * or after executing a toolbar action.
     */
    fun dismissContextualToolbar() {
        _isToolbarVisible.value = false
    }

    /**
     * Samples the actual document background color directly from [bitmap] right at the corner
     * immediately outside the text glyphs of [originalNormalizedBox] — NEVER defaulting to
     * hardcoded `Color.WHITE`.
     */
    private fun sampleNaturalBackgroundColor(
        bitmap: Bitmap,
        originalNormalizedBox: RectF,
        textColor: Int = AndroidColor.BLACK
    ): Int {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= 0 || h <= 0) return AndroidColor.TRANSPARENT

        // Primary sample at top-left corner of the original bounding box
        val sampleX = (originalNormalizedBox.left * w).toInt().coerceIn(0, w - 1)
        val sampleY = (originalNormalizedBox.top * h).toInt().coerceIn(0, h - 1)
        val rightX = (originalNormalizedBox.right * w).toInt().coerceIn(0, w - 1)
        val bottomY = (originalNormalizedBox.bottom * h).toInt().coerceIn(0, h - 1)

        val outLeft = (sampleX - 2).coerceIn(0, w - 1)
        val outTop = (sampleY - 2).coerceIn(0, h - 1)
        val outRight = (rightX + 2).coerceIn(0, w - 1)
        val outBottom = (bottomY + 2).coerceIn(0, h - 1)

        // Sample the 4 corners immediately outside the text glyphs (never the interior glyphs)
        val cornerSamples = listOf(
            sampleX to sampleY,
            outLeft to outTop,
            rightX to sampleY,
            outRight to outTop,
            sampleX to bottomY,
            outLeft to outBottom,
            rightX to bottomY,
            outRight to outBottom
        )

        val textR = AndroidColor.red(textColor)
        val textG = AndroidColor.green(textColor)
        val textB = AndroidColor.blue(textColor)

        val counts = HashMap<Int, Int>(16)
        val representativeColor = HashMap<Int, Int>(16)

        for ((cx, cy) in cornerSamples) {
            val pixel = bitmap.getPixel(cx, cy)
            val r = AndroidColor.red(pixel)
            val g = AndroidColor.green(pixel)
            val b = AndroidColor.blue(pixel)
            val dr = r - textR
            val dg = g - textG
            val db = b - textB
            val distSqToInk = dr * dr + dg * dg + db * db
            val weight = if (distSqToInk > 1200) 3 else 1
            val key = ((r shr 2) shl 12) or ((g shr 2) shl 6) or (b shr 2)
            counts[key] = (counts[key] ?: 0) + weight
            if (!representativeColor.containsKey(key)) {
                representativeColor[key] = pixel
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

        return representativeColor[bestKey] ?: bitmap.getPixel(sampleX, sampleY)
    }

    /**
     * Paints [originalNormalizedBox] onto [targetBitmap] using [naturalBackgroundColor] with
     * `Paint.Style.FILL` and zero feathering — never using hardcoded white.
     */
    private fun paintNaturalBackgroundRectOnBitmap(
        targetBitmap: Bitmap,
        originalNormalizedBox: RectF,
        naturalBackgroundColor: Int
    ) {
        if (targetBitmap.isRecycled || targetBitmap.width <= 0 || targetBitmap.height <= 0) return
        val originalRectPx = CoordinateMapper.toScreenRect(
            normalizedBox = originalNormalizedBox,
            bitmapWidth = targetBitmap.width,
            bitmapHeight = targetBitmap.height,
            bleedPx = 0f
        )
        val canvas = android.graphics.Canvas(targetBitmap)
        val erasePaint = Paint().apply {
            color = naturalBackgroundColor
            style = Paint.Style.FILL
            isAntiAlias = false
        }
        canvas.drawRect(originalRectPx, erasePaint)
    }

    /**
     * Erases an original baked-in OCR text block (`block.isOriginalBakedIn == true`) ONCE at its
     * initial [EditableTextBlock.originalNormalizedBox] on [_pageBitmap] using the dynamically
     * sampled `naturalBackgroundColor` from [originalPageBitmap], records the redaction in
     * [_deletedRedactions] for PDFBox persistence, and marks `isOriginalBakedIn = false` on the
     * block so any subsequent move or delete treats it strictly as a transparent overlay.
     */
    private fun redactOriginalBakedInBlockOnce(block: EditableTextBlock) {
        if (!block.isOriginalBakedIn) return
        val origBox = RectF(block.originalNormalizedBox)

        if (_deletedRedactions.value.none { it.blockId == block.id }) {
            val sourceBitmap = originalPageBitmap?.takeIf { !it.isRecycled }
                ?: _pageBitmap.value?.takeIf { !it.isRecycled }
                ?: return

            val naturalBackgroundColor = sampleNaturalBackgroundColor(
                bitmap = sourceBitmap,
                originalNormalizedBox = origBox,
                textColor = block.textColor
            )

            val currentBitmap = _pageBitmap.value
            if (currentBitmap != null && !currentBitmap.isRecycled) {
                val mutableBitmap = currentBitmap.copy(Bitmap.Config.ARGB_8888, true)
                paintNaturalBackgroundRectOnBitmap(
                    targetBitmap = mutableBitmap,
                    originalNormalizedBox = origBox,
                    naturalBackgroundColor = naturalBackgroundColor
                )
                _pageBitmap.value = mutableBitmap
            }

            _deletedRedactions.update { list ->
                if (list.any { it.blockId == block.id }) {
                    list
                } else {
                    list + DeletedRedaction(
                        blockId = block.id,
                        normalizedBox = origBox,
                        ambientBackgroundColor = naturalBackgroundColor
                    )
                }
            }
            OcrSessionCache.putRedactions(documentId, _pageIndex.value, _deletedRedactions.value)
        }

        _textBlocks.update { list ->
            list.map { item ->
                if (item.id == block.id && item.isOriginalBakedIn) {
                    item.copy(isOriginalBakedIn = false)
                } else {
                    item
                }
            }
        }
    }

    /**
     * Rebuilds [_pageBitmap] from [originalPageBitmap] and re-applies all active [DeletedRedaction]
     * items using their sampled `ambientBackgroundColor` (used when undoing or redoing a text block
     * deletion or edit).
     */
    private fun rebuildPageBitmapWithRedactions(redactions: List<DeletedRedaction>) {
        val base = originalPageBitmap ?: return
        if (base.isRecycled) return
        val mutableBitmap = base.copy(Bitmap.Config.ARGB_8888, true)
        for (redaction in redactions) {
            paintNaturalBackgroundRectOnBitmap(
                targetBitmap = mutableBitmap,
                originalNormalizedBox = redaction.normalizedBox,
                naturalBackgroundColor = redaction.ambientBackgroundColor
            )
        }
        _pageBitmap.value = mutableBitmap
        OcrSessionCache.putRedactions(documentId, _pageIndex.value, redactions)
    }

    /**
     * **1. Cut Action**: Copies the selected text block's text to the Android clipboard.
     * - If the block was untouched original baked-in OCR text (`isOriginalBakedIn == true`),
     *   samples the natural background color at `originalNormalizedBox` and erases it once.
     * - If the block is an overlay (added, duplicated, or already moved/edited), removes it
     *   directly from [_textBlocks] without painting any rectangle on the page Bitmap.
     */
    fun cutSelectedTextBlock(onCopyToClipboard: ((String) -> Unit)? = null) {
        val id = _selectedTextBlockId.value ?: getSelectedTextBlock()?.id ?: return
        val block = _textBlocks.value.find { it.id == id } ?: return
        onCopyToClipboard?.invoke(block.currentText)

        if (block.isOriginalBakedIn) {
            redactOriginalBakedInBlockOnce(block)
        }

        val currentBlock = _textBlocks.value.find { it.id == id } ?: block.copy(isOriginalBakedIn = false)
        executeAction(
            EditorAction.RemoveTextBlock(
                pageIndex = _pageIndex.value,
                block = currentBlock
            )
        )
        OcrSessionCache.putBlocks(documentId, _pageIndex.value, _textBlocks.value)
        _selectedTextBlockId.value = null
        _isToolbarVisible.value = false
        _isInlineEditingText.value = false
        _snackbarMessage.value = "Text block cut to clipboard"
    }

    /**
     * **2. Copy Action**: Copies the selected text block's text to the Android clipboard and hides
     * the contextual toolbar.
     */
    fun copySelectedTextBlock(onCopyToClipboard: ((String) -> Unit)? = null) {
        val current = getSelectedTextBlock() ?: return
        onCopyToClipboard?.invoke(current.currentText)
        _isToolbarVisible.value = false
        _snackbarMessage.value = "Text copied to clipboard"
    }

    /**
     * **3. Delete Action**:
     * - For text overlays (annotations, added, duplicated, or already moved/edited text blocks where
     *   `isOriginalBakedIn == false`): simply removes the block from [_textBlocks]
     *   (`_textBlocks.update { it.filterNot { b -> b.id == id } }`) without drawing any rectangle
     *   on the page Bitmap.
     * - For untouched original baked-in OCR text (`isOriginalBakedIn == true`): samples the natural
     *   background color right at the corner (`sampleX, sampleY`) of `originalNormalizedBox` and
     *   paints the original rect with `naturalBackgroundColor` (never hardcoded White), then removes
     *   the block from [_textBlocks].
     */
    fun deleteSelectedBlock() {
        val id = _selectedTextBlockId.value ?: getSelectedTextBlock()?.id ?: return
        val block = _textBlocks.value.find { it.id == id } ?: return

        if (block.isOriginalBakedIn) {
            redactOriginalBakedInBlockOnce(block)
        }

        val currentBlock = _textBlocks.value.find { it.id == id } ?: block.copy(isOriginalBakedIn = false)
        executeAction(
            EditorAction.RemoveTextBlock(
                pageIndex = _pageIndex.value,
                block = currentBlock
            )
        )
        OcrSessionCache.putBlocks(documentId, _pageIndex.value, _textBlocks.value)
        _selectedTextBlockId.value = null
        _isToolbarVisible.value = false
        _isInlineEditingText.value = false
        _snackbarMessage.value = "Text block deleted"
    }

    fun deleteSelectedTextBlock() {
        deleteSelectedBlock()
    }

    /**
     * **4. Duplicate / Add Action**: Creates a clone of the selected [EditableTextBlock] with a
     * slight normalized offset, a new unique ID, and `isOriginalBakedIn = false` so moving or
     * deleting it later simply updates or removes it in memory without painting any rectangle on
     * the page Bitmap.
     */
    fun duplicateSelectedTextBlock() {
        val current = getSelectedTextBlock() ?: return
        val srcBox = current.normalizedBox
        val width = srcBox.width().coerceAtLeast(0.18f)
        val height = srcBox.height().coerceAtLeast(0.06f)

        val offsetX = 0.025f
        val offsetY = 0.045f
        val newLeft = (srcBox.left + offsetX).coerceIn(0.02f, (0.98f - width).coerceAtLeast(0.02f))
        val newTop = (srcBox.top + offsetY).coerceIn(0.02f, (0.98f - height).coerceAtLeast(0.02f))
        val clonedBox = RectF(
            newLeft,
            newTop,
            (newLeft + width).coerceAtMost(0.98f),
            (newTop + height).coerceAtMost(0.98f)
        )

        val clonedBlock = current.copy(
            id = java.util.UUID.randomUUID().toString(),
            normalizedBox = clonedBox,
            originalNormalizedBox = RectF(clonedBox),
            backgroundColor = AndroidColor.TRANSPARENT,
            isTransparentBackground = true,
            isTransparent = true,
            isModified = true,
            isOriginalBakedIn = false
        )

        executeAction(
            EditorAction.AddTextBlock(
                pageIndex = _pageIndex.value,
                block = clonedBlock
            )
        )
        OcrSessionCache.putBlocks(documentId, _pageIndex.value, _textBlocks.value)
        _selectedTextBlockId.value = clonedBlock.id
        _isToolbarVisible.value = false
        _snackbarMessage.value = "Text block duplicated"
    }

    /**
     * **5. Edit Action**: Dismisses the floating contextual pill toolbar and opens the inline
     * text editor and keyboard formatting bar for the selected [EditableTextBlock].
     */
    fun editSelectedTextBlock() {
        val current = getSelectedTextBlock() ?: return
        selectTextBlock(current.id, startInlineEditing = true)
    }

    /**
     * **6. Eraser / Clear Action**: Resets `isModified = false` and clears custom styles (restoring
     * the original OCR text, original coordinates, and default styling if the block was modified),
     * or clears the text content if unmodified.
     */
    fun clearOrResetSelectedTextBlock() {
        val current = getSelectedTextBlock() ?: return
        val hadOriginalRedaction = _deletedRedactions.value.any { it.blockId == current.id }
        val updated = if (current.isModified) {
            current.copy(
                currentText = current.originalText,
                normalizedBox = RectF(current.originalNormalizedBox),
                fontName = EditableTextBlock.FONT_ROBOTO,
                backgroundColor = AndroidColor.TRANSPARENT,
                isTransparentBackground = true,
                isTransparent = true,
                isBold = false,
                isItalic = false,
                alignment = Paint.Align.LEFT,
                lineSpacing = 1.15f,
                isModified = false,
                isOriginalBakedIn = hadOriginalRedaction || current.isOriginalBakedIn
            )
        } else {
            redactOriginalBakedInBlockOnce(current)
            current.copy(
                currentText = "",
                backgroundColor = AndroidColor.TRANSPARENT,
                isTransparentBackground = true,
                isTransparent = true,
                isModified = true,
                isOriginalBakedIn = false
            )
        }
        updateTextBlock(current, updated)
        _isToolbarVisible.value = false
        _isInlineEditingText.value = false
        _snackbarMessage.value = if (updated.isModified) "Text cleared" else "Restored original text & styles"
    }

    fun setInlineEditingText(editing: Boolean) {
        _isInlineEditingText.value = editing
        if (editing) {
            _isToolbarVisible.value = false
        }
    }

    /**
     * Opens or toggles one of the purple `#3B00B3` BottomSheets in the Premium OCR Editor:
     * `FONT` (`TT`), `SIZE` (`AA`), `TEXT_COLOR` (Color circle), `LINE_SPACING`, `HIGHLIGHT`, or `DRAWING`.
     */
    fun toggleOcrSheet(sheetType: OcrInspectorSheetType) {
        _activeOcrSheet.update { current ->
            if (current == sheetType) OcrInspectorSheetType.NONE else sheetType
        }
        if (sheetType == OcrInspectorSheetType.HIGHLIGHT) {
            _activeTool.value = FreeAnnotationTool.TEXT_HIGHLIGHT
        } else if (sheetType == OcrInspectorSheetType.DRAWING) {
            _activeTool.value = FreeAnnotationTool.DRAWING
        }
    }

    fun closeOcrSheet() {
        _activeOcrSheet.value = OcrInspectorSheetType.NONE
    }

    fun setTextColorTab(tab: TextColorTab) {
        _textColorTab.value = tab
    }

    fun setCustomColorInputMode(mode: CustomColorInputMode) {
        _customColorInputMode.value = mode
    }

    fun getSelectedTextBlock(): EditableTextBlock? {
        val id = _selectedTextBlockId.value
        val blocks = _textBlocks.value
        return blocks.firstOrNull { it.id == id } ?: blocks.firstOrNull()
    }

    /**
     * Finds the specific [EditableTextBlock] by its unique [blockId] in [_textBlocks],
     * updates its `currentText` to [newText], and sets `isModified = true` on that exact
     * block in place without creating a new item or duplicating objects.
     */
    fun updateTextBlockText(blockId: String, newText: String) {
        val current = _textBlocks.value.firstOrNull { it.id == blockId } ?: return
        if (current.currentText == newText && current.isModified) return
        if (current.isOriginalBakedIn) {
            redactOriginalBakedInBlockOnce(current)
        }
        val latest = _textBlocks.value.firstOrNull { it.id == blockId } ?: current
        val updated = latest.copy(
            currentText = newText,
            isModified = true,
            isOriginalBakedIn = false,
            backgroundColor = AndroidColor.TRANSPARENT,
            isTransparentBackground = true,
            isTransparent = true
        )
        updateTextBlock(current, updated)
    }

    /**
     * Updates the text content of the currently selected [EditableTextBlock] in real time,
     * marking `isModified = true`.
     */
    fun updateSelectedBlockText(newText: String) {
        val current = getSelectedTextBlock() ?: return
        updateTextBlockText(current.id, newText)
    }

    private var dragStartBlockSnapshot: EditableTextBlock? = null

    /**
     * Called when the user begins dragging [blockId] on the canvas:
     * - Sets [blockId] as the active selected text block.
     * - Captures its pre-drag snapshot for Undo/Redo on drag completion.
     * - If the block was untouched original baked-in OCR text (`isOriginalBakedIn == true`),
     *   erases its original position (`originalNormalizedBox`) ONCE using `naturalBackgroundColor`
     *   and marks `isOriginalBakedIn = false`.
     * - For text overlays (`isOriginalBakedIn == false`), does NOT touch the page Bitmap at all.
     */
    fun onStartMoveTextBlock(blockId: String) {
        val current = _textBlocks.value.firstOrNull { it.id == blockId } ?: return
        _selectedTextBlockId.value = blockId
        _isInlineEditingText.value = false
        dragStartBlockSnapshot = current.copy(
            normalizedBox = RectF(current.normalizedBox),
            originalNormalizedBox = RectF(current.originalNormalizedBox)
        )
        if (current.isOriginalBakedIn) {
            redactOriginalBakedInBlockOnce(current)
        }
    }

    /**
     * Moves [blockId] in real time by ([deltaNormalizedX], [deltaNormalizedY]) in normalized page
     * space (`0.0f..1.0f`), clamping the bounding box within the PDF page boundaries
     * (`0.0f <= left < right <= 1.0f` and `0.0f <= top < bottom <= 1.0f`).
     *
     * Simply updates `block.normalizedBox` coordinates in [_textBlocks] without drawing any
     * whiteout rectangle at old or intermediate coordinates so the Compose Canvas re-renders the
     * text at the new position and leaves the old position 100% transparent and clean.
     */
    fun moveTextBlock(
        blockId: String,
        deltaNormalizedX: Float,
        deltaNormalizedY: Float
    ) {
        val current = _textBlocks.value.firstOrNull { it.id == blockId } ?: return
        if (_selectedTextBlockId.value != blockId) {
            _selectedTextBlockId.value = blockId
        }
        if (dragStartBlockSnapshot?.id != blockId) {
            dragStartBlockSnapshot = current.copy(
                normalizedBox = RectF(current.normalizedBox),
                originalNormalizedBox = RectF(current.originalNormalizedBox)
            )
        }
        if (current.isOriginalBakedIn) {
            redactOriginalBakedInBlockOnce(current)
        }

        val latest = _textBlocks.value.firstOrNull { it.id == blockId } ?: current
        val clampedBox = CoordinateMapper.shiftAndClampNormalizedBox(
            normalizedBox = latest.normalizedBox,
            deltaNormalizedX = deltaNormalizedX,
            deltaNormalizedY = deltaNormalizedY
        )

        val updated = latest.copy(
            normalizedBox = clampedBox,
            isModified = true,
            isOriginalBakedIn = false,
            backgroundColor = AndroidColor.TRANSPARENT,
            isTransparentBackground = true,
            isTransparent = true
        )

        _textBlocks.update { list ->
            list.map { if (it.id == blockId) updated else it }
        }
    }

    /**
     * Called when a drag gesture on [blockId] ends or cancels: commits the move onto the Undo stack
     * and persists the updated blocks in [OcrSessionCache].
     */
    fun commitMoveTextBlock(blockId: String) {
        val finalBlock = _textBlocks.value.firstOrNull { it.id == blockId }
        val startSnapshot = dragStartBlockSnapshot
        dragStartBlockSnapshot = null

        if (finalBlock != null) {
            if (startSnapshot != null && startSnapshot.id == blockId && startSnapshot.normalizedBox != finalBlock.normalizedBox) {
                _undoStack.update {
                    it + EditorAction.UpdateTextBlock(
                        pageIndex = _pageIndex.value,
                        previousBlock = startSnapshot,
                        updatedBlock = finalBlock
                    )
                }
                _redoStack.value = emptyList()
            }
            OcrSessionCache.putBlocks(documentId, _pageIndex.value, _textBlocks.value)
        }
    }

    /**
     * Resizes the selected [EditableTextBlock]'s normalized bounding box (`0.0f..1.0f`) when
     * the user drags the top-left blue circular resize handle (`#2563EB`).
     */
    fun resizeSelectedBlockFromTopLeft(deltaNormX: Float, deltaNormY: Float) {
        val current = getSelectedTextBlock() ?: return
        if (current.isOriginalBakedIn) {
            redactOriginalBakedInBlockOnce(current)
        }
        val latest = _textBlocks.value.firstOrNull { it.id == current.id } ?: current
        val box = latest.normalizedBox
        val minWidth = 0.22f
        val minHeight = 0.07f

        val newLeft = (box.left + deltaNormX).coerceIn(0.02f, (box.right - minWidth).coerceAtLeast(0.02f))
        val newTop = (box.top + deltaNormY).coerceIn(0.03f, (box.bottom - minHeight).coerceAtLeast(0.03f))
        val updatedBox = RectF(newLeft, newTop, box.right, box.bottom)

        val updated = latest.copy(
            normalizedBox = updatedBox,
            isModified = true,
            isOriginalBakedIn = false,
            backgroundColor = AndroidColor.TRANSPARENT,
            isTransparentBackground = true,
            isTransparent = true
        )
        _textBlocks.update { list ->
            list.map { if (it.id == updated.id) updated else it }
        }
        OcrSessionCache.putBlocks(documentId, _pageIndex.value, _textBlocks.value)
    }

    /**
     * Updates the selected text block's Font (`Carrois Gothic SC`, `Courier`, `Cutive Mono`,
     * `Dancing Script`, `Droid Sans Mono`, `Helvetica`, `Roboto`).
     */
    fun setSelectedBlockFont(fontName: String) {
        val current = getSelectedTextBlock() ?: return
        val updated = current.copy(
            fontName = fontName,
            isModified = true
        )
        updateTextBlock(current, updated)
    }

    /**
     * Updates the selected text block's Font Size (`14` to `72` sp).
     */
    fun setSelectedBlockFontSize(sizeSp: Float) {
        val current = getSelectedTextBlock() ?: return
        val clamped = sizeSp.coerceIn(14f, 72f)
        val updated = current.copy(
            fontSizeSp = clamped,
            isModified = true
        )
        updateTextBlock(current, updated)
    }

    /**
     * Records [colorInt] at the front of `Recently Used` colours (up to 8 swatches).
     */
    fun recordRecentColor(colorInt: Int) {
        _recentlyUsedTextColors.update { currentList ->
            if (currentList.firstOrNull() == colorInt) {
                currentList
            } else {
                (listOf(colorInt) + currentList.filterNot { it == colorInt }).take(8)
            }
        }
    }

    /**
     * Updates the selected text block's Text Colour (`Palette` or `Custom` Hex/HSL/RGB)
     * and records the colour in `Recently Used`. If no text block is present, also updates
     * the active annotation stroke colour in real time.
     */
    fun setSelectedBlockTextColor(colorInt: Int, recordInRecents: Boolean = true) {
        if (recordInRecents) {
            recordRecentColor(colorInt)
        }
        val current = getSelectedTextBlock()
        if (current != null) {
            val updated = current.copy(
                textColor = colorInt,
                isModified = true
            )
            updateTextBlock(current, updated)
        } else {
            _drawingStrokeColor.value = colorInt
            syncLatestDrawingStrokeIfDemo()
        }
    }

    /**
     * Sets whether the selected [EditableTextBlock]'s background is fully transparent
     * (`isTransparentBackground = true`, `isTransparent = true`, `backgroundColor = Color.TRANSPARENT`)
     * or filled with [backgroundColor].
     */
    fun setSelectedBlockBackgroundTransparent(
        isTransparent: Boolean = true,
        backgroundColor: Int? = null
    ) {
        val current = getSelectedTextBlock() ?: return
        val resolvedBg = if (isTransparent) {
            AndroidColor.TRANSPARENT
        } else {
            backgroundColor ?: if (current.backgroundColor != AndroidColor.TRANSPARENT) {
                current.backgroundColor
            } else {
                AndroidColor.WHITE
            }
        }
        val updated = current.copy(
            backgroundColor = resolvedBg,
            isTransparentBackground = isTransparent,
            isTransparent = isTransparent,
            isModified = true
        )
        updateTextBlock(current, updated)
    }

    /**
     * Toggles the selected [EditableTextBlock]'s background between fully Transparent
     * (`Color.Transparent`, `isTransparentBackground = true`) and Opaque (`isTransparentBackground = false`).
     */
    fun toggleSelectedBlockBackgroundTransparency() {
        val current = getSelectedTextBlock() ?: return
        val currentlyTransparent = current.isTransparentBackground ||
            current.isTransparent ||
            current.backgroundColor == AndroidColor.TRANSPARENT
        setSelectedBlockBackgroundTransparent(
            isTransparent = !currentlyTransparent
        )
    }

    /**
     * Toggles Bold (`B`) on the selected OCR text block.
     */
    fun toggleSelectedBlockBold() {
        val current = getSelectedTextBlock() ?: return
        val updated = current.copy(
            isBold = !current.isBold,
            isModified = true
        )
        updateTextBlock(current, updated)
    }

    /**
     * Toggles Italic (`I`) on the selected OCR text block.
     */
    fun toggleSelectedBlockItalic() {
        val current = getSelectedTextBlock() ?: return
        val updated = current.copy(
            isItalic = !current.isItalic,
            isModified = true
        )
        updateTextBlock(current, updated)
    }

    /**
     * Cycles or sets the text alignment (`LEFT`, `CENTER`, `RIGHT`) on the selected OCR text block.
     */
    fun cycleOrSetSelectedBlockAlignment(targetAlign: Paint.Align? = null) {
        val current = getSelectedTextBlock() ?: return
        val nextAlign = targetAlign ?: when (current.alignment) {
            Paint.Align.LEFT -> Paint.Align.CENTER
            Paint.Align.CENTER -> Paint.Align.RIGHT
            Paint.Align.RIGHT -> Paint.Align.LEFT
        }
        val updated = current.copy(
            alignment = nextAlign,
            isModified = true
        )
        updateTextBlock(current, updated)
    }

    /**
     * Updates the selected text block's Line Spacing (`1`, `1.15`, `1.5`, `2`).
     */
    fun setSelectedBlockLineSpacing(lineSpacing: Float) {
        val current = getSelectedTextBlock() ?: return
        val updated = current.copy(
            lineSpacing = lineSpacing,
            isModified = true
        )
        updateTextBlock(current, updated)
    }

    // -------------------------------------------------------------------------------------
    // Freehand Drawing & Text Highlight Properties
    // -------------------------------------------------------------------------------------

    fun setDrawingStrokeColor(
        color: Int,
        closePicker: Boolean = false,
        recordInRecents: Boolean = true
    ) {
        if (recordInRecents) {
            recordRecentColor(color)
        }
        _drawingStrokeColor.value = color
        if (closePicker) {
            _activeColorPicker.value = ActiveColorPickerType.NONE
        }
        syncLatestDrawingStrokeIfDemo()
    }

    fun setDrawingFillColor(
        color: Int?,
        closePicker: Boolean = false,
        recordInRecents: Boolean = true
    ) {
        if (color != null && recordInRecents) {
            recordRecentColor(color)
        }
        _drawingFillColor.value = color
        if (closePicker) {
            _activeColorPicker.value = ActiveColorPickerType.NONE
        }
        syncLatestDrawingStrokeIfDemo()
    }

    fun setDrawingThicknessPt(thicknessPt: Float) {
        val clamped = thicknessPt.coerceIn(1f, 50f)
        _drawingThicknessPt.value = clamped
        syncLatestDrawingStrokeIfDemo()
    }

    fun setDrawingOpacityPercent(percent: Float) {
        val fraction = (percent.coerceIn(0f, 100f)) / 100f
        _drawingOpacity.value = fraction
        syncLatestDrawingStrokeIfDemo()
    }

    fun setHighlightColor(color: Int) {
        recordRecentColor(color)
        _highlightColor.value = color
    }

    fun setHighlightOpacityPercent(percent: Float) {
        val fraction = (percent.coerceIn(0f, 100f)) / 100f
        _highlightOpacity.value = fraction
    }

    private fun syncLatestDrawingStrokeIfDemo() {
        _strokes.update { list ->
            if (list.isEmpty()) return@update list
            val lastIdx = list.indexOfLast { !it.isHighlight }
            if (lastIdx < 0) return@update list
            val target = list[lastIdx]
            val updated = target.copy(
                strokeColor = _drawingStrokeColor.value,
                fillColor = _drawingFillColor.value,
                thicknessPt = _drawingThicknessPt.value,
                opacity = _drawingOpacity.value
            )
            list.toMutableList().apply { set(lastIdx, updated) }
        }
    }

    /**
     * Commits a newly drawn normalized freehand or highlighter stroke onto the page and
     * pushes an [EditorAction.AddStroke] onto the Undo stack.
     */
    fun commitNormalizedStroke(normalizedPoints: List<Offset>) {
        if (normalizedPoints.size < 2) return
        val isHighlight = _activeTool.value == FreeAnnotationTool.TEXT_HIGHLIGHT ||
            _activeOcrSheet.value == OcrInspectorSheetType.HIGHLIGHT

        val stroke = DrawingStroke(
            normalizedPoints = normalizedPoints,
            strokeColor = if (isHighlight) _highlightColor.value else _drawingStrokeColor.value,
            fillColor = if (isHighlight) null else _drawingFillColor.value,
            thicknessPt = if (isHighlight) _highlightThicknessPt.value else _drawingThicknessPt.value,
            opacity = if (isHighlight) _highlightOpacity.value else _drawingOpacity.value,
            isHighlight = isHighlight,
            pageIndex = _pageIndex.value
        )

        executeAction(
            EditorAction.AddStroke(
                pageIndex = _pageIndex.value,
                stroke = stroke
            )
        )
    }

    /**
     * Updates an OCR [EditableTextBlock] and records the mutation on the Undo stack.
     */
    fun updateTextBlock(previousBlock: EditableTextBlock, updatedBlock: EditableTextBlock) {
        executeAction(
            EditorAction.UpdateTextBlock(
                pageIndex = _pageIndex.value,
                previousBlock = previousBlock,
                updatedBlock = updatedBlock
            )
        )
        OcrSessionCache.putBlocks(documentId, _pageIndex.value, _textBlocks.value)
    }

    fun undo() {
        val currentUndo = _undoStack.value
        if (currentUndo.isEmpty()) return
        val action = currentUndo.last()
        _undoStack.value = currentUndo.dropLast(1)
        applyActionStateOnly(action.invert())
        _redoStack.update { it + action }
        OcrSessionCache.putBlocks(documentId, _pageIndex.value, _textBlocks.value)
    }

    fun redo() {
        val currentRedo = _redoStack.value
        if (currentRedo.isEmpty()) return
        val action = currentRedo.last()
        _redoStack.value = currentRedo.dropLast(1)
        applyActionStateOnly(action)
        _undoStack.update { it + action }
        OcrSessionCache.putBlocks(documentId, _pageIndex.value, _textBlocks.value)
    }

    fun clearAllStrokes() {
        val currentStrokes = _strokes.value
        if (currentStrokes.isEmpty()) return
        executeAction(
            EditorAction.ClearStrokes(
                pageIndex = _pageIndex.value,
                removedStrokes = currentStrokes
            )
        )
    }

    private fun executeAction(action: EditorAction) {
        applyActionStateOnly(action)
        _undoStack.update { it + action }
        _redoStack.value = emptyList()
    }

    private fun applyActionStateOnly(action: EditorAction) {
        when (action) {
            is EditorAction.AddStroke -> {
                _strokes.update { it + action.stroke }
            }
            is EditorAction.RemoveStroke -> {
                _strokes.update { list -> list.filterNot { it.id == action.stroke.id } }
            }
            is EditorAction.ClearStrokes -> {
                _strokes.value = emptyList()
            }
            is EditorAction.RestoreStrokes -> {
                _strokes.value = action.restoredStrokes
            }
            is EditorAction.UpdateTextBlock -> {
                val updated = action.updatedBlock
                if (
                    updated.isModified &&
                    (action.previousBlock.isOriginalBakedIn || updated.isOriginalBakedIn) &&
                    _deletedRedactions.value.none { it.blockId == updated.id }
                ) {
                    redactOriginalBakedInBlockOnce(
                        action.previousBlock.copy(isOriginalBakedIn = true)
                    )
                } else if (!updated.isModified && updated.isOriginalBakedIn) {
                    val currentRedactions = _deletedRedactions.value
                    val matchIndex = currentRedactions.indexOfFirst { it.blockId == updated.id }
                    if (matchIndex >= 0) {
                        val updatedRedactions = currentRedactions.toMutableList().apply {
                            removeAt(matchIndex)
                        }
                        _deletedRedactions.value = updatedRedactions
                        rebuildPageBitmapWithRedactions(updatedRedactions)
                    }
                }
                val normalizedUpdated = if (updated.isModified && _deletedRedactions.value.any { it.blockId == updated.id }) {
                    updated.copy(isOriginalBakedIn = false)
                } else {
                    updated
                }
                _textBlocks.update { list ->
                    list.map { if (it.id == normalizedUpdated.id) normalizedUpdated else it }
                }
            }
            is EditorAction.AddTextBlock -> {
                _textBlocks.update { it + action.block }
                // Only remove the original redaction if the restored block is an unmodified original block
                // returning to its exact initial scanned coordinates.
                if (action.block.isOriginalBakedIn && !action.block.isModified) {
                    val currentRedactions = _deletedRedactions.value
                    val matchIndex = currentRedactions.indexOfFirst {
                        it.blockId == action.block.id || it.normalizedBox == action.block.originalNormalizedBox
                    }
                    if (matchIndex >= 0) {
                        val updatedRedactions = currentRedactions.toMutableList().apply {
                            removeAt(matchIndex)
                        }
                        _deletedRedactions.value = updatedRedactions
                        rebuildPageBitmapWithRedactions(updatedRedactions)
                    }
                }
            }
            is EditorAction.RemoveTextBlock -> {
                val existed = _textBlocks.value.any { it.id == action.block.id }
                if (
                    existed &&
                    action.block.isOriginalBakedIn &&
                    _deletedRedactions.value.none { it.blockId == action.block.id }
                ) {
                    redactOriginalBakedInBlockOnce(action.block)
                }
                _textBlocks.update { list -> list.filterNot { it.id == action.block.id } }
            }
            is EditorAction.BatchAction -> {
                action.actions.forEach { applyActionStateOnly(it) }
            }
        }
    }

    /**
     * Saves all drawn strokes, highlights, deleted redactions, and modified OCR text blocks into
     * the underlying 72-DPI PDF file using [PdfBoxSaveEngine] (`PDFBox-Android`), refreshes the
     * Room Database cover thumbnail, and returns to the PDF Viewer screen.
     */
    fun saveAnnotationsToPdf(onSaved: () -> Unit = {}) {
        val doc = _document.value ?: return
        viewModelScope.launch {
            _isSaving.value = true
            val success = repository.saveEngine.flattenAndSavePageEdits(
                pdfFile = File(doc.filePath),
                pageIndex = _pageIndex.value,
                strokes = _strokes.value,
                textBlocks = _textBlocks.value,
                deletedRedactions = _deletedRedactions.value
            )
            if (success) {
                OcrSessionCache.clearForDocument(doc.id)
                repository.refreshThumbnailAndMetadata(doc.id)
                _snackbarMessage.value = "Saved changes to ${doc.name}!"
                onSaved()
            } else {
                _snackbarMessage.value = "Failed to save PDF changes."
            }
            _isSaving.value = false
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        renderEngine.close()
        ocrScannerEngine.close()
    }

    companion object {
        private const val DEMO_STROKE_ID = "initial_stitch_demo_curve"
        val DEFAULT_DRAWING_BLUE: Int = AndroidColor.parseColor("#1E88E5")
        val PREMIUM_DRAWING_BLUE: Int = AndroidColor.parseColor("#2563EB")

        // Exact 7 Stroke Colour swatches in the Stitch Premium OCR Drawing BottomSheet (`code_00c91e97.html`)
        val PREMIUM_STROKE_SWATCHES: List<Int> = listOf(
            AndroidColor.parseColor("#2563EB"), // Blue (#2563EB - Default selected)
            AndroidColor.parseColor("#DC2626"), // Red (#DC2626)
            AndroidColor.parseColor("#059669"), // Emerald (#059669)
            AndroidColor.parseColor("#F59E0B"), // Amber (#F59E0B)
            AndroidColor.parseColor("#7C3AED"), // Violet (#7C3AED)
            AndroidColor.parseColor("#0F172A"), // Slate-900 (#0F172A)
            AndroidColor.parseColor("#EC4899")  // Pink (#EC4899)
        )

        // Exact 6 Fill Colour pastel swatches in the Stitch Premium OCR Drawing BottomSheet (`code_00c91e97.html`)
        val PREMIUM_FILL_SWATCHES: List<Int> = listOf(
            AndroidColor.parseColor("#DBEAFE"), // Blue-100 (#DBEAFE)
            AndroidColor.parseColor("#FEF3C7"), // Amber-100 (#FEF3C7)
            AndroidColor.parseColor("#D1FAE5"), // Emerald-100 (#D1FAE5)
            AndroidColor.parseColor("#EDE9FE"), // Violet-100 (#EDE9FE)
            AndroidColor.parseColor("#FEE2E2"), // Red-100 (#FEE2E2)
            AndroidColor.parseColor("#F1F5F9")  // Slate-100 (#F1F5F9)
        )

        // Initial "Recently Used" swatches matching the uploaded Text Colour PALETTE design
        val INITIAL_RECENT_TEXT_COLORS: List<Int> = listOf(
            AndroidColor.parseColor("#83929B"), // Slate gray
            AndroidColor.parseColor("#000000"), // Pure black (selected with checkmark)
            AndroidColor.parseColor("#B80F0A")  // Deep crimson red
        )

        // Default Colour Palette Row 1 (9 swatches: index 0 is null for transparency checkerboard)
        val DEFAULT_COLOUR_PALETTE_ROW_1: List<Int?> = listOf(
            null,                               // Checkerboard transparent / default reset
            AndroidColor.parseColor("#FFFFFF"), // Pure White
            AndroidColor.parseColor("#C5D1D8"), // Light Blue-Gray
            AndroidColor.parseColor("#83929B"), // Medium Slate Gray
            AndroidColor.parseColor("#5B666D"), // Dark Slate Gray
            AndroidColor.parseColor("#000000"), // Pure Black
            AndroidColor.parseColor("#BD1E42"), // Ruby Crimson
            AndroidColor.parseColor("#E04F47"), // Coral Red
            AndroidColor.parseColor("#E89D2D")  // Warm Amber Orange
        )

        // Default Colour Palette Row 2 (9 vivid chromatic swatches)
        val DEFAULT_COLOUR_PALETTE_ROW_2: List<Int> = listOf(
            AndroidColor.parseColor("#F9E131"), // Bright Yellow
            AndroidColor.parseColor("#95D60A"), // Lime Green
            AndroidColor.parseColor("#2CB63F"), // Emerald Green
            AndroidColor.parseColor("#05C7A6"), // Teal Turquoise
            AndroidColor.parseColor("#1EA1F7"), // Sky Blue
            AndroidColor.parseColor("#536CE8"), // Royal Indigo Blue
            AndroidColor.parseColor("#896BFF"), // Soft Violet
            AndroidColor.parseColor("#DA55FF"), // Orchid Magenta
            AndroidColor.parseColor("#F75C8D")  // Hot Pink
        )

        // Default Colour Palette Row 3 (9 deep/pastel accent swatches for 3-row palette grid)
        val DEFAULT_COLOUR_PALETTE_ROW_3: List<Int> = listOf(
            AndroidColor.parseColor("#7F1D1D"), // Deep Crimson
            AndroidColor.parseColor("#C2410C"), // Burnt Orange
            AndroidColor.parseColor("#CA8A04"), // Deep Gold
            AndroidColor.parseColor("#15803D"), // Forest Green
            AndroidColor.parseColor("#0F766E"), // Deep Teal
            AndroidColor.parseColor("#0369A1"), // Ocean Blue
            AndroidColor.parseColor("#1D4ED8"), // Cobalt Blue
            AndroidColor.parseColor("#6D28D9"), // Deep Purple
            AndroidColor.parseColor("#BE185D")  // Deep Rose Pink
        )

        // Text Colour Palette Swatches for legacy callers
        val TEXT_COLOR_PALETTE: List<Int> =
            DEFAULT_COLOUR_PALETTE_ROW_1.filterNotNull() +
                DEFAULT_COLOUR_PALETTE_ROW_2 +
                DEFAULT_COLOUR_PALETTE_ROW_3

        /**
         * Generates 9 progressive "Colour Variations" (from dark to light) for [baseColorInt].
         * When [baseColorInt] has low saturation (black/gray/white), generates the exact 9-step
         * grayscale ramp (`#000000` -> `#FFFFFF`) shown in the uploaded screenshot.
         */
        fun generateColourVariations(baseColorInt: Int): List<Int> {
            val r = AndroidColor.red(baseColorInt)
            val g = AndroidColor.green(baseColorInt)
            val b = AndroidColor.blue(baseColorInt)
            val (h, s, _) = rgbToHsl(r, g, b)

            if (s <= 8) {
                val graySteps = listOf(0, 38, 68, 99, 130, 160, 192, 223, 255)
                return graySteps.map { v -> AndroidColor.rgb(v, v, v) }
            }

            val lightnessSteps = listOf(12, 22, 32, 42, 52, 64, 76, 86, 94)
            return lightnessSteps.map { l -> hslToColorInt(h, s.coerceAtLeast(45), l) }
        }

        /**
         * Converts RGB (`0..255`) to integer HSL (`H: 0..360`, `S: 0..100`, `L: 0..100`).
         */
        fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Int, Int, Int> {
            val rf = r / 255f
            val gf = g / 255f
            val bf = b / 255f

            val max = maxOf(rf, gf, bf)
            val min = minOf(rf, gf, bf)
            var h = 0f
            var s = 0f
            val l = (max + min) / 2f

            if (max != min) {
                val d = max - min
                s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
                h = when (max) {
                    rf -> (gf - bf) / d + (if (gf < bf) 6f else 0f)
                    gf -> (bf - rf) / d + 2f
                    else -> (rf - gf) / d + 4f
                }
                h /= 6f
            }

            val hDeg = (h * 360f).toInt().coerceIn(0, 360)
            val sPct = (s * 100f).toInt().coerceIn(0, 100)
            val lPct = (l * 100f).toInt().coerceIn(0, 100)
            return Triple(hDeg, sPct, lPct)
        }

        /**
         * Converts integer HSL (`H: 0..360`, `S: 0..100`, `L: 0..100`) to an ARGB [Int] color.
         */
        fun hslToColorInt(h: Int, s: Int, l: Int): Int {
            val hf = (h.coerceIn(0, 360) % 360) / 360f
            val sf = s.coerceIn(0, 100) / 100f
            val lf = l.coerceIn(0, 100) / 100f

            if (sf == 0f) {
                val gray = (lf * 255f).toInt().coerceIn(0, 255)
                return AndroidColor.rgb(gray, gray, gray)
            }

            fun hueToRgb(p: Float, q: Float, tIn: Float): Float {
                var t = tIn
                if (t < 0f) t += 1f
                if (t > 1f) t -= 1f
                if (t < 1f / 6f) return p + (q - p) * 6f * t
                if (t < 1f / 2f) return q
                if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6f
                return p
            }

            val q = if (lf < 0.5f) lf * (1f + sf) else lf + sf - lf * sf
            val p = 2f * lf - q
            val r = (hueToRgb(p, q, hf + 1f / 3f) * 255f).toInt().coerceIn(0, 255)
            val g = (hueToRgb(p, q, hf) * 255f).toInt().coerceIn(0, 255)
            val b = (hueToRgb(p, q, hf - 1f / 3f) * 255f).toInt().coerceIn(0, 255)
            return AndroidColor.rgb(r, g, b)
        }

        val DRAWING_STROKE_PALETTE: List<Int> = listOf(
            AndroidColor.parseColor("#1E88E5"), // Blue (Default in Stitch)
            AndroidColor.parseColor("#2E9B68"), // Emerald Green
            AndroidColor.parseColor("#E53935"), // Crimson Red
            AndroidColor.parseColor("#FB8C00"), // Vivid Orange
            AndroidColor.parseColor("#8E24AA"), // Royal Purple
            AndroidColor.parseColor("#212121"), // Charcoal Black
            AndroidColor.parseColor("#00ACC1"), // Cyan
            AndroidColor.parseColor("#D81B60"), // Magenta
            AndroidColor.parseColor("#FDD835"), // Gold Yellow
            AndroidColor.parseColor("#3949AB"), // Indigo
            AndroidColor.parseColor("#6D4C41"), // Warm Brown
            AndroidColor.parseColor("#546E7A")  // Slate
        )

        // 5x2 Color Grid for the "Text Highlight" BottomSheet
        val HIGHLIGHT_PALETTE_5X2: List<Int> = listOf(
            // Row 1 (5 colors)
            AndroidColor.parseColor("#FFEB3B"), // Classic Highlighter Yellow
            AndroidColor.parseColor("#69F0AE"), // Highlighter Mint Green
            AndroidColor.parseColor("#40C4FF"), // Highlighter Sky Cyan
            AndroidColor.parseColor("#FF80AB"), // Highlighter Rose Pink
            AndroidColor.parseColor("#FFAB40"), // Highlighter Amber Orange
            // Row 2 (5 colors)
            AndroidColor.parseColor("#EA80FC"), // Highlighter Lavender
            AndroidColor.parseColor("#EEFF41"), // Highlighter Neon Lime
            AndroidColor.parseColor("#18FFFF"), // Highlighter Aqua
            AndroidColor.parseColor("#FF5252"), // Highlighter Coral
            AndroidColor.parseColor("#1E88E5")  // Highlighter Electric Blue
        )
    }

    class Factory(
        private val documentId: Long,
        private val initialPageIndex: Int,
        private val initialLaunchMode: EditorLaunchMode,
        private val repository: PdfRepository,
        private val subscriptionManager: SubscriptionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PdfEditorViewModel::class.java)) {
                return PdfEditorViewModel(
                    documentId = documentId,
                    initialPageIndex = initialPageIndex,
                    initialLaunchMode = initialLaunchMode,
                    repository = repository,
                    subscriptionManager = subscriptionManager
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
