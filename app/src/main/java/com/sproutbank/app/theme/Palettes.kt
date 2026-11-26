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

fun CalmTheme.scheme(dark: Boolean): ColorScheme = when (this) {
    CalmTheme.MEADOW_MINT -> if (dark) darkColorScheme(
        primary = Color(0xFF5EEAD4), onPrimary = Color(0xFF00201F),
        primaryContainer = Color(0xFF115E59), onPrimaryContainer = Color(0xFFCCFBF1),
        secondary = Color(0xFFF3B562), onSecondary = Color(0xFF3B2A00),
        background = Color(0xFF112224), onBackground = Color(0xFFE6F4F1),
        surface = Color(0xFF182E30), onSurface = Color(0xFFE6F4F1),
        surfaceVariant = Color(0xFF1F3B3D), onSurfaceVariant = Color(0xFF9BB8B3),
    ) else lightColorScheme(
        primary = Color(0xFF0D9488), onPrimary = Color.White,
        primaryContainer = Color(0xFFDDF5EC), onPrimaryContainer = Color(0xFF0F2A26),
        secondary = Color(0xFFD98A1A), onSecondary = Color.White,
        background = Color(0xFFF4FAF8), onBackground = Color(0xFF243B35),
        surface = Color(0xFFFFFFFF), onSurface = Color(0xFF243B35),
        surfaceVariant = Color(0xFFDDF5EC), onSurfaceVariant = Color(0xFF3E5A55),
    )
    CalmTheme.SUNNY_OAT -> if (dark) darkColorScheme(
        primary = Color(0xFFF8A38C), onPrimary = Color(0xFF3A0E04),
        primaryContainer = Color(0xFF6E2C1C), onPrimaryContainer = Color(0xFFFFD8CC),
        secondary = Color(0xFF8FCFA2), onSecondary = Color(0xFF0F2A17),
        background = Color(0xFF241A15), onBackground = Color(0xFFF6E9DF),
        surface = Color(0xFF2D211B), onSurface = Color(0xFFF6E9DF),
        surfaceVariant = Color(0xFF3A2B23), onSurfaceVariant = Color(0xFFC8A896),
    ) else lightColorScheme(
        primary = Color(0xFFC8644B), onPrimary = Color.White,
        primaryContainer = Color(0xFFFBE4D7), onPrimaryContainer = Color(0xFF3A0E04),
        secondary = Color(0xFF3F7D58), onSecondary = Color.White,
        background = Color(0xFFFDF7F0), onBackground = Color(0xFF2A1E15),
        surface = Color(0xFFFFFFFF), onSurface = Color(0xFF2A1E15),
        surfaceVariant = Color(0xFFFBE4D7), onSurfaceVariant = Color(0xFF6B4A3A),
    )
    CalmTheme.LAVENDER_SEAFOAM -> if (dark) darkColorScheme(
        primary = Color(0xFFC4B5FD), onPrimary = Color(0xFF1E1439),
        primaryContainer = Color(0xFF3F318C), onPrimaryContainer = Color(0xFFE4DEFA),
        secondary = Color(0xFF5EEAD4), onSecondary = Color(0xFF00201F),
        background = Color(0xFF1A172B), onBackground = Color(0xFFEDE9FE),
        surface = Color(0xFF221E38), onSurface = Color(0xFFEDE9FE),
        surfaceVariant = Color(0xFF2E2948), onSurfaceVariant = Color(0xFFB6A9D9),
    ) else lightColorScheme(
        primary = Color(0xFF6D5BD0), onPrimary = Color.White,
        primaryContainer = Color(0xFFEAE4FC), onPrimaryContainer = Color(0xFF1E1439),
        secondary = Color(0xFF0D9488), onSecondary = Color.White,
        background = Color(0xFFF6F3FE), onBackground = Color(0xFF1E1A2E),
        surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1E1A2E),
        surfaceVariant = Color(0xFFEAE4FC), onSurfaceVariant = Color(0xFF5A4E78),
    )
    CalmTheme.OCEAN_PEBBLE -> if (dark) darkColorScheme(
        primary = Color(0xFF93C5FD), onPrimary = Color(0xFF0B1E36),
        primaryContainer = Color(0xFF1D4B7D), onPrimaryContainer = Color(0xFFCCE0F5),
        secondary = Color(0xFF5EEAD4), onSecondary = Color(0xFF00201F),
        background = Color(0xFF121E2B), onBackground = Color(0xFFDCE7F4),
        surface = Color(0xFF1A2A3C), onSurface = Color(0xFFDCE7F4),
        surfaceVariant = Color(0xFF24384E), onSurfaceVariant = Color(0xFF7FA1BE),
    ) else lightColorScheme(
        primary = Color(0xFF2B6CB0), onPrimary = Color.White,
        primaryContainer = Color(0xFFDFEDFC), onPrimaryContainer = Color(0xFF0B1E36),
        secondary = Color(0xFF0F766E), onSecondary = Color.White,
        background = Color(0xFFF3F7FC), onBackground = Color(0xFF0F1926),
        surface = Color(0xFFFFFFFF), onSurface = Color(0xFF0F1926),
        surfaceVariant = Color(0xFFDFEDFC), onSurfaceVariant = Color(0xFF4C6480),
    )
}
