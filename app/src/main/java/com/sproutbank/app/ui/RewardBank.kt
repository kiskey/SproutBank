package com.sproutbank.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    val reduced by vm.reducedAnimations.collectAsState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("My Reward Bank") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { BankHero(state, reduced) }
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
                        StorybookObject(
                            when {
                                e.kind == "REVERSAL" -> StorybookObject.SPARKLE
                                e.account == "POINTS" -> StorybookObject.STAR
                                e.kind == "CREDIT" || e.kind == "BONUS" -> StorybookObject.COIN
                                else -> StorybookObject.SHOPPING_BAG
                            },
                            size = 40.dp,
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
                            Repository.ledgerAmountDisplay(e),
                            style = MaterialTheme.typography.titleSmall,
                            color = when {
                                e.kind == "REVERSAL" -> MaterialTheme.colorScheme.tertiary
                                e.account == "POINTS" -> MaterialTheme.colorScheme.secondary
                                e.amountCents > 0 -> MaterialTheme.colorScheme.primary
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
private fun BankHero(state: ChildUiState, reduced: Boolean) {
    val scheme = MaterialTheme.colorScheme
    Box(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .offset(y = 10.dp)
                .background(scheme.primary.copy(alpha = 0.18f), RoundedCornerShape(28.dp)),
        )
        Card(
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Box {
                StorybookObject(StorybookObject.PIGGY, size = 84.dp,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 12.dp, top = 12.dp))
                Column(Modifier.padding(24.dp)) {
                    Text("MY MONEY", style = MaterialTheme.typography.labelMedium,
                        color = scheme.onPrimaryContainer)
                    Text(Repository.money(state.balanceCents),
                        style = MaterialTheme.typography.displayMedium,
                        color = scheme.onPrimaryContainer)
                    Text("Available to spend or save.",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onPrimaryContainer)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StorybookObject(StorybookObject.STAR, size = 28.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("${state.points} points", style = MaterialTheme.typography.titleMedium,
                            color = scheme.onPrimaryContainer)
                    }
                }
            }
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
    val isPoints = kind == "REDEEM"
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
                    label = { Text(if (isPoints) "Points" else "Amount (USD)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPoints) KeyboardType.Number else KeyboardType.Decimal,
                    ),
                    singleLine = true,
                )
                if (cents > 0) {
                    Text(
                        if (isPoints) "You're asking to redeem $cents ⭐."
                        else "You're asking to spend ${Repository.money(cents)}.",
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = cents > 0 && title.isNotBlank(),
                onClick = {
                    val stored = if (isPoints) (amount.toLongOrNull() ?: 0L) else cents
                    onSubmit(kind, stored, title)
                }) { Text("Send Request") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
