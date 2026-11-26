package com.sproutbank.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

enum class StorybookObject {
    COIN, STAR, TROPHY, PIGGY,
    BED, TOOTHBRUSH, BOOK, PET_BOWL, WATERING_CAN, GIFT, SHOPPING_BAG,
    SPARKLE,
}

fun emojiToStorybook(emoji: String): StorybookObject = when (emoji) {
    "🛏️" -> StorybookObject.BED
    "🪥" -> StorybookObject.TOOTHBRUSH
    "📚" -> StorybookObject.BOOK
    "🐾" -> StorybookObject.PET_BOWL
    "🪴" -> StorybookObject.WATERING_CAN
    "🎁" -> StorybookObject.GIFT
    "🛍️" -> StorybookObject.SHOPPING_BAG
    "🏆" -> StorybookObject.TROPHY
    "🐷" -> StorybookObject.PIGGY
    "⭐" -> StorybookObject.STAR
    "🪙" -> StorybookObject.COIN
    else -> StorybookObject.STAR
}

@Composable
fun StorybookObject(
    kind: StorybookObject,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        // soft drop shadow
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.Black.copy(alpha = 0.14f), Color.Transparent),
                center = Offset(cx, cy + s * 0.34f),
                radius = s * 0.40f,
            ),
            topLeft = Offset(cx - s * 0.32f, cy + s * 0.28f),
            size = Size(s * 0.64f, s * 0.14f),
        )
        when (kind) {
            StorybookObject.COIN -> drawCoin3D(cx, cy, s * 0.34f)
            StorybookObject.STAR -> drawStar3D(cx, cy, s * 0.34f)
            StorybookObject.TROPHY -> drawTrophy3D(cx, cy, s)
            StorybookObject.PIGGY -> drawPiggy3D(cx, cy, s)
            StorybookObject.BED -> drawBed3D(cx, cy, s)
            StorybookObject.TOOTHBRUSH -> drawToothbrush3D(cx, cy, s)
            StorybookObject.BOOK -> drawBook3D(cx, cy, s)
            StorybookObject.PET_BOWL -> drawPetBowl3D(cx, cy, s)
            StorybookObject.WATERING_CAN -> drawWateringCan3D(cx, cy, s)
            StorybookObject.GIFT -> drawGift3D(cx, cy, s)
            StorybookObject.SHOPPING_BAG -> drawShoppingBag3D(cx, cy, s)
            StorybookObject.SPARKLE -> drawSparkle3D(cx, cy, s * 0.30f)
        }
    }
}

// ── Drawing primitives ────────────────────────────────────────────────

internal fun DrawScope.drawCoin3D(cx: Float, cy: Float, r: Float, alpha: Float = 1f) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFE58A).copy(alpha = alpha),
                Color(0xFFE0A93B).copy(alpha = alpha),
                Color(0xFF9C6E14).copy(alpha = alpha),
            ),
            center = Offset(cx - r * 0.4f, cy - r * 0.4f),
            radius = r * 1.7f,
        ),
        radius = r, center = Offset(cx, cy),
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF2BC).copy(alpha = alpha),
                Color(0xFFE0A93B).copy(alpha = alpha),
            ),
            center = Offset(cx - r * 0.3f, cy - r * 0.3f),
            radius = r * 1.2f,
        ),
        radius = r * 0.72f, center = Offset(cx, cy),
    )
    // little star on face
    val inner = r * 0.26f
    val outer = r * 0.42f
    val path = Path().apply {
        for (i in 0 until 10) {
            val a = Math.toRadians((-90 + i * 36).toDouble())
            val rad = if (i % 2 == 0) outer else inner
            val x = cx + (rad * cos(a)).toFloat()
            val y = cy + (rad * sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(path, Color(0xFFB5841F).copy(alpha = alpha))
    drawOval(
        color = Color.White.copy(alpha = 0.65f * alpha),
        topLeft = Offset(cx - r * 0.55f, cy - r * 0.72f),
        size = Size(r * 0.5f, r * 0.20f),
    )
}

internal fun DrawScope.drawStar3D(cx: Float, cy: Float, r: Float, alpha: Float = 1f) {
    val path = Path().apply {
        val inner = r * 0.42f
        for (i in 0 until 10) {
            val a = Math.toRadians((-90 + i * 36).toDouble())
            val rad = if (i % 2 == 0) r else inner
            val x = cx + (rad * cos(a)).toFloat()
            val y = cy + (rad * sin(a)).toFloat()
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFF1A6).copy(alpha = alpha),
                Color(0xFFF2C14E).copy(alpha = alpha),
                Color(0xFFC99622).copy(alpha = alpha),
            ),
            center = Offset(cx - r * 0.3f, cy - r * 0.3f),
            radius = r * 1.6f,
        ),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.75f * alpha),
        radius = r * 0.10f,
        center = Offset(cx - r * 0.25f, cy - r * 0.35f),
    )
}

