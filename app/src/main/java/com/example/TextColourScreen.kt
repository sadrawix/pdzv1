package com.example

import android.graphics.Color as AndroidColor
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.InterFontFamily
import kotlin.math.roundToInt

// Exact Dark Theme Palette for the "Text Colour" Screen & Bottom Sheet
private val TextColourHeaderBlack = Color(0xFF000000)
private val TextColourBodyDark = Color(0xFF141414)
private val TextColourTabBarDark = Color(0xFF383838)
private val TextColourAccentRed = Color(0xFFF26B5B)
private val TextColourInactiveTabGray = Color(0xFFBDBDBD)
private val TextColourSliderTrackBlack = Color(0xFF050505)

/**
 * Stateful `TextColourScreen` connected directly to [PdfEditorViewModel].
 * Updates the active OCR [EditableTextBlock] text colour, transparent text box background flag,
 * and active annotation stroke colour in real time.
 */
@Composable
fun TextColourScreen(
    viewModel: PdfEditorViewModel,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val textBlocks by viewModel.textBlocks.collectAsStateWithLifecycle()
    val selectedTextBlockId by viewModel.selectedTextBlockId.collectAsStateWithLifecycle()
    val textColorTab by viewModel.textColorTab.collectAsStateWithLifecycle()
    val customColorInputMode by viewModel.customColorInputMode.collectAsStateWithLifecycle()
    val recentlyUsedTextColors by viewModel.recentlyUsedTextColors.collectAsStateWithLifecycle()
    val drawingStrokeColor by viewModel.drawingStrokeColor.collectAsStateWithLifecycle()

    val selectedBlock = remember(textBlocks, selectedTextBlockId) {
        textBlocks.firstOrNull { it.id == selectedTextBlockId } ?: textBlocks.firstOrNull()
    }
    val activeColorInt = selectedBlock?.textColor ?: drawingStrokeColor
    val isBgTransparent = selectedBlock?.isTransparentBackground ?: true

    TextColourBottomSheet(
        selectedColorInt = activeColorInt,
        activeTab = textColorTab,
        customColorInputMode = customColorInputMode,
        recentlyUsedColors = recentlyUsedTextColors,
        onTabSelected = { viewModel.setTextColorTab(it) },
        onCustomColorModeSelected = { viewModel.setCustomColorInputMode(it) },
        onColorSelected = { colorInt, recordInRecents ->
            viewModel.setSelectedBlockTextColor(colorInt, recordInRecents)
        },
        onBackClick = onBackClick,
        onCloseClick = onCloseClick,
        allowTransparentSelection = true,
        isTransparentSelected = isBgTransparent,
        onTransparentSelected = {
            viewModel.setSelectedBlockBackgroundTransparent(
                isTransparent = true,
                backgroundColor = AndroidColor.TRANSPARENT
            )
        },
        modifier = modifier
    )
}

/**
 * Complete Jetpack Compose UI component for the dark-theme **"Text Colour"** Editor Screen &
 * Bottom Sheet (`PALETTE` and `CUSTOM` tabs):
 *
 * 1. **Top Header (`#000000`)**:
 *    - Back arrow (`←`), `"Text Colour"` title, and close `'X'` icon.
 * 2. **Split Color Preview Box**:
 *    - Left (`35%`): Solid color preview block.
 *    - Right (`65%`): Checkerboard transparency pattern with live `#HEX`, `HSL`, and `RGB` metadata.
 * 3. **Tab Switcher (`#383838`)**:
 *    - Horizontal tabs for `"PALETTE"` and `"CUSTOM"` with an active coral-red (`#F26B5B`) underline indicator.
 * 4. **PALETTE Tab**:
 *    - `"Recently Used"` horizontal list of rounded-square color swatches with a circular checkmark icon on the selected color.
 *    - `"Default Colour Palette"` 3-row grid of standard color blocks (including the checkerboard transparent swatch at Row 1 Col 1).
 *    - `"Colour Variations"` horizontal row of 9 shade and tint gradient variations.
 * 5. **CUSTOM Tab**:
 *    - Interactive slider rows for **Hue** (`0..360`), **Saturation** (`0..100`), and **Lightness** (`0..100`)
 *      (or **Red**, **Green**, **Blue** `0..255` when `RGB` is selected, or **Hex** input + HSL sliders when `Hex` is selected)
 *      with coral-red slider handles/tracks and editable numeric value text fields on the right.
 *    - Bottom radio selector options for **Hex**, **HSL** (selected by default with red radio dot), and **RGB**.
 */
