package com.sproutbank.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * Toy-like layered card: two soft shadow offsets under a gradient-front surface.
 * The public signature is unchanged from before — callers need no edits.
 */
@Composable
fun ThemedCard(
    modifier: Modifier = Modifier,
    gradient: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(24.dp)

    Box(modifier) {
        // Deepest shadow layer
        Box(
            Modifier
                .matchParentSize()
                .offset(y = 8.dp)
                .background(scheme.primary.copy(alpha = 0.10f), shape),
        )
        // Mid shadow layer
        Box(
            Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .background(scheme.primary.copy(alpha = 0.15f), shape),
        )
        // Front card
        Box(
            Modifier
                .shadow(
                    elevation = 6.dp,
                    shape = shape,
                    ambientColor = scheme.primary.copy(alpha = 0.20f),
                    spotColor = scheme.primary.copy(alpha = 0.25f),
                )
                .clip(shape)
                .background(
                    brush = if (gradient)
                        Brush.verticalGradient(
                            listOf(scheme.surface, scheme.surfaceVariant.copy(alpha = 0.55f)),
                        )
                    else
                        Brush.verticalGradient(listOf(scheme.surface, scheme.surface)),
                ),
        ) {
            Column(Modifier.padding(20.dp), content = content)
        }
    }
}
