package com.sproutbank.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sproutbank.app.data.*
import com.sproutbank.app.theme.CalmTheme
import com.sproutbank.app.ui.components.GoalDialog
import com.sproutbank.app.ui.components.ThemedCard
import com.sproutbank.app.vm.SproutViewModel

@Composable
fun ParentGate(vm: SproutViewModel, onBack: () -> Unit) {
    var unlocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var storedPin by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        storedPin = vm.getParentPin()
        if (storedPin.isNullOrEmpty()) unlocked = true
    }
    if (storedPin == null) return

    if (!unlocked) {
        AlertDialog(
            onDismissRequest = onBack,
            title = { Text("Parent Access") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter your 4-digit PIN")
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) enteredPin = it },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (enteredPin == storedPin) unlocked = true else enteredPin = ""
                }) { Text("Unlock") }
            },
            dismissButton = { TextButton(onClick = onBack) { Text("Cancel") } },
        )
        return
    }

    ParentScreen(vm, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentScreen(vm: SproutViewModel, onBack: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    val children by vm.children.collectAsState()
    val requests by vm.allRequests.collectAsState()
    val completions by vm.allCompletions.collectAsState()
    val chores by vm.allChores.collectAsState()
    val themeId by vm.themeId.collectAsState()
    val reduced by vm.reducedAnimations.collectAsState()
    val state by vm.uiState.collectAsState()

    var declineTarget by remember { mutableStateOf<SpendRequestEntity?>(null) }
    var rejectChoreTarget by remember { mutableStateOf<ChoreCompletionEntity?>(null) }
    var goalTarget by remember { mutableStateOf<ChildEntity?>(null) }
    var reverseTarget by remember { mutableStateOf<LedgerEntity?>(null) }
    var showAddChore by remember { mutableStateOf(false) }
    var showBonus by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Parent Hub") },
                navigationIcon = { TextButton(onClick = onBack) { Text("←") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            TabRow(selectedTabIndex = tab, containerColor = Color.Transparent) {
                listOf("Approvals", "Family", "Ledger", "Settings").forEachIndexed { i, l ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(l) })
                }
            }
            when (tab) {
                0 -> LazyColumn(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { Text("Needs Your Attention", style = MaterialTheme.typography.titleLarge) }
                    item {
                        Text(
                            "${completions.count { it.status == "PENDING" } + requests.count { it.status == "PENDING" }} items waiting",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item { Text("Chore Approvals", style = MaterialTheme.typography.titleMedium) }
                    items(completions.filter { it.status == "PENDING" }) { c ->
                        val chore = chores.firstOrNull { it.id == c.choreId }
                        if (chore != null) {
                            ThemedCard(Modifier.fillMaxWidth()) {
                                Text("${chore.emoji} ${chore.title}", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    Repository.money(chore.rewardCents) +
                                        if (chore.rewardPoints > 0) " · ${chore.rewardPoints} ⭐" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { vm.approveChore(c) }) { Text("Approve") }
                                    OutlinedButton(onClick = { rejectChoreTarget = c }) { Text("Reject") }
                                }
                            }
                        }
                    }
                    item { Text("Spend Requests", style = MaterialTheme.typography.titleMedium) }
                    items(requests.filter { it.status == "PENDING" }) { r ->
                        ThemedCard(Modifier.fillMaxWidth()) {
                            Text(r.title, style = MaterialTheme.typography.titleSmall)
                            Text(Repository.requestAmountDisplay(r),
                                style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.approveRequest(r) }) { Text("Approve") }
                                OutlinedButton(onClick = { declineTarget = r }) { Text("Decline") }
                            }
                        }
                    }
                }
                1 -> LazyColumn(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showAddChore = true }) { Text("+ Add Chore") }
                            OutlinedButton(onClick = { showBonus = true }) { Text("Give Bonus") }
                        }
                    }
                    items(children) { child ->
                        ThemedCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(child.emoji, style = MaterialTheme.typography.headlineMedium)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(child.name, style = MaterialTheme.typography.titleMedium)
                                    if (state.active?.id == child.id) {
                                        Text(
                                            "Cash ${Repository.money(state.balanceCents)} · ${state.points} ⭐",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                                OutlinedButton(onClick = { goalTarget = child }) {
                                    Text(if (child.goalName == null) "Set Goal" else "Edit Goal")
                                }
                            }
                        }
                    }
                }
                2 -> LazyColumn(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { Text("Ledger (append-only)", style = MaterialTheme.typography.titleLarge) }
                    item {
                        Text("Every correction is a reversal that references the original. Nothing is edited or deleted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    items(state.ledger.reversed()) { e ->
                        ThemedCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("${e.kind} · ${e.account}",
                                        style = MaterialTheme.typography.titleSmall)
                                    Text(e.note.ifBlank { "—" },
                                        style = MaterialTheme.typography.bodySmall)
                                    Text("tx ${e.transactionId.take(8)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (e.reversesId != null) {
                                        Text("↩ reverses #${e.reversesId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.tertiary)
                                    }
                                }
                                Text(Repository.ledgerAmountDisplay(e),
                                    style = MaterialTheme.typography.titleSmall)
                                if (e.kind != "REVERSAL") {
                                    Spacer(Modifier.width(8.dp))
                                    OutlinedButton(onClick = { reverseTarget = e }) { Text("Reverse") }
                                }
                            }
                        }
                    }
                }
                else -> LazyColumn(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { Text("Themes", style = MaterialTheme.typography.titleMedium) }
                    items(CalmTheme.values().size) { i ->
                        val t = CalmTheme.values()[i]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = themeId == t.id, onClick = { vm.selectTheme(t.id) })
                            Text(t.label)
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Reduced animations", Modifier.weight(1f))
                            Switch(checked = reduced, onCheckedChange = { vm.setReducedAnimations(it) })
                        }
                    }
                    item {
                        OutlinedButton(onClick = { showPinSetup = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Set / Change Parent PIN")
                        }
                    }
                }
            }
        }
    }

    declineTarget?.let { r ->
        DeclineReasonDialog(
            title = "Why is this declined?",
            onDismiss = { declineTarget = null },
            onConfirm = { note -> vm.declineRequest(r, note); declineTarget = null },
        )
    }
    rejectChoreTarget?.let { c ->
        DeclineReasonDialog(
            title = "Why is this rejected?",
            onDismiss = { rejectChoreTarget = null },
            onConfirm = { note -> vm.rejectChore(c, note); rejectChoreTarget = null },
        )
    }
    goalTarget?.let { c ->
        GoalDialog(
            child = c,
            onDismiss = { goalTarget = null },
            onConfirm = { n, e, t, etf -> vm.setChildGoal(c, n, e, t, etf); goalTarget = null },
        )
    }
    reverseTarget?.let { e ->
        DeclineReasonDialog(
            title = "Reverse this transaction?",
            onDismiss = { reverseTarget = null },
            onConfirm = { reason -> vm.reverseTransaction(e.id, reason); reverseTarget = null },
        )
    }
    if (showAddChore) {
        AddChoreDialog(
            children = children,
            onDismiss = { showAddChore = false },
            onConfirm = { childId, title, emoji, cents, points ->
                vm.addChore(childId, title, emoji, cents, points)
                showAddChore = false
            },
        )
    }
    if (showBonus) {
        BonusDialog(
            children = children,
            onDismiss = { showBonus = false },
            onConfirm = { childId, cents, points, note ->
                vm.addBonus(childId, cents, points, note)
                showBonus = false
            },
        )
    }
    if (showPinSetup) {
        PinSetupDialog(
            onDismiss = { showPinSetup = false },
            onConfirm = { pin -> vm.setParentPin(pin); showPinSetup = false },
        )
    }
}