@Composable
fun TextColourBottomSheet(
    selectedColorInt: Int,
    activeTab: TextColorTab,
    customColorInputMode: CustomColorInputMode,
    recentlyUsedColors: List<Int>,
    onTabSelected: (TextColorTab) -> Unit,
    onCustomColorModeSelected: (CustomColorInputMode) -> Unit,
    onColorSelected: (Int, Boolean) -> Unit,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Text Colour",
    showHeader: Boolean = true,
    allowTransparentSelection: Boolean = true,
    isTransparentSelected: Boolean = true,
    onTransparentSelected: (() -> Unit)? = null
) {
    Surface(
        color = TextColourBodyDark,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        shadowElevation = 18.dp,
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("text_colour_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(TextColourBodyDark)
        ) {
            if (showHeader) {
                TextColourTopHeader(
                    title = title,
                    onBackClick = onBackClick,
                    onCloseClick = onCloseClick
                )
            }

            TextColourEditorContent(
                selectedColorInt = selectedColorInt,
                activeTab = activeTab,
                customColorInputMode = customColorInputMode,
                recentlyUsedColors = recentlyUsedColors,
                onTabSelected = onTabSelected,
                onCustomColorModeSelected = onCustomColorModeSelected,
                onColorSelected = onColorSelected,
                allowTransparentSelection = allowTransparentSelection,
                isTransparentSelected = isTransparentSelected,
                onTransparentSelected = onTransparentSelected
            )
        }
    }
}

/**
 * Top Header bar (`#000000` background) with Back arrow, `"Text Colour"` title, and close `'X'` icon.
 */
