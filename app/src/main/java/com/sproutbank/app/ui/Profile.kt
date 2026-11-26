package com.sproutbank.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.sproutbank.app.theme.CalmTheme
import com.sproutbank.app.ui.components.*
import com.sproutbank.app.vm.ChildUiState
import com.sproutbank.app.vm.SproutViewModel

@Composable
fun ProfileScreen(
    vm: SproutViewModel,
    state: ChildUiState,
    onLeaveChild: () -> Unit,
    onOpenParent: () -> Unit,
) {
    val active = state.active ?: return
    val themeId by vm.themeId.collectAsState()
    val reduced by vm.reducedAnimations.collectAsState()
    var showAvatarPicker by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ThemedCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarCircle(
                        active.emoji,
                        Color(android.graphics.Color.parseColor(active.avatarBg)),
                        Color(android.graphics.Color.parseColor(active.avatarRing)),
                        size = 88.dp,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(active.name, style = MaterialTheme.typography.headlineMedium)
                        Text("Tap to change your picture",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { showAvatarPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Change Avatar")
                }
            }
        }

        item { Text("My Color Garden", style = MaterialTheme.typography.titleLarge) }
        items(CalmTheme.values().size) { i ->
            val t = CalmTheme.values()[i]
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .tactileClickable(reduced = reduced) { vm.selectTheme(t.id) },
                colors = CardDefaults.cardColors(
                    containerColor = if (themeId == t.id) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                ),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = themeId == t.id, onClick = { vm.selectTheme(t.id) })
                    Text(t.label, style = MaterialTheme.typography.titleSmall)
                }
            }
        }

        item {
            ThemedCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Reduced animations", style = MaterialTheme.typography.titleSmall)
                        Text("Less movement, same rewards",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = reduced, onCheckedChange = { vm.setReducedAnimations(it) })
                }
            }
        }

        item {
            OutlinedButton(onClick = onOpenParent, modifier = Modifier.fillMaxWidth()) {
                Text("🔒 Open Parent Hub")
            }
        }
        item {
            OutlinedButton(onClick = onLeaveChild, modifier = Modifier.fillMaxWidth()) {
                Text("Switch Profile")
            }
        }
    }

    if (showAvatarPicker) {
        var pickedEmoji by remember { mutableStateOf(active.emoji) }
        var pickedBg by remember { mutableStateOf(active.avatarBg) }
        var pickedRing by remember { mutableStateOf(active.avatarRing) }
        AlertDialog(
            onDismissRequest = { showAvatarPicker = false },
            title = { Text("Choose your avatar") },
            text = {
                Column {
                    AvatarCircle(
                        pickedEmoji,
                        Color(android.graphics.Color.parseColor(pickedBg)),
                        Color(android.graphics.Color.parseColor(pickedRing)),
                        size = 96.dp,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    Spacer(Modifier.height(12.dp))
                    AvatarPickerGrid(pickedEmoji) { a ->
                        pickedEmoji = a.emoji
                        pickedBg = colorToHex(a.bgColor)
                        pickedRing = colorToHex(a.ringColor)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.updateAvatar(active, pickedEmoji, pickedBg, pickedRing)
                    showAvatarPicker = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAvatarPicker = false }) { Text("Cancel") } },
        )
    }
}

// Color.toArgb() returns Int; avoids the ULong/Int mismatch from `value shr 32`.
private fun colorToHex(c: Color): String {
    val argb = c.toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return "#%02X%02X%02X".format(r, g, b)
}
