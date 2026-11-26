package com.sproutbank.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sproutbank.app.data.Repository
import com.sproutbank.app.ui.components.BankEntryDialog
import com.sproutbank.app.vm.ChildUiState
import com.sproutbank.app.vm.SproutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardBankScreen(vm: SproutViewModel, state: ChildUiState) {
    var showEntry by remember { mutableStateOf(false) }
    var showRequest by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Reward Bank") },
            actions = {
                TextButton(onClick = { showEntry = true }) { Text("Parent Entry") }
            })
    }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Balance", style = MaterialTheme.typography.labelMedium)
                        Text(Repository.money(state.balanceCents),
                            style = MaterialTheme.typography.headlineLarge)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showRequest = true }) { Text("Request Spend") }
                }
            }
            item { Text("Request History", style = MaterialTheme.typography.titleMedium) }
            items(state.requests) { req ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(req.title, style = MaterialTheme.typography.titleSmall)
                        Text("${Repository.money(req.amountCents)} · ${req.kind}",
                            style = MaterialTheme.typography.bodySmall)
                        Text(
                            when (req.status) {
                                "PENDING" -> "⏳ Waiting for parent"
                                "APPROVED" -> "✅ Approved!"
                                else -> "💬 Not this time"
                            }
                        )
                        if (req.status == "DECLINED" && req.decisionNote != null) {
                            Text("Parent: ${req.decisionNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { Text("Ledger", style = MaterialTheme.typography.titleMedium) }
            items(state.ledger.reversed()) { e ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${e.kind} · ${e.note}", Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium)
                    Text(Repository.money(e.amountCents),
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    if (showEntry) BankEntryDialog(
        onDismiss = { showEntry = false },
        onSubmit = { k, a, n -> vm.recordCredit(k, a, n); showEntry = false },
    )
    if (showRequest) RequestSpendDialog(
        onDismiss = { showRequest = false },
        onRequest = { kind, cents, title ->
            vm.requestSpend(kind, cents, title); showRequest = false
        },
    )
}

@Composable
private fun RequestSpendDialog(
    onDismiss: () -> Unit,
    onRequest: (String, Long, String) -> Unit,
) {
    var kind by remember { mutableStateOf("SPEND") }
    var amountStr by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    val cents = (amountStr.toDoubleOrNull()?.times(100) ?: 0.0).toLong()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Spend") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("SPEND", "REDEEM").forEach {
                        FilterChip(selected = kind == it, onClick = { kind = it }, label = { Text(it) })
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("What for?") }, singleLine = true)
                OutlinedTextField(value = amountStr, onValueChange = { amountStr = it },
                    label = { Text("Amount (USD)") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(enabled = cents > 0 && title.isNotBlank(),
                onClick = { onRequest(kind, cents, title) }) { Text("Send to Parent") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
