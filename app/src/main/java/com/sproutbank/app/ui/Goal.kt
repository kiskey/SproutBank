package com.sproutbank.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sproutbank.app.data.Repository
import com.sproutbank.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalScreen(state: com.sproutbank.app.vm.ChildUiState, onBack: () -> Unit) {
    val active = state.active ?: return
    val goal = state.goal ?: return
    val reduced = false
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("My Savings Goal") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(active.goalEmoji, style = MaterialTheme.typography.displayLarge)
                    Text("My ${active.goalName ?: "Dream"}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("Target ${Repository.money(goal.targetCents)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            ThemedCard(Modifier.fillMaxWidth()) {
                Text("My Money Saved", style = MaterialTheme.typography.titleMedium)
                Text("${Repository.money(goal.cashSavedCents)} / ${Repository.money(goal.targetCents)}",
                    style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { goal.cashRatio.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)),
                )
            }
            val anim by animateFloatAsState(goal.growthRatio.toFloat(), spring(), label = "g")
            ThemedCard(Modifier.fillMaxWidth()) {
                Text("If My Money Grew (${active.goalEtfSymbol})",
                    style = MaterialTheme.typography.titleMedium)
                Text("${Repository.money(goal.simulatedGrowthCents)} / ${Repository.money(goal.targetCents)}",
                    style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { anim },
                    modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.height(8.dp))
                Text("Your money has a chance to grow when invested, but its value can also go down.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (goal.marketBoostCents > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Sprout(mood = SproutMood.CELEBRATING, size = 72.dp, reduced = reduced)
                    Spacer(Modifier.width(16.dp))
                    Text("Market boost so far: +${Repository.money(goal.marketBoostCents)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
