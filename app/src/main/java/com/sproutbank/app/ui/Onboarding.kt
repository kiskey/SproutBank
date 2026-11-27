package com.sproutbank.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sproutbank.app.data.ChoreEntity
import com.sproutbank.app.ui.components.*
import com.sproutbank.app.vm.SproutViewModel

/**
 * First-launch onboarding wizard (issue 10). Steps:
 *   1. Welcome + parent PIN
 *   2. Add kids (name + avatar, repeatable)
 *   3. Chores: suggested library (tap to include / remove), values editable,
 *      optional deductions
 *   4. Done → settings.onboarded = true, then ProfileSelect.
 *
 * Nothing is seeded into the DB before the parent confirms here; the DB seed
 * only runs for legacy installs upgrading from a pre-redesign version.
 */
@Composable
fun OnboardingScreen(vm: SproutViewModel) {
    var step by remember { mutableStateOf(0) }
    val children by vm.childrenAll.collectAsState()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LinearProgressIndicator(
            progress = { (step + 1) / 4f },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        when (step) {
            0 -> WelcomeStep(vm, onNext = { step = 1 })
            1 -> KidsStep(vm, onNext = { step = 2 })
            2 -> ChoresStep(vm, kids = children, onNext = { step = 3 })
            else -> DoneStep(vm)
        }
    }
}

@Composable
private fun WelcomeStep(vm: SproutViewModel, onNext: () -> Unit) {
    val reduced by vm.reducedAnimations.collectAsState()
    var pin by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Sprout(mood = SproutMood.HAPPY, size = 120.dp, reduced = reduced)
        Spacer(Modifier.height(16.dp))
        Text("Welcome to SproutBank!", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        Text("Little steps. Big dreams.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        ThemedCard(Modifier.fillMaxWidth()) {
            Text("First, set a grown-up PIN.", style = MaterialTheme.typography.titleSmall)
            Text("You'll use it to open the Parent Hub.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pin = it },
                label = { Text("4-digit PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = pin.length == 4,
            onClick = { vm.setParentPin(pin); onNext() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continue") }
    }
}

@Composable
private fun KidsStep(vm: SproutViewModel, onNext: () -> Unit) {
    val children by vm.childrenAll.collectAsState()
    var showEditor by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Text("Who's in the family?", style = MaterialTheme.typography.headlineSmall)
        Text("Add each kid — name and a fun avatar.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(children) { kid ->
                ThemedCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarCircle(
                            kid.emoji,
                            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(kid.avatarBg)),
                            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(kid.avatarRing)),
                            size = 48.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(kid.name, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { showEditor = true }, modifier = Modifier.fillMaxWidth()) {
            Text("+ Add Kid")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            enabled = children.isNotEmpty(),
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continue") }
    }

    if (showEditor) {
        KidEditorDialog(existing = null, onDismiss = { showEditor = false }) { name, emoji, bg, ring ->
            vm.addKid(name, emoji, bg, ring)
            showEditor = false
        }
    }
}

/** Suggested starter chores shown in onboarding, editable before saving. */
private data class SuggestedChore(
    val title: String,
    val emoji: String,
    val cents: Long,
    val points: Long,
)

private val SUGGESTED = listOf(
    SuggestedChore("Make My Bed", "🛏️", 100, 0),
    SuggestedChore("Brush My Teeth", "🪥", 0, 10),
    SuggestedChore("Read a Book", "📚", 50, 5),
    SuggestedChore("Feed the Pet", "🐾", 75, 0),
    SuggestedChore("Water the Plants", "🪴", 0, 8),
    SuggestedChore("Tidy My Toys", "🧸", 50, 3),
    SuggestedChore("Take Out Trash", "🗑️", 75, 0),
    SuggestedChore("Homework Done", "✏️", 150, 0),
)

@Composable
private fun ChoresStep(vm: SproutViewModel, kids: List<com.sproutbank.app.data.ChildEntity>, onNext: () -> Unit) {
    val savedChores by vm.allChores.collectAsState()
    // Local selection of suggestions; toggling saves/edits immediately via VM.
    var selected by remember { mutableStateOf(SUGGESTED.map { false }) }
    var editorTarget by remember { mutableStateOf<ChoreEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Text("Pick the starter chores", style = MaterialTheme.typography.headlineSmall)
        Text("Tap to include. Values can be edited later in Parent Hub — including deductions.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(SUGGESTED.size) { i ->
                val s = SUGGESTED[i]
                ThemedCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChoreIconBadge(s.emoji, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                listOfNotNull(
                                    if (s.cents > 0) "+${com.sproutbank.app.data.Repository.money(s.cents)}" else null,
                                    if (s.points > 0) "+${s.points} ⭐" else null,
                                ).joinToString(" · ").ifBlank { "—" },
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        Checkbox(checked = selected[i], onCheckedChange = { checked ->
                            selected = selected.toMutableList().also { it[i] = checked }
                            if (checked) {
                                vm.saveChore(ChoreEntity(
                                    childId = kids.firstOrNull()?.id ?: 0L,
                                    title = s.title, emoji = s.emoji,
                                    rewardCents = s.cents, rewardPoints = s.points,
                                ))
                            } else {
                                savedChores.firstOrNull { it.title == s.title }?.let { vm.deleteChore(it) }
                            }
                        })
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showEditor = true }, modifier = Modifier.weight(1f)) {
                Text("+ Custom Chore")
            }
            Button(onClick = onNext, modifier = Modifier.weight(1f)) {
                Text("Continue")
            }
        }
    }

    if (showEditor) {
        ChoreEditorDialog(children = kids, existing = null, onDismiss = { showEditor = false }) { chore ->
            vm.saveChore(chore)
            showEditor = false
        }
    }
}

@Composable
private fun DoneStep(vm: SproutViewModel) {
    val reduced by vm.reducedAnimations.collectAsState()
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Sprout(mood = SproutMood.CELEBRATING, size = 120.dp, reduced = reduced)
        Spacer(Modifier.height(16.dp))
        Text("All set! 🎉", style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary)
        Text("Hand the device to your kid — they pick their profile and start earning.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Button(onClick = { vm.completeOnboarding() }, modifier = Modifier.fillMaxWidth()) {
            Text("Start SproutBank")
        }
    }
}
