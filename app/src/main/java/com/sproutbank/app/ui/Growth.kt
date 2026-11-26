package com.sproutbank.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sproutbank.app.ai.SproutAssistant
import com.sproutbank.app.data.MarketData
import com.sproutbank.app.data.Repository
import com.sproutbank.app.ui.components.LineChart
import com.sproutbank.app.ui.components.Series
import com.sproutbank.app.ui.components.ThemedCard
import com.sproutbank.app.vm.ChildUiState
import com.sproutbank.app.vm.SproutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrowthScreen(vm: SproutViewModel, state: ChildUiState) {
    var tab by remember { mutableStateOf(0) }
    val labels = listOf("Charts", "Spend Impact", "Learn & AI")
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = tab) {
            labels.forEachIndexed { i, l ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(l) })
            }
        }
        when (tab) {
            0 -> ChartsTab(vm, state)
            1 -> SpendImpactTab(vm, state)
            else -> LearnTab(state)
        }
    }
}

@Composable
private fun ChartsTab(vm: SproutViewModel, state: ChildUiState) {
    var etf by remember { mutableStateOf("VOO") }
    var range by remember { mutableStateOf("90D") }
    val series: List<com.sproutbank.app.rust.PortfolioPoint> = remember(etf, range, state.ledger) {
        runCatching { vm.spendImpact(etf).unspent.takeLast(daysFor(range)) }
            .getOrDefault(emptyList())
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MarketData.trackedEtfs().forEach {
                    FilterChip(selected = etf == it, onClick = { etf = it }, label = { Text(it) })
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("7D", "30D", "90D", "1Y", "All").forEach {
                    FilterChip(selected = range == it, onClick = { range = it }, label = { Text(it) })
                }
            }
        }
        item {
            ThemedCard(Modifier.fillMaxWidth()) {
                LineChart(
                    series = listOf(
                        Series(
                            label = etf,
                            points = series.map { it.cashValueCents.toFloat() },
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    ),
                )
            }
        }
        item {
            ThemedCard(Modifier.fillMaxWidth()) {
                Text("${etf} Return", style = MaterialTheme.typography.titleMedium)
                val first: Long = series.firstOrNull()?.cashValueCents ?: 0L
                val last: Long = series.lastOrNull()?.cashValueCents ?: 0L
                val delta: Long = last - first
                Text(
                    Repository.money(delta),
                    style = MaterialTheme.typography.headlineMedium,
                    color = if (delta >= 0L) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun SpendImpactTab(vm: SproutViewModel, state: ChildUiState) {
    val impact = remember(state.ledger) { vm.spendImpact("VOO") }
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ThemedCard(Modifier.fillMaxWidth()) {
                LineChart(
                    series = listOf(
                        Series(
                            label = "Original",
                            points = impact.unspent.map { it.cashValueCents.toFloat() },
                            color = MaterialTheme.colorScheme.primary,
                        ),
                        Series(
                            label = "After spends",
                            points = impact.afterSpend.map { it.cashValueCents.toFloat() },
                            color = MaterialTheme.colorScheme.secondary,
                        ),
                    ),
                )
            }
        }
        item {
            ThemedCard(Modifier.fillMaxWidth()) {
                Text("Opportunity Cost", style = MaterialTheme.typography.labelMedium)
                Text(
                    Repository.money(impact.opportunityCostCents),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    "What your early spends gave up in ETF growth.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        item { Text("Fractional share sales", style = MaterialTheme.typography.titleMedium) }
        items(impact.soldBreakdown) { s ->
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "${s.etfSymbol} · ${"%.4f".format(s.sharesMicro / 1_000_000.0)} sh",
                    Modifier.weight(1f),
                )
                Text(Repository.money(s.cashCents))
            }
        }
    }
}

@Composable
private fun LearnTab(state: ChildUiState) {
    var question by remember { mutableStateOf("") }
    val goal = state.goal
    val answer: String? = remember(question, goal) {
        if (question.isBlank()) null else SproutAssistant.ask(
            question = question,
            goalCashCents = goal?.cashSavedCents ?: 0L,
            goalGrowthCents = goal?.simulatedGrowthCents ?: 0L,
            goalTargetCents = goal?.targetCents ?: 0L,
            etf = state.active?.goalEtfSymbol ?: "VOO",
        )
    }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExplainCard("VOO", "S&P 500 index, low fees, broad diversification.")
        ExplainCard("SCHG", "Growth-tilted large caps; more upside, more swing.")
        ExplainCard("VT", "Whole-world market — maximum diversification.")
        ExplainCard("VTI", "Total US market — small + mid + large caps.")
        ThemedCard(Modifier.fillMaxWidth()) {
            Text("Ask Sprout 🤖", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Ask about spending, ETFs, goals…") },
                modifier = Modifier.fillMaxWidth(),
            )
            if (answer != null) {
                Spacer(Modifier.height(8.dp))
                Text(answer, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ExplainCard(symbol: String, body: String) {
    ThemedCard(Modifier.fillMaxWidth()) {
        Text(symbol, style = MaterialTheme.typography.titleSmall)
        Text(body, style = MaterialTheme.typography.bodySmall)
    }
}

private fun daysFor(range: String): Int = when (range) {
    "7D" -> 7
    "30D" -> 30
    "90D" -> 90
    "1Y" -> 365
    else -> 10_000
}
