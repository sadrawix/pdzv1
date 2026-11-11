package com.example

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.AppBackground
import com.example.ui.theme.BannerDark
import com.example.ui.theme.BrandRed
import com.example.ui.theme.GoldEnd
import com.example.ui.theme.GoldStart
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.PillPink
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stitch AI Home Screen implementation preserving 100% of the uploaded visual design,
 * colors, custom SVG icons, horizontal PDF cover carousel, dark gold-accented Premium
 * banner, and 5-tab Bottom Navigation Bar wired to [HomeViewModel] and [DocumentScannerController].
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToPdfViewer: (Long) -> Unit
) {
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedBottomTab.collectAsStateWithLifecycle()
    val filteredDocuments by viewModel.filteredDocuments.collectAsStateWithLifecycle()
    val allRecentDocuments by viewModel.allRecentDocuments.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchBarVisible by viewModel.isSearchBarVisible.collectAsStateWithLifecycle()
    val isPremiumUser by viewModel.isPremiumUser.collectAsStateWithLifecycle()
    val showPremiumDialog by viewModel.showPremiumDialog.collectAsStateWithLifecycle()
    val isProcessingScan by viewModel.isProcessingScan.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val filesViewModelFactory = remember(viewModel.repositoryRef, viewModel.subscriptionManagerRef) {
        FilesViewModel.Factory(viewModel.repositoryRef, viewModel.subscriptionManagerRef)
    }
    val filesViewModel: FilesViewModel = viewModel(factory = filesViewModelFactory)
    val filesSnackbarMessage by filesViewModel.snackbarMessage.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val settingsViewModelFactory = remember(context, viewModel.subscriptionManagerRef) {
        SettingsViewModel.Factory(context, viewModel.subscriptionManagerRef)
    }
    val settingsViewModel: SettingsViewModel = viewModel(factory = settingsViewModelFactory)
    val settingsSnackbarMessage by settingsViewModel.snackbarMessage.collectAsStateWithLifecycle()

    val toolsViewModelFactory = remember(viewModel.repositoryRef, viewModel.subscriptionManagerRef) {
        ToolsViewModel.Factory(viewModel.repositoryRef, viewModel.subscriptionManagerRef)
    }
    val toolsViewModel: ToolsViewModel = viewModel(factory = toolsViewModelFactory)
    val toolsSnackbarMessage by toolsViewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbarMessage()
        }
    }

    LaunchedEffect(filesSnackbarMessage) {
        val msg = filesSnackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            filesViewModel.clearSnackbarMessage()
        }
    }

    LaunchedEffect(settingsSnackbarMessage) {
        val msg = settingsSnackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            settingsViewModel.clearSnackbarMessage()
        }
    }

    LaunchedEffect(toolsSnackbarMessage) {
        val msg = toolsSnackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            toolsViewModel.clearSnackbarMessage()
        }
    }

    var shouldFallbackNavigateOnError by remember { mutableStateOf(false) }

    // Google ML Kit Document Scanner Launcher (SCANNER_MODE_FULL, PDF + JPEG, multi-page)
    val scannerController = rememberDocumentScannerLauncher(
        onScanResult = { pdfUri, jpegUris ->
            shouldFallbackNavigateOnError = false
            viewModel.onMlKitScanCompleted(
                pdfUri = pdfUri,
                jpegPageUris = jpegUris,
                onNavigateToViewer = onNavigateToPdfViewer
            )
        },
        onScanCancelled = {
            shouldFallbackNavigateOnError = false
        },
        onScannerError = { error ->
            if (shouldFallbackNavigateOnError) {
                shouldFallbackNavigateOnError = false
                viewModel.onScannerHardwareUnavailableFallback(
                    error = error,
                    onNavigateToViewer = onNavigateToPdfViewer
                )
            }
        }
    )

    // System PDF file picker for importing external PDFs
    val importPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importExternalPdf(
                uri = uri,
                onNavigateToViewer = onNavigateToPdfViewer
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            StitchBottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { tab ->
                    viewModel.selectBottomTab(tab)
                    if (tab == HomeBottomTab.SCANNER) {
                        // Trigger GmsDocumentScanner on opening the Scanner tab; if unavailable
                        // on a virtual emulator, stay on ScannerViewfinderScreen until Shutter tap
                        shouldFallbackNavigateOnError = false
                        scannerController.launchScanner()
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            val showDefaultHomeHeader = selectedTab == HomeBottomTab.HOME

            if (showDefaultHomeHeader) {
                // Subtle decorative horizontal red gradient line at top (matches Stitch `top-28 h-[2px]`)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 68.dp)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    BrandRed.copy(alpha = 0.40f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                if (showDefaultHomeHeader) {
                    // Stitch App Header (Logo + "PDF Z" + Search + Red-Bordered Profile Avatar)
                    StitchAppHeader(
                        isPremium = isPremiumUser,
                        onSearchClick = { viewModel.toggleSearchBar() },
                        onProfileClick = { viewModel.openPremiumUpgradeDialog() }
                    )

                    // Expandable inline search bar when user taps the Search lens icon
                    AnimatedVisibility(
                        visible = isSearchBarVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = {
                                Text(
                                    text = "Search PDF files...",
                                    fontFamily = InterFontFamily,
                                    fontSize = 14.sp
                                )
                            },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { viewModel.toggleSearchBar() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close search"
                                    )
                                }
                            },
                            shape = RoundedCornerShape(999.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = BrandRed,
                                unfocusedBorderColor = Slate200
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                                .testTag("home_search_input")
                        )
                    }
                }

                // Active Tab Body
                when (selectedTab) {
                    HomeBottomTab.HOME -> {
                        StitchHomeTabContent(
                            selectedFilter = selectedFilter,
                            documents = filteredDocuments,
                            isPremiumUser = isPremiumUser,
                            onSelectFilter = { viewModel.selectFilter(it) },
                            onDocumentClick = { doc ->
                                viewModel.openDocument(doc, onNavigateToPdfViewer)
                            },
                            onStartFreeTrialClick = {
                                viewModel.openPremiumUpgradeDialog()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HomeBottomTab.FILES -> {
                        FilesScreen(
                            viewModel = filesViewModel,
                            onNavigateToPdfViewer = onNavigateToPdfViewer,
                            onOpenPremiumPaywall = {
                                viewModel.openPremiumUpgradeDialog()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HomeBottomTab.TOOLS -> {
                        ToolsScreen(
                            viewModel = toolsViewModel,
                            onNavigateToPdfViewer = onNavigateToPdfViewer,
                            onOpenPremiumPaywall = { viewModel.openPremiumUpgradeDialog() },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HomeBottomTab.SETTINGS -> {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onOpenPremiumPaywall = { viewModel.openPremiumUpgradeDialog() },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HomeBottomTab.SCANNER -> {
                        ScannerViewfinderScreen(
                            onTriggerMlKitScanner = {
                                shouldFallbackNavigateOnError = true
                                scannerController.launchScanner()
                            },
                            onGalleryImagesSelected = { imageUris ->
                                viewModel.onMlKitScanCompleted(
                                    pdfUri = null,
                                    jpegPageUris = imageUris,
                                    onNavigateToViewer = onNavigateToPdfViewer
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (isProcessingScan) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                color = BrandRed,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Saving PDF & rendering cover...",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate900
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPremiumDialog) {
        PremiumUpgradeDialog(
            isPremiumUser = isPremiumUser,
            onDismiss = { viewModel.dismissPremiumUpgradeDialog() },
            onStartFreeTrial = { viewModel.startFreeTrialAndActivatePremium() },
            onTogglePlanForTesting = {
                viewModel.togglePremiumForTesting()
                viewModel.dismissPremiumUpgradeDialog()
            }
        )
    }
}

/**
 * Exact Stitch App Header:
 * Left: 32dp rounded logo badge + "PDF" (slate-900, font-black) + "Z" (brand-red, font-black)
 * Right: Custom Search icon + 40dp circular User Profile with 2dp brand-red border.
 */
@Composable
private fun StitchAppHeader(
    isPremium: Boolean,
    onSearchClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand Logo + "PDF Z"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("brand_header_logo")
        ) {
            // 32dp x 32dp rounded-lg logo icon
            Image(
                painter = painterResource(id = R.drawable.pdf_z_prism_logo_1793907355526),
                contentDescription = "PDF Z Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(32.dp)
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "PDF",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp,
                    color = Slate900
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Z",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = (-0.5).sp,
                    color = BrandRed
                )
            }
        }

        // Right Action Icons: Search & Profile Avatar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Button (exact SVG from Stitch: circle cx=11 cy=11 r=7 + line M16.5 16.5L21.5 21.5)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSearchClick)
                    .padding(6.dp)
                    .testTag("header_search_button"),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(26.dp)) {
                    val scale = size.width / 24f
                    drawCircle(
                        color = Slate800,
                        radius = 7f * scale,
                        center = Offset(11f * scale, 11f * scale),
                        style = Stroke(width = 2.4f * scale)
                    )
                    drawLine(
                        color = Slate800,
                        start = Offset(16.5f * scale, 16.5f * scale),
                        end = Offset(21.5f * scale, 21.5f * scale),
                        strokeWidth = 2.4f * scale,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Circular User Profile (w-10 h-10 rounded-full border-2 border-brand-red p-0.5 bg-white)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .shadow(elevation = 1.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(
                        width = 2.dp,
                        color = if (isPremium) GoldStart else BrandRed,
                        shape = CircleShape
                    )
                    .clickable(onClick = onProfileClick)
                    .padding(2.dp)
                    .testTag("header_profile_button"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Slate200),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(26.dp)) {
                        val s = size.width / 24f
                        // Head circle: cx=12, cy=9, r=4
                        drawCircle(
                            color = Slate400,
                            radius = 4f * s,
                            center = Offset(12f * s, 9f * s)
                        )
                        // Shoulders arc path
                        val bodyPath = Path().apply {
                            moveTo(4f * s, 21f * s)
                            cubicTo(
                                4f * s, 16.5f * s,
                                20f * s, 16.5f * s,
                                20f * s, 21f * s
                            )
                            lineTo(20f * s, 23f * s)
                            lineTo(4f * s, 23f * s)
                            close()
                        }
                        drawPath(path = bodyPath, color = Slate400)
                    }
                }
            }
        }
    }
}

/**
 * Main Stitch Home Tab Content:
 * 1. "Recent files" section title
 * 2. "Downloads" and "Opened" pill filter chips
 * 3. Horizontal PDF cards carousel (`114.dp x 154.dp`)
 * 4. "Save Time, Achieve More with Premium" dark gold-accented promotional banner with "Start free trial" CTA
 */
@Composable
private fun StitchHomeTabContent(
    selectedFilter: DocumentFilter,
    documents: List<PdfDocumentEntity>,
    isPremiumUser: Boolean,
    onSelectFilter: (DocumentFilter) -> Unit,
    onDocumentClick: (PdfDocumentEntity) -> Unit,
    onStartFreeTrialClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Section Header & Chips (`px-5 pt-3`)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Recent files",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                letterSpacing = (-0.5).sp,
                color = Slate900,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Filter Chips Row: "Downloads" and "Opened"
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StitchFilterPillChip(
                    label = "Downloads",
                    selected = selectedFilter == DocumentFilter.DOWNLOADS,
                    onClick = { onSelectFilter(DocumentFilter.DOWNLOADS) },
                    testTag = "chip_downloads"
                )

                StitchFilterPillChip(
                    label = "Opened",
                    selected = selectedFilter == DocumentFilter.OPENED,
                    onClick = { onSelectFilter(DocumentFilter.OPENED) },
                    testTag = "chip_opened"
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Carousel Cards (`mt-4 pl-5 flex space-x-4 py-2`)
        if (documents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(186.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedFilter == DocumentFilter.OPENED) {
                        "No opened PDF files yet. Tap a file in Downloads to open it."
                    } else {
                        "No downloaded PDF files found."
                    },
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = Slate600,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recent_files_carousel")
            ) {
                items(
                    items = documents,
                    key = { it.id }
                ) { doc ->
                    StitchHorizontalPdfCard(
                        document = doc,
                        onClick = { onDocumentClick(doc) }
                    )
                }

                // Partial peek card for carousel affordance matching Stitch Card 4
                item(key = "carousel_peek_affordance") {
                    Column(
                        modifier = Modifier
                            .width(28.dp)
                            .padding(end = 12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(154.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                                .background(Slate900.copy(alpha = 0.40f))
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "s...",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Slate800.copy(alpha = 0.40f),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Premium Banner Section (`px-4 mt-6 mb-4`)
        StitchPremiumBanner(
            isPremiumUser = isPremiumUser,
            onStartFreeTrialClick = onStartFreeTrialClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Stitch Filter Chip (`Downloads` / `Opened`):
 * - Active: `bg-pill-pink` (`#FCD5CE`), `text-slate-900`, `font-semibold`, `text-xs`, `px-3.5 py-1.5`, `rounded-full`, leading check icon
 * - Inactive: `border border-slate-200 bg-white text-slate-700 font-medium text-xs px-3.5 py-1.5 rounded-full`
 */
@Composable
private fun StitchFilterPillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val backgroundColor = if (selected) PillPink else Color.White
    val contentColor = if (selected) Slate900 else Slate700

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = if (selected) 1.dp else 0.dp,
        modifier = Modifier
            .then(
                if (!selected) {
                    Modifier.border(1.dp, Slate200, CircleShape)
                } else {
                    Modifier
                }
            )
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Slate900,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = label,
                fontFamily = InterFontFamily,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 12.sp,
                color = contentColor
            )
        }
    }
}

/**
 * Stitch Horizontal PDF Card (`w-[114px]` with `w-[114px] h-[154px] rounded-xl shadow-md` cover
 * and truncated filename underneath).
 *
 * Uses the real first-page cover thumbnail rendered by `android.graphics.pdf.PdfRenderer` and
 * preserves the exact Stitch artwork for the Welcome Guide, Fractal Trading book spread, and
 * generic PDF cards.
 */
@Composable
private fun StitchHorizontalPdfCard(
    document: PdfDocumentEntity,
    onClick: () -> Unit
) {
    val thumbnailBitmap by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = document.thumbnailPath
    ) {
        value = withContext(Dispatchers.IO) {
            val path = document.thumbnailPath ?: return@withContext null
            val file = File(path)
            if (file.exists() && file.length() > 0L) {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            } else {
                null
            }
        }
    }

    val isWelcomeGuide = document.name.contains("User guide", ignoreCase = true) ||
        document.name.contains("Welcome", ignoreCase = true)
    val isFractalBook2 = document.name.contains("fractal_trading(2)", ignoreCase = true)
    val isFractalBook1 = document.name.contains("fractal_trading(1)", ignoreCase = true)

    Column(
        modifier = Modifier
            .width(114.dp)
            .clickable(onClick = onClick)
            .testTag("pdf_card_${document.id}"),
        horizontalAlignment = Alignment.Start
    ) {
        // Cover Box: 114.dp x 154.dp rounded-xl shadow-md
        Box(
            modifier = Modifier
                .width(114.dp)
                .height(154.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .then(
                    if (!isWelcomeGuide) {
                        Modifier.border(1.dp, Slate100, RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                    }
                )
        ) {
            when {
                isWelcomeGuide -> {
                    // Exact Stitch Card 1: Welcome Guide Gradient Cover
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF2B2B4E),
                                        Color(0xFF544673),
                                        Color(0xFFEB5A66)
                                    )
                                )
                            )
                            .padding(10.dp)
                    ) {
                        // Top Center Brand Logo Badge
                        Image(
                            painter = painterResource(id = R.drawable.pdf_z_prism_logo_1793907355526),
                            contentDescription = "PDF Z Welcome Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(40.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .align(Alignment.TopCenter)
                        )

                        // Center/Bottom Subtitle
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Welcome Guide",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "PDF tools for productive people",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 6.5.sp,
                                lineHeight = 8.sp,
                                color = Color(0xFFFCE7F3),
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            text = "www.ilovepdf.com",
                            fontFamily = InterFontFamily,
                            fontSize = 5.5.sp,
                            color = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }

                isFractalBook2 -> {
                    // Exact Stitch Card 2: Fractal Book Scanned Page Preview
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .padding(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, Slate200, RoundedCornerShape(6.dp))
                        ) {
                            // Left dark page with spiral fractal SVG
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(Color(0xFF111111)),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(36.dp, 44.dp)) {
                                    val cx = size.width / 2f
                                    val cy = size.height / 2f
                                    drawCircle(
                                        color = Slate400.copy(alpha = 0.7f),
                                        radius = size.minDimension * 0.35f,
                                        center = Offset(cx, cy),
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                    drawCircle(
                                        color = Slate400.copy(alpha = 0.5f),
                                        radius = size.minDimension * 0.22f,
                                        center = Offset(cx, cy),
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 2.dp.toPx(),
                                        center = Offset(cx, cy)
                                    )
                                }
                            }

                            // Right scanned page with FRACTAL TRADING text
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(Color.White)
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "FRACTAL\nTRADING",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 5.5.sp,
                                    lineHeight = 7.sp,
                                    color = Slate800,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.5.dp)
                                        .background(Slate200, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(0.8f)
                                        .height(1.5.dp)
                                        .background(Slate200, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.5.dp)
                                        .background(Slate200, RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "Soul Esprit",
                                    fontFamily = InterFontFamily,
                                    fontSize = 4.5.sp,
                                    color = Slate400
                                )
                            }
                        }
                    }
                }

                isFractalBook1 -> {
                    // Exact Stitch Card 3: Generic PDF File Preview with Red PDF Tag & 3 Red Bars
                    StitchGenericPdfCoverContent()
                }

                thumbnailBitmap != null -> {
                    // Real first-page PDF cover rendered by Android PdfRenderer
                    Image(
                        bitmap = thumbnailBitmap!!,
                        contentDescription = "${document.name} cover preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                else -> {
                    StitchGenericPdfCoverContent()
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Truncated Document Name (`text-sm font-semibold text-slate-800 mt-2.5 truncate w-full tracking-tight`)
        Text(
            text = formatStitchFileName(document.name),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            letterSpacing = (-0.2).sp,
            color = Slate800,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun StitchGenericPdfCoverContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Red PDF Tag (`bg-brand-red text-white text-[9px] font-black px-1.5 py-0.5 rounded uppercase mb-1.5`)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(BrandRed)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = "PDF",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 9.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3 Red Horizontal Lines (`w-7 h-[2px] bg-brand-red rounded`)
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BrandRed)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BrandRed)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(2.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BrandRed)
        )
    }
}

/**
 * Formats filenames to match the middle-ellipsis style in the Stitch screenshot
 * (`User guide.pdf` -> `User g...e.pdf`, `fractal_trading(2).pdf` -> `fractal...2).pdf`).
 */
private fun formatStitchFileName(name: String): String {
    return when {
        name.equals("User guide.pdf", ignoreCase = true) -> "User g...e.pdf"
        name.equals("fractal_trading(2).pdf", ignoreCase = true) -> "fractal...2).pdf"
        name.equals("fractal_trading(1).pdf", ignoreCase = true) -> "fractal...1).pdf"
        name.length > 15 && name.endsWith(".pdf", ignoreCase = true) -> {
            val prefix = name.take(7)
            val suffix = name.takeLast(6)
            "$prefix...$suffix"
        }
        else -> name
    }
}

/**
 * Exact Stitch Promotional Banner:
 * - Container: `bg-[#0a0a0c] border border-amber-500/30 rounded-2xl py-5 px-4`
 * - Left & right glowing amber chevrons (`M2 8 L18 30 L2 52` and `M22 8 L6 30 L22 52`)
 * - Title: "Save Time, Achieve More with Premium"
 * - Button: Gold gradient `from-[#FFD700] to-[#FFA000]` "Start free trial" wired to [onStartFreeTrialClick].
 */
@Composable
private fun StitchPremiumBanner(
    isPremiumUser: Boolean,
    onStartFreeTrialClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(BannerDark)
            .border(
                width = 1.dp,
                color = Color(0xFFF59E0B).copy(alpha = 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        // Subtle horizontal amber ambient gradient overlay (`from-amber-500/10 via-transparent to-amber-500/10`)
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFF59E0B).copy(alpha = 0.14f),
                            Color.Transparent,
                            Color(0xFFF59E0B).copy(alpha = 0.14f)
                        )
                    )
                )
        )

        // Left Glowing Amber Chevron (`M2 8 L18 30 L2 52`)
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .size(width = 32.dp, height = 80.dp)
        ) {
            val sx = size.width / 24f
            val sy = size.height / 60f
            val chevronPath = Path().apply {
                moveTo(2f * sx, 8f * sy)
                lineTo(18f * sx, 30f * sy)
                lineTo(2f * sx, 52f * sy)
            }
            // Outer amber glow pass
            drawPath(
                path = chevronPath,
                color = Color(0xFFFBBF24).copy(alpha = 0.35f),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Crisp inner stroke
            drawPath(
                path = chevronPath,
                color = Color(0xFFFBBF24),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Right Glowing Amber Chevron (`M22 8 L6 30 L22 52`)
        Canvas(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .size(width = 32.dp, height = 80.dp)
        ) {
            val sx = size.width / 24f
            val sy = size.height / 60f
            val chevronPath = Path().apply {
                moveTo(22f * sx, 8f * sy)
                lineTo(6f * sx, 30f * sy)
                lineTo(22f * sx, 52f * sy)
            }
            // Outer amber glow pass
            drawPath(
                path = chevronPath,
                color = Color(0xFFFBBF24).copy(alpha = 0.35f),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Crisp inner stroke
            drawPath(
                path = chevronPath,
                color = Color(0xFFFBBF24),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Banner Text & Gold Button Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Save Time, Achieve More\nwith Premium",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                lineHeight = 25.sp,
                letterSpacing = (-0.3).sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Gold Gradient Button (`bg-gradient-to-r from-[#FFD700] to-[#FFA000]`)
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(8.dp),
                        ambientColor = Color(0xFFFFC107),
                        spotColor = Color(0xFFFFC107)
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(GoldStart, GoldEnd)
                        )
                    )
                    .clickable(onClick = onStartFreeTrialClick)
                    .padding(horizontal = 24.dp, vertical = 10.dp)
                    .testTag("start_free_trial_button"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPremiumUser) "Premium Active • Manage" else "Start free trial",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = Slate950
                )
            }
        }
    }
}

/**
 * Exact 5-Tab Stitch Bottom Navigation Bar:
 * 1. Home (Active state with soft pink pill `bg-pill-pink w-16 h-8 rounded-full`)
 * 2. Files (Folder icon)
 * 3. Tools (Wrench icon)
 * 4. Scanner (Viewfinder crop corners + center circle icon)
 * 5. Settings (Gear icon)
 */
@Composable
private fun StitchBottomNavigationBar(
    selectedTab: HomeBottomTab,
    onTabSelected: (HomeBottomTab) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        HorizontalDivider(thickness = 1.dp, color = Slate100)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            HomeBottomTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                val interactionSource = remember { MutableInteractionSource() }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onTabSelected(tab) }
                        )
                        .testTag("bottom_nav_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Pill container (64.dp x 32.dp)
                    Box(
                        modifier = Modifier
                            .width(64.dp)
                            .height(32.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) PillPink else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        StitchBottomNavIcon(
                            tab = tab,
                            isSelected = isSelected
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = tab.label,
                        fontFamily = InterFontFamily,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        color = if (isSelected) Slate900 else Slate600
                    )
                }
            }
        }
    }
}

