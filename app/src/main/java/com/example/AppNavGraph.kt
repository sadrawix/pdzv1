package com.example

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

/**
 * Strongly-typed navigation routes for PDF Z Scanner.
 */
object AppRoutes {
    const val HOME = "home"
    const val VIEWER = "viewer/{documentId}"
    const val FREE_EDITOR = "free_editor/{documentId}/{pageIndex}"
    const val PREMIUM_OCR_EDITOR = "premium_ocr_editor/{documentId}/{pageIndex}"
    const val EDITOR = "editor/{documentId}/{pageIndex}/{mode}"

    fun viewerRoute(documentId: Long): String = "viewer/$documentId"

    fun freeEditorRoute(documentId: Long, pageIndex: Int): String =
        "free_editor/$documentId/$pageIndex"

    fun premiumOcrEditorRoute(documentId: Long, pageIndex: Int): String =
        "premium_ocr_editor/$documentId/$pageIndex"

    fun editorRoute(
        documentId: Long,
        pageIndex: Int,
        mode: EditorLaunchMode
    ): String = when (mode) {
        EditorLaunchMode.FREE_ANNOTATION -> freeEditorRoute(documentId, pageIndex)
        EditorLaunchMode.PREMIUM_OCR_TEXT_EDIT -> premiumOcrEditorRoute(documentId, pageIndex)
    }
}

/**
 * Cohesive application navigation graph connecting:
 * 1. [HomeScreen] (Recent files, Downloads/Opened filter chips, ML Kit Document Scanner, Premium banner)
 * 2. [PdfViewerScreen] (High-res zoomable PdfRenderer view, OCR & TTS with crown badges, Edit pen, thumbnail strip)
 * 3. [FreeAnnotationEditor] (Free Plan Drawing & Text Highlight with purple `#3B00B3` BottomSheets)
 * 4. [PremiumOcrEditor] (Premium Plan ML Kit OCR dashed blue text boxes, resize handle, inline editing, and formatting BottomSheets)
 */
@Composable
fun AppNavGraph(
    repository: PdfRepository,
    subscriptionManager: SubscriptionManager,
    navController: NavHostController = rememberNavController()
) {
    val homeViewModelFactory = remember(repository, subscriptionManager) {
        HomeViewModel.Factory(repository, subscriptionManager)
    }
    val homeViewModel: HomeViewModel = viewModel(factory = homeViewModelFactory)

    NavHost(
        navController = navController,
        startDestination = AppRoutes.HOME
    ) {
        // 1. Stitch AI Home Screen + ML Kit Document Scanner
        composable(route = AppRoutes.HOME) {
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToPdfViewer = { documentId ->
                    navController.navigate(AppRoutes.viewerRoute(documentId))
                }
            )
        }

        // 2. Stitch AI PDF Viewer Screen + PdfRenderer Engine
        composable(
            route = AppRoutes.VIEWER,
            arguments = listOf(
                navArgument("documentId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getLong("documentId") ?: 0L
            val viewerFactory = remember(documentId, repository, subscriptionManager) {
                PdfViewerViewModel.Factory(
                    documentId = documentId,
                    repository = repository,
                    subscriptionManager = subscriptionManager
                )
            }
            val viewerViewModel: PdfViewerViewModel = viewModel(
                key = "viewer_$documentId",
                factory = viewerFactory
            )

            // Reload PDF page & thumbnails whenever returning from FreeAnnotationEditor or PremiumOcrEditor
            val shouldRefresh by backStackEntry.savedStateHandle
                .getStateFlow("refresh_pdf", false)
                .collectAsStateWithLifecycle()

            LaunchedEffect(shouldRefresh) {
                if (shouldRefresh) {
                    viewerViewModel.loadDocumentAndPages(forceReload = true)
                    backStackEntry.savedStateHandle["refresh_pdf"] = false
                }
            }

            PdfViewerScreen(
                viewModel = viewerViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEditor = { docId, pageIndex, mode ->
                    navController.navigate(
                        AppRoutes.editorRoute(
                            documentId = docId,
                            pageIndex = pageIndex,
                            mode = mode
                        )
                    )
                }
            )
        }

        // 3. Stitch AI Free Plan Editor (Freehand Drawing & Multiply-Blended Text Highlight)
        composable(
            route = AppRoutes.FREE_EDITOR,
            arguments = listOf(
                navArgument("documentId") { type = NavType.LongType },
                navArgument("pageIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getLong("documentId") ?: 0L
            val pageIndex = backStackEntry.arguments?.getInt("pageIndex") ?: 0

            val factory = remember(documentId, pageIndex, repository, subscriptionManager) {
                PdfEditorViewModel.Factory(
                    documentId = documentId,
                    initialPageIndex = pageIndex,
                    initialLaunchMode = EditorLaunchMode.FREE_ANNOTATION,
                    repository = repository,
                    subscriptionManager = subscriptionManager
                )
            }
            val editorViewModel: PdfEditorViewModel = viewModel(
                key = "free_editor_${documentId}_$pageIndex",
                factory = factory
            )

            FreeAnnotationEditor(
                viewModel = editorViewModel,
                onNavigateBack = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refresh_pdf", true)
                    navController.popBackStack()
                }
            )
        }

        // 4. Stitch AI Premium OCR & Direct Text Editor
        composable(
            route = AppRoutes.PREMIUM_OCR_EDITOR,
            arguments = listOf(
                navArgument("documentId") { type = NavType.LongType },
                navArgument("pageIndex") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getLong("documentId") ?: 0L
            val pageIndex = backStackEntry.arguments?.getInt("pageIndex") ?: 0

            val factory = remember(documentId, pageIndex, repository, subscriptionManager) {
                PdfEditorViewModel.Factory(
                    documentId = documentId,
                    initialPageIndex = pageIndex,
                    initialLaunchMode = EditorLaunchMode.PREMIUM_OCR_TEXT_EDIT,
                    repository = repository,
                    subscriptionManager = subscriptionManager
                )
            }
            val editorViewModel: PdfEditorViewModel = viewModel(
                key = "premium_ocr_editor_${documentId}_$pageIndex",
                factory = factory
            )

            PremiumOcrEditor(
                viewModel = editorViewModel,
                onNavigateBack = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refresh_pdf", true)
                    navController.popBackStack()
                }
            )
        }

        // Unified Editor route supporting both Free and Premium launch modes
        composable(
            route = AppRoutes.EDITOR,
            arguments = listOf(
                navArgument("documentId") { type = NavType.LongType },
                navArgument("pageIndex") { type = NavType.IntType },
                navArgument("mode") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getLong("documentId") ?: 0L
            val pageIndex = backStackEntry.arguments?.getInt("pageIndex") ?: 0
            val modeName = backStackEntry.arguments?.getString("mode") ?: EditorLaunchMode.FREE_ANNOTATION.name
            val launchMode = runCatching { EditorLaunchMode.valueOf(modeName) }
                .getOrDefault(EditorLaunchMode.FREE_ANNOTATION)

            PdfEditorScreen(
                documentId = documentId,
                initialPageIndex = pageIndex,
                launchMode = launchMode,
                repository = repository,
                subscriptionManager = subscriptionManager,
                onNavigateBack = {
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("refresh_pdf", true)
                    navController.popBackStack()
                }
            )
        }
    }
}
