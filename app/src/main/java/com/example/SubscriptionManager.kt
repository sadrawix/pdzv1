package com.example

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Defines all major capabilities in PDF Z Scanner and whether they belong to the
 * Free Plan or require the Premium Plan.
 */
enum class SubscriptionFeature(
    val title: String,
    val description: String,
    val requiresPremium: Boolean
) {
    // Free Plan Features
    PDF_VIEWING(
        title = "PDF Viewing",
        description = "High-resolution multi-page rendering via Android PdfRenderer",
        requiresPremium = false
    ),
    DOCUMENT_SCANNING(
        title = "Document Scanning",
        description = "Google ML Kit Document Scanner with edge detection & perspective correction",
        requiresPremium = false
    ),
    FREEHAND_DRAWING(
        title = "Freehand Ink Drawing",
        description = "Normalized vector ink strokes mapped to 72-DPI PDFBox paths",
        requiresPremium = false
    ),
    TEXT_HIGHLIGHTING(
        title = "Text Highlighting",
        description = "Semi-transparent multiply-blend marker highlights on PDF pages",
        requiresPremium = false
    ),

    // Premium Plan Features
    OCR_TEXT_EXTRACTION(
        title = "OCR Text Extraction",
        description = "Google ML Kit Text Recognition v2 with normalized bounding boxes",
        requiresPremium = true
    ),
    DIRECT_TEXT_EDITING(
        title = "Direct Text Block Editing",
        description = "In-place PDF text block redaction and replacement on 72-DPI canvas",
        requiresPremium = true
    ),
    TYPOGRAPHY_CUSTOMIZATION(
        title = "Font, Size & Line Spacing",
        description = "Customize font family, point size, alignment, bold/italic & line spacing",
        requiresPremium = true
    ),
    TEXT_TO_SPEECH(
        title = "Text-to-Speech Reader",
        description = "Read extracted OCR or PDF page text aloud with playback controls",
        requiresPremium = true
    )
}

/**
 * Reactive manager for the user's subscription tier (Free vs. Premium).
 * Defaults to `false` (Free Plan) and provides a toggle function for testing
 * Premium Plan capabilities at runtime.
 */
class SubscriptionManager(context: Context? = null) {

    private val prefs: SharedPreferences? = context?.applicationContext?.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _isPremiumUser = MutableStateFlow(
        prefs?.getBoolean(KEY_IS_PREMIUM, false) ?: false
    )

    /**
     * Reactive stream emitting `true` when the user has Premium access unlocked,
     * or `false` when on the Free Plan.
     */
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    /**
     * Toggles between Free Plan (`false`) and Premium Plan (`true`) for testing
     * and live feature-gate verification.
     */
    fun togglePremiumStatus(): Boolean {
        val nextValue = !_isPremiumUser.value
        setPremiumStatus(nextValue)
        return nextValue
    }

    /**
     * Explicitly sets the user's Premium status and persists it if a Context was provided.
     */
    fun setPremiumStatus(isPremium: Boolean) {
        _isPremiumUser.update { isPremium }
        prefs?.edit()?.putBoolean(KEY_IS_PREMIUM, isPremium)?.apply()
    }

    /**
     * Returns `true` if the specified [feature] is accessible under the current plan.
     */
    fun canAccessFeature(feature: SubscriptionFeature): Boolean {
        return !feature.requiresPremium || _isPremiumUser.value
    }

    /**
     * Executes [onAllowed] if the current subscription allows [feature], otherwise invokes
     * [onPaywallRequired] so the UI can display an upgrade prompt.
     */
    inline fun withFeatureAccess(
        feature: SubscriptionFeature,
        onPaywallRequired: (SubscriptionFeature) -> Unit = {},
        onAllowed: () -> Unit
    ) {
        if (canAccessFeature(feature)) {
            onAllowed()
        } else {
            onPaywallRequired(feature)
        }
    }

    companion object {
        private const val PREFS_NAME = "pdf_z_scanner_subscription_prefs"
        private const val KEY_IS_PREMIUM = "is_premium_user"

        @Volatile
        private var INSTANCE: SubscriptionManager? = null

        fun getInstance(context: Context? = null): SubscriptionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SubscriptionManager(context).also { INSTANCE = it }
            }
        }
    }
}
