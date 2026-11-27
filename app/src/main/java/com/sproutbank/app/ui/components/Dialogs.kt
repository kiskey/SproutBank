package com.sproutbank.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sproutbank.app.data.ChildEntity
import com.sproutbank.app.data.SpendRequestEntity
import com.sproutbank.app.data.MarketData

// Color.toArgb() returns Int; avoids the ULong/Int mismatch from `value shr 32`.
internal fun colorToHex(c: androidx.compose.ui.graphics.Color): String {
    val argb = c.toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "#%02X%02X%02X".format(r, g, b)
}

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

/**
 * Kid add/edit dialog (issue 1): name + avatar picker + colors, all saved as
 * one ChildEntity upsert.
 */
@Composable
fun KidEditorDialog(
    existing: ChildEntity?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, bg: String, ring: String) -> Unit,
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var pickedEmoji by remember { mutableStateOf(existing?.emoji ?: "🦊") }
    var pickedBg by remember { mutableStateOf(existing?.avatarBg ?: "#DDF5EC") }
    var pickedRing by remember { mutableStateOf(existing?.avatarRing ?: "#0D9488") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add a Kid" else "Edit ${existing.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true,
                )
                AvatarCircle(
                    pickedEmoji,
                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(pickedBg)),
                    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(pickedRing)),
                    size = 64.dp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                AvatarPickerGrid(pickedEmoji) { a ->
                    pickedEmoji = a.emoji
                    pickedBg = colorToHex(a.bgColor)
                    pickedRing = colorToHex(a.ringColor)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                onConfirm(name.trim(), pickedEmoji, pickedBg, pickedRing)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * Chore editor (issue 4): icon library instead of an emoji text field, values
 * with money + points, and multi-kid assignment (empty selection = the whole
 * family). Supports deductions (negative values, issue 10) via a toggle.
 */
@Composable
fun ChoreEditorDialog(
    children: List<ChildEntity>,
    existing: com.sproutbank.app.data.ChoreEntity?,
    onDismiss: () -> Unit,
    onConfirm: (chore: com.sproutbank.app.data.ChoreEntity) -> Unit,
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var emoji by remember { mutableStateOf(existing?.emoji ?: "🛏️") }
    var dollars by remember {
        mutableStateOf(((existing?.rewardCents ?: 100L) / 100.0)
            .let { if (it == 0.0) "" else "%.2f".format(it) })
    }
    var pointsStr by remember { mutableStateOf(existing?.rewardPoints?.toString() ?: "0") }
    var cadence by remember { mutableStateOf(existing?.cadence ?: "DAILY") }
    var isDeduction by remember {
        mutableStateOf((existing?.rewardCents ?: 0L) < 0 || (existing?.rewardPoints ?: 0L) < 0)
    }
    // assigneeIds is stored as JSON; [] means "everyone".
    var selectedKids by remember {
        mutableStateOf(existing?.assigneeIds
            ?.trim('[', ']')?.split(',')?.mapNotNull { it.trim().toLongOrNull() }?.toSet()
            ?: emptySet())
    }

    val cents = ((dollars.toDoubleOrNull() ?: 0.0) * 100).toLong().let {
        if (isDeduction) -kotlin.math.abs(it) else kotlin.math.abs(it)
    }
    val points = (pointsStr.toLongOrNull() ?: 0L).let {
        if (isDeduction) -kotlin.math.abs(it) else kotlin.math.abs(it)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Chore" else "Edit Chore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ChoreIconPicker(selected = emoji, onPick = { emoji = it.emoji })
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Chore name") }, singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dollars, onValueChange = { dollars = it },
                        label = { Text(if (isDeduction) "Deduction ($)" else "Reward ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true, modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = pointsStr, onValueChange = { pointsStr = it },
                        label = { Text("Points ⭐") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true, modifier = Modifier.weight(1f),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(selected = !isDeduction, onClick = { isDeduction = false },
                        label = { Text("Earn") })
                    Spacer(Modifier.width(8.dp))
                    FilterChip(selected = isDeduction, onClick = { isDeduction = true },
                        label = { Text("Deduction") })
                }
                Text("Repeat", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = cadence == "DAILY", onClick = { cadence = "DAILY" },
                        label = { Text("Daily") })
                    FilterChip(selected = cadence == "WEEKLY", onClick = { cadence = "WEEKLY" },
                        label = { Text("Weekly") })
                }
                Text("Who does it?", style = MaterialTheme.typography.labelLarge)
                Text("(none selected = everyone)", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    children.forEach { kid ->
                        FilterChip(
                            selected = kid.id in selectedKids,
                            onClick = {
                                selectedKids = if (kid.id in selectedKids)
                                    selectedKids - kid.id else selectedKids + kid.id
                            },
                            label = { Text(kid.name) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank() && (cents != 0L || points != 0L), onClick = {
                val assigneeJson = selectedKids.sorted().joinToString(prefix = "[", postfix = "]")
                onConfirm(
                    com.sproutbank.app.data.ChoreEntity(
                        id = existing?.id ?: 0L,
                        childId = existing?.childId ?: children.firstOrNull()?.id ?: 0L,
                        title = title.trim(),
                        emoji = emoji,
                        rewardCents = cents,
                        rewardPoints = points,
                        assigneeIds = assigneeJson,
                        cadence = cadence,
                    )
                )
            }) { Text(if (existing == null) "Add" else "Save") }
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




