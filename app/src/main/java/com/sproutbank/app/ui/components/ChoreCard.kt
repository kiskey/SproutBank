package com.sproutbank.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sproutbank.app.data.ChoreEntity
import com.sproutbank.app.data.Repository
import com.sproutbank.app.vm.ChoreUiState

data class ChoreDisplay(
    val chore: ChoreEntity,
    val state: ChoreUiState,
)

@Composable
fun ChoreCard(
    display: ChoreDisplay,
    onMarkDone: () -> Unit,
) {
    val chore = display.chore
    ThemedCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(chore.emoji, fontSize = 30.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(chore.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (chore.rewardCents > 0)
                        Text("+${Repository.money(chore.rewardCents)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary)
                    if (chore.rewardPoints > 0)
                        Text("+${chore.rewardPoints} ⭐",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary)
                }
            }
            StatusBadge(display.state)
        }
        Spacer(Modifier.height(12.dp))
        when (display.state) {
            ChoreUiState.AVAILABLE -> Button(
                onClick = onMarkDone,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Mark as Done") }
            ChoreUiState.PENDING -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Waiting for parent…", style = MaterialTheme.typography.bodyMedium)
            }
            ChoreUiState.DONE -> Text("Great job today! ✓",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary)
            ChoreUiState.REJECTED -> Text("Let's try again together.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatusBadge(state: ChoreUiState) {
    val (label, color) = when (state) {
        ChoreUiState.AVAILABLE -> "Ready" to MaterialTheme.colorScheme.primaryContainer
        ChoreUiState.PENDING -> "Pending" to MaterialTheme.colorScheme.secondary
        ChoreUiState.DONE -> "Done ✓" to MaterialTheme.colorScheme.primary
        ChoreUiState.REJECTED -> "Try Again" to MaterialTheme.colorScheme.surfaceVariant
    }
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