@Composable
fun TextColourTopHeader(
    title: String,
    onBackClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TextColourHeaderBlack)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("text_color_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = title,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 20.sp,
                color = Color.White
            )
        }

        IconButton(
            onClick = onCloseClick,
            modifier = Modifier
                .size(32.dp)
                .testTag("close_ocr_sheet_button")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Text Colour",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Body content of the "Text Colour" editor containing the Split Color Preview Box,
 * `PALETTE` | `CUSTOM` Tab Switcher, and active tab content.
 */
@Composable
fun TextColourEditorContent(
    selectedColorInt: Int,
    activeTab: TextColorTab,
    customColorInputMode: CustomColorInputMode,
    recentlyUsedColors: List<Int>,
    onTabSelected: (TextColorTab) -> Unit,
    onCustomColorModeSelected: (CustomColorInputMode) -> Unit,
    onColorSelected: (Int, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    allowTransparentSelection: Boolean = true,
    isTransparentSelected: Boolean = true,
    onTransparentSelected: (() -> Unit)? = null
) {
    val redVal = AndroidColor.red(selectedColorInt)
    val greenVal = AndroidColor.green(selectedColorInt)
    val blueVal = AndroidColor.blue(selectedColorInt)
    val hexFormatted = remember(selectedColorInt) {
        String.format("#%06X", 0xFFFFFF and selectedColorInt)
    }

    val initialHsl = remember(selectedColorInt) {
        PdfEditorViewModel.rgbToHsl(redVal, greenVal, blueVal)
    }
    var hueVal by remember(selectedColorInt) { mutableIntStateOf(initialHsl.first) }
    var satVal by remember(selectedColorInt) { mutableIntStateOf(initialHsl.second) }
    var lightVal by remember(selectedColorInt) { mutableIntStateOf(initialHsl.third) }
    var hexInput by remember(selectedColorInt) { mutableStateOf(hexFormatted) }

    val colourVariations = remember(selectedColorInt) {
        PdfEditorViewModel.generateColourVariations(selectedColorInt)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TextColourBodyDark)
    ) {
        // 1. Top Split Color Preview Box (Left Solid Color Preview + Right Checkerboard with #HEX / HSL / RGB)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .background(TextColourHeaderBlack)
        ) {
            // Left ~35%: Solid Selected Colour Preview Block
            Box(
                modifier = Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .background(Color(selectedColorInt))
                    .testTag("text_color_solid_preview")
            )

            // Right ~65%: Checkerboard Transparency Pattern + Live #HEX / HSL / RGB Metadata
            Box(
                modifier = Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellPx = 12.dp.toPx()
                    val cols = (size.width / cellPx).toInt() + 1
                    val rows = (size.height / cellPx).toInt() + 1
                    val lightSquare = Color.White
                    val graySquare = Color(0xFFDFDFDF)

                    for (r in 0..rows) {
                        for (c in 0..cols) {
                            drawRect(
                                color = if ((r + c) % 2 == 0) graySquare else lightSquare,
                                topLeft = Offset(c * cellPx, r * cellPx),
                                size = Size(cellPx, cellPx)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val readoutStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.White,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.55f),
                            offset = Offset(1f, 1f),
                            blurRadius = 3f
                        )
                    )
                    Text(
                        text = hexFormatted,
                        style = readoutStyle,
                        modifier = Modifier.testTag("text_color_hex_readout")
                    )
                    Text(
                        text = "HSL $hueVal $satVal $lightVal",
                        style = readoutStyle,
                        modifier = Modifier.testTag("text_color_hsl_readout")
                    )
                    Text(
                        text = "RGB $redVal $greenVal $blueVal",
                        style = readoutStyle,
                        modifier = Modifier.testTag("text_color_rgb_readout")
                    )
                }
            }
        }

        // 2. Horizontal Tab Switcher ("PALETTE" | "CUSTOM") with Active Red Underline Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .background(TextColourTabBarDark)
        ) {
            TextColorTab.entries.forEach { tab ->
                val isSelected = tab == activeTab
                val tabTitle = if (tab == TextColorTab.PALETTE) "PALETTE" else "CUSTOM"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(tab) }
                        .testTag("text_color_tab_${tabTitle.lowercase()}")
                ) {
                    Text(
                        text = tabTitle,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        letterSpacing = 0.8.sp,
                        color = if (isSelected) TextColourAccentRed else TextColourInactiveTabGray,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .background(TextColourAccentRed)
                        )
                    }
                }
            }
        }

        // 3. Switchable Tab Content ("PALETTE" vs "CUSTOM")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 350.dp)
                .verticalScroll(rememberScrollState())
                .background(TextColourBodyDark)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (activeTab == TextColorTab.PALETTE) {
                // Section 1: Recently Used (horizontal list of swatches with checkmark on active color)
                Text(
                    text = "Recently Used",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = Color.White
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    recentlyUsedColors.take(8).forEachIndexed { idx, recentColor ->
                        val isSelected = recentColor == selectedColorInt
                        TextColorRoundedSquareSwatch(
                            colorInt = recentColor,
                            isSelected = isSelected,
                            onClick = { onColorSelected(recentColor, false) },
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("recent_text_color_$idx")
                        )
                    }
                }

                // Section 2: Default Colour Palette (3-row grid of standard color blocks,
                // starting with the checkerboard transparent swatch option at Row 1 Col 1)
                Text(
                    text = "Default Colour Palette",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = Color.White
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Row 1 (9 swatches: index 0 is checkerboard transparent option, 1..8 are white/greys/black/reds/orange)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PdfEditorViewModel.DEFAULT_COLOUR_PALETTE_ROW_1.forEachIndexed { idx, swatchColor ->
                            val isSelected = if (swatchColor == null) {
                                allowTransparentSelection && isTransparentSelected
                            } else {
                                swatchColor == selectedColorInt
                            }
                            TextColorRoundedSquareSwatch(
                                colorInt = swatchColor,
                                isSelected = isSelected,
                                onClick = {
                                    if (swatchColor == null && allowTransparentSelection && onTransparentSelected != null) {
                                        onTransparentSelected()
                                    } else {
                                        val target = swatchColor ?: AndroidColor.parseColor("#000000")
                                        onColorSelected(target, true)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .testTag("default_palette_r1_$idx")
                            )
                        }
                    }

                    // Row 2 (9 vivid chromatic swatches: yellows, greens, teals, blues, purples, pinks)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PdfEditorViewModel.DEFAULT_COLOUR_PALETTE_ROW_2.forEachIndexed { idx, swatchColor ->
                            val isSelected = swatchColor == selectedColorInt
                            TextColorRoundedSquareSwatch(
                                colorInt = swatchColor,
                                isSelected = isSelected,
                                onClick = { onColorSelected(swatchColor, true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .testTag("default_palette_r2_$idx")
                            )
                        }
                    }

                    // Row 3 (9 deep chromatic accent swatches completing the 3-row palette grid)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PdfEditorViewModel.DEFAULT_COLOUR_PALETTE_ROW_3.forEachIndexed { idx, swatchColor ->
                            val isSelected = swatchColor == selectedColorInt
                            TextColorRoundedSquareSwatch(
                                colorInt = swatchColor,
                                isSelected = isSelected,
                                onClick = { onColorSelected(swatchColor, true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .testTag("default_palette_r3_$idx")
                            )
                        }
                    }
                }

                // Section 3: Colour Variations (horizontal row of 9 shade and tint gradient variations)
                Text(
                    text = "Colour Variations",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = Color.White
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    colourVariations.forEachIndexed { idx, variationColor ->
                        val isSelected = variationColor == selectedColorInt
                        TextColorRoundedSquareSwatch(
                            colorInt = variationColor,
                            isSelected = isSelected,
                            onClick = { onColorSelected(variationColor, true) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .testTag("colour_variation_$idx")
                        )
                    }
                }

                // Transparent Text Box Background Toggle Row
                if (allowTransparentSelection && onTransparentSelected != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1F1F1F))
                            .clickable { onTransparentSelected() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("transparent_text_box_bg_toggle"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TextColorRoundedSquareSwatch(
                                colorInt = null,
                                isSelected = isTransparentSelected,
                                onClick = { onTransparentSelected() },
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "Transparent Text Box Background",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.5.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isTransparentSelected) {
                                        "Active — underlying PDF shows through"
                                    } else {
                                        "Opaque white fill behind text box"
                                    },
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.5.sp,
                                    color = TextColourInactiveTabGray
                                )
                            }
                        }

                        Switch(
                            checked = isTransparentSelected,
                            onCheckedChange = { onTransparentSelected() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TextColourAccentRed,
                                uncheckedThumbColor = TextColourInactiveTabGray,
                                uncheckedTrackColor = TextColourSliderTrackBlack
                            )
                        )
                    }
                }
            } else {
                // CUSTOM Tab Implementation:
                // Interactive slider rows with red slider tracks/handles and numeric value text fields on the right
                when (customColorInputMode) {
                    CustomColorInputMode.HSL -> {
                        CustomColorChannelSliderRow(
                            label = "Hue",
                            value = hueVal,
                            valueRange = 0f..360f,
                            testTag = "custom_hsl_hue_slider",
                            onValueChange = { newH ->
                                hueVal = newH
                                val updatedColor = PdfEditorViewModel.hslToColorInt(hueVal, satVal, lightVal)
                                onColorSelected(updatedColor, true)
                            }
                        )

                        CustomColorChannelSliderRow(
                            label = "Saturation",
                            value = satVal,
                            valueRange = 0f..100f,
                            testTag = "custom_hsl_saturation_slider",
                            onValueChange = { newS ->
                                satVal = newS
                                val updatedColor = PdfEditorViewModel.hslToColorInt(hueVal, satVal, lightVal)
                                onColorSelected(updatedColor, true)
                            }
                        )

                        CustomColorChannelSliderRow(
                            label = "Lightness",
                            value = lightVal,
                            valueRange = 0f..100f,
                            testTag = "custom_hsl_lightness_slider",
                            onValueChange = { newL ->
                                lightVal = newL
                                val updatedColor = PdfEditorViewModel.hslToColorInt(hueVal, satVal, lightVal)
                                onColorSelected(updatedColor, true)
                            }
                        )
                    }

                    CustomColorInputMode.RGB -> {
                        CustomColorChannelSliderRow(
                            label = "Red",
                            value = redVal,
                            valueRange = 0f..255f,
                            testTag = "custom_rgb_red_slider",
                            onValueChange = { newR ->
                                onColorSelected(AndroidColor.rgb(newR, greenVal, blueVal), true)
                            }
                        )

                        CustomColorChannelSliderRow(
                            label = "Green",
                            value = greenVal,
                            valueRange = 0f..255f,
                            testTag = "custom_rgb_green_slider",
                            onValueChange = { newG ->
                                onColorSelected(AndroidColor.rgb(redVal, newG, blueVal), true)
                            }
                        )

                        CustomColorChannelSliderRow(
                            label = "Blue",
                            value = blueVal,
                            valueRange = 0f..255f,
                            testTag = "custom_rgb_blue_slider",
                            onValueChange = { newB ->
                                onColorSelected(AndroidColor.rgb(redVal, greenVal, newB), true)
                            }
                        )
                    }

                    CustomColorInputMode.HEX -> {
                        OutlinedTextField(
                            value = hexInput,
                            onValueChange = { value ->
                                hexInput = value
                                val cleaned = value.trim()
                                if (cleaned.matches(Regex("^#?[0-9A-Fa-f]{6}$"))) {
                                    val normalizedHex = if (cleaned.startsWith("#")) cleaned else "#$cleaned"
                                    runCatching {
                                        onColorSelected(AndroidColor.parseColor(normalizedHex), true)
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = "Hex (#RRGGBB)",
                                    color = TextColourInactiveTabGray
                                )
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                color = Color.White
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TextColourAccentRed,
                                unfocusedBorderColor = TextColourInactiveTabGray.copy(alpha = 0.5f),
                                cursorColor = TextColourAccentRed
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("custom_hex_color_input")
                        )

                        CustomColorChannelSliderRow(
                            label = "Hue",
                            value = hueVal,
                            valueRange = 0f..360f,
                            testTag = "custom_hex_hue_slider",
                            onValueChange = { newH ->
                                hueVal = newH
                                val updatedColor = PdfEditorViewModel.hslToColorInt(hueVal, satVal, lightVal)
                                onColorSelected(updatedColor, true)
                            }
                        )

                        CustomColorChannelSliderRow(
                            label = "Saturation",
                            value = satVal,
                            valueRange = 0f..100f,
                            testTag = "custom_hex_saturation_slider",
                            onValueChange = { newS ->
                                satVal = newS
                                val updatedColor = PdfEditorViewModel.hslToColorInt(hueVal, satVal, lightVal)
                                onColorSelected(updatedColor, true)
                            }
                        )

                        CustomColorChannelSliderRow(
                            label = "Lightness",
                            value = lightVal,
                            valueRange = 0f..100f,
                            testTag = "custom_hex_lightness_slider",
                            onValueChange = { newL ->
                                lightVal = newL
                                val updatedColor = PdfEditorViewModel.hslToColorInt(hueVal, satVal, lightVal)
                                onColorSelected(updatedColor, true)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Radio Selector Options for Color Models: Hex, HSL (default selected), and RGB
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomColorRadioOption(
                        label = "Hex",
                        selected = customColorInputMode == CustomColorInputMode.HEX,
                        onClick = { onCustomColorModeSelected(CustomColorInputMode.HEX) },
                        testTag = "custom_mode_radio_hex"
                    )

                    CustomColorRadioOption(
                        label = "HSL",
                        selected = customColorInputMode == CustomColorInputMode.HSL,
                        onClick = { onCustomColorModeSelected(CustomColorInputMode.HSL) },
                        testTag = "custom_mode_radio_hsl"
                    )

                    CustomColorRadioOption(
                        label = "RGB",
                        selected = customColorInputMode == CustomColorInputMode.RGB,
                        onClick = { onCustomColorModeSelected(CustomColorInputMode.RGB) },
                        testTag = "custom_mode_radio_rgb"
                    )
                }
            }
        }
    }
}

/**
 * Rounded-square swatch (`RoundedCornerShape(6.dp)`) used in `Recently Used`,
 * `Default Colour Palette`, and `Colour Variations`.
 * - Renders a white & gray checkerboard pattern when [colorInt] is `null`.
 * - Renders a circular border + checkmark (`✓`) icon when [isSelected] is `true`.
 */
@Composable
fun TextColorRoundedSquareSwatch(
    colorInt: Int?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .then(
                if (colorInt != null) {
                    Modifier.background(Color(colorInt))
                } else {
                    Modifier.background(Color.White)
                }
            )
            .border(
                width = 0.8.dp,
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (colorInt == null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellPx = 5.dp.toPx()
                val cols = (size.width / cellPx).toInt() + 1
                val rows = (size.height / cellPx).toInt() + 1
                val gray = Color(0xFFD0D0D0)
                for (r in 0..rows) {
                    for (c in 0..cols) {
                        if ((r + c) % 2 == 0) {
                            drawRect(
                                color = gray,
                                topLeft = Offset(c * cellPx, r * cellPx),
                                size = Size(cellPx, cellPx)
                            )
                        }
                    }
                }
            }
        }

        if (isSelected) {
            val checkTint = if (colorInt != null) {
                val luminance = (0.299 * AndroidColor.red(colorInt) +
                    0.587 * AndroidColor.green(colorInt) +
                    0.114 * AndroidColor.blue(colorInt)) / 255.0
                if (luminance > 0.72) Color(0xFF141414) else Color.White
            } else {
                Color(0xFF141414)
            }

            Box(
                modifier = Modifier
                    .size(22.dp)
                    .border(1.8.dp, checkTint, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected Colour",
                    tint = checkTint,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Interactive Slider Row in the `CUSTOM` tab (`Hue`, `Saturation`, `Lightness`, or `Red`, `Green`, `Blue`)
 * featuring a coral-red (`#F26B5B`) slider thumb/track and an interactive underlined numeric value
 * text field on the right.
 */
@Composable
fun CustomColorChannelSliderRow(
    label: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    testTag: String,
    onValueChange: (Int) -> Unit
) {
    val minInt = valueRange.start.toInt()
    val maxInt = valueRange.endInclusive.toInt()
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = Color.White
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Slider(
                value = value.toFloat().coerceIn(valueRange.start, valueRange.endInclusive),
                onValueChange = {
                    val coerced = it.roundToInt().coerceIn(minInt, maxInt)
                    textValue = coerced.toString()
                    onValueChange(coerced)
                },
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    thumbColor = TextColourAccentRed,
                    activeTrackColor = TextColourAccentRed,
                    inactiveTrackColor = TextColourSliderTrackBlack
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag(testTag)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(48.dp)
            ) {
                BasicTextField(
                    value = textValue,
                    onValueChange = { input ->
                        val digitsOnly = input.filter { it.isDigit() }.take(3)
                        textValue = digitsOnly
                        val parsed = digitsOnly.toIntOrNull()
                        if (parsed != null) {
                            onValueChange(parsed.coerceIn(minInt, maxInt))
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 17.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    ),
                    cursorBrush = SolidColor(TextColourAccentRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${testTag}_input")
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(TextColourSliderTrackBlack)
                )
            }
        }
    }
}

/**
 * Radio button option (`Hex`, `HSL`, `RGB`) at the bottom of the `CUSTOM` tab
 * with a coral-red (`#F26B5B`) ring and inner dot when selected.
 */
@Composable
fun CustomColorRadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .border(
                    width = 2.dp,
                    color = if (selected) TextColourAccentRed else Color(0xFF9E9E9E),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(TextColourAccentRed)
                )
            }
        }

        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.5.sp,
            color = Color.White
        )
    }
}
