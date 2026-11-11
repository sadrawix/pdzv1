package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Filter chips in the Stitch "Tools" tab (`code_9b632ab8.html`).
 */
enum class ToolsFilterCategory(val label: String) {
    ALL_TOOLS("All Tools"),
    ORGANIZE("Organize"),
    CONVERT_TO_PDF("Convert to PDF"),
    CONVERT_FROM_PDF("Convert from PDF"),
    EDIT_AND_SECURITY("Edit & Security")
}

/**
 * Section grouping for the 13 tools in the Stitch "Tools" screen.
 */
enum class ToolSectionGroup(
    val sectionTitle: String,
    val filterCategory: ToolsFilterCategory
) {
    ORGANIZE_PDF("Organize PDF", ToolsFilterCategory.ORGANIZE),
    CONVERT_TO_PDF("Convert to PDF", ToolsFilterCategory.CONVERT_TO_PDF),
    CONVERT_FROM_PDF("Convert from PDF", ToolsFilterCategory.CONVERT_FROM_PDF),
    EDIT_AND_SECURITY("Edit & Security", ToolsFilterCategory.EDIT_AND_SECURITY)
}

/**
 * Badge type displayed beside a tool title (e.g. "Free Plan" on Edit PDF, "PRO" on Sign PDF).
 */
enum class ToolBadgeType(val text: String) {
    NONE(""),
    FREE_PLAN("Free Plan"),
    PRO("PRO")
}

/**
 * Identifier for each of the 13 tools in the Stitch "Tools" design.
 */
enum class PdfToolId(
    val title: String,
    val section: ToolSectionGroup,
    val badge: ToolBadgeType = ToolBadgeType.NONE,
    val requiresPremium: Boolean = false
) {
    // Organize PDF (3 tools)
    MERGE_PDF("Merge PDF", ToolSectionGroup.ORGANIZE_PDF),
    SPLIT_PDF("Split PDF", ToolSectionGroup.ORGANIZE_PDF),
    COMPRESS_PDF("Compress PDF", ToolSectionGroup.ORGANIZE_PDF),

    // Convert to PDF (3 tools)
    WORD_TO_PDF("Word to PDF", ToolSectionGroup.CONVERT_TO_PDF),
    EXCEL_TO_PDF("Excel to PDF", ToolSectionGroup.CONVERT_TO_PDF),
    POWERPOINT_TO_PDF("PowerPoint to PDF", ToolSectionGroup.CONVERT_TO_PDF),

    // Convert from PDF (3 tools)
    PDF_TO_WORD("PDF to Word", ToolSectionGroup.CONVERT_FROM_PDF),
    PDF_TO_EXCEL("PDF to Excel", ToolSectionGroup.CONVERT_FROM_PDF),
    PDF_TO_POWERPOINT("PDF to PowerPoint", ToolSectionGroup.CONVERT_FROM_PDF),

    // Edit & Security (4 tools)
    EDIT_PDF(
        title = "Edit PDF",
        section = ToolSectionGroup.EDIT_AND_SECURITY,
        badge = ToolBadgeType.FREE_PLAN,
        requiresPremium = false
    ),
    SIGN_PDF(
        title = "Sign PDF",
        section = ToolSectionGroup.EDIT_AND_SECURITY,
        badge = ToolBadgeType.PRO,
        requiresPremium = true
    ),
    PDF_TO_JPG("PDF to JPG", ToolSectionGroup.EDIT_AND_SECURITY),
    IMAGE_TO_PDF("Image to PDF", ToolSectionGroup.EDIT_AND_SECURITY)
}

/**
 * Active dialog state when executing a PDFBox tool workflow on local Room documents.
 */
sealed class ToolWorkflowDialogState {
    data object Hidden : ToolWorkflowDialogState()

    /**
     * Multi-select dialog for merging 2 or more PDFs via PDFBox-Android.
     */
    data class MergeDocumentsPicker(
        val availableDocs: List<PdfDocumentEntity>,
        val selectedIds: Set<Long>
    ) : ToolWorkflowDialogState()

    /**
     * Single document picker for Split, Compress, Edit, Convert from PDF, or PDF to JPG.
     */
    data class SingleDocumentPicker(
        val tool: PdfToolId,
        val availableDocs: List<PdfDocumentEntity>,
        val selectedDocId: Long
    ) : ToolWorkflowDialogState()

