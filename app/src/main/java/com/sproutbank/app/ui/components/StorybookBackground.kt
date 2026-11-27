package com.sproutbank.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.sproutbank.app.theme.StorybookPalette

enum class StoryIntensity { FULL, SUBTLE }

@Composable
fun StorybookBackground(
    intensity: StoryIntensity,
    palette: StorybookPalette,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            drawRect(
                brush = Brush.verticalGradient(listOf(palette.skyTop, palette.skyBottom)),
                size = size,
            )
            if (intensity == StoryIntensity.FULL) {
                drawCloud(Offset(w * 0.20f, h * 0.15f), w * 0.11f, palette.cloud)
                drawCloud(Offset(w * 0.72f, h * 0.10f), w * 0.09f, palette.cloud)
                drawCloud(Offset(w * 0.55f, h * 0.24f), w * 0.07f, palette.cloud)
            }
            val backHill = Path().apply {
                moveTo(0f, h * 0.72f)
                cubicTo(w * 0.25f, h * 0.55f, w * 0.45f, h * 0.80f, w * 0.65f, h * 0.68f)
                cubicTo(w * 0.80f, h * 0.60f, w * 0.92f, h * 0.75f, w, h * 0.68f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(backHill, palette.hillBack)
            val frontHill = Path().apply {
                moveTo(0f, h * 0.85f)
                cubicTo(w * 0.20f, h * 0.72f, w * 0.50f, h * 0.95f, w * 0.75f, h * 0.82f)
                cubicTo(w * 0.85f, h * 0.78f, w * 0.95f, h * 0.86f, w, h * 0.82f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(frontHill, palette.hillFront)
            if (intensity == StoryIntensity.FULL) {
                repeat(7) { i ->
                    val x = w * (0.05f + i * 0.14f)
                    val y = h * 0.94f
                    drawOval(
                        color = palette.grass,
                        topLeft = Offset(x - w * 0.02f, y),
                        size = Size(w * 0.04f, h * 0.02f),
                    )
                }
            }
        }
        content()
    }
}

private fun DrawScope.drawCloud(center: Offset, radius: Float, color: Color) {
    drawCircle(color.copy(alpha = 0.90f), radius, center)
    drawCircle(color.copy(alpha = 0.90f), radius * 0.8f, center + Offset(radius * 0.9f, radius * 0.1f))
    drawCircle(color.copy(alpha = 0.90f), radius * 0.7f, center - Offset(radius * 0.9f, radius * 0.1f))
    drawOval(
        color = color.copy(alpha = 0.80f),
        topLeft = Offset(center.x - radius * 1.4f, center.y + radius * 0.2f),
        size = Size(radius * 2.8f, radius * 0.7f),
    )
}




