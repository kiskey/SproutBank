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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner

enum class SproutMood { HAPPY, CELEBRATING, THINKING, SLEEPING, GROWING, ENCOURAGING, SURPRISED }

@Composable
fun Sprout(
    mood: SproutMood = SproutMood.HAPPY,
    size: Dp = 96.dp,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    reduced: Boolean = false,
) {
    // Issue 6 (battery): the bounce is an infinite animation. It only runs
    // while the app is at least STARTED (foreground) — the moment the app is
    // backgrounded the transition stops driving frames. Decorative moods
    // (SLEEPING = idle state) are drawn static.
    val lifecycleOwner = LocalLifecycleOwner.current
    var isStarted by remember { mutableStateOf(false) }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { owner, _ ->
            isStarted = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        isStarted = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val wantsBounce = animate && !reduced && mood != SproutMood.SLEEPING && isStarted
    val bounce = if (wantsBounce) {
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
        drawLeaf3D(cx + s * 0.15f, cy - s * 0.34f, s * 0.15f, s * 0.10f, 22f)

        // body (3D radial gradient)
        val bodyR = s * 0.24f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFA7F3D0), Color(0xFF34D399), Color(0xFF059669)),
                center = Offset(cx - bodyR * 0.4f, cy - bodyR * 0.3f),
                radius = bodyR * 1.8f,
            ),
            radius = bodyR,
            center = Offset(cx, cy + s * 0.05f),
        )

        // cheeks
        drawCircle(Color(0x55FF8DA4), bodyR * 0.28f, Offset(cx - bodyR * 0.75f, cy + s * 0.06f))
        drawCircle(Color(0x55FF8DA4), bodyR * 0.28f, Offset(cx + bodyR * 0.75f, cy + s * 0.06f))

        // eyes
        when (mood) {
            SproutMood.SLEEPING -> {
                drawLine(Color(0xFF065F46), Offset(cx - bodyR * 0.5f, cy + s * 0.02f), Offset(cx - bodyR * 0.2f, cy + s * 0.02f), strokeWidth = s * 0.02f)
                drawLine(Color(0xFF065F46), Offset(cx + bodyR * 0.2f, cy + s * 0.02f), Offset(cx + bodyR * 0.5f, cy + s * 0.02f), strokeWidth = s * 0.02f)
            }
            SproutMood.CELEBRATING, SproutMood.SURPRISED -> {
                drawCircle(Color(0xFF065F46), bodyR * 0.22f, Offset(cx - bodyR * 0.45f, cy + s * 0.02f))
                drawCircle(Color(0xFF065F46), bodyR * 0.22f, Offset(cx + bodyR * 0.45f, cy + s * 0.02f))
                drawCircle(Color.White, bodyR * 0.09f, Offset(cx - bodyR * 0.38f, cy))
                drawCircle(Color.White, bodyR * 0.09f, Offset(cx + bodyR * 0.52f, cy))
            }
            else -> {
                drawCircle(Color(0xFF065F46), bodyR * 0.18f, Offset(cx - bodyR * 0.42f, cy + s * 0.02f))
                drawCircle(Color(0xFF065F46), bodyR * 0.18f, Offset(cx + bodyR * 0.42f, cy + s * 0.02f))
                drawCircle(Color.White, bodyR * 0.06f, Offset(cx - bodyR * 0.35f, cy))
                drawCircle(Color.White, bodyR * 0.06f, Offset(cx + bodyR * 0.49f, cy))
            }
        }

        // mouth
        when (mood) {
            SproutMood.SLEEPING -> drawArc(
                color = Color(0xFF065F46),
                startAngle = 200f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(cx - bodyR * 0.22f, cy + bodyR * 0.25f),
                size = Size(bodyR * 0.44f, bodyR * 0.20f),
                style = Stroke(width = s * 0.014f),
            )
            SproutMood.THINKING -> drawLine(
                color = Color(0xFF065F46),
                start = Offset(cx - bodyR * 0.22f, cy + bodyR * 0.42f),
                end = Offset(cx + bodyR * 0.22f, cy + bodyR * 0.42f),
                strokeWidth = s * 0.014f,
            )
            else -> drawArc(
                color = Color(0xFF065F46),
                startAngle = 20f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(cx - bodyR * 0.35f, cy + bodyR * 0.22f),
                size = Size(bodyR * 0.70f, bodyR * 0.32f),
                style = Stroke(width = s * 0.016f),
            )
        }

        // celebrate sparkles
        if (mood == SproutMood.CELEBRATING) {
            listOf(
                Offset(cx - s * 0.36f, cy - s * 0.28f),
                Offset(cx + s * 0.34f, cy - s * 0.20f),
                Offset(cx - s * 0.30f, cy + s * 0.30f),
                Offset(cx + s * 0.36f, cy + s * 0.32f),
            ).forEach { sparkle(it, s * 0.035f) }
        }
    }
}

@Composable
fun SproutInline(size: Dp = 32.dp, modifier: Modifier = Modifier) {
    Sprout(mood = SproutMood.HAPPY, size = size, modifier = modifier)
}

private fun DrawScope.drawLeaf3D(cx: Float, cy: Float, rx: Float, ry: Float, rotationDeg: Float) {
    rotate(rotationDeg, pivot = Offset(cx, cy)) {
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF6EE7B7), Color(0xFF10B981), Color(0xFF047857)),
                center = Offset(cx - rx * 0.3f, cy - ry * 0.3f),
                radius = rx * 1.7f,
            ),
            topLeft = Offset(cx - rx, cy - ry),
            size = Size(rx * 2f, ry * 2f),
        )
        drawLine(
            color = Color(0xFF065F46),
            start = Offset(cx - rx * 0.7f, cy),
            end = Offset(cx + rx * 0.7f, cy),
            strokeWidth = ry * 0.18f,
        )
    }
}

private fun DrawScope.sparkle(at: Offset, r: Float) {
    val path = Path().apply {
        moveTo(at.x, at.y - r * 1.8f)
        quadraticBezierTo(at.x + r * 0.3f, at.y - r * 0.3f, at.x + r * 1.8f, at.y)
        quadraticBezierTo(at.x + r * 0.3f, at.y + r * 0.3f, at.x, at.y + r * 1.8f)
        quadraticBezierTo(at.x - r * 0.3f, at.y + r * 0.3f, at.x - r * 1.8f, at.y)
        quadraticBezierTo(at.x - r * 0.3f, at.y - r * 0.3f, at.x, at.y - r * 1.8f)
        close()
    }
    drawPath(path, Color.White)
}




