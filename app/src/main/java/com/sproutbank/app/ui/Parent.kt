package com.sproutbank.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sproutbank.app.data.*
import com.sproutbank.app.theme.CalmTheme
import com.sproutbank.app.ui.components.*
import com.sproutbank.app.vm.SproutViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Parent hub gate (issue 4): PIN prompt on entry. When no PIN is set yet the
 * parent can create one right here (mirrors the onboarding setup) instead of
 * being silently let in — the setup prompt appears on first entry.
 */
@Composable
fun ParentGate(vm: SproutViewModel, onBack: () -> Unit) {
    var unlocked by remember { mutableStateOf(false) }
    var needsSetup by remember { mutableStateOf<Boolean?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var storedPin by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val stored = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            vm.getParentPin()
        }
        storedPin = stored
        needsSetup = stored.isNullOrEmpty()
        unlocked = stored.isNullOrEmpty()
    }
    if (needsSetup == null) return

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
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (enteredPin.length == 4 && enteredPin == storedPin) {
                        unlocked = true
                    } else {
                        enteredPin = ""
                    }
                }) { Text("Unlock") }
            },
            dismissButton = { TextButton(onClick = onBack) { Text("Cancel") } },
        )
        return
    }

    if (needsSetup == true) {
        PinSetupInlineDialog(
            onDismiss = onBack,
            onSaved = { unlocked = true },
            onSave = { pin -> vm.setParentPin(pin) },
        )
        return
    }

    ParentScreen(vm, onBack)
}

