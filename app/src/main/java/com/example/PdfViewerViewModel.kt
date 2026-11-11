package com.example

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Shared in-memory handoff cache for OCR text blocks extracted when the user taps the
 * OCR (crown) toolbar button in [PdfViewerScreen] before entering the Editor Screen.
 */
object OcrSessionCache {
    private val pageBlocksMap = mutableMapOf<String, List<EditableTextBlock>>()
    private val pageRedactionsMap = mutableMapOf<String, List<DeletedRedaction>>()

    private fun key(documentId: Long, pageIndex: Int): String = "${documentId}_$pageIndex"

    fun putBlocks(documentId: Long, pageIndex: Int, blocks: List<EditableTextBlock>) {
        synchronized(this) {
            pageBlocksMap[key(documentId, pageIndex)] = blocks
        }
    }

    fun getBlocks(documentId: Long, pageIndex: Int): List<EditableTextBlock>? {
        synchronized(this) {
            return pageBlocksMap[key(documentId, pageIndex)]
        }
    }

    fun putRedactions(documentId: Long, pageIndex: Int, redactions: List<DeletedRedaction>) {
        synchronized(this) {
            pageRedactionsMap[key(documentId, pageIndex)] = redactions
        }
    }

    fun getRedactions(documentId: Long, pageIndex: Int): List<DeletedRedaction> {
        synchronized(this) {
            return pageRedactionsMap[key(documentId, pageIndex)].orEmpty()
        }
    }

    fun clearForDocument(documentId: Long) {
        synchronized(this) {
            val prefix = "${documentId}_"
            pageBlocksMap.keys.removeAll { it.startsWith(prefix) }
            pageRedactionsMap.keys.removeAll { it.startsWith(prefix) }
        }
    }
}

/**
 * ViewModel for the Stitch AI PDF Viewer Screen, managing [PdfRenderEngine] (`PdfRenderer` +
 * `LruCache<Int, Bitmap>`), active page selection, thumbnail strip state, OCR extraction via
 * ML Kit Text Recognition v2, Text-to-Speech extraction, and Free vs. Premium toolbar feature gating.
 */
