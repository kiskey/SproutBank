package com.sproutbank.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sproutbank.app.data.ChoreEntity
import com.sproutbank.app.data.Repository
import com.sproutbank.app.vm.ChoreUiState

data class ChoreDisplay(
    val chore: ChoreEntity,
    val state: ChoreUiState,
)

/**
 * Issue 6 fix: the PENDING state is a static "Sent ✓ waiting for approval"
 * chip — no indeterminate spinner (which redrew at 60fps and drained
 * battery while waiting). The card renders exactly once per state change.
 */
@Composable
fun ChoreCard(
    display: ChoreDisplay,
    onMarkDone: () -> Unit,
) {
    val chore = display.chore
    val isDeduction = chore.rewardCents < 0 || chore.rewardPoints < 0
    ThemedCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = if (isDeduction) listOf(
                                MaterialTheme.colorScheme.errorContainer,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.18f),
                            ) else listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                StorybookObject(emojiToStorybook(chore.emoji), size = 48.dp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(chore.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (chore.rewardCents != 0L)
                        Text(
                            "${if (chore.rewardCents > 0) "+" else ""}${Repository.money(chore.rewardCents)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isDeduction) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary,
                        )
                    if (chore.rewardPoints != 0L)
                        Text(
                            "${if (chore.rewardPoints > 0) "+" else ""}${chore.rewardPoints} ⭐",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isDeduction) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.secondary,
                        )
                }
            }
            StatusBadge(display.state)
        }
        Spacer(Modifier.height(12.dp))
        when (display.state) {
            ChoreUiState.AVAILABLE -> Button(
                onClick = onMarkDone,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (isDeduction) "Ask grown-up to apply" else "Mark as Done") }
            ChoreUiState.PENDING -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("✓", style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
                Text("Sent — waiting for your grown-up to approve",
                    style = MaterialTheme.typography.bodyMedium)
            }
            ChoreUiState.DONE -> Text(
                if (isDeduction) "Recorded ✓" else "Great job today! ✓",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            ChoreUiState.REJECTED -> Text("Let's try again together.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private data class BadgeColors(val bg: Color, val fg: Color)

@Composable
private fun StatusBadge(state: ChoreUiState) {
    val scheme = MaterialTheme.colorScheme
    val (label, colors) = when (state) {
        ChoreUiState.AVAILABLE  -> "Ready"     to BadgeColors(scheme.primaryContainer,   scheme.onPrimaryContainer)
        ChoreUiState.PENDING    -> "Sent"      to BadgeColors(scheme.secondaryContainer, scheme.onSecondaryContainer)
        ChoreUiState.DONE       -> "Done ✓"    to BadgeColors(scheme.primary,            scheme.onPrimary)
        ChoreUiState.REJECTED   -> "Try Again" to BadgeColors(scheme.surfaceVariant,     scheme.onSurfaceVariant)
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.fg)
    }
}
