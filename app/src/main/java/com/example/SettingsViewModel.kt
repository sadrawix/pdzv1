package com.example

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "pdf_z_settings_prefs")

enum class AppThemeMode(val label: String, val subtitle: String) {
    LIGHT("Light", "Adaptive light appearance"),
    DARK("Dark", "Night contrast appearance")
}

data class OcrLanguageOption(
    val code: String,
    val displayName: String,
    val scriptHint: String
)

/**
 * ViewModel for the Stitch "Settings" tab (`SettingsScreen.kt`).
 *
 * Persists user preferences via Jetpack DataStore (`pdf_z_settings_prefs`):
 * - [appThemeMode]: Light or Dark mode segmented pill toggle
 * - [ocrLanguage]: Selected OCR recognition language (default `"English (US)"`)
 * - [askBeforeMobileData]: Toggle for "Ask before using mobile data"
 * - [loggedInAccountEmail]: Optional signed-in account email for the profile card
 *
 * Also observes and controls [SubscriptionManager] for the "PDF Z Premium VIP" paywall
 * and Membership tier status.
 */
class SettingsViewModel(
    private val dataStore: DataStore<Preferences>,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("app_theme_mode")
        private val KEY_OCR_LANGUAGE = stringPreferencesKey("ocr_language")
        private val KEY_ASK_MOBILE_DATA = booleanPreferencesKey("ask_before_mobile_data")
        private val KEY_ACCOUNT_EMAIL = stringPreferencesKey("account_email")
        private val KEY_ACCOUNT_NAME = stringPreferencesKey("account_name")

        val SUPPORTED_OCR_LANGUAGES = listOf(
            OcrLanguageOption("en-US", "English (US)", "Latin Script • High Accuracy"),
            OcrLanguageOption("en-GB", "English (UK)", "Latin Script • High Accuracy"),
            OcrLanguageOption("es-ES", "Spanish (ES)", "Latin Script • Español"),
            OcrLanguageOption("fr-FR", "French (FR)", "Latin Script • Français"),
            OcrLanguageOption("de-DE", "German (DE)", "Latin Script • Deutsch"),
            OcrLanguageOption("pt-BR", "Portuguese (BR)", "Latin Script • Português"),
            OcrLanguageOption("it-IT", "Italian (IT)", "Latin Script • Italiano"),
            OcrLanguageOption("ja-JP", "Japanese (JP)", "Japanese Script • 日本語"),
            OcrLanguageOption("zh-CN", "Chinese (Simplified)", "Hanzi Script • 简体中文")
        )
    }

    val isPremiumUser: StateFlow<Boolean> = subscriptionManager.isPremiumUser

    private val preferencesFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    val appThemeMode: StateFlow<AppThemeMode> = preferencesFlow
        .map { prefs ->
            val raw = prefs[KEY_THEME_MODE] ?: AppThemeMode.LIGHT.name
            runCatching { AppThemeMode.valueOf(raw) }.getOrDefault(AppThemeMode.LIGHT)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppThemeMode.LIGHT
        )

    val ocrLanguage: StateFlow<String> = preferencesFlow
        .map { prefs ->
            prefs[KEY_OCR_LANGUAGE] ?: "English (US)"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = "English (US)"
        )

    val askBeforeMobileData: StateFlow<Boolean> = preferencesFlow
        .map { prefs ->
            prefs[KEY_ASK_MOBILE_DATA] ?: true
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    val accountEmail: StateFlow<String?> = preferencesFlow
        .map { prefs ->
            prefs[KEY_ACCOUNT_EMAIL]?.takeIf { it.isNotBlank() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val accountName: StateFlow<String?> = preferencesFlow
        .map { prefs ->
            prefs[KEY_ACCOUNT_NAME]?.takeIf { it.isNotBlank() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    private val _showOcrLanguageDialog = MutableStateFlow(false)
    val showOcrLanguageDialog: StateFlow<Boolean> = _showOcrLanguageDialog.asStateFlow()

    private val _showAccountDialog = MutableStateFlow(false)
    val showAccountDialog: StateFlow<Boolean> = _showAccountDialog.asStateFlow()

    private val _showFeedbackDialog = MutableStateFlow(false)
    val showFeedbackDialog: StateFlow<Boolean> = _showFeedbackDialog.asStateFlow()

    private val _activeLegalDocument = MutableStateFlow<LegalDocumentType?>(null)
    val activeLegalDocument: StateFlow<LegalDocumentType?> = _activeLegalDocument.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    fun setAppThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_THEME_MODE] = mode.name
            }
            _snackbarMessage.value = "App Theme set to ${mode.label} (${mode.subtitle})"
        }
    }

    fun setOcrLanguage(languageDisplayName: String) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_OCR_LANGUAGE] = languageDisplayName
            }
            _showOcrLanguageDialog.value = false
            _snackbarMessage.value = "OCR recognition language set to $languageDisplayName"
        }
    }

    fun toggleAskBeforeMobileData() {
        val nextValue = !askBeforeMobileData.value
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_ASK_MOBILE_DATA] = nextValue
            }
            _snackbarMessage.value = if (nextValue) {
                "Mobile data confirmation enabled"
            } else {
                "Mobile data confirmation disabled"
            }
        }
    }

    fun setAskBeforeMobileData(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_ASK_MOBILE_DATA] = enabled
            }
        }
    }

    fun signInAccount(name: String, email: String) {
        val cleanEmail = email.trim()
        val cleanName = name.trim().ifBlank { cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } }
        if (cleanEmail.isBlank()) return
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[KEY_ACCOUNT_EMAIL] = cleanEmail
                prefs[KEY_ACCOUNT_NAME] = cleanName
            }
            _showAccountDialog.value = false
            _snackbarMessage.value = "Signed in as $cleanEmail"
        }
    }

    fun signOutAccount() {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs.remove(KEY_ACCOUNT_EMAIL)
                prefs.remove(KEY_ACCOUNT_NAME)
            }
            _showAccountDialog.value = false
            _snackbarMessage.value = "Signed out of PDF Z account"
        }
    }

    fun toggleSearchVisibility() {
        val next = !_isSearchVisible.value
        _isSearchVisible.value = next
        if (!next) {
            _searchQuery.value = ""
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openOcrLanguageDialog() {
        _showOcrLanguageDialog.value = true
    }

    fun dismissOcrLanguageDialog() {
        _showOcrLanguageDialog.value = false
    }

    fun openAccountDialog() {
        _showAccountDialog.value = true
    }

    fun dismissAccountDialog() {
        _showAccountDialog.value = false
    }

    fun openFeedbackDialog() {
        _showFeedbackDialog.value = true
    }

    fun dismissFeedbackDialog() {
        _showFeedbackDialog.value = false
    }

    fun submitFeedback(message: String, rating: Int) {
        _showFeedbackDialog.value = false
        if (message.isNotBlank()) {
            _snackbarMessage.value = "Thank you! Your $rating-star feedback has been sent."
        } else {
            _snackbarMessage.value = "Thank you for rating PDF Z Scanner ($rating/5 stars)!"
        }
    }

    fun openLegalDocument(type: LegalDocumentType) {
        _activeLegalDocument.value = type
    }

    fun dismissLegalDocument() {
        _activeLegalDocument.value = null
    }

    fun togglePremiumMembershipForTesting() {
        val isNowPremium = subscriptionManager.togglePremiumStatus()
        _snackbarMessage.value = if (isNowPremium) {
            "PDF Z Premium VIP activated"
        } else {
            "Switched to Membership Free"
        }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    class Factory(
        private val context: Context,
        private val subscriptionManager: SubscriptionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(
                    dataStore = context.applicationContext.settingsDataStore,
                    subscriptionManager = subscriptionManager
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}

enum class LegalDocumentType(val title: String) {
    PRIVACY_POLICY("Privacy Policy"),
    TERMS_OF_SERVICE("Terms of Service")
}