/** First-entry PIN creation when none exists yet. */
@Composable
private fun PinSetupInlineDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onSaved: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set a Parent PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Keep the Parent Hub behind a PIN so kids stay in their own world.")
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
                    label = { Text("4 digits") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = pin.length == 4, onClick = { onSave(pin); onSaved() }) {
                Text("Save & Continue")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentScreen(vm: SproutViewModel, onBack: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    val childrenAll by vm.childrenAll.collectAsState()
    val children = childrenAll.filter { !it.isArchived }
    val archived = childrenAll.filter { it.isArchived }
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
    var choreEditorTarget by remember { mutableStateOf<ChoreEntity?>(null) }
    var showNewChore by remember { mutableStateOf(false) }
    var kidEditorTarget by remember { mutableStateOf<ChildEntity?>(null) } // null = add
    var showAddKid by remember { mutableStateOf(false) }
    var showBonus by remember { mutableStateOf(false) }
    var showBackdate by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Parent Hub") },
                navigationIcon = { TextButton(onClick = onBack) { Text("← Kids") } },
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
                // ── Approvals ────────────────────────────────────────────
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
                        val kid = childrenAll.firstOrNull { it.id == c.childId }
                        if (chore != null) {
                            ThemedCard(Modifier.fillMaxWidth()) {
                                Text(
                                    "${chore.emoji} ${chore.title}${kid?.let { " · ${it.name}" } ?: ""}",
                                    style = MaterialTheme.typography.titleSmall,
                                )
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
                        val kid = childrenAll.firstOrNull { it.id == r.childId }
                        ThemedCard(Modifier.fillMaxWidth()) {
                            Text(
                                "${r.title}${kid?.let { " · ${it.name}" } ?: ""}",
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(Repository.requestAmountDisplay(r),
                                style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.approveRequest(r) }) { Text("Approve") }
                                OutlinedButton(onClick = { declineTarget = r }) { Text("Decline") }
                            }
                        }
                    }
                }

                // ── Family (issue 1 + issue 4) ──────────────────────────
                1 -> LazyColumn(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text("Family", style = MaterialTheme.typography.titleLarge)
                        Text("Add, edit or archive kids — history is always kept.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showNewChore = true }) { Text("+ Chore") }
                            OutlinedButton(onClick = { showAddKid = true }) { Text("+ Kid") }
                            OutlinedButton(onClick = { showBonus = true }) { Text("Bonus") }
                            OutlinedButton(onClick = { showBackdate = true }) { Text("Backdate") }
                        }
                    }
                    item { Text("Chores", style = MaterialTheme.typography.titleMedium) }
                    items(chores) { chore ->
                        ThemedCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ChoreIconBadge(chore.emoji, size = 40.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(chore.title, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        listOf(
                                            Repository.money(chore.rewardCents),
                                            if (chore.rewardPoints != 0L) "${chore.rewardPoints} ⭐" else "",
                                            chore.cadence.lowercase(),
                                            if (chore.assigneeIds == "[]") "everyone" else assigneeNames(chore, childrenAll),
                                        ).filter { it.isNotBlank() }.joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = { choreEditorTarget = chore }) { Text("Edit") }
                                TextButton(onClick = { vm.deleteChore(chore) }) { Text("Delete") }
                            }
                        }
                    }
                    item { Text("Kids", style = MaterialTheme.typography.titleMedium) }
                    items(children) { child ->
                        val balance = if (state.active?.id == child.id) state.balanceCents else null
                        ThemedCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(child.emoji, style = MaterialTheme.typography.headlineMedium)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(child.name, style = MaterialTheme.typography.titleMedium)
                                    if (balance != null) {
                                        Text("Cash ${Repository.money(balance)} · ${state.points} ⭐",
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                TextButton(onClick = { kidEditorTarget = child }) { Text("Edit") }
                                TextButton(onClick = { goalTarget = child }) {
                                    Text(if (child.goalName == null) "Goal" else "Edit Goal")
                                }
                                TextButton(onClick = { vm.archiveKid(child.id) }) { Text("Archive") }
                            }
                        }
                    }
                    if (archived.isNotEmpty()) {
                        item {
                            Text("Archived (history kept)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        items(archived) { child ->
                            ThemedCard(Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(child.emoji, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.width(8.dp))
                                    Text(child.name, Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium)
                                    TextButton(onClick = { vm.archiveKid(child.id, false) }) {
                                        Text("Restore")
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Ledger ───────────────────────────────────────────────
                2 -> LazyColumn(
                    Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { Text("Ledger (append-only)", style = MaterialTheme.typography.titleLarge) }
                    item {
                        Text("Every correction is a reversal that references the original. Nothing is edited or deleted. Reversing a chore credit re-opens that chore for the kid the same day.",
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

                // ── Settings ─────────────────────────────────────────────
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

    // ── Dialogs ─────────────────────────────────────────────────────────
    // Issue 7: chore rejections and spend declines are DIFFERENT categories
    // — separate titles and curated preset reasons, grouped coherently.
    declineTarget?.let { r ->
        DeclineReasonDialog(
            title = "Decline this purchase?",
            subtitle = "Pick the reason ${childrenAll.firstOrNull { it.id == r.childId }?.name ?: "your kid"} will see",
            presets = listOf(
                "Let's save a little more first",
                "Wait until the weekend",
                "That's not in our budget this month",
                "Let's talk about it at dinner",
            ),
            onDismiss = { declineTarget = null },
            onConfirm = { note -> vm.declineRequest(r, note); declineTarget = null },
        )
    }
    rejectChoreTarget?.let { c ->
        val chore = chores.firstOrNull { it.id == c.choreId }
        DeclineReasonDialog(
            title = "Return this chore?",
            subtitle = "${chore?.title ?: "Chore"} — what should they hear?",
            presets = listOf(
                "Let's try again together — almost!",
                "It needs a little more effort",
                "Not quite finished — check the corners",
                "Great try! Ask me and we'll do it side by side",
            ),
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
            subtitle = "A reversing entry is appended; the original row is never touched. Chore credits re-open the same day.",
            presets = listOf("Credited by mistake", "Wrong amount", "Duplicate", "Chore wasn't actually done"),
            onDismiss = { reverseTarget = null },
            onConfirm = { reason -> vm.reverseTransaction(e.id, reason); reverseTarget = null },
        )
    }
    if (showNewChore) {
        ChoreEditorDialog(children = children, existing = null, onDismiss = { showNewChore = false }) { chore ->
            vm.saveChore(chore); showNewChore = false
        }
    }
    choreEditorTarget?.let { target ->
        ChoreEditorDialog(children = children, existing = target, onDismiss = { choreEditorTarget = null }) { chore ->
            vm.saveChore(chore); choreEditorTarget = null
        }
    }
    if (showAddKid) {
        KidEditorDialog(existing = null, onDismiss = { showAddKid = false }) { name, emoji, bg, ring ->
            vm.addKid(name, emoji, bg, ring); showAddKid = false
        }
    }
    kidEditorTarget?.let { kid ->
        KidEditorDialog(existing = kid, onDismiss = { kidEditorTarget = null }) { name, emoji, bg, ring ->
            vm.updateChild(kid.copy(name = name, emoji = emoji, avatarBg = bg, avatarRing = ring))
            kidEditorTarget = null
        }
    }
    if (showBonus) {
        BonusDialog(
            children = children,
            onDismiss = { showBonus = false },
            onConfirm = { childId, cents, points, note ->
                vm.addBonus(childId, cents, points, note); showBonus = false
            },
        )
    }
    if (showBackdate) {
        BackdateDialog(
            children = children,
            chores = chores,
            onDismiss = { showBackdate = false },
            onConfirm = { childId, date, cents, points, note, choreId ->
                vm.backdatedCredit(childId, date, cents, points, note, choreId)
                showBackdate = false
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

private fun assigneeNames(chore: ChoreEntity, children: List<ChildEntity>): String {
    val ids = chore.assigneeIds.trim('[', ']').split(',')
        .mapNotNull { it.trim().toLongOrNull() }
    return ids.mapNotNull { id -> children.firstOrNull { it.id == id }?.name }.joinToString(", ")
}

/**
 * Issue 7 fix: one dialog component, but each call passes its own category
 * title, subtitle and curated presets — chores get effort/behavior reasons,
 * money declines get budget/timing reasons, reversals get correction reasons.
 */
@Composable
private fun DeclineReasonDialog(
    title: String,
    subtitle: String,
    presets: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var note by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                presets.forEach { p ->
                    AssistChip(
                        onClick = { note = if (note == p) null else p },
                        label = { Text(p) },
                    )
                }
                OutlinedTextField(
                    value = note ?: "",
                    onValueChange = { note = it },
                    label = { Text("Custom message / reason") },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(note?.ifBlank { null } ?: presets[0]) }) { Text("Confirm") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Issue 9: pick a kid, a day within the past 30, and optionally a chore. */
@Composable
private fun BackdateDialog(
    children: List<ChildEntity>,
    chores: List<ChoreEntity>,
    onDismiss: () -> Unit,
    onConfirm: (childId: Long, date: LocalDate, cents: Long, points: Long, note: String, choreId: Long?) -> Unit,
) {
    var childId by remember { mutableStateOf(children.firstOrNull()?.id ?: 1L) }
    var selectedDate by remember { mutableStateOf(LocalDate.now().minusDays(1)) }
    var dollars by remember { mutableStateOf("") }
    var pointsStr by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }
    var linkedChoreId by remember { mutableStateOf<Long?>(null) }

    val today = LocalDate.now()
    val oldest = today.minusDays(30)
    val days = (0..30).map { today.minusDays((30 - it).toLong()) }
    val cents = (dollars.toDoubleOrNull()?.times(100) ?: 0.0).toLong()
    val points = pointsStr.toLongOrNull() ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Backdate a Credit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Forgot to approve a chore? Credit it to the right day — the ledger stays traceable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    children.forEach {
                        FilterChip(selected = childId == it.id, onClick = { childId = it.id }, label = { Text(it.name) })
                    }
                }
                Text("Day (past 30 days)", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(days.reversed()) { d ->
                        FilterChip(
                            selected = d == selectedDate,
                            onClick = { selectedDate = d },
                            label = { Text(d.format(DateTimeFormatter.ofPattern("MMM d"))) },
                        )
                    }
                }
                Text(
                    "Selected: ${selectedDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dollars, onValueChange = { dollars = it },
                        label = { Text("Amount ($)") },
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
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Note (shows in history)") }, singleLine = true,
                )
                Text("Link to a chore (optional)", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(chores) { chore ->
                        FilterChip(
                            selected = linkedChoreId == chore.id,
                            onClick = { linkedChoreId = if (linkedChoreId == chore.id) null else chore.id },
                            label = { Text("${chore.emoji} ${chore.title}") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedDate in oldest..today && (cents > 0 || points > 0),
                onClick = {
                    onConfirm(childId, selectedDate, cents, points,
                        note.ifBlank { "Backdated credit" }, linkedChoreId)
                },
            ) { Text("Record") }
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
                OutlinedTextField(value = cents, onValueChange = { cents = it },
                    label = { Text("Money cents") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = points, onValueChange = { points = it },
                    label = { Text("Points") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
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
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
        },
        confirmButton = {
            TextButton(enabled = pin.length == 4, onClick = { onConfirm(pin) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
