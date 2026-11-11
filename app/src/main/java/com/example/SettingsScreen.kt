package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.InterFontFamily

// Exact Stitch Settings Color Tokens (from /public/assets/aistudio/code_0e934d2e.html)
private val LightSettingsSurface = Color(0xFFFFF8F7)
private val LightSurfaceContainerLow = Color(0xFFFFF0EE)
private val LightSurfaceContainer = Color(0xFFFFE9E6)
private val LightSurfaceContainerHigh = Color(0xFFFFE2DE)
private val LightSurfaceVariant = Color(0xFFFFDAD5)
private val LightSurfaceLowest = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF2A1614)
private val LightOnSurfaceVariant = Color(0xFF5E3F3A)

// Dark Mode Counterparts (when "Dark" is selected on the App Theme segmented toggle)
private val DarkSettingsSurface = Color(0xFF1A1110)
private val DarkSurfaceContainerLow = Color(0xFF271A18)
private val DarkSurfaceContainer = Color(0xFF32211E)
private val DarkSurfaceContainerHigh = Color(0xFF3E2926)
private val DarkSurfaceVariant = Color(0xFF4A312D)
private val DarkSurfaceLowest = Color(0xFF221614)
private val DarkOnSurface = Color(0xFFFFEDEA)
private val DarkOnSurfaceVariant = Color(0xFFD8C2BE)

// Shared Brand & VIP Card Tokens
private val SettingsPrimaryRed = Color(0xFFBC0003)
private val SettingsInverseSurface = Color(0xFF412B28)
private val SettingsInverseOnSurface = Color(0xFFFFEDEA)
private val SettingsTertiaryContainer = Color(0xFF0D70E9)
private val SettingsTertiaryFixed = Color(0xFFD8E2FF)
private val SettingsAmberBadgeText = Color(0xFFFCD34D)
private val SettingsAmberBadgeBg = Color(0x33FBBF24)
private val SettingsLogoPlaceholderBg = Color(0xFFCFD4D8)

private data class SettingsPalette(
    val surface: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceVariant: Color,
    val surfaceLowest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color
)

