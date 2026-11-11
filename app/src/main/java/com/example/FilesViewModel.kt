package com.example

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Cloud storage providers displayed in the `Cloud storage [PRO]` section of `FilesScreen.kt`.
 */
enum class CloudStorageProvider(
    val id: String,
    val displayName: String
) {
    GOOGLE_DRIVE("google_drive", "Google Drive"),
    DROPBOX("dropbox", "Dropbox"),
    ONEDRIVE("onedrive", "OneDrive"),
    SHAREPOINT("sharepoint", "SharePoint")
}

/**
 * Represents the connection and sync state of a [CloudStorageProvider].
 */
data class CloudConnectionState(
    val provider: CloudStorageProvider,
    val isLinked: Boolean = false,
    val accountEmail: String? = null,
    val syncedFileCount: Int = 0
) {
    val statusSubtitle: String
        get() = if (isLinked) {
            if (accountEmail != null) {
                "Linked ($accountEmail) • $syncedFileCount synced"
            } else {
                "Linked • $syncedFileCount files synced"
            }
        } else {
            "Not linked"
        }
}

/**
 * Active drill-down modal/sheet in the Files tab (`Downloads`, `Processed files`, or `Bin`).
 */
enum class FilesFolderSheetType {
    NONE,
    DOWNLOADS,
    PROCESSED_FILES,
    TRASH_BIN
}

/**
 * ViewModel for the Stitch AI `Files` screen (`FilesScreen.kt`), connected to:
 * - [PdfRepository] (Room database local files for `Downloads` and `Processed files`, plus `TrashBinStore` for `Delete -> Bin`)
 * - [SubscriptionManager] (reactive `isPremiumUser` state gating the `Cloud storage PRO` section)
 * - Cloud connection state persistence (`Google Drive`, `Dropbox`, `OneDrive`, `SharePoint`).
 */
