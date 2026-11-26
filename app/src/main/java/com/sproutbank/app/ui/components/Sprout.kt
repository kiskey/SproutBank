package com.sproutbank.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class SproutMood { HAPPY, CELEBRATING, THINKING, SLEEPING, GROWING, ENCOURAGING, SURPRISED }

/**
 * Custom Sprout mascot: a rounded green sprout with two leaves, expressive eyes,
 * and a friendly smile. Every element is theme-reactive via the `body`/`leaf`
 * colours passed in.
 */
@Composable
fun Sprout(
    mood: SproutMood = SproutMood.HAPPY,
    size: Dp = 96.dp,
    body: Color = Color(0xFF34D399),
    leaf: Color = Color(0xFF10B981),
    accent: Color = Color(0xFF065F46),
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        scale(s / 200f, s / 200f, pivot = Offset(100f, 100f)) {
            // Body
            drawCircle(body, radius = 46f, center = Offset(100f, 118f))
            // Stem
            drawLine(accent, start = Offset(100f, 74f), end = Offset(100f, 44f), strokeWidth = 6f)
            // Left leaf
            val lL = Path().apply {
                moveTo(100f, 62f)
                cubicTo(76f, 62f, 56f, 46f, 56f, 26f)
                cubicTo(78f, 28f, 98f, 42f, 100f, 62f)
                close()
            }
            drawPath(lL, leaf)
            // Right leaf
            val rL = Path().apply {
                moveTo(100f, 70f)
                cubicTo(124f, 70f, 144f, 54f, 144f, 34f)
                cubicTo(122f, 36f, 102f, 50f, 100f, 70f)
                close()
            }
            drawPath(rL, leaf)

            // Cheeks (soft)
            drawCircle(Color(0x33D9426B), 10f, Offset(78f, 122f))
            drawCircle(Color(0x33D9426B), 10f, Offset(122f, 122f))

            // Eyes
            when (mood) {
                SproutMood.SLEEPING -> {
                    drawLine(accent, Offset(76f, 112f), Offset(92f, 112f), strokeWidth = 4f)
                    drawLine(accent, Offset(108f, 112f), Offset(124f, 112f), strokeWidth = 4f)
                }
                SproutMood.CELEBRATING, SproutMood.SURPRISED -> {
                    drawCircle(accent, 9f, Offset(84f, 112f))
                    drawCircle(accent, 9f, Offset(116f, 112f))
                    drawCircle(Color.White, 3f, Offset(87f, 109f))
                    drawCircle(Color.White, 3f, Offset(119f, 109f))
                }
                else -> {
                    drawCircle(accent, 7f, Offset(84f, 112f))
                    drawCircle(accent, 7f, Offset(116f, 112f))
                }
            }

            // Mouth
            when (mood) {
                SproutMood.SLEEPING -> {
                    // tiny snore
                    drawArc(accent, 0f, 180f, false, Offset(92f, 130f), Size(16f, 10f))
                }
                SproutMood.THINKING -> {
                    drawLine(accent, Offset(92f, 132f), Offset(108f, 132f), strokeWidth = 3f)
                }
                SproutMood.ENCOURAGING -> {
                    drawArc(accent, 20f, 140f, false, Offset(88f, 128f), Size(24f, 12f))
                }
                else -> {
                    // happy smile
                    drawArc(accent, 0f, 180f, false, Offset(86f, 124f), Size(28f, 16f))
                }
            }

            // Cheer accents
            if (mood == SproutMood.CELEBRATING || mood == SproutMood.SURPRISED) {
                listOf(Offset(30f, 60f), Offset(170f, 60f), Offset(30f, 140f), Offset(170f, 140f)).forEach {
                    drawCircle(Color(0xFFF2C14E), 4f, it)
                }
            }
        }
    }
}

/**
 * Tiny inline version for headers.
 */
@Composable
fun SproutInline(size: Dp = 32.dp, modifier: Modifier = Modifier) {
    Sprout(mood = SproutMood.HAPPY, size = size, modifier = modifier)
}

/**
 * Draw a simple gold coin at the given offset using the caller's draw scope.
 * Used by the celebration overlay for coin-fly animation.
 */
fun drawCoin(scope: androidx.compose.ui.graphics.drawscope.DrawScope, at: Offset, radius: Float = 12f) {
    scope.drawCircle(Color(0xFFF2C14E), radius, at)
    scope.drawCircle(Color(0xFFC89B2E), radius, at, style = Stroke(width = 3f))
    scope.drawLine(Color(0xFFC89B2E), start = at - Offset(0f, radius * 0.4f), end = at + Offset(0f, radius * 0.4f), strokeWidth = 2f)
}