/**
 * Production Jetpack Compose implementation of the Stitch AI "Settings" screen
 * (`code_0e934d2e.html` & `screen_787024b8.png`), wired to [SettingsViewModel] (Jetpack DataStore),
 * Dark/Light segmented theme switches, OCR language picker, mobile data guard switch,
 * and the [SubscriptionManager] VIP paywall dialog.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onOpenPremiumPaywall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPremiumUser by viewModel.isPremiumUser.collectAsStateWithLifecycle()
    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val ocrLanguage by viewModel.ocrLanguage.collectAsStateWithLifecycle()
    val askBeforeMobileData by viewModel.askBeforeMobileData.collectAsStateWithLifecycle()
    val accountEmail by viewModel.accountEmail.collectAsStateWithLifecycle()
    val accountName by viewModel.accountName.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchVisible by viewModel.isSearchVisible.collectAsStateWithLifecycle()
    val showOcrLanguageDialog by viewModel.showOcrLanguageDialog.collectAsStateWithLifecycle()
    val showAccountDialog by viewModel.showAccountDialog.collectAsStateWithLifecycle()
    val showFeedbackDialog by viewModel.showFeedbackDialog.collectAsStateWithLifecycle()
    val activeLegalDocument by viewModel.activeLegalDocument.collectAsStateWithLifecycle()

    val isDark = appThemeMode == AppThemeMode.DARK
    val palette = remember(isDark) {
        if (isDark) {
            SettingsPalette(
                surface = DarkSettingsSurface,
                surfaceContainerLow = DarkSurfaceContainerLow,
                surfaceContainer = DarkSurfaceContainer,
                surfaceContainerHigh = DarkSurfaceContainerHigh,
                surfaceVariant = DarkSurfaceVariant,
                surfaceLowest = DarkSurfaceLowest,
                onSurface = DarkOnSurface,
                onSurfaceVariant = DarkOnSurfaceVariant
            )
        } else {
            SettingsPalette(
                surface = LightSettingsSurface,
                surfaceContainerLow = LightSurfaceContainerLow,
                surfaceContainer = LightSurfaceContainer,
                surfaceContainerHigh = LightSurfaceContainerHigh,
                surfaceVariant = LightSurfaceVariant,
                surfaceLowest = LightSurfaceLowest,
                onSurface = LightOnSurface,
                onSurfaceVariant = LightOnSurfaceVariant
            )
        }
    }

    val normalizedQuery = searchQuery.trim().lowercase()
    fun matchesSearch(vararg keywords: String): Boolean {
        if (normalizedQuery.isEmpty()) return true
        return keywords.any { it.lowercase().contains(normalizedQuery) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.surface)
            .testTag("settings_screen_root")
    ) {
        // Fixed Top Header matching Stitch (`h-16 px-4 flex items-center justify-between`)
        StitchSettingsTopHeader(
            palette = palette,
            isSignedIn = accountEmail != null,
            onSearchClick = { viewModel.toggleSearchVisibility() },
            onAvatarClick = { viewModel.openAccountDialog() }
        )

        // Expandable Search Bar when lens icon is tapped
        AnimatedVisibility(
            visible = isSearchVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = {
                    Text(
                        text = "Search settings (e.g. Theme, OCR, VIP, Privacy)...",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = palette.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = palette.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    IconButton(onClick = { viewModel.toggleSearchVisibility() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close settings search",
                            tint = palette.onSurfaceVariant
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(999.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = palette.surfaceContainerLow,
                    unfocusedContainerColor = palette.surfaceContainerLow,
                    focusedBorderColor = SettingsPrimaryRed,
                    unfocusedBorderColor = palette.surfaceVariant,
                    focusedTextColor = palette.onSurface,
                    unfocusedTextColor = palette.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("settings_search_input")
            )
        }

        // Main Scrollable Settings Content (`px-4 space-y-6 pb-8`)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Account Profile Card + Dark Promotional VIP Upgrade Card
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (matchesSearch("log in", "sign up", "account", "profile", accountEmail ?: "")) {
                    StitchAccountProfileCard(
                        palette = palette,
                        accountName = accountName,
                        accountEmail = accountEmail,
                        isPremiumUser = isPremiumUser,
                        onClick = { viewModel.openAccountDialog() }
                    )
                }

                if (matchesSearch("vip", "premium", "pro", "upgrade", "ocr", "cloud", "sync")) {
                    StitchVipPromotionalCard(
                        isPremiumUser = isPremiumUser,
                        onUpgradeClick = onOpenPremiumPaywall
                    )
                }
            }

            // Section 2: GENERAL PREFERENCES
            val showGeneralSection = matchesSearch(
                "general", "membership", "free", "vip", "theme", "light", "dark",
                "ocr", "language", "english", "mobile data", "sync"
            )
            if (showGeneralSection) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "GENERAL PREFERENCES",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        color = palette.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.surfaceContainerLow,
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Item 1: Membership Free (or VIP Pro)
                            if (matchesSearch("membership", "free", "vip", "plan")) {
                                StitchMembershipPreferenceRow(
                                    palette = palette,
                                    isPremiumUser = isPremiumUser,
                                    onClick = onOpenPremiumPaywall
                                )
                            }

                            // Item 2: App Theme (M3 Segmented Pill Toggle Switch)
                            if (matchesSearch("app theme", "theme", "light", "dark", "appearance")) {
                                StitchAppThemePreferenceRow(
                                    palette = palette,
                                    selectedMode = appThemeMode,
                                    onModeSelected = { viewModel.setAppThemeMode(it) }
                                )
                            }

                            // Item 3: OCR language
                            if (matchesSearch("ocr", "language", ocrLanguage)) {
                                StitchOcrLanguagePreferenceRow(
                                    palette = palette,
                                    selectedLanguage = ocrLanguage,
                                    onClick = { viewModel.openOcrLanguageDialog() }
                                )
                            }

                            // Item 4: Ask before using mobile data
                            if (matchesSearch("ask before using mobile data", "mobile data", "cloud", "sync")) {
                                StitchMobileDataPreferenceRow(
                                    palette = palette,
                                    checked = askBeforeMobileData,
                                    onToggle = { viewModel.toggleAskBeforeMobileData() }
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: SUPPORT & ABOUT
            val showSupportSection = matchesSearch(
                "support", "about", "feedback", "privacy", "policy", "terms", "service", "version", "v4.8.2"
            )
            if (showSupportSection) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SUPPORT & ABOUT",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        color = palette.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.surfaceContainerLow,
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Item 1: Send Feedback
                            if (matchesSearch("send feedback", "feedback", "support")) {
                                StitchSupportRow(
                                    palette = palette,
                                    title = "Send Feedback",
                                    icon = Icons.Outlined.ChatBubbleOutline,
                                    testTag = "settings_send_feedback_row",
                                    onClick = { viewModel.openFeedbackDialog() }
                                )
                            }

                            // Item 2: Privacy Policy
                            if (matchesSearch("privacy policy", "privacy", "security")) {
                                StitchSupportRow(
                                    palette = palette,
                                    title = "Privacy Policy",
                                    icon = Icons.Outlined.Security,
                                    testTag = "settings_privacy_policy_row",
                                    onClick = { viewModel.openLegalDocument(LegalDocumentType.PRIVACY_POLICY) }
                                )
                            }

                            // Item 3: Terms of Service
                            if (matchesSearch("terms of service", "terms", "legal")) {
                                StitchSupportRow(
                                    palette = palette,
                                    title = "Terms of Service",
                                    icon = Icons.Outlined.Description,
                                    testTag = "settings_terms_of_service_row",
                                    onClick = { viewModel.openLegalDocument(LegalDocumentType.TERMS_OF_SERVICE) }
                                )
                            }

                            // Item 4: App Version (v4.8.2)
                            if (matchesSearch("app version", "version", "v4.8.2")) {
                                StitchAppVersionRow(palette = palette)
                            }
                        }
                    }
                }
            }
        }
    }

    // OCR Language Selection Dialog (persisted in Jetpack DataStore)
    if (showOcrLanguageDialog) {
        OcrLanguagePickerDialog(
            selectedLanguage = ocrLanguage,
            onSelectLanguage = { viewModel.setOcrLanguage(it) },
            onDismiss = { viewModel.dismissOcrLanguageDialog() }
        )
    }

    // Account Log in / Sign up Dialog (persisted in Jetpack DataStore)
    if (showAccountDialog) {
        AccountSignInDialog(
            currentName = accountName,
            currentEmail = accountEmail,
            isPremiumUser = isPremiumUser,
            onSignIn = { name, email -> viewModel.signInAccount(name, email) },
            onSignOut = { viewModel.signOutAccount() },
            onOpenVipPaywall = {
                viewModel.dismissAccountDialog()
                onOpenPremiumPaywall()
            },
            onDismiss = { viewModel.dismissAccountDialog() }
        )
    }

    // Send Feedback Dialog
    if (showFeedbackDialog) {
        SendFeedbackDialog(
            onSubmit = { message, rating -> viewModel.submitFeedback(message, rating) },
            onDismiss = { viewModel.dismissFeedbackDialog() }
        )
    }

    // Privacy Policy / Terms of Service Dialog
    val legalDoc = activeLegalDocument
    if (legalDoc != null) {
        LegalDocumentDialog(
            documentType = legalDoc,
            onDismiss = { viewModel.dismissLegalDocument() }
        )
    }
}

/**
 * Top Header matching Stitch (`h-16 px-margin flex items-center justify-between`):
 * Left: `32.dp` `img` logo badge + `"Settings"` (`22.sp`, `SemiBold`, `#2A1614`)
 * Right: `search` icon button + `32.dp` `#BC0003` circle with white `person` icon
 */
