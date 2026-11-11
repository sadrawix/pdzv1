package com.example

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InterFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Exact Stitch AI Scanner Viewfinder Palette (`code_73714d18.html` & `screen_bde6a213.png`)
private val ScannerSurface = Color(0xFFFFF8F7)
private val ScannerOnSurface = Color(0xFF2A1614)
private val ScannerPrimaryRed = Color(0xFFBC0003)
private val ScannerInverseSurface = Color(0xFF412B28)
private val ScannerTertiaryBlue = Color(0xFF0058BC)
private val ScannerElectricBlue = Color(0xFF0D70E9)
private val ScannerTertiaryFixedDim = Color(0xFFADC6FF)
private val ScannerSurfaceContainerHighest = Color(0xFFFFDAD5)
private val ScannerSurfaceContainerHigh = Color(0xFFFFE2DE)
private val ScannerFlashYellow = Color(0xFFFDE047)
private val ScannerWalnutWoodDark = Color(0xFF3A2418)
private val ScannerWalnutWoodMid = Color(0xFF4E3222)

/**
 * Flash states matching the Stitch Viewfinder script (`Auto`, `On`, `Off`).
 */
enum class ScannerFlashMode(val label: String) {
    AUTO("Auto"),
    ON("On"),
    OFF("Off")
}

/**
 * Document type presets in the top scrollable pill selector bar.
 */
enum class ScannerDocumentPreset(
    val label: String,
    val badgeCode: String,
    val statusDescription: String,
    val defaultCorners: List<Offset>
) {
    DOCUMENT(
        label = "Document",
        badgeCode = "A4 • 90°",
        statusDescription = "Hold steady • 4 corners locked (A4)",
        defaultCorners = listOf(
            Offset(0.06f, 0.04f),
            Offset(0.94f, 0.08f),
            Offset(0.90f, 0.95f),
            Offset(0.08f, 0.91f)
        )
    ),
    ID_CARD(
        label = "ID Card",
        badgeCode = "ID-1 • 90°",
        statusDescription = "Hold steady • 4 corners locked (ID Card)",
        defaultCorners = listOf(
            Offset(0.08f, 0.24f),
            Offset(0.92f, 0.25f),
            Offset(0.91f, 0.76f),
            Offset(0.09f, 0.75f)
        )
    ),
    PASSPORT(
        label = "Passport",
        badgeCode = "B7 • 90°",
        statusDescription = "Hold steady • 4 corners locked (Passport)",
        defaultCorners = listOf(
            Offset(0.10f, 0.14f),
            Offset(0.90f, 0.16f),
            Offset(0.89f, 0.86f),
            Offset(0.11f, 0.84f)
        )
    ),
    BOOK(
        label = "Book",
        badgeCode = "2-Page • 90°",
        statusDescription = "Hold steady • Dual page spread locked",
        defaultCorners = listOf(
            Offset(0.04f, 0.08f),
            Offset(0.96f, 0.08f),
            Offset(0.95f, 0.92f),
            Offset(0.05f, 0.92f)
        )
    )
}

/**
 * Enhancement filter modes cycled by the bottom-right `Enhance` magic wand button.
 */
enum class ScannerEnhanceFilter(val label: String) {
    MAGIC_COLOR("Magic Color"),
    CRISP_BW("Crisp B&W"),
    HIGH_CONTRAST("High Contrast"),
    ORIGINAL("Original")
}

/**
 * Stitch AI Advanced "Scanner" Viewfinder Screen (`code_73714d18.html` & `screen_bde6a213.png`):
 * - Top header with red `document_scanner` icon and `"Scanner"` title.
 * - Rounded `24.dp` Viewfinder Frame (`bg-inverse-surface`) featuring:
 *   - Realistic overhead business contract document camera feed on a walnut executive desk.
 *   - Dynamic `3x3` Rule-of-Thirds composition grid overlay (`grid_on` toggle).
 *   - Live neon-blue (`#0D70E9`) polygon edge detection quad overlay with animated vertical laser
 *     scanning beam and 4 interactive corner loupe target anchors (`A4 • 90°` badge on top-left).
 *   - Top bar controls: Flash Mode Toggle (`Auto` / `On` / `Off`), Grid Toggle, Camera Flip (`180°` animation),
 *     and Preset Pill Selector (`Document`, `ID Card`, `Passport`, `Book`).
 *   - Center/Bottom status pill (`"Hold steady • 4 corners locked (A4)"`), `Auto` / `Manual` detection
 *     mode switcher, and `Batch (3)` multi-page selector.
 *   - Bottom Control Deck: Gallery `Import` thumbnail button, Jetpack Compose Shutter Button (`"Tap to scan"`),
 *     and `Enhance` magic wand filter toggle.
 * - Wired to Google ML Kit `GmsDocumentScanner` (`SCANNER_MODE_FULL`, `RESULT_FORMAT_PDF`,
 *   `RESULT_FORMAT_JPEG`, multi-page enabled) and zero-permission Gallery Photo Picker.
 */
