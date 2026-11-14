package com.letsdrive.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Cores exatas do Figma Let's Drive
val FigmaDarkBg = Color(0xFF0F0F0F)
val FigmaSheetBg = Color(0xFF242424)
val FigmaMenuBarBg = Color(0xFF3A3A3A)
val FigmaSecondaryButtonBg = Color(0xFF464646)
val FigmaCardOverlay = Color(0xFF262626)

val FigmaTealPrimary = Color(0xFF10CEAB)
val FigmaMintLogo = Color(0xFF0CEAC1)
val FigmaNeonCyan = Color(0xFF38F0FC)
val FigmaOrangeLogo = Color(0xFFFF7E10)

val FigmaTextWhite = Color(0xFFFFFFFF)
val FigmaTextLightGray = Color(0xFFE8E8E8)
val FigmaTextMuted = Color(0xFF989898)
val FigmaTextSubtle = Color(0xFF686767)
val FigmaDangerRed = Color(0xFFF43F5E)

private val LetsDriveFigmaDarkScheme = darkColorScheme(
    primary = FigmaTealPrimary,
    onPrimary = Color.White,
    secondary = FigmaNeonCyan,
    onSecondary = Color(0xFF002022),
    tertiary = FigmaOrangeLogo,
    background = FigmaDarkBg,
    onBackground = FigmaTextWhite,
    surface = FigmaSheetBg,
    onSurface = FigmaTextWhite,
    surfaceVariant = FigmaMenuBarBg,
    onSurfaceVariant = FigmaTextMuted,
    error = FigmaDangerRed
)

@Composable
fun LetsDriveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LetsDriveFigmaDarkScheme,
        content = content
    )
}
