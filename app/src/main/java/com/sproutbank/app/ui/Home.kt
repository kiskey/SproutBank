package com.sproutbank.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sproutbank.app.data.Repository
import com.sproutbank.app.ui.components.*
import com.sproutbank.app.vm.ChoreUiState
import com.sproutbank.app.vm.ChildUiState
import com.sproutbank.app.vm.SproutViewModel

@Composable
fun HomeScreen(
    vm: SproutViewModel,
    state: ChildUiState,
    onOpenGoal: () -> Unit,
) {
    val active = state.active ?: return
    val reduced by vm.reducedAnimations.collectAsState()

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarCircle(
                    active.emoji,
                    Color(android.graphics.Color.parseColor(active.avatarBg)),
                    Color(android.graphics.Color.parseColor(active.avatarRing)),
                    size = 56.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(greeting(), style = MaterialTheme.typography.titleLarge)
                    Text(active.name, style = MaterialTheme.typography.headlineMedium)
                }
            }
        }

        item { HeroBankCard(state) }

        item { SavingsGoalCard(state, onOpenGoal) }

        item { Text("Today's Adventure", style = MaterialTheme.typography.titleLarge) }

        val todays = state.chores.filter { chore ->
            state.completions.none { it.choreId == chore.id && it.status == "APPROVED" }
        }
        if (todays.isEmpty()) {
            item {
                ThemedCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Sprout(mood = SproutMood.SLEEPING, size = 56.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("All chores done for today — nice work!",
                            style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        } else {
            items(todays) { chore ->
                ChoreCard(ChoreDisplay(chore, vm.choreState(chore))) {
                    vm.submitChore(chore)
                }
            }
        }

        if (state.requests.isNotEmpty()) {
            item { Text("Recent Request Updates", style = MaterialTheme.typography.titleMedium) }
            items(state.requests.take(3)) { req -> RequestStatusRow(req) }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

private fun greeting(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        h < 12 -> "Good morning,"
        h < 18 -> "Good afternoon,"
        else -> "Good evening,"
    }
}

@Composable
private fun HeroBankCard(state: ChildUiState) {
    val scheme = MaterialTheme.colorScheme
    Card(
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer),
    ) {
        Column(Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Sprout(mood = SproutMood.GROWING, size = 72.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("MY MONEY", style = MaterialTheme.typography.labelMedium,
                        color = scheme.onPrimaryContainer)
                    Text(Repository.money(state.balanceCents),
                        style = MaterialTheme.typography.displayMedium,
                        color = scheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${state.points} ⭐", style = MaterialTheme.typography.titleMedium,
                    color = scheme.onPrimaryContainer)
                Spacer(Modifier.weight(1f))
                Text("Look how much you've earned!",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun SavingsGoalCard(state: ChildUiState, onOpenGoal: () -> Unit) {
    val active = state.active ?: return
    val goal = state.goal ?: return
    val anim = animateFloatAsState(goal.growthRatio.toFloat(), spring(), label = "goalFill")
    ThemedCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(active.goalEmoji, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("My ${active.goalName ?: "Dream"}",
                    style = MaterialTheme.typography.titleMedium)
                Text("${Repository.money(goal.simulatedGrowthCents)} / ${Repository.money(goal.targetCents)}",
                    style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onOpenGoal) { Text("See →") }
        }
        Spacer(Modifier.height(12.dp))
        GoalBar("Cash saved", goal.cashRatio.toFloat(), MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        GoalBar("With ${active.goalEtfSymbol} growth", anim.value, MaterialTheme.colorScheme.secondary)
        if (goal.marketBoostCents > 0) {
            Spacer(Modifier.height(8.dp))
            Text("Market boost so far: +${Repository.money(goal.marketBoostCents)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
internal fun GoalBar(label: String, progress: Float, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
            color = color,
        )
    }
}

@Composable
internal fun RequestStatusRow(req: com.sproutbank.app.data.SpendRequestEntity) {
    ThemedCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                when (req.status) {
                    "PENDING" -> "⏳"
                    "APPROVED" -> "✅"
                    else -> "💬"
                },
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(req.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    when (req.status) {
                        "PENDING" -> "Waiting for parent…"
                        "APPROVED" -> "Approved!"
                        else -> "Not this time"
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                if (req.status == "DECLINED" && req.decisionNote != null) {
                    Text("Parent: ${req.decisionNote}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(Repository.money(req.amountCents),
                style = MaterialTheme.typography.titleSmall)
        }
    }
}
