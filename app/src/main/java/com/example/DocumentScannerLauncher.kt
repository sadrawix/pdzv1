package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

/**
 * Controller returned by [rememberDocumentScannerLauncher] to trigger Google ML Kit
 * Document Scanner from any Composable (such as the Bottom Navigation "Scanner" button).
 */
@Stable
class DocumentScannerController(
    private val onLaunch: () -> Unit
) {
    fun launchScanner() {
        onLaunch()
    }
}

/**
 * Configures and remembers a Google ML Kit Document Scanner launcher with:
 * - `SCANNER_MODE_FULL` (edge detection, perspective correction, filters, cleaning)
 * - `RESULT_FORMAT_PDF` and `RESULT_FORMAT_JPEG`
 * - Multi-page scanning enabled (no single-page restriction)
 * - Gallery import enabled
 */
@Composable
fun rememberDocumentScannerLauncher(
    onScanResult: (pdfUri: Uri?, jpegPageUris: List<Uri>) -> Unit,
    onScanCancelled: () -> Unit = {},
    onScannerError: (Throwable) -> Unit = {}
): DocumentScannerController {
    val context = LocalContext.current

    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .setResultFormats(
                GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
            )
            .setGalleryImportAllowed(true)
            .build()
    }

    val scannerClient = remember(scannerOptions) {
        GmsDocumentScanning.getClient(scannerOptions)
    }

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            val result = GmsDocumentScanningResult.fromActivityResultIntent(activityResult.data)
            val pdfUri = result?.pdf?.uri
            val jpegUris = result?.pages?.mapNotNull { it.imageUri }.orEmpty()

            if (pdfUri != null || jpegUris.isNotEmpty()) {
                onScanResult(pdfUri, jpegUris)
            } else {
                onScannerError(IllegalStateException("Scanner returned an empty document result."))
            }
        } else {
            onScanCancelled()
        }
    }

    return remember(context, scannerClient, intentSenderLauncher) {
        DocumentScannerController {
            val activity = context.findActivity()
            if (activity == null) {
                onScannerError(IllegalStateException("Unable to resolve host Activity for ML Kit Scanner."))
                return@DocumentScannerController
            }

            scannerClient.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    runCatching {
                        val request = IntentSenderRequest.Builder(intentSender).build()
                        intentSenderLauncher.launch(request)
                    }.onFailure { launchErr ->
                        onScannerError(launchErr)
                    }
                }
                .addOnFailureListener { error ->
                    onScannerError(error)
                }
        }
    }
}

/**
 * Unwraps a [Context] to locate the hosting [Activity].
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