@Composable
private fun DeclineReasonDialog(title: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var note by remember { mutableStateOf("") }
    val presets = listOf("Let's save a little more first", "Wait until the weekend", "Needs a little more effort")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { p -> AssistChip(onClick = { note = p }, label = { Text(p) }) }
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Custom message / reason") })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(note.ifBlank { presets[0] }) }) { Text("Confirm") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun AddChoreDialog(
    children: List<ChildEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, String, String, Long, Long) -> Unit,
) {
    var childId by remember { mutableStateOf(children.firstOrNull()?.id ?: 1L) }
    var title by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🛏️") }
    var cents by remember { mutableStateOf("100") }
    var points by remember { mutableStateOf("0") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Chore") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    children.forEach {
                        FilterChip(selected = childId == it.id, onClick = { childId = it.id }, label = { Text(it.name) })
                    }
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                OutlinedTextField(value = emoji, onValueChange = { if (it.length <= 2) emoji = it }, label = { Text("Emoji") })
                OutlinedTextField(value = cents, onValueChange = { cents = it }, label = { Text("Money cents") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = points, onValueChange = { points = it }, label = { Text("Points") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            }
        },
        confirmButton = {
            TextButton(enabled = title.isNotBlank(), onClick = {
                onConfirm(childId, title, emoji, cents.toLongOrNull() ?: 0L, points.toLongOrNull() ?: 0L)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun BonusDialog(
    children: List<ChildEntity>,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long, Long, String) -> Unit,
) {
    var childId by remember { mutableStateOf(children.firstOrNull()?.id ?: 1L) }
    var cents by remember { mutableStateOf("") }
    var points by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Give Bonus") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    children.forEach {
                        FilterChip(selected = childId == it.id, onClick = { childId = it.id }, label = { Text(it.name) })
                    }
                }
                OutlinedTextField(value = cents, onValueChange = { cents = it }, label = { Text("Money cents") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = points, onValueChange = { points = it }, label = { Text("Points") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Note") })
            }
        },
        confirmButton = {
            TextButton(enabled = (cents.toLongOrNull() ?: 0L) > 0 || (points.toLongOrNull() ?: 0L) > 0,
                onClick = { onConfirm(childId, cents.toLongOrNull() ?: 0L, points.toLongOrNull() ?: 0L, note) }) {
                Text("Give")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PinSetupDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Parent PIN") },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
                label = { Text("4 digits") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
        },
        confirmButton = {
            TextButton(enabled = pin.length == 4, onClick = { onConfirm(pin) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
