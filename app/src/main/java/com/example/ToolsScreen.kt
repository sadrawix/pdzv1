package com.example

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.CallMerge
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CoPresent
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.BrandRed
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.PillPink
import com.example.ui.theme.Slate100
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate600
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate900

// Exact Stitch Tailwind Colors from /public/assets/aistudio/code_9b632ab8.html
private val ToolsPageBg = Color(0xFFF8F9FA)
private val Slate500 = Color(0xFF64748B)

// Tool Icon Box Pastel Backgrounds & Foregrounds
private val Red50 = Color(0xFFFEF2F2)
private val Orange50 = Color(0xFFFFF7ED)
private val Orange600 = Color(0xFFEA580C)
private val Rose50 = Color(0xFFFFF1F2)
private val Rose600 = Color(0xFFE11D48)
private val Blue50 = Color(0xFFEFF6FF)
private val Blue600 = Color(0xFF2563EB)
private val Teal50 = Color(0xFFF0FDFA)
private val Teal600 = Color(0xFF0D9488)
private val Amber50 = Color(0xFFFFFBEB)
private val Amber600 = Color(0xFFD97706)
private val Sky50 = Color(0xFFF0F9FF)
private val Sky600 = Color(0xFF0284C7)
private val Emerald50 = Color(0xFFECFDF5)
private val Emerald600 = Color(0xFF059669)
private val Indigo50 = Color(0xFFEEF2FF)
private val Indigo600 = Color(0xFF4F46E5)

// Pill Badge Colors (Free Plan & PRO)
private val Indigo100 = Color(0xFFE0E7FF)
private val Indigo700 = Color(0xFF4338CA)
private val Amber100 = Color(0xFFFEF3C7)
private val Amber800 = Color(0xFF92400E)

/**
 * Production Jetpack Compose implementation of the Stitch AI "Tools" screen
 * (`code_9b632ab8.html` & `screen_dedb3f6f.png`), wired to [ToolsViewModel],
 * [SubscriptionManager] PRO feature gating, and real PDFBox-Android document engines.
 */
