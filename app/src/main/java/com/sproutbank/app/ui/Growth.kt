package com.sproutbank.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sproutbank.app.data.MarketData
import com.sproutbank.app.data.Repository
import com.sproutbank.app.ui.components.LineChart
import com.sproutbank.app.ui.components.Series
import com.sproutbank.app.vm.ChildUiState
import com.sproutbank.app.vm.SproutViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
            else -> LearnTab()
        }
    }
}

@Composable
private fun ChartsTab(vm: SproutViewModel, state: ChildUiState) {
    var etf by remember { mutableStateOf("VOO") }
    var range by remember { mutableStateOf("90D") }
    val series = remember(etf, range, state.ledger) {
        runCatching {
            val all = vm.spendImpact(etf).unspent
            all.takeLast(daysFor(range))
        }.getOrDefault(emptyList())
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
            LineChart(
                series = listOf(Series(etf, series.map { it.cashValueCents.toFloat() },
                    MaterialTheme.colorScheme.primary)),
            )
        }
        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("${etf} Return",
                        style = MaterialTheme.typography.titleMedium)
                    val first = series.firstOrNull()?.cashValueCents ?: 0
                    val last = series.lastOrNull()?.cashValueCents ?: 0
                    val delta = last - first
                    Text("${Repository.money(delta)}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (delta >= 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error)
                }
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
            LineChart(
                series = listOf(
                    Series("Original", impact.unspent.map { it.cashValueCents.toFloat() },
                        MaterialTheme.colorScheme.primary),
                    Series("After spends", impact.afterSpend.map { it.cashValueCents.toFloat() },
                        MaterialTheme.colorScheme.secondary),
                )
            )
        }
        item {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Opportunity Cost",
                        style = MaterialTheme.typography.labelMedium)
                    Text(Repository.money(impact.opportunityCostCents),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.secondary)
                    Text("What your early spends gave up in ETF growth.",
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("Fractional share sales", style = MaterialTheme.typography.titleMedium) }
        items(impact.soldBreakdown) { s ->
            Row(Modifier.fillMaxWidth()) {
                Text("${s.etfSymbol} · ${"%.4f".format(s.sharesMicro / 1_000_000.0)} sh",
                    Modifier.weight(1f))
                Text(Repository.money(s.cashCents))
            }
        }
    }
}

@Composable
private fun LearnTab() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ExplainCard("VOO", "S&P 500 index, low fees, broad diversification.")
        ExplainCard("SCHG", "Growth-tilted large caps; more upside, more swing.")
        ExplainCard("VT", "Whole-world market — maximum diversification.")
        ExplainCard("VTI", "Total US market — small + mid + large caps.")
        Card(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Ask Sprout 🤖", style = MaterialTheme.typography.titleMedium)
                Text("Why does spending early cost more than the amount I spend?",
                    style = MaterialTheme.typography.bodySmall)
                Text("Because the money you sell loses all future compounding.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

@Composable
private fun ExplainCard(symbol: String, body: String) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(symbol, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun daysFor(range: String) = when (range) {
    "7D" -> 7; "30D" -> 30; "90D" -> 90; "1Y" -> 365; else -> 10_000
}