@Composable
private fun StitchSettingsTopHeader(
    palette: SettingsPalette,
    isSignedIn: Boolean,
    onSearchClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(palette.surface.copy(alpha = 0.92f))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: `img` placeholder badge + "Settings"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.pdf_z_prism_logo_1793907355526),
                contentDescription = "PDF Z Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .shadow(elevation = 1.dp, shape = RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            )

            Text(
                text = "Settings",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = (-0.3).sp,
                color = palette.onSurface
            )
        }

        // Right: Search icon button + solid red circular person badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSearchClick)
                    .testTag("settings_search_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search settings",
                    tint = palette.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SettingsPrimaryRed)
                    .clickable(onClick = onAvatarClick)
                    .testTag("settings_header_avatar"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSignedIn) Icons.Default.Person else Icons.Outlined.Person,
                    contentDescription = "Account profile",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Stitch Profile & Account Card (`bg-surface-container-low rounded-2xl p-4 shadow-sm`):
 * Left: `56.dp` (`w-14 h-14`) circle `bg-primary/20` with `#BC0003` `person` icon (`28.dp`)
 * Center: `"Log in or Sign up"` (`16.sp`, `SemiBold`, `#2A1614`)
 * Right: `chevron_right` (`20.dp`, `#5E3F3A`)
 */