@Composable
fun ToolsScreen(
    viewModel: ToolsViewModel,
    onNavigateToPdfViewer: (Long) -> Unit,
    onOpenPremiumPaywall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchVisible by viewModel.isSearchVisible.collectAsStateWithLifecycle()
    val workflowDialogState by viewModel.workflowDialogState.collectAsStateWithLifecycle()
    val isProcessingTool by viewModel.isProcessingTool.collectAsStateWithLifecycle()

    // Zero-permission Android Photo Picker for "Image to PDF"
    val pickMultipleImagesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 15)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.onImagesSelectedForPdf(
                imageUris = uris,
                onNavigateToPdfViewer = onNavigateToPdfViewer
            )
        }
    }

    val visibleSections = remember(selectedCategory, searchQuery) {
        val query = searchQuery.trim().lowercase()
        val sections = when (selectedCategory) {
            ToolsFilterCategory.ALL_TOOLS -> ToolSectionGroup.entries
            ToolsFilterCategory.ORGANIZE -> listOf(ToolSectionGroup.ORGANIZE_PDF)
            ToolsFilterCategory.CONVERT_TO_PDF -> listOf(ToolSectionGroup.CONVERT_TO_PDF)
            ToolsFilterCategory.CONVERT_FROM_PDF -> listOf(ToolSectionGroup.CONVERT_FROM_PDF)
            ToolsFilterCategory.EDIT_AND_SECURITY -> listOf(ToolSectionGroup.EDIT_AND_SECURITY)
        }

        sections.mapNotNull { section ->
            val toolsInSection = PdfToolId.entries.filter { tool ->
                tool.section == section && (
                    query.isEmpty() ||
                        tool.title.lowercase().contains(query) ||
                        section.sectionTitle.lowercase().contains(query) ||
                        tool.badge.text.lowercase().contains(query)
                    )
            }
            if (toolsInSection.isNotEmpty()) {
                section to toolsInSection
            } else {
                null
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ToolsPageBg)
            .testTag("tools_screen_root")
    ) {
        // Subtle horizontal red gradient line (`top-28 h-[2px] bg-gradient-to-r from-transparent via-brand-red to-transparent opacity-40`)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 96.dp)
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

        Column(modifier = Modifier.fillMaxSize()) {
            // App Header (`px-5 pt-3 pb-3 flex justify-between items-center bg-[#F8F9FA]`)
            StitchToolsTopHeader(
                onSearchClick = { viewModel.toggleSearchBar() },
                onAccountClick = onOpenPremiumPaywall
            )

            // Expandable Search Input when lens icon is tapped
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
                            text = "Search PDF tools (Merge, Split, Sign, OCR)...",
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            color = Slate500
                        )
                    },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = { viewModel.toggleSearchBar() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close search",
                                tint = Slate600
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
                        .padding(horizontal = 20.dp, vertical = 4.dp)
                        .testTag("tools_search_input")
                )
            }

            // Horizontal Filter Chips Row (`px-5 pt-1 pb-3 overflow-x-auto`)
            StitchToolsFilterChipsRow(
                selectedCategory = selectedCategory,
                onSelectCategory = { viewModel.selectCategory(it) }
            )

            // Categorized Tool Sections (`px-4 space-y-6 pb-6`)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                visibleSections.forEach { (section, tools) ->
                    StitchToolCategorySection(
                        section = section,
                        tools = tools,
                        onToolClick = { tool ->
                            viewModel.onToolCardClicked(
                                tool = tool,
                                onOpenPremiumPaywall = onOpenPremiumPaywall,
                                onLaunchImagePickerForPdf = {
                                    pickMultipleImagesLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onNavigateToPdfViewer = onNavigateToPdfViewer
                            )
                        }
                    )
                }
            }
        }

        // Loading Overlay while PDFBox-Android processes documents
        if (isProcessingTool) {
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
                            modifier = Modifier.size(26.dp),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Processing PDF with PDFBox Engine...",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Slate900
                        )
                    }
                }
            }
        }
    }

    // Active PDFBox Workflow Dialogs
    when (val dialogState = workflowDialogState) {
        is ToolWorkflowDialogState.Hidden -> Unit

        is ToolWorkflowDialogState.MergeDocumentsPicker -> {
            MergePdfsDialog(
                state = dialogState,
                onToggleDoc = { viewModel.toggleMergeDocumentSelection(it) },
                onConfirmMerge = { viewModel.executeMergePdfs() },
                onDismiss = { viewModel.dismissWorkflowDialog() }
            )
        }

        is ToolWorkflowDialogState.SingleDocumentPicker -> {
            SingleDocumentToolDialog(
                state = dialogState,
                onSelectDoc = { viewModel.selectSingleDocumentInDialog(it) },
                onConfirm = { viewModel.executeSingleDocumentTool(onNavigateToPdfViewer) },
                onDismiss = { viewModel.dismissWorkflowDialog() }
            )
        }

        is ToolWorkflowDialogState.SignPdfDialog -> {
            SignPdfProDialog(
                state = dialogState,
                onSelectDoc = { viewModel.selectSingleDocumentInDialog(it) },
                onUpdateFields = { name, reason -> viewModel.updateSignDialogFields(name, reason) },
                onConfirmSign = { viewModel.executeSignPdf() },
                onDismiss = { viewModel.dismissWorkflowDialog() }
            )
        }

        is ToolWorkflowDialogState.ConvertToPdfDialog -> {
            ConvertToPdfGeneratorDialog(
                state = dialogState,
                onUpdateFields = { title, content -> viewModel.updateConvertToPdfFields(title, content) },
                onConfirmConvert = { viewModel.executeConvertToPdf() },
                onDismiss = { viewModel.dismissWorkflowDialog() }
            )
        }

        is ToolWorkflowDialogState.ToolResultSummary -> {
            ToolResultSummaryDialog(
                state = dialogState,
                onOpenPdf = {
                    val docId = dialogState.outputDocument.id
                    viewModel.dismissWorkflowDialog()
                    onNavigateToPdfViewer(docId)
                },
                onDismiss = { viewModel.dismissWorkflowDialog() }
            )
        }
    }
}

/**
 * Stitch Tools Top Header (`px-5 pt-3 pb-3 flex justify-between items-center bg-[#F8F9FA]`):
 * Left: `"Tools"` (`22.sp`, `Bold`, `text-slate-900`)
 * Right: `search` icon button + `account_circle` icon button (`text-slate-700`).
 */
