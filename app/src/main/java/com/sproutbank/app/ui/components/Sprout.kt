package com.sproutbank.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class SproutMood { HAPPY, CELEBRATING, THINKING, SLEEPING, GROWING, ENCOURAGING, SURPRISED }

@Composable
fun Sprout(
    mood: SproutMood = SproutMood.HAPPY,
    size: Dp = 96.dp,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    reduced: Boolean = false,
) {
    val bounce = if (animate && !reduced) {
        val t = rememberInfiniteTransition(label = "sprout")
        val a by t.animateFloat(
            initialValue = -2f, targetValue = 2f,
            animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
            label = "bounce",
        )
        a
    } else 0f

    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f + bounce

        // shadow under body
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent),
                center = Offset(cx, cy + s * 0.42f),
                radius = s * 0.36f,
            ),
            topLeft = Offset(cx - s * 0.28f, cy + s * 0.34f),
            size = Size(s * 0.56f, s * 0.14f),
        )

        // stem
        drawLine(
            color = Color(0xFF2F8A5E),
            start = Offset(cx, cy - s * 0.05f),
            end = Offset(cx, cy - s * 0.36f),
            strokeWidth = s * 0.035f,
        )

        // leaves
        drawLeaf3D(cx - s * 0.15f, cy - s * 0.32f, s * 0.14f, s * 0.09f, -22f)
        drawLeaf3D(cx + s * 0.15f, cy - s * 0.34f, s * 0.15f
