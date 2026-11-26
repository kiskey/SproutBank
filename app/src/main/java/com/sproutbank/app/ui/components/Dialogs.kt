package com.sproutbank.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sproutbank.app.data.ChildEntity
import com.sproutbank.app.data.SpendRequestEntity
import com.sproutbank.app.data.MarketData

@Composable
fun GoalDialog(
    child: ChildEntity,
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, targetCents: Long, etf: String) -> Unit,
) {
    var name by remember { mutableStateOf(child.goalName ?: "") }
    var emoji by remember { mutableStateOf(child.goalEmoji) }
    var targetStr by remember { mutableStateOf((child.goalTargetCents / 100).toString()) }
    var etf by remember { mutableStateOf(child.goalEtfSymbol) }
    val targetCents = (targetStr.toDoubleOrNull()?.times(100) ?: 0.0).toLong()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set ${child.name}'s Savings Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Goal name") }, singleLine = true)
                OutlinedTextField(value = emoji, onValueChange = { if (it.length <= 2) emoji = it },
                    label = { Text("Emoji") }, singleLine = true)
                OutlinedTextField(
                    value = targetStr, onValueChange = { targetStr = it },
                    label = { Text("Target amount (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Text("Tracked ETF", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MarketData.trackedEtfs().forEach {
                        FilterChip(selected = etf == it, onClick = { etf = it }, label = { Text(it) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank() && targetCents > 0,
                onClick = { onConfirm(name, emoji, targetCents, etf) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun DeclineDialog(
    request: SpendRequestEntity,
    onDismiss: () -> Unit,
    onDecline: (note: String) -> Unit,
) {
    var note by remember { mutableStateOf("") }
    val presets = listOf(
        "Let's save a little more first",
        "Wait until the weekend",
        "Needs a little more chore progress",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Why is this declined?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Pick a quick reason or write your own:")
                presets.forEach { p ->
                    AssistChip(onClick = { note = p }, label = { Text(p) })
                }
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Custom message") }, minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDecline(note.ifBlank { presets[0] }) }) { Text("Decline") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
fun BankEntryDialog(
    onDismiss: () -> Unit,
    onSubmit: (kind: String, amountCents: Long, note: String) -> Unit,
) {
    var kind by remember { mutableStateOf("BONUS") }
    var amountStr by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val amountCents = (amountStr.toDoubleOrNull()?.times(100) ?: 0.0).toLong()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("BONUS", "DEPOSIT", "SPEND", "REDEEM").forEach {
                        FilterChip(selected = kind == it, onClick = { kind = it }, label = { Text(it) })
                    }
                }
                OutlinedTextField(
                    value = amountStr, onValueChange = { amountStr = it },
                    label = { Text("Amount (USD)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(value = note, onValueChange = { note = it },
                    label = { Text("Note") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(enabled = amountCents > 0,
                onClick = { onSubmit(kind, amountCents, note) }) { Text("Record") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