@Composable
fun ScannerViewfinderScreen(
    onTriggerMlKitScanner: () -> Unit,
    onGalleryImagesSelected: (List<Uri>) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var flashMode by remember { mutableStateOf(ScannerFlashMode.AUTO) }
    var isGridVisible by remember { mutableStateOf(true) }
    var isFrontCamera by remember { mutableStateOf(false) }
    var cameraFlipRotation by remember { mutableStateOf(0f) }

    var selectedPreset by remember { mutableStateOf(ScannerDocumentPreset.DOCUMENT) }
    var isAutoDetectionMode by remember { mutableStateOf(true) }
    var batchCount by remember { mutableIntStateOf(3) }
    var enhanceFilter by remember { mutableStateOf(ScannerEnhanceFilter.MAGIC_COLOR) }
    var showShutterFlash by remember { mutableStateOf(false) }

    // Normalized polygon corner anchors (draggable in Manual mode, animated in Auto mode)
    var cornerTopLeft by remember { mutableStateOf(selectedPreset.defaultCorners[0]) }
    var cornerTopRight by remember { mutableStateOf(selectedPreset.defaultCorners[1]) }
    var cornerBottomRight by remember { mutableStateOf(selectedPreset.defaultCorners[2]) }
    var cornerBottomLeft by remember { mutableStateOf(selectedPreset.defaultCorners[3]) }

    LaunchedEffect(selectedPreset) {
        cornerTopLeft = selectedPreset.defaultCorners[0]
        cornerTopRight = selectedPreset.defaultCorners[1]
        cornerBottomRight = selectedPreset.defaultCorners[2]
        cornerBottomLeft = selectedPreset.defaultCorners[3]
    }

    // Zero-permission Android Photo Picker for the Gallery "Import" button
    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            onGalleryImagesSelected(uris)
        }
    }

    // Infinite animations for the laser scanning beam (6%..86% vertical sweep in 2.8s) and neon pulse
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_viewfinder_transition")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.86f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_scan_progress"
    )
    val polygonPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.10f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "polygon_pulse_alpha"
    )
    val statusBounceOffsetDp by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_chip_bounce"
    )

    val animatedFlipRotation by animateFloatAsState(
        targetValue = cameraFlipRotation,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "camera_flip_rotation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScannerSurface)
            .testTag("scanner_viewfinder_screen")
    ) {
        // 1. Top Scanner Header (`h-16 px-gutter flex items-center gap-space-sm`)
        StitchScannerTopHeader()

        // 2. Main Viewfinder Frame Container (`rounded-3xl overflow-hidden bg-inverse-surface shadow-2xl`)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .shadow(elevation = 14.dp, shape = RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(ScannerInverseSurface)
        ) {
            // A. Realistic Viewfinder Camera Feed Simulation (Service Agreement Contract on Walnut Desk)
            SimulatedContractCameraFeed(
                enhanceFilter = enhanceFilter,
                isFrontCamera = isFrontCamera,
                modifier = Modifier.fillMaxSize()
            )

            // B. Subtle Camera Dark Vignette (`bg-gradient-to-b from-black/60 via-transparent to-black/80`)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.58f),
                            0.30f to Color.Transparent,
                            0.68f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.82f)
                        )
                    )
            )

            // C. Rule of Thirds Dynamic 3x3 Grid Overlay (`opacity-40`)
            androidx.compose.animation.AnimatedVisibility(
                visible = isGridVisible,
                enter = fadeIn(tween(250)),
                exit = fadeOut(tween(250)),
                modifier = Modifier.fillMaxSize()
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridColor = Color.White.copy(alpha = 0.26f)
                    val strokeW = 1.dp.toPx()
                    val thirdW = size.width / 3f
                    val thirdH = size.height / 3f

                    // Vertical third lines
                    drawLine(
                        color = gridColor,
                        start = Offset(thirdW, 0f),
                        end = Offset(thirdW, size.height),
                        strokeWidth = strokeW
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(thirdW * 2f, 0f),
                        end = Offset(thirdW * 2f, size.height),
                        strokeWidth = strokeW
                    )

                    // Horizontal third lines
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, thirdH),
                        end = Offset(size.width, thirdH),
                        strokeWidth = strokeW
                    )
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, thirdH * 2f),
                        end = Offset(size.width, thirdH * 2f),
                        strokeWidth = strokeW
                    )
                }
            }

            // D. Live Edge Detection Polygon Overlay + Animated Laser Scanning Bar + 4 Corner Loupe Anchors
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .fillMaxHeight(0.74f)
                ) {
                    val boxWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                    val boxHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
                    val density = LocalDensity.current

                    val tlPx = Offset(cornerTopLeft.x * boxWidthPx, cornerTopLeft.y * boxHeightPx)
                    val trPx = Offset(cornerTopRight.x * boxWidthPx, cornerTopRight.y * boxHeightPx)
                    val brPx = Offset(cornerBottomRight.x * boxWidthPx, cornerBottomRight.y * boxHeightPx)
                    val blPx = Offset(cornerBottomLeft.x * boxWidthPx, cornerBottomLeft.y * boxHeightPx)

                    // Polygon Quad + Laser Scanning Line
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val quadPath = Path().apply {
                            moveTo(tlPx.x, tlPx.y)
                            lineTo(trPx.x, trPx.y)
                            lineTo(brPx.x, brPx.y)
                            lineTo(blPx.x, blPx.y)
                            close()
                        }

                        // Semi-transparent electric blue polygon interior fill (`fill="#0d70e9" fill-opacity="0.12"`)
                        drawPath(
                            path = quadPath,
                            color = ScannerElectricBlue.copy(alpha = polygonPulseAlpha),
                            style = Fill
                        )

                        // Soft outer neon blue glow stroke
                        drawPath(
                            path = quadPath,
                            color = ScannerElectricBlue.copy(alpha = 0.35f),
                            style = Stroke(
                                width = 7.dp.toPx(),
                                join = StrokeJoin.Round,
                                cap = StrokeCap.Round
                            )
                        )

                        // Crisp electric blue polygon border (`stroke="#0d70e9" stroke-width="1.2"`)
                        drawPath(
                            path = quadPath,
                            color = ScannerElectricBlue,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                join = StrokeJoin.Round,
                                cap = StrokeCap.Round
                            )
                        )

                        // Animated Laser Scanning Bar (`from-transparent via-[#0d70e9] to-transparent`)
                        val laserY = laserProgress * size.height
                        val trailHeight = 28.dp.toPx()
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    ScannerElectricBlue.copy(alpha = 0.24f),
                                    Color.Transparent
                                ),
                                startY = laserY,
                                endY = (laserY + trailHeight).coerceAtMost(size.height)
                            ),
                            topLeft = Offset(size.width * 0.06f, laserY),
                            size = Size(size.width * 0.88f, trailHeight)
                        )
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ScannerElectricBlue,
                                    ScannerElectricBlue,
                                    Color.Transparent
                                )
                            ),
                            start = Offset(size.width * 0.04f, laserY),
                            end = Offset(size.width * 0.96f, laserY),
                            strokeWidth = 2.2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }

                    // Center Alignment Horizon Reticle (`w-8 h-8 rounded-full border border-white/25`)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center)
                            .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.75f))
                        )
                    }

                    // 4 Adjustable Corner Loupe Target Anchors
                    CornerLoupeAnchor(
                        centerPx = tlPx,
                        badgeText = selectedPreset.badgeCode,
                        onDragDelta = { dx, dy ->
                            isAutoDetectionMode = false
                            cornerTopLeft = Offset(
                                (cornerTopLeft.x + dx / boxWidthPx).coerceIn(0.01f, 0.45f),
                                (cornerTopLeft.y + dy / boxHeightPx).coerceIn(0.01f, 0.45f)
                            )
                        }
                    )
                    CornerLoupeAnchor(
                        centerPx = trPx,
                        badgeText = null,
                        onDragDelta = { dx, dy ->
                            isAutoDetectionMode = false
                            cornerTopRight = Offset(
                                (cornerTopRight.x + dx / boxWidthPx).coerceIn(0.55f, 0.99f),
                                (cornerTopRight.y + dy / boxHeightPx).coerceIn(0.01f, 0.45f)
                            )
                        }
                    )
                    CornerLoupeAnchor(
                        centerPx = brPx,
                        badgeText = null,
                        onDragDelta = { dx, dy ->
                            isAutoDetectionMode = false
                            cornerBottomRight = Offset(
                                (cornerBottomRight.x + dx / boxWidthPx).coerceIn(0.55f, 0.99f),
                                (cornerBottomRight.y + dy / boxHeightPx).coerceIn(0.55f, 0.99f)
                            )
                        }
                    )
                    CornerLoupeAnchor(
                        centerPx = blPx,
                        badgeText = null,
                        onDragDelta = { dx, dy ->
                            isAutoDetectionMode = false
                            cornerBottomLeft = Offset(
                                (cornerBottomLeft.x + dx / boxWidthPx).coerceIn(0.01f, 0.45f),
                                (cornerBottomLeft.y + dy / boxHeightPx).coerceIn(0.55f, 0.99f)
                            )
                        }
                    )
                }
            }

            // E. Foreground Viewfinder Controls Column (Top Overlay + Center/Bottom Status + Bottom Control Deck)
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Viewfinder Top Bar Overlay (Flash, Grid, Flip + Preset Pill Selector Bar)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, start = 12.dp, end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Status & Quick Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Flash Mode Toggle (`flash_auto` / `flash_on` / `flash_off`)
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ScannerInverseSurface.copy(alpha = 0.78f),
                            shadowElevation = 4.dp,
                            modifier = Modifier
                                .height(42.dp)
                                .clickable {
                                    val modes = ScannerFlashMode.entries
                                    flashMode = modes[(flashMode.ordinal + 1) % modes.size]
                                }
                                .testTag("scanner_flash_toggle")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FlashModeVectorIcon(
                                    mode = flashMode,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = flashMode.label,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.5.sp,
                                    color = Color.White
                                )
                            }
                        }

                        // Right Quick Actions: Grid lines + Camera Flip
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Grid Toggle Button (`grid_on`)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .shadow(elevation = 4.dp, shape = CircleShape)
                                    .clip(CircleShape)
                                    .background(ScannerInverseSurface.copy(alpha = 0.78f))
                                    .clickable { isGridVisible = !isGridVisible }
                                    .testTag("scanner_grid_toggle"),
                                contentAlignment = Alignment.Center
                            ) {
                                GridVectorIcon(
                                    color = if (isGridVisible) {
                                        ScannerTertiaryFixedDim
                                    } else {
                                        Color.White.copy(alpha = 0.40f)
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Camera Switch (Flip) Button (`flip_camera_ios`)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .shadow(elevation = 4.dp, shape = CircleShape)
                                    .clip(CircleShape)
                                    .background(ScannerInverseSurface.copy(alpha = 0.78f))
                                    .clickable {
                                        isFrontCamera = !isFrontCamera
                                        cameraFlipRotation += 180f
                                    }
                                    .testTag("scanner_flip_camera_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                FlipCameraVectorIcon(
                                    color = Color.White,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .rotate(animatedFlipRotation)
                                )
                            }
                        }
                    }

                    // Center Preset Pill Selector Bar (`Document`, `ID Card`, `Passport`, `Book`)
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(ScannerInverseSurface.copy(alpha = 0.86f))
                                .horizontalScroll(rememberScrollState())
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ScannerDocumentPreset.entries.forEach { preset ->
                                val isSelected = preset == selectedPreset
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (isSelected) ScannerPrimaryRed else Color.Transparent
                                        )
                                        .clickable { selectedPreset = preset }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                        .testTag("scanner_preset_${preset.name.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = preset.label,
                                        fontFamily = InterFontFamily,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isSelected) {
                                            Color.White
                                        } else {
                                            ScannerSurfaceContainerHighest.copy(alpha = 0.82f)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Center/Bottom Status Chip + Auto/Manual & Batch Mode Switcher + Bottom Control Deck
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Auto Detect State Pill (`Hold steady • 4 corners locked (A4)`)
                    Box(
                        modifier = Modifier
                            .offset(y = statusBounceOffsetDp.dp)
                            .padding(horizontal = 16.dp)
                            .shadow(elevation = 8.dp, shape = RoundedCornerShape(50))
                            .clip(RoundedCornerShape(50))
                            .background(ScannerInverseSurface.copy(alpha = 0.92f))
                            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Pulsing Electric Blue Lock Dot
                            Box(
                                modifier = Modifier.size(10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(ScannerTertiaryBlue.copy(alpha = 0.55f))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(ScannerElectricBlue)
                                )
                            }

                            Text(
                                text = if (isAutoDetectionMode) {
                                    selectedPreset.statusDescription
                                } else {
                                    "Manual crop • Drag blue corner anchors"
                                },
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = Color.White,
                                letterSpacing = 0.2.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Mode (`Auto` / `Manual`) & `Batch (3)` Configuration Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Detection Mode Switcher (`Auto` | `Manual`)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isAutoDetectionMode) Color.White else Color.Transparent
                                    )
                                    .clickable {
                                        isAutoDetectionMode = true
                                        cornerTopLeft = selectedPreset.defaultCorners[0]
                                        cornerTopRight = selectedPreset.defaultCorners[1]
                                        cornerBottomRight = selectedPreset.defaultCorners[2]
                                        cornerBottomLeft = selectedPreset.defaultCorners[3]
                                    }
                                    .padding(horizontal = 11.dp, vertical = 4.dp)
                                    .testTag("scanner_mode_auto"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Auto",
                                    fontFamily = InterFontFamily,
                                    fontWeight = if (isAutoDetectionMode) FontWeight.SemiBold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (isAutoDetectionMode) {
                                        ScannerOnSurface
                                    } else {
                                        ScannerSurfaceContainerHighest.copy(alpha = 0.75f)
                                    }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (!isAutoDetectionMode) Color.White else Color.Transparent
                                    )
                                    .clickable { isAutoDetectionMode = false }
                                    .padding(horizontal = 11.dp, vertical = 4.dp)
                                    .testTag("scanner_mode_manual"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Manual",
                                    fontFamily = InterFontFamily,
                                    fontWeight = if (!isAutoDetectionMode) FontWeight.SemiBold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (!isAutoDetectionMode) {
                                        ScannerOnSurface
                                    } else {
                                        ScannerSurfaceContainerHighest.copy(alpha = 0.75f)
                                    }
                                )
                            }
                        }

                        // Single / Batch Mode Selector (`Batch (3)`)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable {
                                    batchCount = if (batchCount >= 5) 1 else batchCount + 1
                                }
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                                .testTag("scanner_batch_toggle"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LayersVectorIcon(
                                color = ScannerTertiaryFixedDim,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (batchCount == 1) "Single (1)" else "Batch ($batchCount)",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = ScannerSurfaceContainerHighest.copy(alpha = 0.92f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Viewfinder Bottom Control Deck (Translucent M3 Dark Sheet)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.80f),
                                        Color.Black
                                    )
                                )
                            )
                            .padding(horizontal = 22.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Recent Import / Gallery Thumbnail (`Gallery` + red `Import` badge)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    galleryPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                                .testTag("scanner_gallery_import_button")
                        ) {
                            Box(
                                modifier = Modifier.size(56.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(ScannerSurfaceContainerHighest.copy(alpha = 0.22f))
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Miniature receipt/invoice preview matching Stitch thumbnail
                                    GalleryReceiptMiniatureThumbnail(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(11.dp))
                                    )
                                }

                                // Red "Import" pill badge at bottom-right
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .clip(RoundedCornerShape(50))
                                        .background(ScannerPrimaryRed)
                                        .padding(horizontal = 6.dp, vertical = 1.5.dp)
                                ) {
                                    Text(
                                        text = "Import",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.5.sp,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Gallery",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = ScannerSurfaceContainerHighest.copy(alpha = 0.75f)
                            )
                        }

                        // Center: Jetpack Compose Style Shutter Button (`Tap to scan`)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        coroutineScope.launch {
                                            showShutterFlash = true
                                            delay(120)
                                            showShutterFlash = false
                                            onTriggerMlKitScanner()
                                        }
                                    }
                                    .testTag("scanner_shutter_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                // Outer Pulsing Translucent Ring (`border-4 border-surface-container-lowest/80`)
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .border(
                                            width = 4.dp,
                                            color = Color.White.copy(alpha = 0.85f),
                                            shape = CircleShape
                                        )
                                )

                                // Inner Classic Shutter Core (`w-16 h-16 rounded-full bg-surface-container-lowest`)
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .shadow(elevation = 6.dp, shape = CircleShape)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Red Recording Accent (`w-4 h-4 rounded-full bg-primary`)
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(ScannerPrimaryRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.85f))
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Tap to scan",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }

                        // Right: Magic OCR & Filters Action (`Enhance`)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    val filters = ScannerEnhanceFilter.entries
                                    enhanceFilter = filters[(enhanceFilter.ordinal + 1) % filters.size]
                                }
                                .testTag("scanner_enhance_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(ScannerSurfaceContainerHighest.copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AutoFixHighVectorIcon(
                                    color = ScannerTertiaryFixedDim,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (enhanceFilter == ScannerEnhanceFilter.MAGIC_COLOR) {
                                    "Enhance"
                                } else {
                                    enhanceFilter.label
                                },
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = ScannerSurfaceContainerHighest.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            // F. Shutter Flash Feedback Overlay (`bg-white z-50 transition-opacity`)
            androidx.compose.animation.AnimatedVisibility(
                visible = showShutterFlash,
                enter = fadeIn(tween(40)),
                exit = fadeOut(tween(240)),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )
            }
        }
    }
}

