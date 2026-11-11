package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import com.example.ui.theme.MyApplicationTheme

/**
 * Main entry point for **PDF Z Scanner**.
 *
 * Initializes `PDFBoxResourceLoader`, reactive [SubscriptionManager], and [PdfRepository],
 * then hosts [AppNavGraph] connecting `HomeScreen`, `PdfViewerScreen`, `FreeAnnotationEditor`,
 * and `PremiumOcrEditor`.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        PdfBoxSaveEngine.ensurePdfBoxInitialized(applicationContext)
        val subscriptionManager = SubscriptionManager.getInstance(applicationContext)
        val repository = PdfRepository(applicationContext)

        setContent {
            MyApplicationTheme {
                PdfZScannerApp(
                    repository = repository,
                    subscriptionManager = subscriptionManager
                )
            }
        }
    }
}

/**
 * Safe [TextToolbar] implementation that tracks status in Compose without spawning native
 * `DecorView` `TYPE_FLOATING` `FloatingActionMode` instances that can race during rapid
 * inline OCR text block switches or bottom sheet transitions.
 */
private class ComposeSafeTextToolbar : TextToolbar {
    private var currentStatus by mutableStateOf(TextToolbarStatus.Hidden)

    override val status: TextToolbarStatus
        get() = currentStatus

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        currentStatus = TextToolbarStatus.Shown
    }

    override fun hide() {
        currentStatus = TextToolbarStatus.Hidden
    }
}

@Composable
fun PdfZScannerApp(
    repository: PdfRepository,
    subscriptionManager: SubscriptionManager
) {
    val safeTextToolbar = remember { ComposeSafeTextToolbar() }
    CompositionLocalProvider(LocalTextToolbar provides safeTextToolbar) {
        AppNavGraph(
            repository = repository,
            subscriptionManager = subscriptionManager
        )
    }
}