@Composable
private fun StitchAccountProfileCard(
    palette: SettingsPalette,
    accountName: String?,
    accountEmail: String?,
    isPremiumUser: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = palette.surfaceContainerLow,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_account_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 56.dp circular pink avatar (`bg-primary/20 text-primary`)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SettingsPrimaryRed.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = SettingsPrimaryRed,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (accountEmail != null) {
                        accountName ?: accountEmail
                    } else {
                        "Log in or Sign up"
                    },
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = palette.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (accountEmail != null) {
                    Text(
                        text = buildString {
                            append(accountEmail)
                            append(" • ")
                            append(if (isPremiumUser) "VIP Pro" else "Free Tier")
                        },
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = palette.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Account details",
                    tint = palette.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Stitch Dark Promotional VIP Upgrade Card (`rounded-2xl bg-inverse-surface text-inverse-on-surface p-4 shadow-lg`):
 * - Ambient blue/red glow radial gradients
 * - Header with `VIP PRO` amber pill badge + `LIMITED OFFER` + `$7.99` strikethrough + `$3.99/mo`
 * - Title `"PDF Z Premium VIP"` + subtitle `"High-accuracy document engine with unlimited cloud sync."`
 * - 3 feature rows with `#D8E2FF` (`tertiary-fixed`) filled `check_circle` icons
 * - Full-width `48.dp` `#0D70E9` (`tertiary-container`) rounded-full button `"Upgrade to VIP Now ->"`
 */
@Composable
private fun StitchVipPromotionalCard(
    isPremiumUser: Boolean,
    onUpgradeClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SettingsInverseSurface,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_vip_promo_card")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Ambient background glow circles matching Stitch (`bg-tertiary-container/20` and `bg-primary/20`)
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SettingsTertiaryContainer.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.92f, size.height * 0.1f),
                        radius = size.minDimension * 0.7f
                    ),
                    center = Offset(size.width * 0.92f, size.height * 0.1f),
                    radius = size.minDimension * 0.7f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            SettingsPrimaryRed.copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.08f, size.height * 0.92f),
                        radius = size.minDimension * 0.65f
                    ),
                    center = Offset(size.width * 0.08f, size.height * 0.92f),
                    radius = size.minDimension * 0.65f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card Header Row: VIP PRO badge + LIMITED OFFER + Price ($7.99 -> $3.99/mo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Amber VIP PRO pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(SettingsAmberBadgeBg)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = SettingsAmberBadgeText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "VIP PRO",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = SettingsAmberBadgeText
                                )
                            }
                        }

                        Text(
                            text = if (isPremiumUser) "ACTIVE PLAN" else "LIMITED OFFER",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            letterSpacing = 1.1.sp,
                            color = LightSurfaceVariant.copy(alpha = 0.70f)
                        )
                    }

                    // Price display: strikethrough $7.99 + bold $3.99/mo
                    Row(
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "$7.99",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            textDecoration = TextDecoration.LineThrough,
                            color = LightSurfaceVariant.copy(alpha = 0.60f),
                            modifier = Modifier.padding(end = 6.dp, bottom = 2.dp)
                        )
                        Text(
                            text = "$3.99",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            lineHeight = 26.sp,
                            color = SettingsInverseOnSurface
                        )
                        Text(
                            text = "/mo",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = LightSurfaceVariant.copy(alpha = 0.80f),
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }

                // Title & Subtitle
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "PDF Z Premium VIP",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        color = SettingsInverseOnSurface
                    )
                    Text(
                        text = "High-accuracy document engine with unlimited cloud sync.",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = LightSurfaceVariant.copy(alpha = 0.80f)
                    )
                }

                // VIP Feature Checklist (3 items with #D8E2FF check_circle icons)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    VipChecklistItem(text = "Unlimited high-precision OCR & text extraction")
                    VipChecklistItem(text = "E-signatures & synchronized cloud drives")
                    VipChecklistItem(text = "Batch multi-page scans & lossless compression")
                }

                // Call To Action Button (`bg-tertiary-container hover:bg-tertiary text-on-tertiary h-12 rounded-full`)
                Button(
                    onClick = onUpgradeClick,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SettingsTertiaryContainer,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("upgrade_vip_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isPremiumUser) {
                                "VIP Pro Active • Manage Plan"
                            } else {
                                "Upgrade to VIP Now"
                            },
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VipChecklistItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = SettingsTertiaryFixed,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            color = SettingsInverseOnSurface
        )
    }
}