// =============================================================================================
// SUB-COMPONENTS & CUSTOM VECTOR ICONS MATCHING STITCH `code_73714d18.html`
// =============================================================================================

@Composable
private fun StitchScannerTopHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Red `document_scanner` icon (`w-10 h-10 rounded-full flex items-center justify-center text-primary`)
        Box(
            modifier = Modifier.size(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(24.dp)) {
                val w = size.width
                val h = size.height
                val stroke = 2.dp.toPx()
                val cornerLen = w * 0.24f

                // 4 scanner viewfinder brackets
                val brackets = Path().apply {
                    // Top-left
                    moveTo(w * 0.08f, h * 0.28f)
                    lineTo(w * 0.08f, h * 0.08f)
                    lineTo(w * 0.08f + cornerLen, h * 0.08f)
                    // Top-right
                    moveTo(w * 0.92f - cornerLen, h * 0.08f)
                    lineTo(w * 0.92f, h * 0.08f)
                    lineTo(w * 0.92f, h * 0.28f)
                    // Bottom-right
                    moveTo(w * 0.92f, h * 0.72f)
                    lineTo(w * 0.92f, h * 0.92f)
                    lineTo(w * 0.92f - cornerLen, h * 0.92f)
                    // Bottom-left
                    moveTo(w * 0.08f + cornerLen, h * 0.92f)
                    lineTo(w * 0.08f, h * 0.92f)
                    lineTo(w * 0.08f, h * 0.72f)
                }
                drawPath(
                    path = brackets,
                    color = ScannerPrimaryRed,
                    style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Inner document page + text lines
                drawRoundRect(
                    color = ScannerPrimaryRed,
                    topLeft = Offset(w * 0.25f, h * 0.22f),
                    size = Size(w * 0.50f, h * 0.56f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = Stroke(width = 1.7.dp.toPx())
                )
                for (i in 0..2) {
                    val y = h * (0.36f + i * 0.13f)
                    drawLine(
                        color = ScannerPrimaryRed,
                        start = Offset(w * 0.34f, y),
                        end = Offset(w * 0.66f, y),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Text(
            text = "Scanner",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            color = ScannerOnSurface,
            letterSpacing = (-0.3).sp
        )
    }
}

/**
 * Draggable corner loupe target anchor (`w-8 h-8 rounded-full bg-inverse-surface/80 border border-white/20`
 * with inner `w-3.5 h-3.5 rounded-full bg-tertiary-container shadow-[0_0_8px_#0d70e9]`) and optional
 * `"A4 • 90°"` floating badge pill.
 */
@Composable
private fun CornerLoupeAnchor(
    centerPx: Offset,
    badgeText: String?,
    onDragDelta: (Float, Float) -> Unit
) {
    val density = LocalDensity.current
    val halfSizePx = with(density) { 16.dp.toPx() }

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (centerPx.x - halfSizePx).roundToInt(),
                    y = (centerPx.y - halfSizePx).roundToInt()
                )
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDragDelta(dragAmount.x, dragAmount.y)
                }
            }
    ) {
        // Optional "A4 • 90°" badge above top-left corner anchor
        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .offset(x = (-2).dp, y = (-20).dp)
                    .clip(RoundedCornerShape(50))
                    .background(ScannerInverseSurface.copy(alpha = 0.92f))
                    .border(0.8.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = badgeText,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.sp,
                    color = ScannerTertiaryFixedDim
                )
            }
        }

        // Outer translucent dark loupe circle (`w-8 h-8`)
        Box(
            modifier = Modifier
                .size(32.dp)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(ScannerInverseSurface.copy(alpha = 0.82f))
                .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Glowing electric blue inner circle (`w-3.5 h-3.5 bg-tertiary-container`)
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .shadow(elevation = 4.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(ScannerElectricBlue)
            )
        }
    }
}

