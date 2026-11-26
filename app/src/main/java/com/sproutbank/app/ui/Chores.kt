package com.sproutbank.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sproutbank.app.ui.components.ChoreCard
import com.sproutbank.app.ui.components.ChoreDisplay
import com.sproutbank.app.vm.ChoreUiState
import com.sproutbank.app.vm.ChildUiState
import com.sproutbank.app.vm.SproutViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun ChoresScreen(vm: SproutViewModel, state: ChildUiState) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { month = month.minusMonths(1) }) {
                    Text("‹", style = MaterialTheme.typography.headlineSmall)
                }
                Text(
                    month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = { month = month.plusMonths(1) }) {
                    Text("›", style = MaterialTheme.typography.headlineSmall)
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
                items(days) { d ->
                    DayChip(
                        date = d,
                        selected = d == selectedDate,
                        status = vm.choreStateOfDate(state, d),
                        onClick = { selectedDate = d },
                    )
                }
            }
        }
        item {
            Text(
                if (selectedDate == LocalDate.now()) "Today's Chores"
                else "Chores on ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d"))}",
                style = MaterialTheme.typography.titleLarge,
            )
        }
        if (state.chores.isEmpty()) {
            item {
                Text(
                    "No chores yet — ask a parent to add some!",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            items(state.chores) { chore ->
                ChoreCard(ChoreDisplay(chore, vm.choreState(chore))) {
                    vm.submitChore(chore)
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Your Amazing Week!", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..6).forEach { offset ->
                    val d = LocalDate.now().minusDays((6 - offset).toLong())
                    val done = vm.choreStateOfDate(state, d) == ChoreUiState.DONE
                    Text(
                        if (done) "⭐" else "·",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayChip(
    date: LocalDate,
    selected: Boolean,
    status: ChoreUiState,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val bg = when {
        selected -> scheme.primary
        status == ChoreUiState.DONE -> scheme.primaryContainer
        status == ChoreUiState.PENDING -> scheme.secondary.copy(alpha = 0.5f)
        else -> scheme.surfaceVariant
    }
    val fg = if (selected) scheme.onPrimary else scheme.onSurfaceVariant
    Column(
        Modifier
            .width(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            date.format(DateTimeFormatter.ofPattern("EEE")),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
        )
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = fg,
        )
        Text(
            when (status) {
                ChoreUiState.DONE -> "✓"
                ChoreUiState.PENDING -> "⏳"
                else -> " "
            },
            style = MaterialTheme.typography.labelMedium,
            color = fg,
        )
    }
}