/**
 * Item 1 in GENERAL PREFERENCES: `Membership Free` (or `Membership VIP Pro` when subscribed)
 * with `#FFDAD5` (`surface-variant`) `40.dp` rounded-xl box and `#BC0003` `card_membership` icon.
 */
@Composable
private fun StitchMembershipPreferenceRow(
    palette: SettingsPalette,
    isPremiumUser: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("settings_membership_row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            // 40.dp rounded-xl icon box (`bg-surface-variant text-primary`)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                MembershipCardVectorIcon(tint = SettingsPrimaryRed)
            }

            Text(
                text = if (isPremiumUser) "Membership VIP Pro" else "Membership Free",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = palette.onSurface
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = palette.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Custom vector for `card_membership` matching the Stitch icon (filled card with bottom ribbon).
 */
@Composable
private fun MembershipCardVectorIcon(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        // Card body
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.12f, h * 0.12f),
            size = Size(w * 0.76f, h * 0.58f),
            cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
            style = Stroke(width = w * 0.10f)
        )
        // Horizontal stripe inside card
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.46f),
            end = Offset(w * 0.85f, h * 0.46f),
            strokeWidth = w * 0.09f
        )
        // Bottom ribbon tails
        val ribbon = Path().apply {
            moveTo(w * 0.36f, h * 0.70f)
            lineTo(w * 0.36f, h * 0.92f)
            lineTo(w * 0.50f, h * 0.82f)
            lineTo(w * 0.64f, h * 0.92f)
            lineTo(w * 0.64f, h * 0.70f)
            close()
        }
        drawPath(path = ribbon, color = tint)
    }
}

