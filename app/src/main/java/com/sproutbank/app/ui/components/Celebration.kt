package com.sproutbank.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.sproutbank.app.data.CelebrationEntity
import kotlinx.coroutines.delay

@Composable
fun CelebrationBanner(celebration: CelebrationEntity?, reducedAnimations: Boolean) {
    var current by remember { mutableStateOf<CelebrationEntity?>(null) }
    LaunchedEffect(celebration?.id) {
        if (celebration != null && celebration.id != current?.id) {
            current = celebration
            delay(3000)
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
            Row(
                Modifier
                    .padding(12.dp)
                    .background(
                        Brush.horizontalGradient(listOf(scheme.primaryContainer, scheme.secondary)),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("✨", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(c.title, style = MaterialTheme.typography.titleMedium, color = scheme.onPrimaryContainer)
                    if (c.subtitle != null)
                        Text(c.subtitle, style = MaterialTheme.typography.bodySmall, color = scheme.onPrimaryContainer)
                }
                Spacer(Modifier.weight(1f))
                c.amountLabel?.let {
                    Text(it, style = MaterialTheme.typography.titleLarge, color = scheme.onPrimaryContainer)
                }
            }
        }
    }
}
