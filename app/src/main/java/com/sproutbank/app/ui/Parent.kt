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
import com.sproutbank.app.data.SpendRequestEntity
import com.sproutbank.app.theme.CalmTheme
import com.sproutbank.app.ui.components.DeclineDialog
import com.sproutbank.app.ui.components.GoalDialog
import com.sproutbank.app.vm.SproutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentScreen(vm: SproutViewModel) {
    val children by vm.children.collectAsState()
    val requests by vm.allRequests.collectAsState()
    var declineTarget by remember { mutableStateOf<SpendRequestEntity?>(null) }
    var goalTarget by remember { mutableStateOf<com.sproutbank.app.data.ChildEntity?>(null) }

    Scaffold(topBar = { TopAppBar(title = { Text("Parent Hub") }) }) { pad ->
        LazyColumn(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text("Spend & Redemption Requests",
                style = MaterialTheme.typography.titleMedium) }
            if (requests.isEmpty()) {
                item { Text("No requests yet.", style = MaterialTheme.typography.bodySmall) }
            }
            items(requests) { r ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(r.title, style = MaterialTheme.typography.titleSmall)
                        Text("${Repository.money(r.amountCents)} · ${r.status}",
                            style = MaterialTheme.typography.bodySmall)
                        if (r.status == "PENDING") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { vm.decideRequest(r, true, null) }) { Text("Confirm") }
                                OutlinedButton(onClick = { declineTarget = r }) { Text("Decline") }
                            }
                        } else if (r.status == "DECLINED" && r.decisionNote != null) {
                            Text("Reason: ${r.decisionNote}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item { Text("Children", style = MaterialTheme.typography.titleMedium) }
            items(children) { child ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Text("${child.emoji} ${child.name}",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall)
                        OutlinedButton(onClick = { goalTarget = child }) {
                            Text(if (child.goalName == null) "Set Goal" else "Edit Goal")
                        }
                    }
                }
            }
            item { Text("Themes", style = MaterialTheme.typography.titleMedium) }
            item {
                val themeId by vm.themeId.collectAsState()
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    CalmTheme.values().forEach { t ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(selected = themeId == t.id, onClick = { vm.selectTheme(t.id) })
                            Text(t.label)
                        }
                    }
                    val reduced by vm.reducedAnimations.collectAsState()
                    Row {
                        Text("Reduced animations", Modifier.weight(1f))
                        Switch(checked = reduced, onCheckedChange = { vm.setReducedAnimations(it) })
                    }
                }
            }
        }
    }

    declineTarget?.let { r ->
        DeclineDialog(
            request = r,
            onDismiss = { declineTarget = null },
            onDecline = { note -> vm.decideRequest(r, false, note); declineTarget = null },
        )
    }
    goalTarget?.let { c ->
        GoalDialog(
            child = c,
            onDismiss = { goalTarget = null },
            onConfirm = { n, e, t, etf -> vm.setChildGoal(c, n, e, t, etf); goalTarget = null },
        )
    }
}
