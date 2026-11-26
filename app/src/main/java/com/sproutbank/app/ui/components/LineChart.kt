package com.sproutbank.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

data class Series(val label: String, val points: List<Float>, val color: Color)

@Composable
fun LineChart(
    series: List<Series>,
    modifier: Modifier = Modifier,
    reveal: Boolean = true,
) {
    val anim = if (reveal) {
        val a = remember { androidx.compose.animation.core.Animatable(0f) }
        LaunchedEffect(series) { a.snapTo(0f); a.animateTo(1f, androidx.compose.animation.core.tween(800)) }
        a.value
    } else 1f

    Canvas(modifier = modifier.fillMaxWidth().height(200.dp).padding(8.dp)) {
        val all = series.flatMap { it.points }
        if (all.size < 2) return@Canvas
        val minV = all.min(); val maxV = all.max()
        val span = (maxV - minV).takeIf { it > 0f } ?: 1f
        val w = size.width; val h = size.height

        fun pt(i: Int, v: Float) = Offset(
            x = w * i.toFloat() / (all.size - 1).coerceAtLeast(1),
            y = h - ((v - minV) / span) * h
        )

        series.forEach { s ->
            if (s.points.size < 2) return@forEach
            val path = Path()
            s.points.forEachIndexed { i, v ->
                val p = pt(i, v)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            // reveal: clip x proportion
            val revealWidth = w * anim
            clipRect(0f, 0f, revealWidth, h) {
                drawPath(path, s.color, style = Stroke(width = 6f, cap = StrokeCap.Round))
            }
        }
    }
}

private inline fun androidx.compose.ui.graphics.drawscope.DrawScope.clipRect(
    l: Float, t: Float, r: Float, b: Float, block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
) { androidx.compose.ui.graphics.drawscope.clipRect(l, t, r, b) { block() } }
