package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Custom state holder for Fiori Status Colors
@Stable
class FioriStatusColors(
    val positive: Color,
    val positiveBg: Color,
    val critical: Color,
    val criticalBg: Color,
    val negative: Color,
    val negativeBg: Color,
    val informative: Color,
    val informativeBg: Color,
    val neutral: Color,
    val neutralBg: Color
)

private val LightFioriStatusColors = FioriStatusColors(
    positive = FioriPositiveLight,
    positiveBg = FioriPositiveBgLight,
    critical = FioriCriticalLight,
    criticalBg = FioriCriticalBgLight,
    negative = FioriNegativeLight,
    negativeBg = FioriNegativeBgLight,
    informative = FioriInformativeLight,
    informativeBg = FioriInformativeBgLight,
    neutral = FioriNeutralLight,
    neutralBg = FioriNeutralBgLight
)

private val DarkFioriStatusColors = FioriStatusColors(
    positive = FioriPositiveDark,
    positiveBg = FioriPositiveBgDark,
    critical = FioriCriticalDark,
    criticalBg = FioriCriticalBgDark,
    negative = FioriNegativeDark,
    negativeBg = FioriNegativeBgDark,
    informative = FioriInformativeDark,
    informativeBg = FioriInformativeBgDark,
    neutral = FioriNeutralDark,
    neutralBg = FioriNeutralBgDark
)

val LocalFioriStatusColors = staticCompositionLocalOf { LightFioriStatusColors }

object FioriTheme {
    val statusColors: FioriStatusColors
        @Composable
        get() = LocalFioriStatusColors.current
}

private val LightColorScheme = lightColorScheme(
    primary = FioriBlueLight,
    onPrimary = Color.White,
    secondary = FioriSlateLight,
    onSecondary = Color.White,
    tertiary = FioriTealLight,
    onTertiary = Color.White,
    background = FioriBackgroundLight,
    surface = FioriSurfaceLight,
    onBackground = FioriOnBackgroundLight,
    onSurface = FioriOnSurfaceLight,
    outline = FioriOutlineLight,
    surfaceVariant = Color(0xFFE5E9EC)
)

private val DarkColorScheme = darkColorScheme(
    primary = FioriBlueDark,
    onPrimary = Color(0xFF12171E),
    secondary = FioriSlateDark,
    onSecondary = Color(0xFF12171E),
    tertiary = FioriTealDark,
    onTertiary = Color(0xFF12171E),
    background = FioriBackgroundDark,
    surface = FioriSurfaceDark,
    onBackground = FioriOnBackgroundDark,
    onSurface = FioriOnSurfaceDark,
    outline = FioriOutlineDark,
    surfaceVariant = Color(0xFF262E3A)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val statusColors = if (darkTheme) DarkFioriStatusColors else LightFioriStatusColors

    CompositionLocalProvider(
        LocalFioriStatusColors provides statusColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
