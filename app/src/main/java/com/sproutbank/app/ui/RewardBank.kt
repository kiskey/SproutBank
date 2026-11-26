package com.sproutbank.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sproutbank.app.data.Repository
import com.sproutbank.app.ui.components.*
import com.sproutbank.app.vm.ChildUiState
import com.sproutbank.app.vm.SproutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardBankScreen(vm: SproutViewModel, state: ChildUiState) {
    var showRequest by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { Text("My Reward Bank") }) }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { BankHero(state) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { showRequest = true }, modifier = Modifier.weight(1f)) {
                        Text("🎁 Request Spend")
                    }
                    OutlinedButton(onClick = { showRequest = true }, modifier = Modifier.weight(1f)) {
                        Text("⭐ Redeem Points")
                    }
                }
            }
            item { Text("Request History", style = MaterialTheme.typography.titleLarge) }
            if (state.requests.isEmpty()) {
                item { Text("No requests yet.", style = MaterialTheme.typography.bodyMedium) }
            }
            items(state.requests) { req -> RequestStatusRow(req) }

            item { Text("Recent Transactions", style = MaterialTheme.typography.titleLarge) }
            items(state.ledger.reversed()) { e ->
                ThemedCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            when (e.kind) {
                                "CREDIT", "BONUS" -> "🟢"
                                "POINTS" -> "⭐"
                                else -> "🛍️"
                            },
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.note.ifBlank { e.kind }, style = MaterialTheme.typography.titleSmall)
                            Text(
                                java.text.SimpleDateFormat("MMM d", java.util.Locale.getDefault())
                                    .format(java.util.Date(e.timestampMs)),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            if (e.kind == "POINTS") "${e.amountCents} ⭐"
                            else Repository.money(if (e.kind == "SPEND" || e.kind == "REDEEM") -e.amountCents else e.amountCents),
                            style = MaterialTheme.typography.titleSmall,
                            color = when (e.kind) {
                                "CREDIT", "BONUS" -> MaterialTheme.colorScheme.primary
                                "POINTS" -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }

    if (showRequest) {
        RequestSpendDialog(
            onDismiss = { showRequest = false },
            onSubmit = { kind, cents, title -> vm.requestSpend(kind, cents, title); showRequest = false },
        )
    }
}

@Composable
private fun BankHero(state: ChildUiState) {
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
                    Text("Available to spend or save.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("${state.points} ⭐ points",
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun RequestSpendDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, Long, String) -> Unit,
) {
    var kind by remember { mutableStateOf("SPEND") }
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    val cents = (amount.toDoubleOrNull()?.times(100) ?: 0.0).toLong()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What would you like to spend on?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = kind == "SPEND", onClick = { kind = "SPEND" }, label = { Text("Spend") })
                    FilterChip(selected = kind == "REDEEM", onClick = { kind = "REDEEM" }, label = { Text("Redeem") })
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Item name") }, singleLine = true)
                OutlinedTextField(
                    value = amount, onValueChange = { amount = it },
                    label = { Text("Amount (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                if (cents > 0) Text("You're asking to spend ${Repository.money(cents)}.")
            }
        },
        confirmButton = {
            TextButton(enabled = cents > 0 && title.isNotBlank(),
                onClick = { onSubmit(kind, cents, title) }) { Text("Send Request") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
