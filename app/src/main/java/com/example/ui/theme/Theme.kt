package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val StitchLightColorScheme = lightColorScheme(
    primary = BrandRed,
    onPrimary = Color.White,
    primaryContainer = PillPink,
    onPrimaryContainer = Slate900,
    secondary = PrimaryRed,
    onSecondary = Color.White,
    secondaryContainer = SurfaceContainerHigh,
    onSecondaryContainer = Slate900,
    tertiary = AccentElectricBlue,
    onTertiary = Color.White,
    background = AppBackground,
    onBackground = Slate900,
    surface = SurfaceContainerLowest,
    onSurface = Slate900,
    surfaceVariant = SurfaceContainerLow,
    onSurfaceVariant = Slate700,
    outline = Slate200
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StitchLightColorScheme,
        typography = Typography,
        content = content
    )
}
