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

    // ─────────────────────────────────────────────────────────────────
    CalmTheme.MEADOW_MINT -> if (dark) darkColorScheme(
        primary = Color(0xFF5EEAD4), onPrimary = Color(0xFF00201F),
        primaryContainer = Color(0xFF115E59), onPrimaryContainer = Color(0xFFCCFBF1),
        inversePrimary = Color(0xFF0D9488),
        secondary = Color(0xFFF3B562), onSecondary = Color(0xFF3B2A00),
        secondaryContainer = Color(0xFF5C3F00), onSecondaryContainer = Color(0xFFFFE9B5),
        tertiary = Color(0xFFB69CFF), onTertiary = Color(0xFF241A4D),
        tertiaryContainer = Color(0xFF3E2F78), onTertiaryContainer = Color(0xFFE9DDFF),
        background = Color(0xFF081213), onBackground = Color(0xFFE6F4F1),
        surface = Color(0xFF1E3336), onSurface = Color(0xFFE6F4F1),
        surfaceVariant = Color(0xFF2A4649), onSurfaceVariant = Color(0xFFA8C7C1),
        surfaceTint = Color(0xFF5EEAD4),
        inverseSurface = Color(0xFFE6F4F1), inverseOnSurface = Color(0xFF112222),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF7FA09A), outlineVariant = Color(0xFF3A5C58),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF325155),
        surfaceDim = Color(0xFF0E1E1F),
        surfaceContainerLowest = Color(0xFF0A1617),
        surfaceContainerLow = Color(0xFF1E3336),
        surfaceContainer = Color(0xFF264043),
        surfaceContainerHigh = Color(0xFF2E4A4D),
        surfaceContainerHighest = Color(0xFF365559),
    ) else lightColorScheme(
        primary = Color(0xFF0D9488), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDDF5EC), onPrimaryContainer = Color(0xFF0F2A26),
        inversePrimary = Color(0xFF5EEAD4),
        secondary = Color(0xFFD98A1A), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFFFE9B5), onSecondaryContainer = Color(0xFF4A2D00),
        tertiary = Color(0xFF7C5DDA), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFE9DDFF), onTertiaryContainer = Color(0xFF241A4D),
        background = Color(0xFFF4FAF8), onBackground = Color(0xFF243B35),
        surface = Color(0xFFFFFFFF), onSurface = Color(0xFF243B35),
        surfaceVariant = Color(0xFFDDF5EC), onSurfaceVariant = Color(0xFF3E5A55),
        surfaceTint = Color(0xFF0D9488),
        inverseSurface = Color(0xFF243B35), inverseOnSurface = Color(0xFFE6F4F1),
        error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
        outline = Color(0xFF7A9590), outlineVariant = Color(0xFFBFDDD6),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFD5E5E0),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF8FCFA),
        surfaceContainer = Color(0xFFEEF7F3),
        surfaceContainerHigh = Color(0xFFE3F1EC),
        surfaceContainerHighest = Color(0xFFD8EBE5),
    )

    // ─────────────────────────────────────────────────────────────────
    CalmTheme.SUNNY_OAT -> if (dark) darkColorScheme(
        primary = Color(0xFFF8A38C), onPrimary = Color(0xFF3A0E04),
        primaryContainer = Color(0xFF6E2C1C), onPrimaryContainer = Color(0xFFFFD8CC),
        inversePrimary = Color(0xFFC8644B),
        secondary = Color(0xFF8FCFA2), onSecondary = Color(0xFF0F2A17),
        secondaryContainer = Color(0xFF2E5138), onSecondaryContainer = Color(0xFFCBEBCF),
        tertiary = Color(0xFFD7B0FF), onTertiary = Color(0xFF2A1145),
        tertiaryContainer = Color(0xFF4A2B6D), onTertiaryContainer = Color(0xFFEDD9FF),
        background = Color(0xFF140E0A), onBackground = Color(0xFFF6E9DF),
        surface = Color(0xFF38281F), onSurface = Color(0xFFF6E9DF),
        surfaceVariant = Color(0xFF4A362A), onSurfaceVariant = Color(0xFFC8A896),
        surfaceTint = Color(0xFFF8A38C),
        inverseSurface = Color(0xFFF6E9DF), inverseOnSurface = Color(0xFF241810),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFFB59280), outlineVariant = Color(0xFF5A4134),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF4A362A),
        surfaceDim = Color(0xFF231710),
        surfaceContainerLowest = Color(0xFF150D08),
        surfaceContainerLow = Color(0xFF38281F),
        surfaceContainer = Color(0xFF433024),
        surfaceContainerHigh = Color(0xFF4F3A2A),
        surfaceContainerHighest = Color(0xFF5A4432),
    ) else lightColorScheme(
        primary = Color(0xFFC8644B), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFBE4D7), onPrimaryContainer = Color(0xFF3A0E04),
        inversePrimary = Color(0xFFF8A38C),
        secondary = Color(0xFF3F7D58), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCBEBCF), onSecondaryContainer = Color(0xFF0F2A17),
        tertiary = Color(0xFF7C3AED), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFEDD9FF), onTertiaryContainer = Color(0xFF2A1145),
        background = Color(0xFFFDF7F0), onBackground = Color(0xFF2A1E15),
        surface = Color(0xFFFFFFFF), onSurface = Color(0xFF2A1E15),
        surfaceVariant = Color(0xFFFBE4D7), onSurfaceVariant = Color(0xFF6B4A3A),
        surfaceTint = Color(0xFFC8644B),
        inverseSurface = Color(0xFF2A1E15), inverseOnSurface = Color(0xFFF6E9DF),
        error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
        outline = Color(0xFF9B7A66), outlineVariant = Color(0xFFE5CBBC),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFE6D5C5),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFEF9F3),
        surfaceContainer = Color(0xFFF8ECDF),
        surfaceContainerHigh = Color(0xFFF2E2D1),
        surfaceContainerHighest = Color(0xFFEBD8C3),
    )

    // ─────────────────────────────────────────────────────────────────
    CalmTheme.LAVENDER_SEAFOAM -> if (dark) darkColorScheme(
        primary = Color(0xFFC4B5FD), onPrimary = Color(0xFF1E1439),
        primaryContainer = Color(0xFF3F318C), onPrimaryContainer = Color(0xFFE4DEFA),
        inversePrimary = Color(0xFF6D5BD0),
        secondary = Color(0xFF5EEAD4), onSecondary = Color(0xFF00201F),
        secondaryContainer = Color(0xFF115E59), onSecondaryContainer = Color(0xFFCCFBF1),
        tertiary = Color(0xFFF8B266), onTertiary = Color(0xFF3D2200),
        tertiaryContainer = Color(0xFF5C3D00), onTertiaryContainer = Color(0xFFFFE0B2),
        background = Color(0xFF0E0A1A), onBackground = Color(0xFFEDE9FE),
        surface = Color(0xFF302848), onSurface = Color(0xFFEDE9FE),
        surfaceVariant = Color(0xFF463C68), onSurfaceVariant = Color(0xFFB6A9D9),
        surfaceTint = Color(0xFFC4B5FD),
        inverseSurface = Color(0xFFEDE9FE), inverseOnSurface = Color(0xFF1F1737),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF9F93C9), outlineVariant = Color(0xFF4E4473),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF443A66),
        surfaceDim = Color(0xFF1A1330),
        surfaceContainerLowest = Color(0xFF0E0A1A),
        surfaceContainerLow = Color(0xFF302848),
        surfaceContainer = Color(0xFF3A3058),
        surfaceContainerHigh = Color(0xFF453A68),
        surfaceContainerHighest = Color(0xFF51447A),
    ) else lightColorScheme(
        primary = Color(0xFF6D5BD0), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFEAE4FC), onPrimaryContainer = Color(0xFF1E1439),
        inversePrimary = Color(0xFFC4B5FD),
        secondary = Color(0xFF0D9488), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCFBF1), onSecondaryContainer = Color(0xFF00201F),
        tertiary = Color(0xFFD97706), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFE0B2), onTertiaryContainer = Color(0xFF3D2200),
        background = Color(0xFFF6F3FE), onBackground = Color(0xFF1E1A2E),
        surface = Color(0xFFFFFFFF), onSurface = Color(0xFF1E1A2E),
        surfaceVariant = Color(0xFFEAE4FC), onSurfaceVariant = Color(0xFF5A4E78),
        surfaceTint = Color(0xFF6D5BD0),
        inverseSurface = Color(0xFF1E1A2E), inverseOnSurface = Color(0xFFEDE9FE),
        error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
        outline = Color(0xFF8B7FB0), outlineVariant = Color(0xFFD4CCEC),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFE2DAF7),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF9F6FF),
        surfaceContainer = Color(0xFFF1EBFF),
        surfaceContainerHigh = Color(0xFFEAE1FF),
        surfaceContainerHighest = Color(0xFFE2D6FF),
    )

    // ─────────────────────────────────────────────────────────────────
    CalmTheme.OCEAN_PEBBLE -> if (dark) darkColorScheme(
        primary = Color(0xFF93C5FD), onPrimary = Color(0xFF0B1E36),
        primaryContainer = Color(0xFF1D4B7D), onPrimaryContainer = Color(0xFFCCE0F5),
        inversePrimary = Color(0xFF2B6CB0),
        secondary = Color(0xFF5EEAD4), onSecondary = Color(0xFF00201F),
        secondaryContainer = Color(0xFF115E59), onSecondaryContainer = Color(0xFFCCFBF1),
        tertiary = Color(0xFFF87171), onTertiary = Color(0xFF3D0505),
        tertiaryContainer = Color(0xFF5C1111), onTertiaryContainer = Color(0xFFFFD5D5),
        background = Color(0xFF071220), onBackground = Color(0xFFDCE7F4),
        surface = Color(0xFF1E2F42), onSurface = Color(0xFFDCE7F4),
        surfaceVariant = Color(0xFF2B4260), onSurfaceVariant = Color(0xFF9FB6CC),
        surfaceTint = Color(0xFF93C5FD),
        inverseSurface = Color(0xFFDCE7F4), inverseOnSurface = Color(0xFF0F1A28),
        error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF7A9BBC), outlineVariant = Color(0xFF2F4A6A),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFF2A3F57),
        surfaceDim = Color(0xFF0E1B2B),
        surfaceContainerLowest = Color(0xFF071220),
        surfaceContainerLow = Color(0xFF1E2F42),
        surfaceContainer = Color(0xFF253B54),
        surfaceContainerHigh = Color(0xFF2E4A68),
        surfaceContainerHighest = Color(0xFF37567A),
    ) else lightColorScheme(
        primary = Color(0xFF2B6CB0), onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFDFEDFC), onPrimaryContainer = Color(0xFF0B1E36),
        inversePrimary = Color(0xFF93C5FD),
        secondary = Color(0xFF0F766E), onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCFBF1), onSecondaryContainer = Color(0xFF00201F),
        tertiary = Color(0xFFE11D48), onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFFFD5D5), onTertiaryContainer = Color(0xFF3D0505),
        background = Color(0xFFF3F7FC), onBackground = Color(0xFF0F1926),
        surface = Color(0xFFFFFFFF), onSurface = Color(0xFF0F1926),
        surfaceVariant = Color(0xFFDFEDFC), onSurfaceVariant = Color(0xFF4C6480),
        surfaceTint = Color(0xFF2B6CB0),
        inverseSurface = Color(0xFF0F1926), inverseOnSurface = Color(0xFFDCE7F4),
        error = Color(0xFFB3261E), onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFF9DEDC), onErrorContainer = Color(0xFF410E0B),
        outline = Color(0xFF7D97B0), outlineVariant = Color(0xFFC9DBED),
        scrim = Color(0xFF000000),
        surfaceBright = Color(0xFFFFFFFF),
        surfaceDim = Color(0xFFDAE4F0),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF7FBFF),
        surfaceContainer = Color(0xFFEEF4FB),
        surfaceContainerHigh = Color(0xFFE6EFF8),
        surfaceContainerHighest = Color(0xFFDDE9F5),
    )
}

