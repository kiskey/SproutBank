package com.sproutbank.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class CalmTheme(val id: String, val label: String) {
    MEADOW_MINT("meadow_mint", "Meadow Mint"),
    SUNNY_OAT("sunny_oat", "Sunny Oat"),
    LAVENDER_SEAFOAM("lavender_seafoam", "Lavender & Seafoam"),
    OCEAN_PEBBLE("ocean_pebble", "Ocean Pebble");

    companion object {
        fun fromId(id: String?) = values().firstOrNull { it.id == id } ?: MEADOW_MINT
    }
}

private fun scheme(
    bg: Color, surface: Color, surfaceVar: Color,
    primary: Color, primaryCont: Color, onPrimaryCont: Color,
    secondary: Color, onBg: Color, onSurfaceVar: Color,
    dark: Boolean,
): ColorScheme = if (dark) darkColorScheme(
    primary = primary, onPrimary = Color(0xFF00201F),
    primaryContainer = primaryCont, onPrimaryContainer = onPrimaryCont,
    secondary = secondary, onSecondary = Color(0xFF0A2A22),
    background = bg, onBackground = onBg,
    surface = surface, onSurface = onBg,
    surfaceVariant = surfaceVar, onSurfaceVariant = onSurfaceVar,
) else lightColorScheme(
    primary = primary, onPrimary = Color.White,
    primaryContainer = primaryCont, onPrimaryContainer = onPrimaryCont,
    secondary = secondary, onSecondary = Color.White,
    background = bg, onBackground = onBg,
    surface = surface, onSurface = onBg,
    surfaceVariant = surfaceVar, onSurfaceVariant = onSurfaceVar,
)

fun CalmTheme.scheme(dark: Boolean): ColorScheme = when (this) {
    CalmTheme.MEADOW_MINT -> scheme(
        bg = Color(if (dark) 0xFF112224 else 0xFFF4FAF8),
        surface = Color(if (dark) 0xFF182E30 else 0xFFFFFFFF),
        surfaceVar = Color(if (dark) 0xFF1F3B3D else 0xFFE7F1EE),
        primary = Color(if (dark) 0xFF5EEAD4 else 0xFF0D9488),
        primaryCont = Color(if (dark) 0xFF115E59 else 0xFFCCFBF1),
        onPrimaryCont = Color(if (dark) 0xFFCCFBF1 else 0xFF115E59),
        secondary = Color(if (dark) 0xFFF3B562 else 0xFFD98A1A),
        onBg = Color(if (dark) 0xFFE6F4F1 else 0xFF0F2A26),
        onSurfaceVar = Color(if (dark) 0xFF9BB8B3 else 0xFF3E5A55),
        dark = dark,
    )
    CalmTheme.SUNNY_OAT -> scheme(
        bg = Color(if (dark) 0xFF241A15 else 0xFFFDF7F0),
        surface = Color(if (dark) 0xFF2D211B else 0xFFFFFFFF),
        surfaceVar = Color(if (dark) 0xFF3A2B23 else 0xFFF7E8DA),
        primary = Color(if (dark) 0xFFF8A38C else 0xFFC8644B),
        primaryCont = Color(if (dark) 0xFF6E2C1C else 0xFFFFD8CC),
        onPrimaryCont = Color(if (dark) 0xFFFFD8CC else 0xFF6E2C1C),
        secondary = Color(if (dark) 0xFF8FCFA2 else 0xFF3F7D58),
        onBg = Color(if (dark) 0xFFF6E9DF else 0xFF2A1E15),
        onSurfaceVar = Color(if (dark) 0xFFC8A896 else 0xFF6B4A3A),
        dark = dark,
    )
    CalmTheme.LAVENDER_SEAFOAM -> scheme(
        bg = Color(if (dark) 0xFF1A172B else 0xFFF6F3FE),
        surface = Color(if (dark) 0xFF221E38 else 0xFFFFFFFF),
        surfaceVar = Color(if (dark) 0xFF2E2948 else 0xFFEBE5FC),
        primary = Color(if (dark) 0xFFC4B5FD else 0xFF6D5BD0),
        primaryCont = Color(if (dark) 0xFF3F318C else 0xFFE4DEFA),
        onPrimaryCont = Color(if (dark) 0xFFE4DEFA else 0xFF3F318C),
        secondary = Color(if (dark) 0xFF5EEAD4 else 0xFF0D9488),
        onBg = Color(if (dark) 0xFFEDE9FE else 0xFF1E1A2E),
        onSurfaceVar = Color(if (dark) 0xFFB6A9D9 else 0xFF5A4E78),
        dark = dark,
    )
    CalmTheme.OCEAN_PEBBLE -> scheme(
        bg = Color(if (dark) 0xFF121E2B else 0xFFF3F7FC),
        surface = Color(if (dark) 0xFF1A2A3C else 0xFFFFFFFF),
        surfaceVar = Color(if (dark) 0xFF24384E else 0xFFE4EEF7),
        primary = Color(if (dark) 0xFF93C5FD else 0xFF2B6CB0),
        primaryCont = Color(if (dark) 0xFF1D4B7D else 0xFFCCE0F5),
        onPrimaryCont = Color(if (dark) 0xFFCCE0F5 else 0xFF1D4B7D),
        secondary = Color(if (dark) 0xFF5EEAD4 else 0xFF0F766E),
        onBg = Color(if (dark) 0xFFDCE7F4 else 0xFF0F1926),
        onSurfaceVar = Color(if (dark) 0xFF7FA1BE else 0xFF4C6480),
        dark = dark,
    )
}
