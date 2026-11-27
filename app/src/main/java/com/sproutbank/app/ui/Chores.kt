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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sproutbank.app.ui.components.ChoreCard
import com.sproutbank.app.ui.components.ChoreDisplay
import com.sproutbank.app.ui.components.Sprout
import com.sproutbank.app.ui.components.SproutMood
import com.sproutbank.app.ui.components.ThemedCard
import com.sproutbank.app.vm.ChoreUiState
import com.sproutbank.app.vm.ChildUiState
import com.sproutbank.app.vm.SproutViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Kid chores screen (issue 2 fix): the day strip and the chore list are both
 * per-date. Selecting a day shows *that day's* completions — an empty day
 * shows explicit empty states instead of other dates' chores.
 */
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
        // The per-date chore list — anchored to selectedDate, not "now" (issue 2).
        val dayMs = dayEpoch(selectedDate)
        val choresToday = state.chores.filter { it.cadence != "WEEKLY" || selectedDate.dayOfWeek == it.weekAnchor() }
        val assigned = choresToday.filter { it.appliesTo(state.active?.id ?: -1L) }

        if (selectedDate == LocalDate.now()) {
            if (assigned.isEmpty()) {
                item { EmptyDayCard(isToday = true) }
            } else {
                items(assigned) { chore ->
                    ChoreCard(ChoreDisplay(chore, vm.choreState(chore, selectedDate))) {
                        vm.submitChore(chore, selectedDate)
                    }
                }
            }
        } else {
            // Past or future day: show what actually happened (issue 2).
            val dayComps = state.completions.filter { it.dateMs == dayMs }
            if (dayComps.isEmpty()) {
                item { PastDayCard() }
            } else {
                items(assigned.filter { chore -> dayComps.any { it.choreId == chore.id } }) { chore ->
                    ChoreCard(ChoreDisplay(chore, vm.choreState(chore, selectedDate))) { }
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

/** No chores planned for this day at all (issue 2: explicit empty state). */
@Composable
private fun EmptyDayCard(isToday: Boolean) {
    ThemedCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Sprout(mood = SproutMood.SLEEPING, size = 56.dp, animate = false)
            Spacer(Modifier.width(12.dp))
            Text(
                if (isToday) "No chores planned for today — enjoy the sunshine! 🌞"
                else "No chores were planned for this day.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

/** A day with a plan but nothing done (issue 2: no bleed from other days). */
@Composable
private fun PastDayCard() {
    ThemedCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Sprout(mood = SproutMood.THINKING, size = 48.dp, animate = false)
            Spacer(Modifier.width(12.dp))
            Text("Nothing was done on this day.", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun com.sproutbank.app.data.ChoreEntity.weekAnchor(): java.time.DayOfWeek =
    java.time.DayOfWeek.of(((id % 7) + 1).toInt().coerceIn(1, 7))

/** Issue 4: assignee filter — "[]" means the whole family. */
private fun com.sproutbank.app.data.ChoreEntity.appliesTo(childId: Long): Boolean {
    if (assigneeIds.isBlank() || assigneeIds == "[]") return true
    return assigneeIds.trim('[', ']').split(',')
        .mapNotNull { it.trim().toLongOrNull() }.contains(childId)
}

private fun dayEpoch(date: LocalDate): Long = com.sproutbank.app.data.Repository.localDayMs(
    java.util.Calendar.getInstance().let { cal ->
        cal.set(date.year, date.monthValue - 1, date.dayOfMonth, 12, 0, 0)
        cal.timeInMillis
    }
)

private data class DayColors(val bg: Color, val fg: Color)

@Composable
private fun DayChip(
    date: LocalDate,
    selected: Boolean,
    status: ChoreUiState,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when {
        selected -> scheme.primary to scheme.onPrimary
        status == ChoreUiState.DONE -> scheme.primaryContainer to scheme.onPrimaryContainer
        status == ChoreUiState.PENDING -> scheme.secondaryContainer to scheme.onSecondaryContainer
        else -> scheme.surfaceVariant to scheme.onSurfaceVariant
    }
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