/**
 * Renders the realistic overhead executive walnut desk + formal two-column `"SERVICE AGREEMENT"`
 * business contract document with signatures and text tables matching `screen_bde6a213.png`.
 */
@Composable
private fun SimulatedContractCameraFeed(
    enhanceFilter: ScannerEnhanceFilter,
    isFrontCamera: Boolean,
    modifier: Modifier = Modifier
) {
    val paperColor = when (enhanceFilter) {
        ScannerEnhanceFilter.MAGIC_COLOR -> Color(0xFFDCE3EC)
        ScannerEnhanceFilter.CRISP_BW -> Color(0xFFF1F5F9)
        ScannerEnhanceFilter.HIGH_CONTRAST -> Color(0xFFFFFFFF)
        ScannerEnhanceFilter.ORIGINAL -> Color(0xFFCFD6DF)
    }
    val inkColor = when (enhanceFilter) {
        ScannerEnhanceFilter.HIGH_CONTRAST -> Color(0xFF0F172A)
        else -> Color(0xFF283548)
    }

    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        ScannerWalnutWoodDark,
                        ScannerWalnutWoodMid,
                        ScannerWalnutWoodDark
                    )
                )
            )
            .graphicsLayer {
                if (isFrontCamera) {
                    scaleX = -1f
                }
            }
    ) {
        // Subtle woodgrain lines on the top executive desk area
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            for (i in 0..8) {
                val y = h * (0.02f + i * 0.022f)
                drawLine(
                    color = Color.Black.copy(alpha = 0.14f),
                    start = Offset(0f, y),
                    end = Offset(w, y + 6.dp.toPx()),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            // Left Document Page ("SERVICE AGREEMENT" + Paragraphs + Financial Table)
            val pageTop = h * 0.21f
            val pageBottom = h * 0.77f
            val pageHeight = pageBottom - pageTop
            val leftPageWidth = w * 0.475f

            drawRoundRect(
                color = paperColor,
                topLeft = Offset(0f, pageTop),
                size = Size(leftPageWidth, pageHeight),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )

            // Right Document Page ("Clauses" + Numbered Clauses + "EXECUTION" + Blue Ink Signatures)
            val rightPageLeft = w * 0.525f
            val rightPageWidth = w - rightPageLeft
            drawRoundRect(
                color = paperColor.copy(alpha = 0.96f),
                topLeft = Offset(rightPageLeft, pageTop + 4.dp.toPx()),
                size = Size(rightPageWidth, pageHeight),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )

            // Left Page Table Grid (middle-left of contract)
            val tableLeft = w * 0.03f
            val tableTop = h * 0.46f
            val tableW = w * 0.39f
            val tableH = h * 0.10f
            drawRect(
                color = inkColor.copy(alpha = 0.32f),
                topLeft = Offset(tableLeft, tableTop),
                size = Size(tableW, tableH),
                style = Stroke(width = 1.dp.toPx())
            )
            for (r in 1..3) {
                val rowY = tableTop + (tableH / 4f) * r
                drawLine(
                    color = inkColor.copy(alpha = 0.24f),
                    start = Offset(tableLeft, rowY),
                    end = Offset(tableLeft + tableW, rowY),
                    strokeWidth = 0.8.dp.toPx()
                )
            }
            drawLine(
                color = inkColor.copy(alpha = 0.24f),
                start = Offset(tableLeft + tableW * 0.58f, tableTop),
                end = Offset(tableLeft + tableW * 0.58f, tableTop + tableH),
                strokeWidth = 0.8.dp.toPx()
            )

            // Simulated paragraph text lines on Left Page
            val leftLineYPositions = listOf(0.32f, 0.34f, 0.36f, 0.38f, 0.41f, 0.43f, 0.59f, 0.61f, 0.63f, 0.67f, 0.69f)
            leftLineYPositions.forEachIndexed { idx, yRatio ->
                val endX = if (idx % 3 == 2) w * 0.34f else w * 0.42f
                drawLine(
                    color = inkColor.copy(alpha = 0.42f),
                    start = Offset(w * 0.03f, h * yRatio),
                    end = Offset(endX, h * yRatio),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Simulated numbered clause text lines on Right Page
            for (clause in 0..14) {
                val y = h * (0.29f + clause * 0.016f)
                val lineW = if (clause % 4 == 3) w * 0.26f else w * 0.38f
                drawLine(
                    color = inkColor.copy(alpha = 0.44f),
                    start = Offset(rightPageLeft + w * 0.05f, y),
                    end = Offset(rightPageLeft + w * 0.05f + lineW, y),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Cursive Blue Ink Signature on Right Page ("Alex R. Chen")
            val sigPath = Path().apply {
                val sx = rightPageLeft + w * 0.07f
                val sy = h * 0.61f
                moveTo(sx, sy)
                cubicTo(sx + 18.dp.toPx(), sy - 14.dp.toPx(), sx + 28.dp.toPx(), sy + 6.dp.toPx(), sx + 44.dp.toPx(), sy - 6.dp.toPx())
                cubicTo(sx + 58.dp.toPx(), sy - 12.dp.toPx(), sx + 74.dp.toPx(), sy + 4.dp.toPx(), sx + 98.dp.toPx(), sy - 3.dp.toPx())
            }
            drawPath(
                path = sigPath,
                color = Color(0xFF1E40AF).copy(alpha = 0.72f),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )
            // Signature underline
            drawLine(
                color = inkColor.copy(alpha = 0.45f),
                start = Offset(rightPageLeft + w * 0.06f, h * 0.625f),
                end = Offset(rightPageLeft + w * 0.32f, h * 0.625f),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Crisp typographic headings on the simulated contract pages (`SERVICE AGREEMENT`, `Clauses`, `EXECUTION`)
        Text(
            text = "SERVICE AGREEMENT",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = inkColor.copy(alpha = 0.82f),
            letterSpacing = 0.4.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 10.dp, top = 148.dp)
        )

        Text(
            text = "Clauses",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = inkColor.copy(alpha = 0.78f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 112.dp, top = 152.dp)
        )

        Text(
            text = "EXECUTION",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 8.5.sp,
            color = inkColor.copy(alpha = 0.78f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 104.dp, top = 46.dp)
        )
    }
}

@Composable
private fun GalleryReceiptMiniatureThumbnail(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF6B5344))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Miniature receipt paper inside the gallery thumbnail
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color(0xFFF8FAFC),
                topLeft = Offset(size.width * 0.14f, size.height * 0.08f),
                size = Size(size.width * 0.72f, size.height * 0.84f),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            for (i in 0..3) {
                val y = size.height * (0.24f + i * 0.15f)
                drawLine(
                    color = Color(0xFF475569),
                    start = Offset(size.width * 0.24f, y),
                    end = Offset(size.width * 0.76f, y),
                    strokeWidth = 1.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun FlashModeVectorIcon(
    mode: ScannerFlashMode,
    modifier: Modifier = Modifier
) {
    val boltColor = when (mode) {
        ScannerFlashMode.AUTO -> ScannerFlashYellow
        ScannerFlashMode.ON -> Color(0xFFFACC15)
        ScannerFlashMode.OFF -> Color.White.copy(alpha = 0.60f)
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val bolt = Path().apply {
                moveTo(w * 0.52f, h * 0.08f)
                lineTo(w * 0.18f, h * 0.54f)
                lineTo(w * 0.46f, h * 0.54f)
                lineTo(w * 0.36f, h * 0.92f)
                lineTo(w * 0.76f, h * 0.42f)
                lineTo(w * 0.48f, h * 0.42f)
                close()
            }
            drawPath(
                path = bolt,
                color = boltColor,
                style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
            )
            if (mode == ScannerFlashMode.OFF) {
                drawLine(
                    color = boltColor,
                    start = Offset(w * 0.12f, h * 0.12f),
                    end = Offset(w * 0.88f, h * 0.88f),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        if (mode == ScannerFlashMode.AUTO) {
            Text(
                text = "A",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 8.sp,
                color = boltColor,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun GridVectorIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.10f, h * 0.10f),
            size = Size(w * 0.80f, h * 0.80f),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
            style = Stroke(width = stroke)
        )
        // 2 inner vertical + 2 inner horizontal lines
        for (i in 1..2) {
            val pos = 0.10f + (0.80f / 3f) * i
            drawLine(
                color = color,
                start = Offset(w * pos, h * 0.10f),
                end = Offset(w * pos, h * 0.90f),
                strokeWidth = 1.3.dp.toPx()
            )
            drawLine(
                color = color,
                start = Offset(w * 0.10f, h * pos),
                end = Offset(w * 0.90f, h * pos),
                strokeWidth = 1.3.dp.toPx()
            )
        }
    }
}

@Composable
private fun FlipCameraVectorIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.6.dp.toPx()

        // Camera body outline
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.10f, h * 0.24f),
            size = Size(w * 0.80f, h * 0.58f),
            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            style = Stroke(width = stroke)
        )
        // Top prism bump
        val bump = Path().apply {
            moveTo(w * 0.32f, h * 0.24f)
            lineTo(w * 0.38f, h * 0.14f)
            lineTo(w * 0.62f, h * 0.14f)
            lineTo(w * 0.68f, h * 0.24f)
        }
        drawPath(path = bump, color = color, style = Stroke(width = stroke, join = StrokeJoin.Round))

        // Inner circular sync arrows
        drawArc(
            color = color,
            startAngle = -30f,
            sweepAngle = 220f,
            useCenter = false,
            topLeft = Offset(w * 0.33f, h * 0.36f),
            size = Size(w * 0.34f, h * 0.34f),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun LayersVectorIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = 1.5.dp.toPx()

        // Top diamond
        val diamond = Path().apply {
            moveTo(w * 0.50f, h * 0.12f)
            lineTo(w * 0.90f, h * 0.38f)
            lineTo(w * 0.50f, h * 0.64f)
            lineTo(w * 0.10f, h * 0.38f)
            close()
        }
        drawPath(path = diamond, color = color, style = Stroke(width = stroke, join = StrokeJoin.Round))

        // Bottom layer chevron
        val lower = Path().apply {
            moveTo(w * 0.12f, h * 0.58f)
            lineTo(w * 0.50f, h * 0.84f)
            lineTo(w * 0.88f, h * 0.58f)
        }
        drawPath(
            path = lower,
            color = color,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun AutoFixHighVectorIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Magic wand diagonal bar
        drawLine(
            color = color,
            start = Offset(w * 0.16f, h * 0.84f),
            end = Offset(w * 0.74f, h * 0.26f),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round
        )
        // Sparkle stars near wand tip
        val sparkles = listOf(
            Offset(w * 0.78f, h * 0.20f) to 3.dp.toPx(),
            Offset(w * 0.28f, h * 0.24f) to 2.dp.toPx(),
            Offset(w * 0.80f, h * 0.66f) to 2.dp.toPx()
        )
        sparkles.forEach { (center, radius) ->
            drawLine(
                color = color,
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = Offset(center.x, center.y - radius),
                end = Offset(center.x, center.y + radius),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