@Composable
private fun StitchToolsTopHeader(
    onSearchClick: () -> Unit,
    onAccountClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ToolsPageBg)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Tools",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            letterSpacing = (-0.4).sp,
            color = Slate900
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onSearchClick)
                    .testTag("tools_search_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search tools",
                    tint = Slate700,
                    modifier = Modifier.size(24.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onAccountClick)
                    .testTag("tools_account_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = "Account profile",
                    tint = Slate700,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Horizontal Filter Chips Row (`[All Tools, Organize, Convert to PDF, Convert from PDF, Edit & Security]`):
 * - Active chip: `bg-pill-pink` (`#FCD5CE`), `text-slate-900`, `font-semibold`, `text-xs`, `px-3.5 py-1.5`, `rounded-full`, shadow-sm, leading `check` icon
 * - Inactive chip: `border border-slate-200 bg-white text-slate-700 font-medium text-xs px-3.5 py-1.5 rounded-full`
 */
@Composable
private fun StitchToolsFilterChipsRow(
    selectedCategory: ToolsFilterCategory,
    onSelectCategory: (ToolsFilterCategory) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp)
            .testTag("tools_filter_chips_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolsFilterCategory.entries.forEach { category ->
            val isSelected = category == selectedCategory
            Surface(
                onClick = { onSelectCategory(category) },
                shape = CircleShape,
                color = if (isSelected) PillPink else Color.White,
                shadowElevation = if (isSelected) 1.dp else 0.dp,
                modifier = Modifier
                    .then(
                        if (!isSelected) {
                            Modifier.border(1.dp, Slate200, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .testTag("tools_chip_${category.name.lowercase()}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Slate900,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = category.label,
                        fontFamily = InterFontFamily,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = if (isSelected) Slate900 else Slate700
                    )
                }
            }
        }
    }
}

/**
 * Categorized Tool Section (`ORGANIZE PDF`, `CONVERT TO PDF`, `CONVERT FROM PDF`, `EDIT & SECURITY`)
 * with section title on the left, `<N> tools` in `#E5322D` on the right, and `10.dp` spaced cards.
 */
@Composable
private fun StitchToolCategorySection(
    section: ToolSectionGroup,
    tools: List<PdfToolId>,
    onToolClick: (PdfToolId) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header (`flex items-center justify-between px-1`)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = section.sectionTitle.uppercase(),
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 0.6.sp,
                color = Slate500
            )
            Text(
                text = "${tools.size} ${if (tools.size == 1) "tool" else "tools"}",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = BrandRed
            )
        }

        // Tool Cards (`grid grid-cols-1 gap-2.5`)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            tools.forEach { tool ->
                StitchToolCard(
                    tool = tool,
                    onClick = { onToolClick(tool) }
                )
            }
        }
    }
}

/**
 * Individual Stitch Tool Card (`bg-white rounded-xl p-3.5 border border-slate-100 shadow-sm flex items-center justify-between`):
 * - `40.dp` (`w-10 h-10`) `RoundedCornerShape(12.dp)` colored icon container
 * - Bold `14.sp` (`text-sm`) tool title + optional `Free Plan` or `PRO` pill badge
 * - Right `chevron_right` (`text-slate-400`).
 */
@Composable
private fun StitchToolCard(
    tool: PdfToolId,
    onClick: () -> Unit
) {
    val (iconBg, iconTint) = remember(tool) {
        when (tool) {
            PdfToolId.MERGE_PDF -> Red50 to BrandRed
            PdfToolId.SPLIT_PDF -> Orange50 to Orange600
            PdfToolId.COMPRESS_PDF -> Rose50 to Rose600
            PdfToolId.WORD_TO_PDF -> Blue50 to Blue600
            PdfToolId.EXCEL_TO_PDF -> Teal50 to Teal600
            PdfToolId.POWERPOINT_TO_PDF -> Amber50 to Amber600
            PdfToolId.PDF_TO_WORD -> Sky50 to Sky600
            PdfToolId.PDF_TO_EXCEL -> Emerald50 to Emerald600
            PdfToolId.PDF_TO_POWERPOINT -> Orange50 to Orange600
            PdfToolId.EDIT_PDF -> Indigo50 to Indigo600
            PdfToolId.SIGN_PDF -> Red50 to BrandRed
            PdfToolId.PDF_TO_JPG -> Amber50 to Amber600
            PdfToolId.IMAGE_TO_PDF -> Rose50 to Rose600
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Slate100, RoundedCornerShape(12.dp))
            .testTag("tool_card_${tool.name.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // 40.dp x 40.dp rounded-xl pastel icon container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    ToolVectorIcon(tool = tool, tint = iconTint)
                }

                // Title + optional Free Plan / PRO pill badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = tool.title,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Slate900
                    )

                    when (tool.badge) {
                        ToolBadgeType.FREE_PLAN -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Indigo100)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Free Plan",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Indigo700
                                )
                            }
                        }

                        ToolBadgeType.PRO -> {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Amber100)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .testTag("sign_pdf_pro_badge")
                            ) {
                                Text(
                                    text = "PRO",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = Amber800
                                )
                            }
                        }

                        ToolBadgeType.NONE -> Unit
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Slate400,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Renders the exact Material Symbol icon for each of the 13 tools in `ToolsScreen.kt`.
 */