private fun DrawScope.drawPiggy3D(cx: Float, cy: Float, s: Float) {
    val r = s * 0.28f
    // legs
    drawOval(color = Color(0xFFD989A6), topLeft = Offset(cx - r * 0.75f, cy + r * 0.65f), size = Size(r * 0.42f, r * 0.32f))
    drawOval(color = Color(0xFFD989A6), topLeft = Offset(cx + r * 0.32f, cy + r * 0.65f), size = Size(r * 0.42f, r * 0.32f))
    // ears
    drawCircle(color = Color(0xFFF3A5BE), radius = r * 0.24f, center = Offset(cx - r * 0.65f, cy - r * 0.68f))
    drawCircle(color = Color(0xFFF3A5BE), radius = r * 0.24f, center = Offset(cx + r * 0.65f, cy - r * 0.68f))
    // body
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFD1E0), Color(0xFFF3A5BE), Color(0xFFD989A6)),
            center = Offset(cx - r * 0.5f, cy - r * 0.5f),
            radius = r * 1.9f,
        ),
        radius = r, center = Offset(cx, cy),
    )
    // snout
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFC1D4), Color(0xFFD989A6)),
            center = Offset(cx - r * 0.4f, cy - r * 0.1f),
            radius = r * 1.2f,
        ),
        radius = r * 0.42f, center = Offset(cx, cy + r * 0.12f),
    )
    drawCircle(Color(0xFF8E4D66), r * 0.075f, Offset(cx - r * 0.15f, cy + r * 0.12f))
    drawCircle(Color(0xFF8E4D66), r * 0.075f, Offset(cx + r * 0.15f, cy + r * 0.12f))
    // eyes
    drawCircle(Color(0xFF5C2036), r * 0.09f, Offset(cx - r * 0.46f, cy - r * 0.24f))
    drawCircle(Color(0xFF5C2036), r * 0.09f, Offset(cx + r * 0.46f, cy - r * 0.24f))
    // shine
    drawOval(
        color = Color.White.copy(alpha = 0.55f),
        topLeft = Offset(cx - r * 0.5f, cy - r * 0.9f),
        size = Size(r * 0.42f, r * 0.18f),
    )
}

private fun DrawScope.drawBed3D(cx: Float, cy: Float, s: Float) {
    val w = s * 0.74f
    val h = s * 0.28f
    val left = cx - w / 2
    val top = cy - h / 2
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF9C6E52), Color(0xFF6E4A34))),
        topLeft = Offset(left, top - h * 0.5f),
        size = Size(w * 0.14f, h * 1.5f),
        cornerRadius = CornerRadius(w * 0.05f),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFFFF8ED), Color(0xFFE6D5BD))),
        topLeft = Offset(left + w * 0.08f, top),
        size = Size(w * 0.86f, h),
        cornerRadius = CornerRadius(h * 0.3f),
    )
    drawOval(
        brush = Brush.radialGradient(listOf(Color.White, Color(0xFFE0CFB8))),
        topLeft = Offset(left + w * 0.12f, top + h * 0.15f),
        size = Size(w * 0.24f, h * 0.55f),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF88C9A6), Color(0xFF4E9F73))),
        topLeft = Offset(left + w * 0.38f, top + h * 0.05f),
        size = Size(w * 0.55f, h * 0.95f),
        cornerRadius = CornerRadius(h * 0.2f),
    )
}

private fun DrawScope.drawToothbrush3D(cx: Float, cy: Float, s: Float) {
    val w = s * 0.30f
    val h = s * 0.66f
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF8AD1E0), Color(0xFF3D8FA0))),
        topLeft = Offset(cx - w * 0.35f, cy - h * 0.2f),
        size = Size(w * 0.7f, h * 0.8f),
        cornerRadius = CornerRadius(w * 0.35f),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFD8E7EC))),
        topLeft = Offset(cx - w * 0.35f, cy - h * 0.45f),
        size = Size(w * 0.7f, h * 0.32f),
        cornerRadius = CornerRadius(w * 0.2f),
    )
    repeat(3) { i ->
        repeat(4) { j ->
            drawCircle(
                color = Color(0xFF5CA9BE),
                radius = w * 0.07f,
                center = Offset(
                    cx - w * 0.28f + i * w * 0.19f,
                    cy - h * 0.40f + j * w * 0.14f,
                ),
            )
        }
    }
    drawOval(
        color = Color.White.copy(alpha = 0.4f),
        topLeft = Offset(cx - w * 0.10f, cy - h * 0.10f),
        size = Size(w * 0.20f, h * 0.10f),
    )
}

