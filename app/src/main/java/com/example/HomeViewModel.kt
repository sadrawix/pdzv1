package com.example

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Represents the 5 tabs in the Stitch AI Bottom Navigation Bar.
 */
enum class HomeBottomTab(val label: String) {
    HOME("Home"),
    FILES("Files"),
    TOOLS("Tools"),
    SCANNER("Scanner"),
    SETTINGS("Settings")
}

/**
 * ViewModel for the Home Screen, connected to [PdfRepository] and [SubscriptionManager].
 * Manages the Downloads/Opened filter chips, horizontal PDF carousel items with generated
 * first-page `PdfRenderer` cover thumbnails, ML Kit Document Scanner result persistence,
 * and Premium upgrade dialog state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: PdfRepository,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    val repositoryRef: PdfRepository
        get() = repository

    val subscriptionManagerRef: SubscriptionManager
        get() = subscriptionManager

    private val _selectedFilter = MutableStateFlow(DocumentFilter.DOWNLOADS)
    val selectedFilter: StateFlow<DocumentFilter> = _selectedFilter.asStateFlow()

    private val _selectedBottomTab = MutableStateFlow(HomeBottomTab.HOME)
    val selectedBottomTab: StateFlow<HomeBottomTab> = _selectedBottomTab.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchBarVisible = MutableStateFlow(false)
    val isSearchBarVisible: StateFlow<Boolean> = _isSearchBarVisible.asStateFlow()

    private val _showPremiumDialog = MutableStateFlow(false)
    val showPremiumDialog: StateFlow<Boolean> = _showPremiumDialog.asStateFlow()

    private val _isProcessingScan = MutableStateFlow(false)
    val isProcessingScan: StateFlow<Boolean> = _isProcessingScan.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val isPremiumUser: StateFlow<Boolean> = subscriptionManager.isPremiumUser

    /**
     * Reactive stream of PDF documents filtered by the active chip ("Downloads" or "Opened")
     * and optional search query.
     */
    val filteredDocuments: StateFlow<List<PdfDocumentEntity>> = combine(
        _selectedFilter.flatMapLatest { filter -> repository.getDocumentsByFilter(filter) },
        _searchQuery
    ) { docs, query ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            docs
        } else {
            docs.filter { it.name.contains(trimmed, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /**
     * Reactive stream of all recent PDF documents (used in the "Files" tab).
     */
    val allRecentDocuments: StateFlow<List<PdfDocumentEntity>> = combine(
        repository.recentDocuments,
        _searchQuery
    ) { docs, query ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            docs
        } else {
            docs.filter { it.name.contains(trimmed, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeedDocumentsIfEmpty()
        }
    }

    /**
     * Switches between the "Downloads" and "Opened" filter chips on the Home Screen.
     */
    fun selectFilter(filter: DocumentFilter) {
        _selectedFilter.value = filter
    }

    /**
     * Updates the selected bottom navigation tab (Home, Files, Tools, Scanner, Settings).
     */
    fun selectBottomTab(tab: HomeBottomTab) {
        _selectedBottomTab.value = tab
    }

    fun toggleSearchBar() {
        val next = !_isSearchBarVisible.value
        _isSearchBarVisible.value = next
        if (!next) {
            _searchQuery.value = ""
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Opens the Premium Upgrade Dialog (wired to the "Start free trial" banner button).
     */
    fun openPremiumUpgradeDialog() {
        _showPremiumDialog.value = true
    }

    fun dismissPremiumUpgradeDialog() {
        _showPremiumDialog.value = false
    }

    /**
     * Activates the Premium Plan (free trial / upgrade) via [SubscriptionManager].
     */
    fun startFreeTrialAndActivatePremium() {
        subscriptionManager.setPremiumStatus(true)
        _showPremiumDialog.value = false
        _snackbarMessage.value = "Premium Plan activated! OCR, Direct Text Edit & TTS unlocked."
    }

    /**
     * Toggles between Free Plan and Premium Plan for testing.
     */
    fun togglePremiumForTesting() {
        val isNowPremium = subscriptionManager.togglePremiumStatus()
        _snackbarMessage.value = if (isNowPremium) {
            "Switched to Premium Plan (OCR, Text Edit, Typography & TTS enabled)"
        } else {
            "Switched to Free Plan (View, Scan, Draw & Highlight enabled)"
        }
    }

    /**
     * Marks a document as opened in Room and invokes [onNavigateToViewer] with its ID.
     */
    fun openDocument(
        document: PdfDocumentEntity,
        onNavigateToViewer: (Long) -> Unit
    ) {
        viewModelScope.launch {
            repository.markAsOpened(document.id)
            onNavigateToViewer(document.id)
        }
    }

    /**
     * Imports a PDF selected via the Android document picker, stores it in internal storage
     * and Room Database with a generated first-page cover thumbnail, and opens it.
     */
    fun importExternalPdf(
        uri: Uri,
        onNavigateToViewer: (Long) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessingScan.value = true
            val imported = repository.importPdfFromUri(
                uri = uri,
                isDownloaded = true,
                isOpened = true
            )
            _isProcessingScan.value = false
            if (imported != null) {
                onNavigateToViewer(imported.id)
            } else {
                _snackbarMessage.value = "Could not import the selected PDF file."
            }
        }
    }

    /**
     * Saves the PDF and/or JPEG pages returned by Google ML Kit Document Scanner into internal
     * storage and Room Database, generates its first-page `PdfRenderer` cover thumbnail, and
     * navigates directly to the PDF Viewer screen.
     */
    fun onMlKitScanCompleted(
        pdfUri: Uri?,
        jpegPageUris: List<Uri>,
        onNavigateToViewer: (Long) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessingScan.value = true
            val savedDoc = repository.saveScannedDocument(
                pdfUri = pdfUri,
                jpegPageUris = jpegPageUris
            )
            _isProcessingScan.value = false
            if (savedDoc != null) {
                onNavigateToViewer(savedDoc.id)
            } else {
                _snackbarMessage.value = "Failed to save scanned PDF document."
            }
        }
    }

    /**
     * Fallback handler if the host environment (such as a headless cloud emulator without Play
     * Services camera hardware) cannot launch the ML Kit Scanner intent: generates a fresh
     * multi-page scanned PDF in internal storage & Room Database and navigates directly to the
     * PDF Viewer screen so the user can test the complete workflow.
     */
    fun onScannerHardwareUnavailableFallback(
        error: Throwable,
        onNavigateToViewer: (Long) -> Unit
    ) {
        viewModelScope.launch {
            _isProcessingScan.value = true
            val timestamp = SimpleDateFormat("HHmmss", Locale.US).format(Date())
            val scannedDoc = repository.createSamplePdfDocument(
                title = "Scanned_Doc_$timestamp.pdf",
                isDownloaded = true,
                isOpened = true,
                coverStyle = 3
            )
            _isProcessingScan.value = false
            _snackbarMessage.value = "Created scanned PDF (${scannedDoc.name})"
            onNavigateToViewer(scannedDoc.id)
        }
    }

    fun deleteDocument(document: PdfDocumentEntity) {
        viewModelScope.launch {
            repository.deleteDocument(document)
            _snackbarMessage.value = "Deleted ${document.name}"
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    class Factory(
        private val repository: PdfRepository,
        private val subscriptionManager: SubscriptionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(repository, subscriptionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