class PdfViewerViewModel(
    private val documentId: Long,
    private val repository: PdfRepository,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    private val renderEngine = PdfRenderEngine()
    private val ocrScannerEngine = OcrScannerEngine()

    private val _document = MutableStateFlow<PdfDocumentEntity?>(null)
    val document: StateFlow<PdfDocumentEntity?> = _document.asStateFlow()

    private val _pageCount = MutableStateFlow(0)
    val pageCount: StateFlow<Int> = _pageCount.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    private val _currentPageBitmap = MutableStateFlow<Bitmap?>(null)
    val currentPageBitmap: StateFlow<Bitmap?> = _currentPageBitmap.asStateFlow()

    private val _currentPageAspectRatio = MutableStateFlow(1.32f)
    val currentPageAspectRatio: StateFlow<Float> = _currentPageAspectRatio.asStateFlow()

    private val _thumbnails = MutableStateFlow<Map<Int, Bitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<Int, Bitmap>> = _thumbnails.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRunningOcr = MutableStateFlow(false)
    val isRunningOcr: StateFlow<Boolean> = _isRunningOcr.asStateFlow()

    private val _showPremiumPaywall = MutableStateFlow(false)
    val showPremiumPaywall: StateFlow<Boolean> = _showPremiumPaywall.asStateFlow()

    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchMatchPages = MutableStateFlow<List<Int>>(emptyList())
    val searchMatchPages: StateFlow<List<Int>> = _searchMatchPages.asStateFlow()

    private val _ttsSpeakingText = MutableStateFlow<String?>(null)
    val ttsSpeakingText: StateFlow<String?> = _ttsSpeakingText.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val isPremiumUser: StateFlow<Boolean> = subscriptionManager.isPremiumUser

    init {
        loadDocumentAndPages()
    }

    fun loadDocumentAndPages(forceReload: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            val doc = repository.getDocumentById(documentId)
            _document.value = doc

            if (doc != null) {
                repository.markAsOpened(doc.id)
                val file = File(doc.filePath)

                // If fractal_trading(2).pdf was previously seeded with < 7 pages, upgrade it to 7 spreads
                val initialCount = renderEngine.openDocument(file, forceReload = forceReload)
                val totalPages = if (
                    doc.name.contains("fractal_trading(2)", ignoreCase = true) && initialCount in 1..2
                ) {
                    renderEngine.close()
                    val upgraded = repository.createSamplePdfDocument(
                        title = doc.name,
                        isDownloaded = doc.isDownloaded,
                        isOpened = true,
                        coverStyle = 2
                    )
                    _document.value = upgraded
                    renderEngine.openDocument(File(upgraded.filePath), forceReload = true)
                } else {
                    initialCount
                }

                _pageCount.value = totalPages
                val validPage = _currentPageIndex.value.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
                _currentPageIndex.value = validPage

                if (totalPages > 0) {
                    val bmp = renderEngine.renderPageHighRes(validPage)
                    _currentPageBitmap.value = bmp
                    _currentPageAspectRatio.value = renderEngine.getCachedPageAspectRatio(
                        validPage,
                        defaultRatio = if (bmp != null && bmp.height > 0) {
                            bmp.width.toFloat() / bmp.height.toFloat()
                        } else {
                            1.32f
                        }
                    )
                    preloadThumbnails(totalPages)
                }
            }
            _isLoading.value = false
        }
    }

    private fun preloadThumbnails(totalPages: Int) {
        viewModelScope.launch {
            for (index in 0 until totalPages) {
                val thumb = renderEngine.renderThumbnail(index, targetWidthPx = 220)
                if (thumb != null) {
                    _thumbnails.update { current -> current + (index to thumb) }
                }
            }
        }
    }

    /**
     * Selects a page from the bottom horizontal thumbnail bar (or swipe gesture),
     * updates the active red selection border immediately, and renders the high-resolution page.
     */
    fun selectPage(pageIndex: Int) {
        val count = _pageCount.value
        if (count <= 0) return
        val clamped = pageIndex.coerceIn(0, count - 1)
        if (clamped == _currentPageIndex.value && _currentPageBitmap.value != null) return

        _currentPageIndex.value = clamped
        viewModelScope.launch {
            val bmp = renderEngine.renderPageHighRes(clamped)
            if (bmp != null) {
                _currentPageBitmap.value = bmp
                _currentPageAspectRatio.value = renderEngine.getCachedPageAspectRatio(
                    clamped,
                    defaultRatio = if (bmp.height > 0) bmp.width.toFloat() / bmp.height.toFloat() else 1.32f
                )
            }
        }
    }

    fun nextPage() {
        if (_currentPageIndex.value + 1 < _pageCount.value) {
            selectPage(_currentPageIndex.value + 1)
        }
    }

    fun previousPage() {
        if (_currentPageIndex.value > 0) {
            selectPage(_currentPageIndex.value - 1)
        }
    }

    /**
     * Top Toolbar Action 1 (Free Plan):
     * Tapping the "Edit" pen icon opens the Editor Screen in **Free Annotation Mode**
     * (Freehand Drawing & Highlighting).
     */
    fun onEditPenClicked(
        onNavigateToEditor: (documentId: Long, pageIndex: Int, mode: EditorLaunchMode) -> Unit
    ) {
        onNavigateToEditor(
            documentId,
            _currentPageIndex.value,
            EditorLaunchMode.FREE_ANNOTATION
        )
    }

    /**
     * Top Toolbar Action 2 (Premium Plan Feature Gate):
     * Tapping the "OCR" icon (with gold crown badge) checks `SubscriptionManager.isPremiumUser`:
     * - If `true`: runs ML Kit Text Recognition v2 (`TextRecognition`) on the current page,
     *   caches the extracted normalized [EditableTextBlock]s, and opens the Editor Screen in
     *   **Premium OCR Text Edit Mode**.
     * - If `false`: displays the Premium Paywall.
     */
    fun onOcrToolbarClicked(
        onNavigateToEditor: (documentId: Long, pageIndex: Int, mode: EditorLaunchMode) -> Unit
    ) {
        subscriptionManager.withFeatureAccess(
            feature = SubscriptionFeature.OCR_TEXT_EXTRACTION,
            onPaywallRequired = {
                _showPremiumPaywall.value = true
            },
            onAllowed = {
                val targetPage = _currentPageIndex.value
                viewModelScope.launch {
                    _isRunningOcr.value = true
                    val extractedBlocks = extractOcrBlocksForPage(targetPage)
                    OcrSessionCache.putBlocks(documentId, targetPage, extractedBlocks)
                    _isRunningOcr.value = false
                    onNavigateToEditor(
                        documentId,
                        targetPage,
                        EditorLaunchMode.PREMIUM_OCR_TEXT_EDIT
                    )
                }
            }
        )
    }

    /**
     * Top Toolbar Action 3 (Premium Plan Feature Gate):
     * Tapping the "Text-to-Speech" icon (with gold crown badge) checks `SubscriptionManager.isPremiumUser`:
     * - If `true`: extracts text from the current page and reads it aloud via Android TTS.
     * - If `false`: displays the Premium Paywall.
     */
    fun onTextToSpeechToolbarClicked() {
        subscriptionManager.withFeatureAccess(
            feature = SubscriptionFeature.TEXT_TO_SPEECH,
            onPaywallRequired = {
                _showPremiumPaywall.value = true
            },
            onAllowed = {
                if (_ttsSpeakingText.value != null) {
                    _ttsSpeakingText.value = null
                    return@withFeatureAccess
                }
                val targetPage = _currentPageIndex.value
                viewModelScope.launch {
                    val blocks = extractOcrBlocksForPage(targetPage)
                    val combinedText = blocks.joinToString(". ") { it.currentText.trim() }
                        .ifBlank {
                            "Page ${targetPage + 1} of ${_document.value?.name ?: "document"}."
                        }
                    _ttsSpeakingText.value = combinedText
                }
            }
        )
    }

    fun stopTextToSpeech() {
        _ttsSpeakingText.value = null
    }

    /**
     * Runs Google ML Kit Text Recognition v2 (`text-recognition:16.0.1`) on [pageIndex],
     * converting bounding boxes to normalized `[0.0f, 1.0f]` coordinates via
     * [NormalizedCoordinateMapper]. Falls back to PDFBox text extraction if the environment
     * has not yet downloaded the native ML Kit OCR vision module.
     */
    private suspend fun extractOcrBlocksForPage(pageIndex: Int): List<EditableTextBlock> =
        withContext(Dispatchers.IO) {
            val bitmap = renderEngine.renderPageHighRes(pageIndex, targetWidthPx = 1440)
            val fallbackFile = _document.value?.filePath?.let { File(it) }
            ocrScannerEngine.extractTextBlocks(
                pageBitmap = bitmap,
                pageIndex = pageIndex,
                fallbackPdfFile = fallbackFile
            )
        }

    fun toggleSearchBar() {
        val next = !_isSearchVisible.value
        _isSearchVisible.value = next
        if (!next) {
            _searchQuery.value = ""
            _searchMatchPages.value = emptyList()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            _searchMatchPages.value = emptyList()
            return
        }

        val pageNum = trimmed.toIntOrNull()
        if (pageNum != null && pageNum in 1.._pageCount.value) {
            selectPage(pageNum - 1)
            _searchMatchPages.value = listOf(pageNum - 1)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val docPath = _document.value?.filePath ?: return@launch
            val pdfFile = File(docPath)
            if (!pdfFile.exists()) return@launch

            val matches = mutableListOf<Int>()
            runCatching {
                PDDocument.load(pdfFile).use { pdDoc ->
                    val total = pdDoc.numberOfPages
                    val stripper = PDFTextStripper()
                    for (p in 1..total) {
                        stripper.startPage = p
                        stripper.endPage = p
                        val pageText = stripper.getText(pdDoc).orEmpty()
                        if (pageText.contains(trimmed, ignoreCase = true)) {
                            matches.add(p - 1)
                        }
                    }
                }
            }
            _searchMatchPages.value = matches
            if (matches.isNotEmpty() && _currentPageIndex.value !in matches) {
                selectPage(matches.first())
            }
        }
    }

    fun dismissPremiumPaywall() {
        _showPremiumPaywall.value = false
    }

    fun activatePremiumTrial() {
        subscriptionManager.setPremiumStatus(true)
        _showPremiumPaywall.value = false
        _snackbarMessage.value = "Premium Plan unlocked! OCR & Text-to-Speech are now active."
    }

    fun togglePremiumPlanForTesting() {
        val unlocked = subscriptionManager.togglePremiumStatus()
        _showPremiumPaywall.value = false
        _snackbarMessage.value = if (unlocked) {
            "Switched to Premium Plan"
        } else {
            "Switched to Free Plan"
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

    class Factory(
        private val documentId: Long,
        private val repository: PdfRepository,
        private val subscriptionManager: SubscriptionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PdfViewerViewModel::class.java)) {
                return PdfViewerViewModel(documentId, repository, subscriptionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