/**
 * Custom vector Canvas icons matching the exact SVGs in the Stitch HTML Bottom Navigation.
 */
@Composable
private fun StitchBottomNavIcon(
    tab: HomeBottomTab,
    isSelected: Boolean
) {
    val iconColor = if (isSelected) Slate900 else Slate600

    Canvas(modifier = Modifier.size(22.dp)) {
        val s = size.width / 24f
        val strokeStyle = Stroke(
            width = 2f * s,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        when (tab) {
            HomeBottomTab.HOME -> {
                // Solid Home Icon: M10 20v-6h4v6h5v-8h3L12 3 2 12h3v8z
                val path = Path().apply {
                    moveTo(10f * s, 20f * s)
                    lineTo(10f * s, 14f * s)
                    lineTo(14f * s, 14f * s)
                    lineTo(14f * s, 20f * s)
                    lineTo(19f * s, 20f * s)
                    lineTo(19f * s, 12f * s)
                    lineTo(22f * s, 12f * s)
                    lineTo(12f * s, 3f * s)
                    lineTo(2f * s, 12f * s)
                    lineTo(5f * s, 12f * s)
                    lineTo(5f * s, 20f * s)
                    close()
                }
                drawPath(path = path, color = iconColor)
            }

            HomeBottomTab.FILES -> {
                // Folder Icon: M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z
                val path = Path().apply {
                    moveTo(3f * s, 6f * s)
                    lineTo(9f * s, 6f * s)
                    lineTo(11f * s, 8f * s)
                    lineTo(21f * s, 8f * s)
                    lineTo(21f * s, 19f * s)
                    lineTo(3f * s, 19f * s)
                    close()
                }
                drawPath(path = path, color = iconColor, style = strokeStyle)
            }

            HomeBottomTab.TOOLS -> {
                // Wrench Icon
                drawCircle(
                    color = iconColor,
                    radius = 4.2f * s,
                    center = Offset(16.5f * s, 7.5f * s),
                    style = strokeStyle
                )
                drawLine(
                    color = iconColor,
                    start = Offset(13.2f * s, 10.8f * s),
                    end = Offset(4.5f * s, 19.5f * s),
                    strokeWidth = 2.2f * s,
                    cap = StrokeCap.Round
                )
            }

            HomeBottomTab.SCANNER -> {
                // Viewfinder Crop Icon: M4 7V4h3 | M20 7V4h-3 | M4 17v3h3 | M20 17v3h-3 | circle(12,12,3)
                val corners = Path().apply {
                    // Top-left
                    moveTo(4f * s, 7.5f * s)
                    lineTo(4f * s, 4f * s)
                    lineTo(7.5f * s, 4f * s)
                    // Top-right
                    moveTo(20f * s, 7.5f * s)
                    lineTo(20f * s, 4f * s)
                    lineTo(16.5f * s, 4f * s)
                    // Bottom-left
                    moveTo(4f * s, 16.5f * s)
                    lineTo(4f * s, 20f * s)
                    lineTo(7.5f * s, 20f * s)
                    // Bottom-right
                    moveTo(20f * s, 16.5f * s)
                    lineTo(20f * s, 20f * s)
                    lineTo(16.5f * s, 20f * s)
                }
                drawPath(path = corners, color = iconColor, style = strokeStyle)
                drawCircle(
                    color = iconColor,
                    radius = 3f * s,
                    center = Offset(12f * s, 12f * s),
                    style = strokeStyle
                )
            }

            HomeBottomTab.SETTINGS -> {
                // Gear Icon: outer toothed ring + inner circle(12,12,3)
                drawCircle(
                    color = iconColor,
                    radius = 6.5f * s,
                    center = Offset(12f * s, 12f * s),
                    style = strokeStyle
                )
                drawCircle(
                    color = iconColor,
                    radius = 2.6f * s,
                    center = Offset(12f * s, 12f * s),
                    style = strokeStyle
                )
                // 8 gear teeth
                val angles = listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f)
                for (deg in angles) {
                    val rad = Math.toRadians(deg.toDouble())
                    val cos = kotlin.math.cos(rad).toFloat()
                    val sin = kotlin.math.sin(rad).toFloat()
                    drawLine(
                        color = iconColor,
                        start = Offset((12f + cos * 6.5f) * s, (12f + sin * 6.5f) * s),
                        end = Offset((12f + cos * 9.0f) * s, (12f + sin * 9.0f) * s),
                        strokeWidth = 2.1f * s,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * Secondary Tab: "Files" — Displays all local PDF documents in Room Database with cover
 * thumbnails, page counts, timestamps, and quick Import / Scan actions.
 */
@Composable
private fun StitchFilesTabContent(
    documents: List<PdfDocumentEntity>,
    onDocumentClick: (PdfDocumentEntity) -> Unit,
    onDeleteDocument: (PdfDocumentEntity) -> Unit,
    onImportPdfClick: () -> Unit,
    onScanNewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "All PDF Files (${documents.size})",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = Slate900
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = onImportPdfClick,
                    shape = CircleShape,
                    color = PillPink,
                    modifier = Modifier.testTag("files_import_pdf_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = "Import PDF",
                            tint = Slate900,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Import",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Slate900
                        )
                    }
                }

                Surface(
                    onClick = onScanNewClick,
                    shape = CircleShape,
                    color = BrandRed,
                    modifier = Modifier.testTag("files_scan_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Scan PDF",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Scan",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(documents, key = { it.id }) { doc ->
                val thumb by produceState<ImageBitmap?>(null, doc.thumbnailPath) {
                    value = withContext(Dispatchers.IO) {
                        val p = doc.thumbnailPath ?: return@withContext null
                        val f = File(p)
                        if (f.exists()) BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap() else null
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDocumentClick(doc) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Slate100)
                                .border(1.dp, Slate200, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (thumb != null) {
                                Image(
                                    bitmap = thumb!!,
                                    contentDescription = doc.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = BrandRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.name,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${doc.pageCount} page${if (doc.pageCount == 1) "" else "s"} • ${
                                    dateFormat.format(Date(doc.lastOpenedAt))
                                }",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = Slate600
                            )
                        }

                        IconButton(onClick = { onDeleteDocument(doc) }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete ${doc.name}",
                                tint = Slate400
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Secondary Tab: "Tools" — Displays Free Plan and Premium Plan PDF tools clearly separated.
 */
@Composable
private fun StitchToolsTabContent(
    isPremiumUser: Boolean,
    onScanClick: () -> Unit,
    onImportPdfClick: () -> Unit,
    onOpenRecentForTool: () -> Unit,
    onRequirePremium: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "PDF Studio Tools",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = Slate900
        )

        Text(
            text = "Free Plan Tools",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Slate600
        )

        ToolRowCard(
            title = "ML Kit Document Scanner",
            subtitle = "Multi-page capture with automatic perspective correction",
            icon = Icons.Default.DocumentScanner,
            isLocked = false,
            onClick = onScanClick
        )
        ToolRowCard(
            title = "Import & View PDF",
            subtitle = "Native PdfRenderer viewing with pinch-to-zoom",
            icon = Icons.Default.FileOpen,
            isLocked = false,
            onClick = onImportPdfClick
        )
        ToolRowCard(
            title = "Freehand Ink & Highlighting",
            subtitle = "Normalized 72-DPI vector strokes and marker highlights",
            icon = Icons.Default.Draw,
            isLocked = false,
            onClick = onOpenRecentForTool
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Premium Plan Tools",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = BrandRed
        )

        ToolRowCard(
            title = "ML Kit OCR Text Extraction",
            subtitle = "Extract recognized text blocks with normalized bounding boxes",
            icon = Icons.Default.TextFields,
            isLocked = !isPremiumUser,
            onClick = { if (isPremiumUser) onOpenRecentForTool() else onRequirePremium() }
        )
        ToolRowCard(
            title = "Direct Text Block Editing",
            subtitle = "Edit PDF text blocks directly on the 72-DPI canvas",
            icon = Icons.Default.EditNote,
            isLocked = !isPremiumUser,
            onClick = { if (isPremiumUser) onOpenRecentForTool() else onRequirePremium() }
        )
        ToolRowCard(
            title = "Font, Size & Line Spacing",
            subtitle = "Customize Helvetica, Times-Roman, Courier, size & spacing",
            icon = Icons.Default.FormatSize,
            isLocked = !isPremiumUser,
            onClick = { if (isPremiumUser) onOpenRecentForTool() else onRequirePremium() }
        )
        ToolRowCard(
            title = "Text-to-Speech Reader",
            subtitle = "Listen to extracted OCR text read aloud",
            icon = Icons.Default.RecordVoiceOver,
            isLocked = !isPremiumUser,
            onClick = { if (isPremiumUser) onOpenRecentForTool() else onRequirePremium() }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ToolRowCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isLocked) Color(0xFFFEF3C7) else PillPink),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isLocked) Color(0xFFD97706) else BrandRed,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Slate900
                )
                Text(
                    text = subtitle,
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = Slate600
                )
            }
            if (isLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Premium Required",
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Unlocked",
                    tint = Color(0xFF16A34A),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Secondary Tab: "Settings" — Allows live testing of the reactive [SubscriptionManager]
 * Free vs. Premium Plan state.
 */
@Composable
private fun StitchSettingsTabContent(
    isPremiumUser: Boolean,
    documentCount: Int,
    onTogglePremium: () -> Unit,
    onOpenUpgradeDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Settings & Plan",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            color = Slate900
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isPremiumUser) "Premium Plan Active" else "Free Plan Active",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Slate900
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Toggle SubscriptionManager state for testing",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = Slate600
                        )
                    }

                    Switch(
                        checked = isPremiumUser,
                        onCheckedChange = { onTogglePremium() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = BrandRed
                        ),
                        modifier = Modifier.testTag("settings_premium_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Stored Local PDFs in Room Database: $documentCount",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = Slate700
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onOpenUpgradeDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "View Premium Plan Details",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Premium Upgrade Dialog opened when the user taps "Start free trial" on the Home Screen banner
 * or taps a Premium-gated tool.
 */
@Composable
fun PremiumUpgradeDialog(
    isPremiumUser: Boolean,
    onDismiss: () -> Unit,
    onStartFreeTrial: () -> Unit,
    onTogglePlanForTesting: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("premium_upgrade_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BannerDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0xFFF59E0B).copy(alpha = 0.4f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(GoldStart, GoldEnd))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = "Premium",
                        tint = Slate950,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "PDF Z Scanner Premium",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Unlock full optical character recognition, direct 72-DPI text editing, custom typography, and voice reader.",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Premium Features Checklist
                val premiumFeatures = remember {
                    SubscriptionFeature.entries.filter { it.requiresPremium }
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    premiumFeatures.forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = GoldStart,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = feature.title,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = feature.description,
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (!isPremiumUser) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.horizontalGradient(listOf(GoldStart, GoldEnd)))
                            .clickable(onClick = onStartFreeTrial)
                            .padding(vertical = 13.dp)
                            .testTag("confirm_free_trial_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Start 7-Day Free Trial",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Slate950
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E293B))
                            .clickable(onClick = onTogglePlanForTesting)
                            .padding(vertical = 13.dp)
                            .testTag("switch_to_free_plan_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Switch Back to Free Plan (Test Mode)",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Close",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}