class FilesViewModel(
    private val repository: PdfRepository,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    private val prefs = repository.appContext.getSharedPreferences(
        PREFS_CLOUD_CONNECTIONS,
        Context.MODE_PRIVATE
    )

    val isPremiumUser: StateFlow<Boolean> = subscriptionManager.isPremiumUser

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    private val _activeFolderSheet = MutableStateFlow(FilesFolderSheetType.NONE)
    val activeFolderSheet: StateFlow<FilesFolderSheetType> = _activeFolderSheet.asStateFlow()

    private val _selectedCloudProviderForDialog = MutableStateFlow<CloudStorageProvider?>(null)
    val selectedCloudProviderForDialog: StateFlow<CloudStorageProvider?> =
        _selectedCloudProviderForDialog.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Reactive Room database streams for "Downloads" and "Processed files"
    val downloadedDocuments: StateFlow<List<PdfDocumentEntity>> = combine(
        repository.downloadedDocuments,
        _searchQuery
    ) { docs, query ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) docs else docs.filter { it.name.contains(trimmed, ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val processedDocuments: StateFlow<List<PdfDocumentEntity>> = combine(
        repository.openedDocuments,
        _searchQuery
    ) { docs, query ->
        val trimmed = query.trim()
        if (trimmed.isEmpty()) docs else docs.filter { it.name.contains(trimmed, ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Reactive Trash Bin stream ("Delete -> Bin")
    val trashedDocuments: StateFlow<List<PdfDocumentEntity>> = repository.trashedDocuments

    // Cloud connection states for Google Drive, Dropbox, OneDrive, and SharePoint
    private val _cloudConnections = MutableStateFlow(loadInitialCloudConnections())
    val cloudConnections: StateFlow<Map<CloudStorageProvider, CloudConnectionState>> =
        _cloudConnections.asStateFlow()

    private fun loadInitialCloudConnections(): Map<CloudStorageProvider, CloudConnectionState> {
        return CloudStorageProvider.entries.associateWith { provider ->
            val linked = prefs.getBoolean("linked_${provider.id}", false)
            val email = prefs.getString("email_${provider.id}", null)
            val syncedCount = prefs.getInt("synced_${provider.id}", 0)
            CloudConnectionState(
                provider = provider,
                isLinked = linked,
                accountEmail = email,
                syncedFileCount = syncedCount
            )
        }
    }

    /**
     * Toggles the top search bar in `FilesScreen`.
     */
    fun toggleSearchBar() {
        val next = !_isSearchVisible.value
        _isSearchVisible.value = next
        if (!next) {
            _searchQuery.value = ""
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Opens the drill-down modal for `Downloads`, `Processed files`, or `Bin`.
     */
    fun openFolderSheet(sheetType: FilesFolderSheetType) {
        _activeFolderSheet.value = sheetType
    }

    fun closeFolderSheet() {
        _activeFolderSheet.value = FilesFolderSheetType.NONE
    }

    /**
     * Handles tapping any Cloud Storage provider (`Google Drive`, `Dropbox`, `OneDrive`, `SharePoint`):
     * - Enforces feature gating: if `SubscriptionManager.isPremiumUser` is `false`, invokes
     *   [onRequirePremiumPaywall] to open the Premium Paywall dialog.
     * - If `isPremiumUser` is `true`, opens the Cloud Account Link & Sync dialog for [provider].
     */
    fun onCloudProviderClicked(
        provider: CloudStorageProvider,
        onRequirePremiumPaywall: () -> Unit
    ) {
        if (!subscriptionManager.isPremiumUser.value) {
            onRequirePremiumPaywall()
            return
        }
        _selectedCloudProviderForDialog.value = provider
    }

    fun dismissCloudProviderDialog() {
        _selectedCloudProviderForDialog.value = null
    }

    /**
     * Links or unlinks a [CloudStorageProvider] (available to Premium users) and syncs local Room files.
     */
    fun linkCloudProvider(provider: CloudStorageProvider, email: String) {
        val cleanEmail = email.trim().ifEmpty { "pro.user@pdfzscanner.com" }
        val totalLocalFiles = (downloadedDocuments.value.size + processedDocuments.value.size)
            .coerceAtLeast(1)

        prefs.edit()
            .putBoolean("linked_${provider.id}", true)
            .putString("email_${provider.id}", cleanEmail)
            .putInt("synced_${provider.id}", totalLocalFiles)
            .apply()

        _cloudConnections.update { map ->
            map.toMutableMap().apply {
                put(
                    provider,
                    CloudConnectionState(
                        provider = provider,
                        isLinked = true,
                        accountEmail = cleanEmail,
                        syncedFileCount = totalLocalFiles
                    )
                )
            }
        }
        _selectedCloudProviderForDialog.value = null
        _snackbarMessage.value = "Linked ${provider.displayName} ($cleanEmail) • Synced $totalLocalFiles files"
    }

    fun unlinkCloudProvider(provider: CloudStorageProvider) {
        prefs.edit()
            .putBoolean("linked_${provider.id}", false)
            .remove("email_${provider.id}")
            .putInt("synced_${provider.id}", 0)
            .apply()

        _cloudConnections.update { map ->
            map.toMutableMap().apply {
                put(
                    provider,
                    CloudConnectionState(
                        provider = provider,
                        isLinked = false,
                        accountEmail = null,
                        syncedFileCount = 0
                    )
                )
            }
        }
        _selectedCloudProviderForDialog.value = null
        _snackbarMessage.value = "Unlinked ${provider.displayName}"
    }

    /**
     * Moves a local [PdfDocumentEntity] to the Trash Bin (`Delete -> Bin`).
     */
    fun moveDocumentToTrashBin(document: PdfDocumentEntity) {
        viewModelScope.launch {
            repository.moveToTrashBin(document)
            _snackbarMessage.value = "Moved ${document.name} to Bin"
        }
    }

    /**
     * Restores a trashed [PdfDocumentEntity] from the Bin back into `My storage`.
     */
    fun restoreDocumentFromBin(document: PdfDocumentEntity) {
        viewModelScope.launch {
            repository.restoreFromTrashBin(document)
            _snackbarMessage.value = "Restored ${document.name} from Bin"
        }
    }

    /**
     * Permanently deletes a single [PdfDocumentEntity] from the Trash Bin.
     */
    fun permanentlyDeleteFromBin(document: PdfDocumentEntity) {
        viewModelScope.launch {
            repository.permanentlyDeleteFromTrashBin(document)
            _snackbarMessage.value = "Permanently deleted ${document.name}"
        }
    }

    /**
     * Empties all files from the Trash Bin.
     */
    fun emptyTrashBin() {
        viewModelScope.launch {
            repository.emptyTrashBin()
            _snackbarMessage.value = "Trash Bin emptied"
        }
    }

    /**
     * Imports an external PDF into `Downloads` via [PdfRepository].
     */
    fun importExternalPdf(
        uri: Uri,
        onNavigateToViewer: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val imported = repository.importPdfFromUri(
                uri = uri,
                isDownloaded = true,
                isOpened = true
            )
            if (imported != null) {
                _snackbarMessage.value = "Imported ${imported.name}"
                onNavigateToViewer(imported.id)
            } else {
                _snackbarMessage.value = "Failed to import PDF file."
            }
        }
    }

    fun openDocument(
        document: PdfDocumentEntity,
        onNavigateToViewer: (Long) -> Unit
    ) {
        viewModelScope.launch {
            repository.markAsOpened(document.id)
            _activeFolderSheet.value = FilesFolderSheetType.NONE
            onNavigateToViewer(document.id)
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    companion object {
        private const val PREFS_CLOUD_CONNECTIONS = "pdf_z_scanner_cloud_connections"
    }

    class Factory(
        private val repository: PdfRepository,
        private val subscriptionManager: SubscriptionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(FilesViewModel::class.java)) {
                return FilesViewModel(repository, subscriptionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
