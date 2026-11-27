package com.sproutbank.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.sproutbank.app.data.CelebrationEntity
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * One-shot celebration banner (issue 5 fix). The caller passes *events*, not
 * the DB "latest row": every event is consumed exactly once here, and there
 * is no re-subscription replay when switching parent↔kid screens.
 * Battery note (issue 6): the finite particle animation only runs while the
 * banner is visible; nothing animates while idle.
 */
@Composable
fun CelebrationBanner(
    events: kotlinx.coroutines.flow.SharedFlow<CelebrationEntity>,
    reducedAnimations: Boolean,
) {
    var current by remember { mutableStateOf<CelebrationEntity?>(null) }
    val progress = remember { Animatable(0f) }

    // Collect events; each id is handled once and auto-dismissed.
    LaunchedEffect(Unit) {
        events.collect { celebration ->
            current = celebration
            if (!reducedAnimations) {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(900))
            }
            delay(2800)
            current = null
        }
    }

    AnimatedVisibility(
        visible = current != null,
        enter = if (reducedAnimations) fadeIn(tween(0))
                else slideInVertically(tween(400)) + fadeIn(tween(400)),
        exit  = fadeOut(tween(300)),
    ) {
        val c = current
        if (c != null) {
            val scheme = MaterialTheme.colorScheme
            Box(
                Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .background(
                        Brush.horizontalGradient(listOf(scheme.primaryContainer, scheme.secondary)),
                        RoundedCornerShape(20.dp),
                    )
                    .clip(RoundedCornerShape(20.dp)),
            ) {
                if (!reducedAnimations) {
                    Canvas(Modifier.matchParentSize()) {
                        val p = progress.value
                        val cx = size.width / 2f
                        val cy = size.height * 0.85f
                        for (i in 0 until 6) {
                            val angle = Math.toRadians((210 + i * 24).toDouble())
                            val dist = 60f * p
                            val x = cx + (cos(angle) * dist).toFloat()
                            val y = cy + (sin(angle) * dist).toFloat()
                            val alpha = (1f - p).coerceIn(0f, 1f)
                            if (i % 2 == 0) drawCoin3D(x, y, 10f, alpha)
                            else drawStar3D(x, y, 10f, alpha)
                        }
                    }
                }
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("✨", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(c.title, style = MaterialTheme.typography.titleMedium,
                            color = scheme.onPrimaryContainer)
                        if (c.subtitle != null)
                            Text(c.subtitle, style = MaterialTheme.typography.bodySmall,
                                color = scheme.onPrimaryContainer)
                    }
                    Spacer(Modifier.weight(1f))
                    c.amountLabel?.let {
                        Text(it, style = MaterialTheme.typography.titleLarge,
                            color = scheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}
