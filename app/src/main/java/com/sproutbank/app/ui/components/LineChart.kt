package com.sproutbank.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

data class Series(val label: String, val points: List<Float>, val color: Color)

@Composable
fun LineChart(
    series: List<Series>,
    modifier: Modifier = Modifier,
    reveal: Boolean = true,
) {
    val all = remember(series) { series.flatMap { it.points } }
    var scrubIndex by remember { mutableStateOf<Int?>(null) }

    if (all.size < 2) {
        Box(modifier.fillMaxWidth().height(200.dp))
        return
    }

    val anim = if (reveal) {
        val a = remember { androidx.compose.animation.core.Animatable(0f) }
        LaunchedEffect(series) { a.snapTo(0f); a.animateTo(1f, androidx.compose.animation.core.tween(800)) }
        a.value
    } else 1f

    Box(modifier.fillMaxWidth().height(220.dp)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .padding(8.dp)
                .pointerInput(series) {
                    detectDragGestures(
                        onDragStart = { offset -> scrubIndex = idx(offset.x, size.width, all.size) },
                        onDrag = { change, _ -> scrubIndex = idx(change.position.x, size.width, all.size) },
                        onDragEnd = { scrubIndex = null },
                        onDragCancel = { scrubIndex = null },
                    )
                }
                .pointerInput(series) {
                    detectTapGestures(
                        onPress = { offset -> scrubIndex = idx(offset.x, size.width, all.size) },
                        onTap = { scrubIndex = null },
                    )
                },
        ) {
            val minV = all.min(); val maxV = all.max()
            val span = (maxV - minV).takeIf { it > 0f } ?: 1f
            val w = size.width; val h = size.height

            fun pt(i: Int, v: Float) = Offset(
                x = w * i.toFloat() / (all.size - 1).coerceAtLeast(1),
                y = h - ((v - minV) / span) * h,
            )

            series.forEach { s ->
                if (s.points.size < 2) return@forEach
                val path = Path()
                s.points.forEachIndexed { i, v ->
                    val p = pt(i, v)
                    if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }
                drawPath(
                    path, s.color,
                    style = Stroke(width = 6f, cap = StrokeCap.Round),
                    alpha = anim,
                )
            }

            scrubIndex?.let { i ->
                val x = w * i.toFloat() / (all.size - 1).coerceAtLeast(1)
                drawLine(
                    color = MaterialTheme.colorScheme.outline,
                    start = Offset(x, 0f), end = Offset(x, h),
                    strokeWidth = 2f,
                )
            }
        }

        scrubIndex?.let { i ->
            val values = series.map { it.label to it.points.getOrNull(i) }
            Surface(
                shape = MaterialTheme.shapes.small,
                tonalElevation = 4.dp,
                modifier = Modifier.align(androidx.compose.ui.Alignment.TopStart).padding(12.dp),
            ) {
                Column(Modifier.padding(8.dp)) {
                    Text("Point ${i + 1}", style = MaterialTheme.typography.labelSmall)
                    values.forEach { (label, v) ->
                        if (v != null) Text(
                            "$label: ${"%.2f".format(v)}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

private fun idx(px: Float, width: Int, count: Int): Int {
    val ratio = (px / width.toFloat()).coerceIn(0f, 1f)
    return (ratio * (count - 1)).toInt().coerceIn(0, count - 1)
}