private fun DrawScope.drawBook3D(cx: Float, cy: Float, s: Float) {
    val w = s * 0.66f
    val h = s * 0.46f
    drawRoundRect(
        color = Color(0xFF9C6E52),
        topLeft = Offset(cx - w / 2, cy - h / 2),
        size = Size(w, h),
        cornerRadius = CornerRadius(h * 0.15f),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFEBDDC9))),
        topLeft = Offset(cx - w / 2 + w * 0.05f, cy - h / 2 + h * 0.08f),
        size = Size(w * 0.45f, h * 0.85f),
        cornerRadius = CornerRadius(h * 0.1f),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFEBDDC9))),
        topLeft = Offset(cx + w * 0.02f, cy - h / 2 + h * 0.08f),
        size = Size(w * 0.45f, h * 0.85f),
        cornerRadius = CornerRadius(h * 0.1f),
    )
    repeat(4) { i ->
        drawLine(
            color = Color(0xFFB6A48B),
            start = Offset(cx - w * 0.40f, cy - h * 0.2f + i * h * 0.14f),
            end = Offset(cx - w * 0.10f, cy - h * 0.2f + i * h * 0.14f),
            strokeWidth = 2.5f,
        )
        drawLine(
            color = Color(0xFFB6A48B),
            start = Offset(cx + w * 0.08f, cy - h * 0.2f + i * h * 0.14f),
            end = Offset(cx + w * 0.38f, cy - h * 0.2f + i * h * 0.14f),
            strokeWidth = 2.5f,
        )
    }
}

private fun DrawScope.drawPetBowl3D(cx: Float, cy: Float, s: Float) {
    drawOval(
        brush = Brush.verticalGradient(listOf(Color(0xFFF3A5BE), Color(0xFFD989A6))),
        topLeft = Offset(cx - s * 0.32f, cy - s * 0.05f),
        size = Size(s * 0.64f, s * 0.34f),
    )
    drawOval(
        brush = Brush.radialGradient(listOf(Color(0xFFFFE0EA), Color(0xFFF3A5BE))),
        topLeft = Offset(cx - s * 0.32f, cy - s * 0.14f),
        size = Size(s * 0.64f, s * 0.20f),
    )
    repeat(5) { i ->
        drawCircle(
            color = Color(0xFF9C6E52),
            radius = s * 0.05f,
            center = Offset(
                cx - s * 0.18f + (i % 3) * s * 0.14f,
                cy - s * 0.05f + (i / 3) * s * 0.06f,
            ),
        )
    }
}

private fun DrawScope.drawWateringCan3D(cx: Float, cy: Float, s: Float) {
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF9AD7C8), Color(0xFF4E9F88)),
            center = Offset(cx - s * 0.15f, cy - s * 0.10f),
            radius = s * 0.55f,
        ),
        topLeft = Offset(cx - s * 0.22f, cy - s * 0.10f),
        size = Size(s * 0.44f, s * 0.36f),
        cornerRadius = CornerRadius(s * 0.08f),
    )
    val spout = Path().apply {
        moveTo(cx - s * 0.22f, cy + s * 0.10f)
        lineTo(cx - s * 0.45f, cy + s * 0.20f)
        lineTo(cx - s * 0.45f, cy + s * 0.26f)
        lineTo(cx - s * 0.18f, cy + s * 0.22f)
        close()
    }
    drawPath(spout, Color(0xFF4E9F88))
    drawArc(
        color = Color(0xFF4E9F88),
        startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx + s * 0.04f, cy - s * 0.15f),
        size = Size(s * 0.30f, s * 0.30f),
        style = Stroke(width = s * 0.05f),
    )
    drawOval(
        color = Color.White.copy(alpha = 0.45f),
        topLeft = Offset(cx - s * 0.15f, cy - s * 0.05f),
        size = Size(s * 0.14f, s * 0.06f),
    )
}

private fun DrawScope.drawGift3D(cx: Float, cy: Float, s: Float) {
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFF3C969), Color(0xFFC99622))),
        topLeft = Offset(cx - s * 0.26f, cy - s * 0.10f),
        size = Size(s * 0.52f, s * 0.36f),
        cornerRadius = CornerRadius(s * 0.04f),
    )
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFF8DE9E), Color(0xFFE0A93B))),
        topLeft = Offset(cx - s * 0.30f, cy - s * 0.20f),
        size = Size(s * 0.60f, s * 0.12f),
        cornerRadius = CornerRadius(s * 0.03f),
    )
    drawRect(
        color = Color(0xFFCF4F7D),
        topLeft = Offset(cx - s * 0.04f, cy - s * 0.20f),
        size = Size(s * 0.08f, s * 0.46f),
    )
    drawCircle(Color(0xFFCF4F7D), s * 0.08f, Offset(cx - s * 0.06f, cy - s * 0.24f))
    drawCircle(Color(0xFFCF4F7D), s * 0.08f, Offset(cx + s * 0.06f, cy - s * 0.24f))
}

