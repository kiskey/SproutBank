package com.sproutbank.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sproutbank.app.theme.CalmTheme
import com.sproutbank.app.theme.SproutTheme
import com.sproutbank.app.ui.*
import com.sproutbank.app.ui.components.CelebrationBanner
import com.sproutbank.app.vm.SproutViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: SproutViewModel = viewModel()
            SproutTheme(vm) {
                Root(vm)
            }
        }
    }
}

private enum class Tab(val label: String) {
    HOME("Home"), BANK("Bank"), GROWTH("Growth"), PARENT("Parent")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Root(vm: SproutViewModel) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    var showThemes by remember { mutableStateOf(false) }
    val state by vm.uiState.collectAsState()
    val reduced by vm.reducedAnimations.collectAsState()
    val celebration by vm.latestCelebration.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.values().forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = {
                            Icon(
                                when (t) {
                                    Tab.HOME -> Icons.Default.Home
                                    Tab.BANK -> Icons.Default.Savings
                                    Tab.GROWTH -> Icons.Default.TrendingUp
                                    Tab.PARENT -> Icons.Default.FamilyRestroom
                                },
                                contentDescription = t.label,
                            )
                        },
                        label = { Text(t.label) },
                    )
                }
            }
        }
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            CelebrationBanner(celebration, reduced)
            when (tab) {
                Tab.HOME -> HomeScreen(vm, state, onOpenThemes = { showThemes = true })
                Tab.BANK -> RewardBankScreen(vm, state)
                Tab.GROWTH -> GrowthScreen(vm, state)
                Tab.PARENT -> ParentScreen(vm)
            }
        }
    }

    if (showThemes) ThemeSheet(vm, onDismiss = { showThemes = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSheet(vm: SproutViewModel, onDismiss: () -> Unit) {
    val themeId by vm.themeId.collectAsState()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Calm Theme", style = MaterialTheme.typography.titleLarge)
            CalmTheme.values().forEach { t ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = themeId == t.id, onClick = { vm.selectTheme(t.id) })
                    Text(t.label)
                }
            }
        }
    }
}