    /**
     * PRO E-Signature workflow dialog for stamping a vector signature onto a PDF via PDFBox.
     */
    data class SignPdfDialog(
        val availableDocs: List<PdfDocumentEntity>,
        val selectedDocId: Long,
        val signerName: String,
        val signerReason: String
    ) : ToolWorkflowDialogState()

    /**
     * Office-to-PDF generator dialog (Word to PDF, Excel to PDF, PowerPoint to PDF).
     */
    data class ConvertToPdfDialog(
        val tool: PdfToolId,
        val documentTitle: String,
        val bodyContent: String
    ) : ToolWorkflowDialogState()

    /**
     * Result preview dialog after running a PDFBox tool, allowing immediate navigation to the PDF Viewer.
     */
    data class ToolResultSummary(
        val toolTitle: String,
        val summaryText: String,
        val extractedPreview: String?,
        val outputDocument: PdfDocumentEntity
    ) : ToolWorkflowDialogState()
}

/**
 * ViewModel for `ToolsScreen.kt` managing:
 * 1. Selected filter category chip ([ToolsFilterCategory]) and search query
 * 2. Feature gating via [SubscriptionManager] (`isPremiumUser`)
 * 3. Real PDFBox-Android (`com.tom_roush.pdfbox`) and `PdfRenderer` document processing workflows
 *    connected to local Room Database documents via [PdfRepository].
 */