/**
 * Environmental pastel colours for the storybook background layer.
 * Light = pastel daylight; Dark = deep slate/ink dark enough to sit clearly
 * BELOW the elevated surfaces so cards always pop.
 */
data class StorybookPalette(
    val skyTop: Color,
    val skyBottom: Color,
    val hillBack: Color,
    val hillFront: Color,
    val cloud: Color,
    val grass: Color,
)

fun CalmTheme.storybook(dark: Boolean): StorybookPalette = when (this) {
    CalmTheme.MEADOW_MINT -> if (dark) StorybookPalette(
        skyTop = Color(0xFF081213), skyBottom = Color(0xFF0B1B1D),
        hillBack = Color(0xFF0E1E20), hillFront = Color(0xFF0A1618),
        cloud = Color(0xFF163236), grass = Color(0xFF2A5A52),
    ) else StorybookPalette(
        skyTop = Color(0xFFDDF5EC), skyBottom = Color(0xFFF4FAF8),
        hillBack = Color(0xFFBFE8D9), hillFront = Color(0xFF9CD3BE),
        cloud = Color(0xFFFFFFFF), grass = Color(0xFF7FC3A8),
    )
    CalmTheme.SUNNY_OAT -> if (dark) StorybookPalette(
        skyTop = Color(0xFF140E0A), skyBottom = Color(0xFF1A130E),
        hillBack = Color(0xFF1F160F), hillFront = Color(0xFF170F09),
        cloud = Color(0xFF3A2A20), grass = Color(0xFF6B4A2F),
    ) else StorybookPalette(
        skyTop = Color(0xFFFBE4D7), skyBottom = Color(0xFFFDF7F0),
        hillBack = Color(0xFFF8D4BC), hillFront = Color(0xFFEDB795),
        cloud = Color(0xFFFFFFFF), grass = Color(0xFFE0A87F),
    )
    CalmTheme.LAVENDER_SEAFOAM -> if (dark) StorybookPalette(
        skyTop = Color(0xFF0E0A1A), skyBottom = Color(0xFF130F22),
        hillBack = Color(0xFF181228), hillFront = Color(0xFF110C1E),
        cloud = Color(0xFF2E2548), grass = Color(0xFF5F4FA6),
    ) else StorybookPalette(
        skyTop = Color(0xFFEAE4FC), skyBottom = Color(0xFFF6F3FE),
        hillBack = Color(0xFFD3C8F5), hillFront = Color(0xFFB6A9E8),
        cloud = Color(0xFFFFFFFF), grass = Color(0xFF9C8FE0),
    )
    CalmTheme.OCEAN_PEBBLE -> if (dark) StorybookPalette(
        skyTop = Color(0xFF071220), skyBottom = Color(0xFF0A1A2A),
        hillBack = Color(0xFF0D1D2C), hillFront = Color(0xFF091622),
        cloud = Color(0xFF1C3550), grass = Color(0xFF3E6189),
    ) else StorybookPalette(
        skyTop = Color(0xFFDFEDFC), skyBottom = Color(0xFFF3F7FC),
        hillBack = Color(0xFFC3DCF5), hillFront = Color(0xFF9FBFE5),
        cloud = Color(0xFFFFFFFF), grass = Color(0xFF7FA1BE),
    )
}




