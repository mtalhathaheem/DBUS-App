package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.ThemeMode
import com.example.model.ThemePalette

private fun buildColorScheme(palette: ThemePalette, isDark: Boolean): ColorScheme {
    return when (palette) {
        ThemePalette.ClassicNavy -> if (isDark) {
            darkColorScheme(
                primary = ClassicNavyPrimaryDark,
                onPrimary = ClassicNavyOnPrimaryDark,
                primaryContainer = ClassicNavyPrimaryContainerDark,
                onPrimaryContainer = ClassicNavyOnPrimaryContainerDark,
                secondary = ClassicNavySecondary,
                onSecondary = Color.White,
                tertiary = ClassicNavyTertiary,
                background = ClassicNavyBackgroundDark,
                surface = ClassicNavySurfaceDark,
                onBackground = ClassicNavyOnSurfaceDark,
                onSurface = ClassicNavyOnSurfaceDark,
                surfaceVariant = Color(0xFF1E2D3E),
                onSurfaceVariant = Color(0xFFC4D1DE)
            )
        } else {
            lightColorScheme(
                primary = ClassicNavyPrimary,
                onPrimary = ClassicNavyOnPrimary,
                primaryContainer = ClassicNavyPrimaryContainer,
                onPrimaryContainer = ClassicNavyOnPrimaryContainer,
                secondary = ClassicNavySecondary,
                onSecondary = ClassicNavyOnSecondary,
                tertiary = ClassicNavyTertiary,
                background = ClassicNavyBackgroundLight,
                surface = ClassicNavySurfaceLight,
                onBackground = ClassicNavyOnSurfaceLight,
                onSurface = ClassicNavyOnSurfaceLight,
                surfaceVariant = Color(0xFFEEF2F6),
                onSurfaceVariant = Color(0xFF475569)
            )
        }

        ThemePalette.PakistanGreen -> if (isDark) {
            darkColorScheme(
                primary = PakGreenPrimaryDark,
                onPrimary = PakGreenOnPrimaryDark,
                primaryContainer = PakGreenPrimaryContainerDark,
                onPrimaryContainer = PakGreenOnPrimaryContainerDark,
                secondary = PakGreenSecondary,
                onSecondary = Color.White,
                tertiary = PakGreenTertiary,
                background = PakGreenBackgroundDark,
                surface = PakGreenSurfaceDark,
                onBackground = PakGreenOnSurfaceDark,
                onSurface = PakGreenOnSurfaceDark,
                surfaceVariant = Color(0xFF1B3124),
                onSurfaceVariant = Color(0xFFC0D8C8)
            )
        } else {
            lightColorScheme(
                primary = PakGreenPrimary,
                onPrimary = PakGreenOnPrimary,
                primaryContainer = PakGreenPrimaryContainer,
                onPrimaryContainer = PakGreenOnPrimaryContainer,
                secondary = PakGreenSecondary,
                onSecondary = PakGreenOnSecondary,
                tertiary = PakGreenTertiary,
                background = PakGreenBackgroundLight,
                surface = PakGreenSurfaceLight,
                onBackground = PakGreenOnSurfaceLight,
                onSurface = PakGreenOnSurfaceLight,
                surfaceVariant = Color(0xFFE9F3EC),
                onSurfaceVariant = Color(0xFF385242)
            )
        }

        ThemePalette.RoyalBlue -> if (isDark) {
            darkColorScheme(
                primary = RoyalBluePrimaryDark,
                onPrimary = RoyalBlueOnPrimaryDark,
                primaryContainer = RoyalBluePrimaryContainerDark,
                onPrimaryContainer = RoyalBlueOnPrimaryContainerDark,
                secondary = RoyalBlueSecondary,
                onSecondary = Color.White,
                tertiary = RoyalBlueTertiary,
                background = RoyalBlueBackgroundDark,
                surface = RoyalBlueSurfaceDark,
                onBackground = RoyalBlueOnSurfaceDark,
                onSurface = RoyalBlueOnSurfaceDark,
                surfaceVariant = Color(0xFF182942),
                onSurfaceVariant = Color(0xFFC3D2E7)
            )
        } else {
            lightColorScheme(
                primary = RoyalBluePrimary,
                onPrimary = RoyalBlueOnPrimary,
                primaryContainer = RoyalBluePrimaryContainer,
                onPrimaryContainer = RoyalBlueOnPrimaryContainer,
                secondary = RoyalBlueSecondary,
                onSecondary = RoyalBlueOnSecondary,
                tertiary = RoyalBlueTertiary,
                background = RoyalBlueBackgroundLight,
                surface = RoyalBlueSurfaceLight,
                onBackground = RoyalBlueOnSurfaceLight,
                onSurface = RoyalBlueOnSurfaceLight,
                surfaceVariant = Color(0xFFEDF2F9),
                onSurfaceVariant = Color(0xFF3E4F69)
            )
        }

        ThemePalette.Teal -> if (isDark) {
            darkColorScheme(
                primary = TealPrimaryDark,
                onPrimary = TealOnPrimaryDark,
                primaryContainer = TealPrimaryContainerDark,
                onPrimaryContainer = TealOnPrimaryContainerDark,
                secondary = TealSecondary,
                onSecondary = Color.White,
                tertiary = TealTertiary,
                background = TealBackgroundDark,
                surface = TealSurfaceDark,
                onBackground = TealOnSurfaceDark,
                onSurface = TealOnSurfaceDark,
                surfaceVariant = Color(0xFF152D30),
                onSurfaceVariant = Color(0xFFBEDBE0)
            )
        } else {
            lightColorScheme(
                primary = TealPrimary,
                onPrimary = TealOnPrimary,
                primaryContainer = TealPrimaryContainer,
                onPrimaryContainer = TealOnPrimaryContainer,
                secondary = TealSecondary,
                onSecondary = TealOnSecondary,
                tertiary = TealTertiary,
                background = TealBackgroundLight,
                surface = TealSurfaceLight,
                onBackground = TealOnSurfaceLight,
                onSurface = TealOnSurfaceLight,
                surfaceVariant = Color(0xFFE5F1F2),
                onSurfaceVariant = Color(0xFF3A595D)
            )
        }

        ThemePalette.MaroonAndGold -> if (isDark) {
            darkColorScheme(
                primary = MaroonPrimaryDark,
                onPrimary = MaroonOnPrimaryDark,
                primaryContainer = MaroonPrimaryContainerDark,
                onPrimaryContainer = MaroonOnPrimaryContainerDark,
                secondary = MaroonSecondary,
                onSecondary = Color.White,
                tertiary = MaroonTertiary,
                background = MaroonBackgroundDark,
                surface = MaroonSurfaceDark,
                onBackground = MaroonOnSurfaceDark,
                onSurface = MaroonOnSurfaceDark,
                surfaceVariant = Color(0xFF381D1E),
                onSurfaceVariant = Color(0xFFE2C5C6)
            )
        } else {
            lightColorScheme(
                primary = MaroonPrimary,
                onPrimary = MaroonOnPrimary,
                primaryContainer = MaroonPrimaryContainer,
                onPrimaryContainer = MaroonOnPrimaryContainer,
                secondary = MaroonSecondary,
                onSecondary = MaroonOnSecondary,
                tertiary = MaroonTertiary,
                background = MaroonBackgroundLight,
                surface = MaroonSurfaceLight,
                onBackground = MaroonOnSurfaceLight,
                onSurface = MaroonOnSurfaceLight,
                surfaceVariant = Color(0xFFF7EBEB),
                onSurfaceVariant = Color(0xFF633D3E)
            )
        }
    }
}

@Composable
fun DBusTheme(
    mode: ThemeMode = ThemeMode.Auto,
    palette: ThemePalette = ThemePalette.ClassicNavy,
    textScale: com.example.model.TextScale = com.example.model.TextScale.Medium,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (mode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.Auto -> systemDark
    }

    val colorScheme = buildColorScheme(palette, isDark)
    val currentDensity = androidx.compose.ui.platform.LocalDensity.current
    val customDensity = androidx.compose.ui.unit.Density(
        density = currentDensity.density,
        fontScale = currentDensity.fontScale * textScale.scale
    )

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides customDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