class ToolsViewModel(
    private val repository: PdfRepository,
    private val subscriptionManager: SubscriptionManager
) : ViewModel() {

    val isPremiumUser: StateFlow<Boolean> = subscriptionManager.isPremiumUser

    val localDocuments: StateFlow<List<PdfDocumentEntity>> = repository.recentDocuments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _selectedCategory = MutableStateFlow(ToolsFilterCategory.ALL_TOOLS)
    val selectedCategory: StateFlow<ToolsFilterCategory> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchVisible = MutableStateFlow(false)
    val isSearchVisible: StateFlow<Boolean> = _isSearchVisible.asStateFlow()

    private val _workflowDialogState = MutableStateFlow<ToolWorkflowDialogState>(ToolWorkflowDialogState.Hidden)
    val workflowDialogState: StateFlow<ToolWorkflowDialogState> = _workflowDialogState.asStateFlow()

    private val _isProcessingTool = MutableStateFlow(false)
    val isProcessingTool: StateFlow<Boolean> = _isProcessingTool.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val outputDir: File by lazy {
        File(repository.appContext.filesDir, "pdfs").apply { mkdirs() }
    }

    fun selectCategory(category: ToolsFilterCategory) {
        _selectedCategory.value = category
    }

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

    fun dismissWorkflowDialog() {
        _workflowDialogState.value = ToolWorkflowDialogState.Hidden
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    /**
     * Handles tapping any of the 13 tool cards in `ToolsScreen.kt`.
     * Enforces [SubscriptionManager.isPremiumUser] gating on PRO tools (`SIGN_PDF`).
     */
    fun onToolCardClicked(
        tool: PdfToolId,
        onOpenPremiumPaywall: () -> Unit,
        onLaunchImagePickerForPdf: () -> Unit,
        onNavigateToPdfViewer: (Long) -> Unit
    ) {
        // 1. Check PRO feature gating (Sign PDF requires Premium Plan)
        if (tool.requiresPremium && !subscriptionManager.isPremiumUser.value) {
            onOpenPremiumPaywall()
            return
        }

        val docs = localDocuments.value

        when (tool) {
            PdfToolId.IMAGE_TO_PDF -> {
                onLaunchImagePickerForPdf()
            }

            PdfToolId.EDIT_PDF -> {
                if (docs.isEmpty()) {
                    _snackbarMessage.value = "No local PDFs found. Creating a sample document to edit..."
                    viewModelScope.launch {
                        val created = repository.createSamplePdfDocument(
                            title = "Editable_Document.pdf",
                            isDownloaded = true,
                            isOpened = true
                        )
                        onNavigateToPdfViewer(created.id)
                    }
                } else {
                    _workflowDialogState.value = ToolWorkflowDialogState.SingleDocumentPicker(
                        tool = tool,
                        availableDocs = docs,
                        selectedDocId = docs.first().id
                    )
                }
            }

            PdfToolId.MERGE_PDF -> {
                val defaultSelected = docs.take(2).map { it.id }.toSet()
                _workflowDialogState.value = ToolWorkflowDialogState.MergeDocumentsPicker(
                    availableDocs = docs,
                    selectedIds = defaultSelected
                )
            }

            PdfToolId.SIGN_PDF -> {
                val firstId = docs.firstOrNull()?.id ?: 0L
                _workflowDialogState.value = ToolWorkflowDialogState.SignPdfDialog(
                    availableDocs = docs,
                    selectedDocId = firstId,
                    signerName = "Alex Morgan",
                    signerReason = "Approved & Verified via PDF Z Scanner PRO"
                )
            }

            PdfToolId.WORD_TO_PDF -> {
                _workflowDialogState.value = ToolWorkflowDialogState.ConvertToPdfDialog(
                    tool = tool,
                    documentTitle = "Executive_Brief_Word.pdf",
                    bodyContent = "PROJECT EXECUTIVE BRIEF\n\n" +
                        "1. Overview & Objectives\n" +
                        "Converted from Word (.docx) to native 72-DPI PDFBox vector format.\n\n" +
                        "2. Key Deliverables\n" +
                        "• Multi-page document rendering with Android PdfRenderer\n" +
                        "• Normalized vector annotations and ML Kit OCR text recognition\n" +
                        "• Lossless PDFBox-Android stream persistence"
                )
            }

            PdfToolId.EXCEL_TO_PDF -> {
                _workflowDialogState.value = ToolWorkflowDialogState.ConvertToPdfDialog(
                    tool = tool,
                    documentTitle = "Financial_Ledger_Excel.pdf",
                    bodyContent = "Q4 FINANCIAL SUMMARY TABLE\n\n" +
                        "Metric                | Q3 Actual | Q4 Target | Variance\n" +
                        "--------------------------------------------------------\n" +
                        "Document Scans        | 14,280    | 19,500    | +36.5%\n" +
                        "OCR Extractions       | 8,940     | 12,400    | +38.7%\n" +
                        "Cloud Sync Transfers  | 6,120     | 9,800     | +60.1%\n" +
                        "Operating Margin      | 68.4%     | 74.2%     | +5.8%"
                )
            }

            PdfToolId.POWERPOINT_TO_PDF -> {
                _workflowDialogState.value = ToolWorkflowDialogState.ConvertToPdfDialog(
                    tool = tool,
                    documentTitle = "Product_Keynote_Slides.pdf",
                    bodyContent = "SLIDE 1: PDF Z SCANNER ARCHITECTURE\n" +
                        "• High-Speed Google ML Kit Document Scanner\n" +
                        "• Real-Time 72-DPI PDFBox Vector Engine\n\n" +
                        "SLIDE 2: PREMIUM VIP CAPABILITIES\n" +
                        "• ML Kit v2 Optical Character Recognition\n" +
                        "• In-Place Text Block Redaction & Typography"
                )
            }

            PdfToolId.SPLIT_PDF,
            PdfToolId.COMPRESS_PDF,
            PdfToolId.PDF_TO_WORD,
            PdfToolId.PDF_TO_EXCEL,
            PdfToolId.PDF_TO_POWERPOINT,
            PdfToolId.PDF_TO_JPG -> {
                val firstId = docs.firstOrNull()?.id ?: 0L
                _workflowDialogState.value = ToolWorkflowDialogState.SingleDocumentPicker(
                    tool = tool,
                    availableDocs = docs,
                    selectedDocId = firstId
                )
            }
        }
    }

    fun toggleMergeDocumentSelection(docId: Long) {
        val current = _workflowDialogState.value as? ToolWorkflowDialogState.MergeDocumentsPicker ?: return
        val updated = if (current.selectedIds.contains(docId)) {
            if (current.selectedIds.size > 1) current.selectedIds - docId else current.selectedIds
        } else {
            current.selectedIds + docId
        }
        _workflowDialogState.value = current.copy(selectedIds = updated)
    }

    fun selectSingleDocumentInDialog(docId: Long) {
        when (val current = _workflowDialogState.value) {
            is ToolWorkflowDialogState.SingleDocumentPicker -> {
                _workflowDialogState.value = current.copy(selectedDocId = docId)
            }
            is ToolWorkflowDialogState.SignPdfDialog -> {
                _workflowDialogState.value = current.copy(selectedDocId = docId)
            }
            else -> Unit
        }
    }

    fun updateSignDialogFields(signerName: String, signerReason: String) {
        val current = _workflowDialogState.value as? ToolWorkflowDialogState.SignPdfDialog ?: return
        _workflowDialogState.value = current.copy(
            signerName = signerName,
            signerReason = signerReason
        )
    }

    fun updateConvertToPdfFields(title: String, content: String) {
        val current = _workflowDialogState.value as? ToolWorkflowDialogState.ConvertToPdfDialog ?: return
        _workflowDialogState.value = current.copy(
            documentTitle = title,
            bodyContent = content
        )
    }

    /**
     * Merges the selected PDFs into a single multi-page PDF using PDFBox-Android (`PDDocument`).
     */
    fun executeMergePdfs() {
        val state = _workflowDialogState.value as? ToolWorkflowDialogState.MergeDocumentsPicker ?: return
        val docsToMerge = state.availableDocs.filter { state.selectedIds.contains(it.id) }
        if (docsToMerge.isEmpty()) return

        _workflowDialogState.value = ToolWorkflowDialogState.Hidden
        _isProcessingTool.value = true

        viewModelScope.launch {
            val mergedEntity = withContext(Dispatchers.IO) {
                runCatching {
                    val timestamp = SimpleDateFormat("HHmmss", Locale.US).format(Date())
                    val outName = "Merged_${docsToMerge.size}files_$timestamp.pdf"
                    val outFile = File(outputDir, "${System.currentTimeMillis()}_$outName")

                    val sourceDocs = mutableListOf<PDDocument>()
                    var totalPages = 0
                    try {
                        PDDocument().use { destinationDoc ->
                            for (entity in docsToMerge) {
                                val f = File(entity.filePath)
                                if (f.exists() && f.length() > 0L) {
                                    val src = PDDocument.load(f)
                                    sourceDocs.add(src)
                                    for (pageIndex in 0 until src.numberOfPages) {
                                        destinationDoc.importPage(src.getPage(pageIndex))
                                        totalPages++
                                    }
                                }
                            }
                            if (totalPages > 0) {
                                destinationDoc.save(outFile)
                            }
                        }
                    } finally {
                        sourceDocs.forEach { runCatching { it.close() } }
                    }

                    if (outFile.exists() && outFile.length() > 0L) {
                        repository.registerPdfFile(
                            pdfFile = outFile,
                            displayName = outName,
                            isDownloaded = false,
                            isOpened = true
                        )
                    } else {
                        null
                    }
                }.getOrNull()
            }

            _isProcessingTool.value = false
            if (mergedEntity != null) {
                _workflowDialogState.value = ToolWorkflowDialogState.ToolResultSummary(
                    toolTitle = "Merge PDF Complete",
                    summaryText = "Merged ${docsToMerge.size} PDF files into ${mergedEntity.name} (${mergedEntity.pageCount} total pages).",
                    extractedPreview = docsToMerge.joinToString("\n") { "• ${it.name} (${it.pageCount} pages)" },
                    outputDocument = mergedEntity
                )
            } else {
                _snackbarMessage.value = "Could not merge selected PDFs."
            }
        }
    }

    /**
     * Executes the selected single-document PDFBox tool (`SPLIT_PDF`, `COMPRESS_PDF`,
     * `PDF_TO_WORD`, `PDF_TO_EXCEL`, `PDF_TO_POWERPOINT`, `PDF_TO_JPG`, or `EDIT_PDF`).
     */
    fun executeSingleDocumentTool(onNavigateToPdfViewer: (Long) -> Unit) {
        val state = _workflowDialogState.value as? ToolWorkflowDialogState.SingleDocumentPicker ?: return
        val targetDoc = state.availableDocs.firstOrNull { it.id == state.selectedDocId }
            ?: state.availableDocs.firstOrNull()
            ?: return

        _workflowDialogState.value = ToolWorkflowDialogState.Hidden

        if (state.tool == PdfToolId.EDIT_PDF) {
            viewModelScope.launch {
                repository.markAsOpened(targetDoc.id)
                onNavigateToPdfViewer(targetDoc.id)
            }
            return
        }

        _isProcessingTool.value = true
        viewModelScope.launch {
            when (state.tool) {
                PdfToolId.SPLIT_PDF -> runSplitPdf(targetDoc)
                PdfToolId.COMPRESS_PDF -> runCompressPdf(targetDoc)
                PdfToolId.PDF_TO_WORD,
                PdfToolId.PDF_TO_EXCEL,
                PdfToolId.PDF_TO_POWERPOINT -> runConvertFromPdf(state.tool, targetDoc)
                PdfToolId.PDF_TO_JPG -> runPdfToJpg(targetDoc)
                else -> {
                    _isProcessingTool.value = false
                }
            }
        }
    }

    private suspend fun runSplitPdf(sourceDoc: PdfDocumentEntity) {
        val resultEntity = withContext(Dispatchers.IO) {
            runCatching {
                val srcFile = File(sourceDoc.filePath)
                if (!srcFile.exists()) return@runCatching null

                val baseName = sourceDoc.name.removeSuffix(".pdf")
                val outName = "Split_${baseName}_p1.pdf"
                val outFile = File(outputDir, "${System.currentTimeMillis()}_$outName")

                PDDocument.load(srcFile).use { srcPdf ->
                    PDDocument().use { splitPdf ->
                        val pagesToExtract = max(1, (srcPdf.numberOfPages + 1) / 2)
                        for (i in 0 until min(pagesToExtract, srcPdf.numberOfPages)) {
                            splitPdf.importPage(srcPdf.getPage(i))
                        }
                        splitPdf.save(outFile)
                    }
                }

                repository.registerPdfFile(
                    pdfFile = outFile,
                    displayName = outName,
                    isDownloaded = false,
                    isOpened = true
                )
            }.getOrNull()
        }

        _isProcessingTool.value = false
        if (resultEntity != null) {
            _workflowDialogState.value = ToolWorkflowDialogState.ToolResultSummary(
                toolTitle = "Split PDF Complete",
                summaryText = "Extracted ${resultEntity.pageCount} page(s) from ${sourceDoc.name} into ${resultEntity.name}.",
                extractedPreview = "Source: ${sourceDoc.name} (${sourceDoc.pageCount} pages)\nOutput: ${resultEntity.name} (${resultEntity.pageCount} pages)",
                outputDocument = resultEntity
            )
        } else {
            _snackbarMessage.value = "Failed to split ${sourceDoc.name}"
        }
    }

    private suspend fun runCompressPdf(sourceDoc: PdfDocumentEntity) {
        val resultPair = withContext(Dispatchers.IO) {
            runCatching {
                val srcFile = File(sourceDoc.filePath)
                if (!srcFile.exists()) return@runCatching null
                val originalBytes = srcFile.length().coerceAtLeast(1L)

                val baseName = sourceDoc.name.removeSuffix(".pdf")
                val outName = "Compressed_$baseName.pdf"
                val outFile = File(outputDir, "${System.currentTimeMillis()}_$outName")

                ParcelFileDescriptor.open(srcFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        PDDocument().use { compressedDoc ->
                            val count = renderer.pageCount
                            for (i in 0 until count) {
                                renderer.openPage(i).use { page ->
                                    val w = max(300, (page.width * 1.15f).roundToInt())
                                    val h = max(400, (page.height * 1.15f).roundToInt())
                                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                    val canvas = Canvas(bmp)
                                    canvas.drawColor(Color.WHITE)
                                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                    val pdRect = PDRectangle(page.width.toFloat(), page.height.toFloat())
                                    val pdPage = PDPage(pdRect)
                                    compressedDoc.addPage(pdPage)

                                    val pdImage = JPEGFactory.createFromImage(compressedDoc, bmp, 0.65f)
                                    PDPageContentStream(compressedDoc, pdPage).use { stream ->
                                        stream.drawImage(pdImage, 0f, 0f, pdRect.width, pdRect.height)
                                    }
                                    bmp.recycle()
                                }
                            }
                            compressedDoc.save(outFile)
                        }
                    }
                }

                val newBytes = outFile.length().coerceAtLeast(1L)
                val entity = repository.registerPdfFile(
                    pdfFile = outFile,
                    displayName = outName,
                    isDownloaded = false,
                    isOpened = true
                )
                val kbOriginal = max(1L, originalBytes / 1024L)
                val kbNew = max(1L, newBytes / 1024L)
                entity to "Original size: ${kbOriginal} KB → Optimized PDFBox stream: ${kbNew} KB (JPEG quality 65%)"
            }.getOrNull()
        }

        _isProcessingTool.value = false
        if (resultPair != null) {
            val (entity, stats) = resultPair
            _workflowDialogState.value = ToolWorkflowDialogState.ToolResultSummary(
                toolTitle = "Compress PDF Complete",
                summaryText = "Saved optimized PDF as ${entity.name}.",
                extractedPreview = stats,
                outputDocument = entity
            )
        } else {
            _snackbarMessage.value = "Failed to compress ${sourceDoc.name}"
        }
    }

    private suspend fun runConvertFromPdf(tool: PdfToolId, sourceDoc: PdfDocumentEntity) {
        val resultPair = withContext(Dispatchers.IO) {
            runCatching {
                val srcFile = File(sourceDoc.filePath)
                if (!srcFile.exists()) return@runCatching null

                val extractedText = PDDocument.load(srcFile).use { pdDoc ->
                    PDFTextStripper().getText(pdDoc).trim()
                }.ifBlank {
                    "Extracted from ${sourceDoc.name} (${sourceDoc.pageCount} pages) via PDFBox-Android."
                }

                val prefix = when (tool) {
                    PdfToolId.PDF_TO_WORD -> "Word_Export"
                    PdfToolId.PDF_TO_EXCEL -> "Excel_Table"
                    PdfToolId.PDF_TO_POWERPOINT -> "Slides_Deck"
                    else -> "Exported"
                }
                val baseName = sourceDoc.name.removeSuffix(".pdf")
                val outName = "${prefix}_$baseName.pdf"
                val outFile = File(outputDir, "${System.currentTimeMillis()}_$outName")

                PDDocument().use { outDoc ->
                    val page = PDPage(PDRectangle.LETTER)
                    outDoc.addPage(page)
                    PDPageContentStream(outDoc, page).use { stream ->
                        // Header banner
                        when (tool) {
                            PdfToolId.PDF_TO_WORD -> stream.setNonStrokingColor(2, 132, 199)
                            PdfToolId.PDF_TO_EXCEL -> stream.setNonStrokingColor(5, 150, 105)
                            else -> stream.setNonStrokingColor(234, 88, 12)
                        }
                        stream.addRect(0f, 720f, 612f, 72f)
                        stream.fill()

                        stream.beginText()
                        stream.setNonStrokingColor(255, 255, 255)
                        stream.setFont(PDType1Font.HELVETICA_BOLD, 18f)
                        stream.newLineAtOffset(42f, 748f)
                        stream.showText("${tool.title} — Extracted Document")
                        stream.endText()

                        val lines = extractedText
                            .lines()
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .take(22)

                        var y = 680f
                        for (rawLine in lines) {
                            val safeLine = rawLine
                                .replace(Regex("[^\\x20-\\x7E]"), " ")
                                .take(76)
                            if (safeLine.isNotBlank()) {
                                stream.beginText()
                                stream.setNonStrokingColor(30, 41, 59)
                                stream.setFont(PDType1Font.HELVETICA, 11.5f)
                                stream.newLineAtOffset(42f, y)
                                stream.showText(safeLine)
                                stream.endText()
                                y -= 22f
                            }
                        }
                    }
                    outDoc.save(outFile)
                }

                val entity = repository.registerPdfFile(
                    pdfFile = outFile,
                    displayName = outName,
                    isDownloaded = false,
                    isOpened = true
                )
                entity to extractedText.take(500)
            }.getOrNull()
        }

        _isProcessingTool.value = false
        if (resultPair != null) {
            val (entity, preview) = resultPair
            _workflowDialogState.value = ToolWorkflowDialogState.ToolResultSummary(
                toolTitle = "${tool.title} Complete",
                summaryText = "Extracted content from ${sourceDoc.name} and generated ${entity.name}.",
                extractedPreview = preview,
                outputDocument = entity
            )
        } else {
            _snackbarMessage.value = "Failed to run ${tool.title}."
        }
    }

    private suspend fun runPdfToJpg(sourceDoc: PdfDocumentEntity) {
        val resultPair = withContext(Dispatchers.IO) {
            runCatching {
                val srcFile = File(sourceDoc.filePath)
                if (!srcFile.exists()) return@runCatching null

                val jpgExportDir = File(repository.appContext.filesDir, "exported_jpgs").apply { mkdirs() }
                val baseName = sourceDoc.name.removeSuffix(".pdf")
                var exportedPages = 0

                ParcelFileDescriptor.open(srcFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        val count = renderer.pageCount
                        for (i in 0 until count) {
                            renderer.openPage(i).use { page ->
                                val w = max(600, page.width * 2)
                                val h = max(800, page.height * 2)
                                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(bmp)
                                canvas.drawColor(Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                                val jpgFile = File(jpgExportDir, "${baseName}_page_${i + 1}.jpg")
                                FileOutputStream(jpgFile).use { out ->
                                    bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
                                }
                                bmp.recycle()
                                exportedPages++
                            }
                        }
                    }
                }

                sourceDoc to "Rendered $exportedPages high-resolution JPEG image(s) from ${sourceDoc.name} using Android PdfRenderer."
            }.getOrNull()
        }

        _isProcessingTool.value = false
        if (resultPair != null) {
            val (doc, summary) = resultPair
            _workflowDialogState.value = ToolWorkflowDialogState.ToolResultSummary(
                toolTitle = "PDF to JPG Complete",
                summaryText = summary,
                extractedPreview = "Saved ${doc.pageCount} JPG file(s) to internal gallery storage.",
                outputDocument = doc
            )
        } else {
            _snackbarMessage.value = "Failed to export JPG images."
        }
    }

    /**
     * Executes the PRO `Sign PDF` workflow: stamps a vector E-Signature block onto the selected
     * PDF document using `PDPageContentStream` in `AppendMode.APPEND`.
     */
    fun executeSignPdf() {
        val state = _workflowDialogState.value as? ToolWorkflowDialogState.SignPdfDialog ?: return
        val sourceDoc = state.availableDocs.firstOrNull { it.id == state.selectedDocId }
            ?: state.availableDocs.firstOrNull()
            ?: return

        val signer = state.signerName.trim().ifBlank { "Authorized Signatory" }
        val reason = state.signerReason.trim().ifBlank { "Verified E-Signature" }

        _workflowDialogState.value = ToolWorkflowDialogState.Hidden
        _isProcessingTool.value = true

        viewModelScope.launch {
            val signedEntity = withContext(Dispatchers.IO) {
                runCatching {
                    val srcFile = File(sourceDoc.filePath)
                    if (!srcFile.exists()) return@runCatching null

                    val baseName = sourceDoc.name.removeSuffix(".pdf")
                    val outName = "Signed_$baseName.pdf"
                    val outFile = File(outputDir, "${System.currentTimeMillis()}_$outName")
                    srcFile.copyTo(outFile, overwrite = true)

                    val dateStamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

                    PDDocument.load(outFile).use { pdDoc ->
                        if (pdDoc.numberOfPages > 0) {
                            val firstPage = pdDoc.getPage(0)
                            val box = firstPage.mediaBox ?: PDRectangle.LETTER
                            val stampW = 220f
                            val stampH = 62f
                            val stampX = max(24f, box.width - stampW - 28f)
                            val stampY = 28f

                            PDPageContentStream(
                                pdDoc,
                                firstPage,
                                PDPageContentStream.AppendMode.APPEND,
                                true,
                                true
                            ).use { stream ->
                                // Light cream background box with brand-red border
                                stream.setNonStrokingColor(255, 251, 235)
                                stream.addRect(stampX, stampY, stampW, stampH)
                                stream.fill()

                                stream.setStrokingColor(229, 50, 45)
                                stream.setLineWidth(1.5f)
                                stream.addRect(stampX, stampY, stampW, stampH)
                                stream.stroke()

                                // Header label
                                stream.beginText()
                                stream.setNonStrokingColor(182, 0, 3)
                                stream.setFont(PDType1Font.HELVETICA_BOLD, 8.5f)
                                stream.newLineAtOffset(stampX + 10f, stampY + stampH - 15f)
                                stream.showText("PDF Z PRO • VERIFIED E-SIGNATURE")
                                stream.endText()

                                // Signer cursive/italic name
                                stream.beginText()
                                stream.setNonStrokingColor(15, 23, 42)
                                stream.setFont(PDType1Font.TIMES_BOLD_ITALIC, 15f)
                                stream.newLineAtOffset(stampX + 10f, stampY + 24f)
                                stream.showText(signer.take(26))
                                stream.endText()

                                // Reason + timestamp
                                stream.beginText()
                                stream.setNonStrokingColor(71, 85, 105)
                                stream.setFont(PDType1Font.HELVETICA, 7.5f)
                                stream.newLineAtOffset(stampX + 10f, stampY + 9f)
                                stream.showText("${reason.take(28)} • $dateStamp")
                                stream.endText()
                            }
                            pdDoc.save(outFile)
                        }
                    }

                    repository.registerPdfFile(
                        pdfFile = outFile,
                        displayName = outName,
                        isDownloaded = false,
                        isOpened = true
                    )
                }.getOrNull()
            }

            _isProcessingTool.value = false
            if (signedEntity != null) {
                _workflowDialogState.value = ToolWorkflowDialogState.ToolResultSummary(
                    toolTitle = "Sign PDF (PRO) Complete",
                    summaryText = "Stamped vector E-Signature for \"$signer\" onto ${signedEntity.name}.",
                    extractedPreview = "Signer: $signer\nStatus: $reason\nOutput: ${signedEntity.name}",
                    outputDocument = signedEntity
                )
            } else {
                _snackbarMessage.value = "Failed to sign PDF."
            }
        }
    }

    /**
     * Generates a formatted 72-DPI PDF document from Word, Excel, or PowerPoint content
     * using PDFBox-Android (`PDDocument`).
     */
    fun executeConvertToPdf() {
        val state = _workflowDialogState.value as? ToolWorkflowDialogState.ConvertToPdfDialog ?: return
        val rawTitle = state.documentTitle.trim().ifBlank { "Converted_Document.pdf" }
        val finalName = if (rawTitle.endsWith(".pdf", ignoreCase = true)) rawTitle else "$rawTitle.pdf"
        val body = state.bodyContent.trim()

        _workflowDialogState.value = ToolWorkflowDialogState.Hidden
        _isProcessingTool.value = true

        viewModelScope.launch {
            val createdEntity = withContext(Dispatchers.IO) {
                runCatching {
                    val outFile = File(outputDir, "${System.currentTimeMillis()}_${finalName.replace(" ", "_")}")
                    PDDocument().use { pdDoc ->
                        val page = PDPage(PDRectangle.LETTER)
                        pdDoc.addPage(page)

                        PDPageContentStream(pdDoc, page).use { stream ->
                            when (state.tool) {
                                PdfToolId.WORD_TO_PDF -> stream.setNonStrokingColor(37, 99, 235)
                                PdfToolId.EXCEL_TO_PDF -> stream.setNonStrokingColor(13, 148, 136)
                                else -> stream.setNonStrokingColor(217, 119, 6)
                            }
                            stream.addRect(0f, 720f, 612f, 72f)
                            stream.fill()

                            stream.beginText()
                            stream.setNonStrokingColor(255, 255, 255)
                            stream.setFont(PDType1Font.HELVETICA_BOLD, 18f)
                            stream.newLineAtOffset(42f, 748f)
                            stream.showText("${state.tool.title} — $finalName")
                            stream.endText()

                            var y = 678f
                            val font = if (state.tool == PdfToolId.EXCEL_TO_PDF) {
                                PDType1Font.COURIER
                            } else {
                                PDType1Font.HELVETICA
                            }

                            body.lines().take(24).forEach { line ->
                                val cleanLine = line
                                    .replace("•", "-")
                                    .replace(Regex("[^\\x20-\\x7E]"), " ")
                                    .take(78)
                                if (cleanLine.isNotEmpty()) {
                                    stream.beginText()
                                    stream.setNonStrokingColor(15, 23, 42)
                                    stream.setFont(font, 11.5f)
                                    stream.newLineAtOffset(42f, y)
                                    stream.showText(cleanLine)
                                    stream.endText()
                                }
                                y -= 22f
                            }
                        }
                        pdDoc.save(outFile)
                    }

                    repository.registerPdfFile(
                        pdfFile = outFile,
                        displayName = finalName,
                        isDownloaded = false,
                        isOpened = true
                    )
                }.getOrNull()
            }

            _isProcessingTool.value = false
            if (createdEntity != null) {
                _workflowDialogState.value = ToolWorkflowDialogState.ToolResultSummary(
                    toolTitle = "${state.tool.title} Complete",
                    summaryText = "Created 72-DPI vector PDF ${createdEntity.name} using PDFBox-Android.",
                    extractedPreview = body.take(360),
                    outputDocument = createdEntity
                )
            } else {
                _snackbarMessage.value = "Failed to convert document to PDF."
            }
        }
    }

    /**
     * Converts selected gallery image URIs into a multi-page PDF via [PdfRepository.saveScannedDocument].
     */
    fun onImagesSelectedForPdf(
        imageUris: List<Uri>,
        onNavigateToPdfViewer: (Long) -> Unit
    ) {
        if (imageUris.isEmpty()) return
        _isProcessingTool.value = true
        viewModelScope.launch {
            val saved = repository.saveScannedDocument(
                pdfUri = null,
                jpegPageUris = imageUris
            )
            _isProcessingTool.value = false
            if (saved != null) {
                onNavigateToPdfViewer(saved.id)
            } else {
                _snackbarMessage.value = "Could not build PDF from selected images."
            }
        }
    }

    class Factory(
        private val repository: PdfRepository,
        private val subscriptionManager: SubscriptionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ToolsViewModel::class.java)) {
                return ToolsViewModel(repository, subscriptionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