@Composable
private fun ToolVectorIcon(
    tool: PdfToolId,
    tint: Color
) {
    when (tool) {
        PdfToolId.MERGE_PDF -> {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CallMerge,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }

        PdfToolId.SPLIT_PDF -> {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.CallSplit,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }

        PdfToolId.COMPRESS_PDF -> {
            Icon(
                imageVector = Icons.Default.Compress,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }

        PdfToolId.WORD_TO_PDF -> {
            Icon(
                imageVector = Icons.Outlined.Description,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }

        PdfToolId.EXCEL_TO_PDF -> {
            Icon(
                imageVector = Icons.Outlined.TableChart,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }

        PdfToolId.POWERPOINT_TO_PDF -> {
            Icon(
                imageVector = Icons.Outlined.Slideshow,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }

        PdfToolId.PDF_TO_WORD -> {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Article,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }

        PdfToolId.PDF_TO_EXCEL -> {
            Icon(
                imageVector = Icons.Default.QueryStats,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }

        PdfToolId.PDF_TO_POWERPOINT -> {
            Icon(
                imageVector = Icons.Default.CoPresent,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }

        PdfToolId.EDIT_PDF -> {
            Icon(
                imageVector = Icons.Default.Draw,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }

        PdfToolId.SIGN_PDF -> {
            // Custom fountain pen nib (`ink_pen`) matching Stitch
            Canvas(modifier = Modifier.size(22.dp)) {
                val w = size.width
                val h = size.height
                val nibPath = Path().apply {
                    moveTo(w * 0.78f, h * 0.14f)
                    lineTo(w * 0.86f, h * 0.22f)
                    lineTo(w * 0.42f, h * 0.76f)
                    lineTo(w * 0.18f, h * 0.82f)
                    lineTo(w * 0.24f, h * 0.58f)
                    close()
                }
                drawPath(
                    path = nibPath,
                    color = tint,
                    style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.18f, h * 0.82f),
                    end = Offset(w * 0.34f, h * 0.66f),
                    strokeWidth = w * 0.08f,
                    cap = StrokeCap.Round
                )
            }
        }

        PdfToolId.PDF_TO_JPG -> {
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }

        PdfToolId.IMAGE_TO_PDF -> {
            Icon(
                imageVector = Icons.Outlined.PhotoLibrary,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Dialog for selecting 2+ local PDFs to merge via PDFBox-Android (`Merge PDF`).
 */
@Composable
private fun MergePdfsDialog(
    state: ToolWorkflowDialogState.MergeDocumentsPicker,
    onToggleDoc: (Long) -> Unit,
    onConfirmMerge: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("merge_pdfs_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Merge PDF Files",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Slate900
                )
                Text(
                    text = "Select the PDF documents to combine into a single multi-page PDF via PDFBox-Android:",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = Slate600
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.availableDocs.forEach { doc ->
                        val isSelected = state.selectedIds.contains(doc.id)
                        Surface(
                            onClick = { onToggleDoc(doc.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Red50 else ToolsPageBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { onToggleDoc(doc.id) },
                                    colors = CheckboxDefaults.colors(checkedColor = BrandRed)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.name,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${doc.pageCount} page(s)",
                                        fontFamily = InterFontFamily,
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", fontFamily = InterFontFamily, color = Slate600)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirmMerge,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "Merge (${state.selectedIds.size})",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog for selecting a single PDF document to process (`Split PDF`, `Compress PDF`,
 * `Edit PDF`, `PDF to Word`, `PDF to Excel`, `PDF to PowerPoint`, `PDF to JPG`).
 */
@Composable
private fun SingleDocumentToolDialog(
    state: ToolWorkflowDialogState.SingleDocumentPicker,
    onSelectDoc: (Long) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("single_doc_tool_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = state.tool.title,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Slate900
                )
                Text(
                    text = "Choose a local PDF document to process:",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = Slate600
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    state.availableDocs.forEach { doc ->
                        val isSelected = doc.id == state.selectedDocId
                        Surface(
                            onClick = { onSelectDoc(doc.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Red50 else ToolsPageBg,
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
                                    onClick = { onSelectDoc(doc.id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = BrandRed)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.name,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = Slate900,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${doc.pageCount} page(s)",
                                        fontFamily = InterFontFamily,
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", fontFamily = InterFontFamily, color = Slate600)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = if (state.tool == PdfToolId.EDIT_PDF) "Open in Editor" else "Run ${state.tool.title}",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * PRO E-Signature Dialog (`Sign PDF`) for stamping a vector signature block onto a PDF via PDFBox.
 */
@Composable
private fun SignPdfProDialog(
    state: ToolWorkflowDialogState.SignPdfDialog,
    onSelectDoc: (Long) -> Unit,
    onUpdateFields: (String, String) -> Unit,
    onConfirmSign: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("sign_pdf_pro_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Sign PDF",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Slate900
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Amber100)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PRO",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Amber800
                        )
                    }
                }

                OutlinedTextField(
                    value = state.signerName,
                    onValueChange = { onUpdateFields(it, state.signerReason) },
                    label = { Text("Signatory Full Name", fontFamily = InterFontFamily) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.signerReason,
                    onValueChange = { onUpdateFields(state.signerName, it) },
                    label = { Text("Verification Stamp Note", fontFamily = InterFontFamily) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Target Document:",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Slate700
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    state.availableDocs.take(3).forEach { doc ->
                        val isSelected = doc.id == state.selectedDocId
                        Surface(
                            onClick = { onSelectDoc(doc.id) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Red50 else ToolsPageBg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectDoc(doc.id) },
                                    colors = RadioButtonDefaults.colors(selectedColor = BrandRed)
                                )
                                Text(
                                    text = doc.name,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = Slate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", fontFamily = InterFontFamily, color = Slate600)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirmSign,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "Stamp E-Signature",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog for generating a 72-DPI PDF from Word, Excel, or PowerPoint content (`Convert to PDF`).
 */
@Composable
private fun ConvertToPdfGeneratorDialog(
    state: ToolWorkflowDialogState.ConvertToPdfDialog,
    onUpdateFields: (String, String) -> Unit,
    onConfirmConvert: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("convert_to_pdf_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = state.tool.title,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Slate900
                )

                OutlinedTextField(
                    value = state.documentTitle,
                    onValueChange = { onUpdateFields(it, state.bodyContent) },
                    label = { Text("Output PDF Filename", fontFamily = InterFontFamily) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = state.bodyContent,
                    onValueChange = { onUpdateFields(state.documentTitle, it) },
                    label = { Text("Document Content", fontFamily = InterFontFamily) },
                    minLines = 5,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", fontFamily = InterFontFamily, color = Slate600)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirmConvert,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "Convert to PDF",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Summary Dialog displayed when a PDFBox tool completes, with one-tap navigation to open the
 * resulting PDF in the PDF Viewer.
 */
@Composable
private fun ToolResultSummaryDialog(
    state: ToolWorkflowDialogState.ToolResultSummary,
    onOpenPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tool_result_summary_dialog")
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
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Emerald600,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = state.toolTitle,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Slate900
                    )
                }

                Text(
                    text = state.summaryText,
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = Slate700
                )

                if (!state.extractedPreview.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ToolsPageBg)
                            .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = state.extractedPreview,
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = Slate700,
                            maxLines = 8,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", fontFamily = InterFontFamily, color = Slate600)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onOpenPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandRed),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "Open PDF",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