private fun DrawScope.drawShoppingBag3D(cx: Float, cy: Float, s: Float) {
    drawArc(
        color = Color(0xFFB5841F),
        startAngle = 200f, sweepAngle = 140f, useCenter = false,
        topLeft = Offset(cx - s * 0.14f, cy - s * 0.26f),
        size = Size(s * 0.28f, s * 0.28f),
        style = Stroke(width = s * 0.04f),
    )
    val body = Path().apply {
        moveTo(cx - s * 0.24f, cy - s * 0.10f)
        lineTo(cx + s * 0.24f, cy - s * 0.10f)
        lineTo(cx + s * 0.20f, cy + s * 0.28f)
        lineTo(cx - s * 0.20f, cy + s * 0.28f)
        close()
    }
    drawPath(
        path = body,
        brush = Brush.verticalGradient(listOf(Color(0xFFFFD1E0), Color(0xFFD989A6))),
    )
    drawOval(
        color = Color.White.copy(alpha = 0.45f),
        topLeft = Offset(cx - s * 0.14f, cy - s * 0.05f),
        size = Size(s * 0.10f, s * 0.04f),
    )
}

private fun DrawScope.drawTrophy3D(cx: Float, cy: Float, s: Float) {
    val bodyTop = cy - s * 0.24f
    val bodyBot = cy + s * 0.10f
    drawArc(
        color = Color(0xFFE0A93B),
        startAngle = 90f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx - s * 0.32f, cy - s * 0.18f),
        size = Size(s * 0.20f, s * 0.24f),
        style = Stroke(width = s * 0.04f),
    )
    drawArc(
        color = Color(0xFFE0A93B),
        startAngle = 270f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(cx + s * 0.12f, cy - s * 0.18f),
        size = Size(s * 0.20f, s * 0.24f),
        style = Stroke(width = s * 0.04f),
    )
    val cup = Path().apply {
        moveTo(cx - s * 0.18f, bodyTop)
        lineTo(cx + s * 0.18f, bodyTop)
        quadraticBezierTo(cx + s * 0.16f, bodyBot - s * 0.02f, cx, bodyBot)
        quadraticBezierTo(cx - s * 0.16f, bodyBot - s * 0.02f, cx - s * 0.18f, bodyTop)
        close()
    }
    drawPath(
        path = cup,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFE58A), Color(0xFFE0A93B), Color(0xFF9C6E14)),
            center = Offset(cx - s * 0.10f, cy - s * 0.10f),
            radius = s * 0.65f,
        ),
    )
    drawRoundRect(
        color = Color(0xFFE0A93B),
        topLeft = Offset(cx - s * 0.05f, cy + s * 0.06f),
        size = Size(s * 0.10f, s * 0.10f),
    )
    drawRoundRect(
        color = Color(0xFF9C6E14),
        topLeft = Offset(cx - s * 0.18f, cy + s * 0.16f),
        size = Size(s * 0.36f, s * 0.08f),
        cornerRadius = CornerRadius(s * 0.02f),
    )
    drawOval(
        color = Color.White.copy(alpha = 0.55f),
        topLeft = Offset(cx - s * 0.10f, cy - s * 0.18f),
        size = Size(s * 0.12f, s * 0.05f),
    )
}

private fun DrawScope.drawSparkle3D(cx: Float, cy: Float, r: Float) {
    drawCircle(
        brush = Brush.radialGradient(listOf(Color.White, Color(0xFFFFF1A6))),
        radius = r, center = Offset(cx, cy),
    )
    val path = Path().apply {
        moveTo(cx, cy - r * 1.8f)
        quadraticBezierTo(cx + r * 0.3f, cy - r * 0.3f, cx + r * 1.8f, cy)
        quadraticBezierTo(cx + r * 0.3f, cy + r * 0.3f, cx, cy + r * 1.8f)
        quadraticBezierTo(cx - r * 0.3f, cy + r * 0.3f, cx - r * 1.8f, cy)
        quadraticBezierTo(cx - r * 0.3f, cy - r * 0.3f, cx, cy - r * 1.8f)
        close()
    }
    drawPath(path, Color.White.copy(alpha = 0.9f))
}
