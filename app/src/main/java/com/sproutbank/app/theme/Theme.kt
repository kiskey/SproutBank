package com.sproutbank.app.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.sproutbank.app.vm.SproutViewModel

val LocalStorybookPalette = compositionLocalOf<StorybookPalette> {
    CalmTheme.MEADOW_MINT.storybook(false)
}

@Composable
fun SproutTheme(vm: SproutViewModel, content: @Composable () -> Unit) {
    val themeId by vm.themeId.collectAsState()
    val reduced by vm.reducedAnimations.collectAsState()
    val dark = isSystemInDarkTheme()
    val target = CalmTheme.fromId(themeId).scheme(dark)
    val duration = if (reduced) 0 else 360

    // animateColorAsState is @Composable, so this helper must be too.
    @Composable
    fun anim(c: Color): Color {
        @Suppress("NAME_SHADOWING")
        val s by animateColorAsState(c, tween(duration), label = "palette")
        return s
    }

    val scheme = target.copy(
        primary = anim(target.primary),
        background = anim(target.background),
        surface = anim(target.surface),
        surfaceVariant = anim(target.surfaceVariant),
        secondary = anim(target.secondary),
        primaryContainer = anim(target.primaryContainer),
    )
    val storybook = CalmTheme.fromId(themeId).storybook(dark)
    CompositionLocalProvider(LocalStorybookPalette provides storybook) {
        MaterialTheme(colorScheme = scheme, typography = SproutTypography, content = content)
    }
}
