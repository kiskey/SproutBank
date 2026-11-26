package com.sproutbank.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sproutbank.app.data.Repository
import com.sproutbank.app.data.MarketData
import com.sproutbank.app.theme.CalmTheme
import com.sproutbank.app.ui.components.LineChart
import com.sproutbank.app.ui.components.Series
import com.sproutbank.app.vm.ChildUiState
import com.sproutbank.app.vm.SproutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(vm: SproutViewModel, state: ChildUiState, onOpenThemes: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Hi ${state.active?.name ?: ""} ${state.active?.emoji ?: ""}") },
            actions = {
                IconButton(onClick = onOpenThemes) { Icon(Icons.Default.Palette, "Theme") }
            },
        )
    }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Cash Balance", style = MaterialTheme.typography.labelMedium)
                        Text(
                            Repository.money(state.balanceCents),
                            style = MaterialTheme.typography.headlineLarge,
                        )
                    }
                }
            }
            state.active?.let { child ->
                item { GoalCard(child, state, vm) }
            }
            item {
                Text("Recent Request Updates",
                    style = MaterialTheme.typography.titleMedium)
            }
            if (state.requests.isEmpty()) {
                item { Text("No spend requests yet.", style = MaterialTheme.typography.bodyMedium) }
            }
            items(state.requests.take(5)) { req ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(req.title, style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(when (req.status) {
                                        "PENDING" -> "⏳ Waiting"
                                        "APPROVED" -> "✅ Approved!"
                                        else -> "💬 Not this time"
                                    })
                                }
                            )
                            Text(Repository.money(req.amountCents),
                                style = MaterialTheme.typography.bodyMedium)
                        }
                        if (req.status == "DECLINED" && req.decisionNote != null) {
                            Text("Parent: ${req.decisionNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalCard(child: com.sproutbank.app.data.ChildEntity, state: ChildUiState, vm: SproutViewModel) {
    val goal = state.goal ?: return
    Card(shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "${child.goalEmoji} My Savings Goal: ${child.goalName ?: "—"}",
                style = MaterialTheme.typography.titleMedium,
            )
            Bar(
                label = "Cash Saved Only",
                value = goal.cashSavedCents,
                ratio = goal.cashRatio.toFloat(),
                color = MaterialTheme.colorScheme.primary,
            )
            Bar(
                label = "With ${child.goalEtfSymbol} Growth",
                value = goal.simulatedGrowthCents,
                ratio = goal.growthRatio.toFloat(),
                color = MaterialTheme.colorScheme.secondary,
            )
            if (goal.marketBoostCents > 0) {
                Text(
                    "Market boost so far: +${Repository.money(goal.marketBoostCents)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun Bar(label: String, value: Long, ratio: Float, color: androidx.compose.ui.graphics.Color) {
    Column(Modifier.fillMaxWidth()) {
        Row {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.weight(1f))
            Text(Repository.money(value), style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier.fillMaxWidth().height(12.dp),
            color = color,
        )
    }
}
