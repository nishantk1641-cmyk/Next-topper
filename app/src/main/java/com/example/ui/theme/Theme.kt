package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = RoyalBlueDark,
    onPrimary = OnRoyalBlueDark,
    primaryContainer = RoyalBlueContainerDark,
    onPrimaryContainer = RoyalBlueDark,
    secondary = TrophyAmberDark,
    secondaryContainer = TrophyAmberContainerDark,
    tertiary = EmeraldDark,
    tertiaryContainer = EmeraldContainerDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    outline = OutlineDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlueLight,
    onPrimary = OnRoyalBlueLight,
    primaryContainer = RoyalBlueContainerLight,
    onPrimaryContainer = RoyalBlueLight,
    secondary = TrophyAmberLight,
    secondaryContainer = TrophyAmberContainerLight,
    tertiary = EmeraldLight,
    tertiaryContainer = EmeraldContainerLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    outline = OutlineLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight
)

@Composable
fun NextTopperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    NextTopperTheme(darkTheme = darkTheme, content = content)
}