/**
 * Item 2 in GENERAL PREFERENCES: `App Theme` with M3 Segmented Pill Toggle Switch (`Light` | `Dark`).
 */
@Composable
private fun StitchAppThemePreferenceRow(
    palette: SettingsPalette,
    selectedMode: AppThemeMode,
    onModeSelected: (AppThemeMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("settings_app_theme_row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = SettingsPrimaryRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "App Theme",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = palette.onSurface
            )
        }

        // M3 Segmented Toggle Switch (`bg-surface-container-high p-1 rounded-full flex items-center gap-1`)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(palette.surfaceContainerHigh)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Light Button
            val isLightSelected = selectedMode == AppThemeMode.LIGHT
            Surface(
                onClick = { onModeSelected(AppThemeMode.LIGHT) },
                shape = RoundedCornerShape(999.dp),
                color = if (isLightSelected) palette.surfaceLowest else Color.Transparent,
                shadowElevation = if (isLightSelected) 1.dp else 0.dp,
                modifier = Modifier.testTag("theme_light_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = null,
                        tint = if (isLightSelected) SettingsPrimaryRed else palette.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Light",
                        fontFamily = InterFontFamily,
                        fontWeight = if (isLightSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 11.sp,
                        color = if (isLightSelected) SettingsPrimaryRed else palette.onSurfaceVariant
                    )
                }
            }

            // Dark Button
            val isDarkSelected = selectedMode == AppThemeMode.DARK
            Surface(
                onClick = { onModeSelected(AppThemeMode.DARK) },
                shape = RoundedCornerShape(999.dp),
                color = if (isDarkSelected) palette.surfaceLowest else Color.Transparent,
                shadowElevation = if (isDarkSelected) 1.dp else 0.dp,
                modifier = Modifier.testTag("theme_dark_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DarkMode,
                        contentDescription = null,
                        tint = if (isDarkSelected) SettingsPrimaryRed else palette.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Dark",
                        fontFamily = InterFontFamily,
                        fontWeight = if (isDarkSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 11.sp,
                        color = if (isDarkSelected) SettingsPrimaryRed else palette.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Item 3 in GENERAL PREFERENCES: `OCR language` with `#FFE9E6` (`surface-container`) pill dropdown.
 */
@Composable
private fun StitchOcrLanguagePreferenceRow(
    palette: SettingsPalette,
    selectedLanguage: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag("settings_ocr_language_row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Translate,
                    contentDescription = null,
                    tint = SettingsPrimaryRed,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "OCR language",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = palette.onSurface
            )
        }

        // Language dropdown pill (`bg-surface-container px-3 py-1 rounded-full`)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(palette.surfaceContainer)
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = selectedLanguage,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = palette.onSurface
            )
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = "Select OCR language",
                tint = palette.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Item 4 in GENERAL PREFERENCES: `Ask before using mobile data` with custom Stitch M3 Switch (`w-12 h-7`).
 */
@Composable
private fun StitchMobileDataPreferenceRow(
    palette: SettingsPalette,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(16.dp)
            .testTag("settings_mobile_data_row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CloudSyncMinimalVectorIcon(tint = SettingsPrimaryRed)
            }

            Text(
                text = "Ask before using mobile data",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = palette.onSurface
            )
        }

        // Custom Stitch M3 Switch (`w-12 h-7 bg-primary rounded-full p-1`)
        StitchCheckmarkSwitch(
            checked = checked,
            palette = palette,
            onToggle = onToggle
        )
    }
}

/**
 * Custom vector matching the Stitch `cloud_sync` icon inside the `40.dp` rounded-xl box.
 */
@Composable
private fun CloudSyncMinimalVectorIcon(tint: Color) {
    Canvas(modifier = Modifier.size(20.dp)) {
        val w = size.width
        val h = size.height
        // Left mini cloud
        val cloudPath = Path().apply {
            moveTo(w * 0.16f, h * 0.62f)
            lineTo(w * 0.44f, h * 0.62f)
            cubicTo(w * 0.50f, h * 0.62f, w * 0.50f, h * 0.46f, w * 0.42f, h * 0.46f)
            cubicTo(w * 0.40f, h * 0.34f, w * 0.22f, h * 0.34f, w * 0.20f, h * 0.46f)
            cubicTo(w * 0.12f, h * 0.46f, w * 0.12f, h * 0.62f, w * 0.16f, h * 0.62f)
            close()
        }
        drawPath(path = cloudPath, color = tint)

        // Right sync arrows arc
        drawArc(
            color = tint,
            startAngle = -60f,
            sweepAngle = 240f,
            useCenter = false,
            topLeft = Offset(w * 0.56f, h * 0.38f),
            size = Size(w * 0.26f, h * 0.26f),
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Stitch M3 Switch Toggle (`w-12 h-7 bg-primary rounded-full p-1` with `w-5 h-5` thumb and `check` icon).
 */
@Composable
private fun StitchCheckmarkSwitch(
    checked: Boolean,
    palette: SettingsPalette,
    onToggle: () -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) SettingsPrimaryRed else palette.surfaceVariant,
        animationSpec = tween(durationMillis = 180),
        label = "switch_track_color"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = tween(durationMillis = 180),
        label = "switch_thumb_offset"
    )

    Box(
        modifier = Modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor)
            .clickable(onClick = onToggle)
            .padding(4.dp)
            .testTag("settings_mobile_data_switch"),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(20.dp)
                .shadow(elevation = 1.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(if (checked) Color.White else palette.onSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = SettingsPrimaryRed,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Support & About row (`Send Feedback`, `Privacy Policy`, `Terms of Service`).
 */
@Composable
private fun StitchSupportRow(
    palette: SettingsPalette,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    testTag: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = title,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = palette.onSurface
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = palette.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Item 4 in SUPPORT & ABOUT: `App Version` (`v4.8.2`) with `40.dp` rounded-xl `img` badge.
 */
@Composable
private fun StitchAppVersionRow(palette: SettingsPalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("settings_app_version_row"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.pdf_z_prism_logo_1793907355526),
            contentDescription = "PDF Z Logo",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .shadow(elevation = 1.dp, shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "App Version",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = palette.onSurface
            )
            Text(
                text = "v4.8.2",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = palette.onSurfaceVariant
            )
        }
    }
}

/**
 * Dialog for picking the OCR Recognition Language, persisted in Jetpack DataStore.
 */
@Composable
private fun OcrLanguagePickerDialog(
    selectedLanguage: String,
    onSelectLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = LightSettingsSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ocr_language_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(LightSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                tint = SettingsPrimaryRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "OCR Recognition Language",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = LightOnSurface
                            )
                            Text(
                                text = "ML Kit Text Recognition v2 engine",
                                fontFamily = InterFontFamily,
                                fontSize = 11.sp,
                                color = LightOnSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = LightOnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SettingsViewModel.SUPPORTED_OCR_LANGUAGES.forEach { option ->
                        val isSelected = option.displayName == selectedLanguage
                        Surface(
                            onClick = { onSelectLanguage(option.displayName) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) LightSurfaceContainerHigh else Color.Transparent,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectLanguage(option.displayName) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = SettingsPrimaryRed,
                                        unselectedColor = LightOnSurfaceVariant
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = option.displayName,
                                        fontFamily = InterFontFamily,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = LightOnSurface
                                    )
                                    Text(
                                        text = option.scriptHint,
                                        fontFamily = InterFontFamily,
                                        fontSize = 11.sp,
                                        color = LightOnSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Interactive Account Sign-In / Profile Dialog opened from the "Log in or Sign up" card.
 */
@Composable
private fun AccountSignInDialog(
    currentName: String?,
    currentEmail: String?,
    isPremiumUser: Boolean,
    onSignIn: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onOpenVipPaywall: () -> Unit,
    onDismiss: () -> Unit
) {
    var nameInput by remember(currentName) { mutableStateOf(currentName ?: "") }
    var emailInput by remember(currentEmail) { mutableStateOf(currentEmail ?: "alex.morgan@pdfzscanner.io") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = LightSettingsSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("account_sign_in_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SettingsPrimaryRed.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = SettingsPrimaryRed,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentEmail != null) "PDF Z Account" else "Log in or Sign up",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = LightOnSurface
                        )
                        Text(
                            text = if (isPremiumUser) "VIP Pro Member • Cloud Sync Enabled" else "Sync documents & preferences",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = LightOnSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Full Name", fontFamily = InterFontFamily) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SettingsPrimaryRed,
                        unfocusedBorderColor = LightSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email Address", fontFamily = InterFontFamily) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SettingsPrimaryRed,
                        unfocusedBorderColor = LightSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (currentEmail != null) {
                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Sign Out",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = SettingsPrimaryRed
                            )
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Cancel",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = LightOnSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { onSignIn(nameInput, emailInput) },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsPrimaryRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (currentEmail != null) "Save Profile" else "Continue",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }

                if (!isPremiumUser) {
                    TextButton(
                        onClick = onOpenVipPaywall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Upgrade to PDF Z Premium VIP ($3.99/mo) →",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = SettingsTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * Interactive Send Feedback Dialog.
 */
@Composable
private fun SendFeedbackDialog(
    onSubmit: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var feedbackText by remember { mutableStateOf("") }
    var selectedStars by remember { mutableIntStateOf(5) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = LightSettingsSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Send Feedback",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = LightOnSurface
                )

                Text(
                    text = "How is your experience with PDF Z Scanner v4.8.2?",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = LightOnSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    for (star in 1..5) {
                        IconButton(onClick = { selectedStars = star }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$star stars",
                                tint = if (star <= selectedStars) Color(0xFFF59E0B) else LightSurfaceVariant,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    placeholder = {
                        Text(
                            text = "Tell us what you love or what we can improve...",
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp
                        )
                    },
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SettingsPrimaryRed,
                        unfocusedBorderColor = LightSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = LightOnSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSubmit(feedbackText, selectedStars) },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsPrimaryRed)
                    ) {
                        Text(
                            text = "Send Feedback",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Legal Document Dialog for Privacy Policy & Terms of Service.
 */
@Composable
private fun LegalDocumentDialog(
    documentType: LegalDocumentType,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = LightSettingsSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (documentType == LegalDocumentType.PRIVACY_POLICY) {
                            Icons.Default.Security
                        } else {
                            Icons.Outlined.Description
                        },
                        contentDescription = null,
                        tint = SettingsPrimaryRed
                    )
                    Text(
                        text = documentType.title,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = LightOnSurface
                    )
                }

                Text(
                    text = if (documentType == LegalDocumentType.PRIVACY_POLICY) {
                        "PDF Z Scanner processes your scanned documents, PDF annotations, and ML Kit Optical Character Recognition (OCR) directly on your Android device. Local PDF files are stored privately inside your app's internal storage and Room Database.\n\nCloud synchronization with Google Drive, Dropbox, OneDrive, and SharePoint is strictly opt-in for PDF Z Premium VIP subscribers and respects your \"Ask before using mobile data\" preference."
                    } else {
                        "By using PDF Z Scanner v4.8.2, you agree that Free Plan features (PDF Viewing, ML Kit Document Scanning, Freehand Drawing, and Text Highlighting) are provided free of charge.\n\nPDF Z Premium VIP ($3.99/mo promotional rate) unlocks unlimited high-precision OCR & text extraction, direct 72-DPI text block editing, E-signatures & synchronized cloud drives, and batch multi-page compression."
                    },
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = LightOnSurfaceVariant
                )

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SettingsPrimaryRed),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "Done",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
