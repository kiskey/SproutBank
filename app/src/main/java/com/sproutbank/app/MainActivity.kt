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
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sproutbank.app.theme.LocalStorybookPalette
import com.sproutbank.app.theme.SproutTheme
import com.sproutbank.app.ui.*
import com.sproutbank.app.ui.components.CelebrationBanner
import com.sproutbank.app.ui.components.StoryIntensity
import com.sproutbank.app.ui.components.StorybookBackground
import com.sproutbank.app.vm.SproutViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: SproutViewModel = viewModel()
            SproutTheme(vm) { Root(vm) }
        }
    }
}

private enum class Tab(val label: String) {
    HOME("Home"), CHORES("Chores"), BANK("Bank"), GROWTH("Grow"), PROFILE("Me")
}

@Composable
private fun Root(vm: SproutViewModel) {
    val activeId by vm.activeChildId.collectAsState()
    val state by vm.uiState.collectAsState()
    val reduced by vm.reducedAnimations.collectAsState()
    val celebration by vm.latestCelebration.collectAsState()
    val palette = LocalStorybookPalette.current

    var tab by remember { mutableStateOf(Tab.HOME) }
    var showGoal by remember { mutableStateOf(false) }
    var showParent by remember { mutableStateOf(false) }

    val intensity = when {
        activeId == null -> StoryIntensity.FULL
        showParent || showGoal -> StoryIntensity.SUBTLE
        else -> StoryIntensity.FULL
    }

    StorybookBackground(intensity = intensity, palette = palette) {
        Surface(color = Color.Transparent, modifier = Modifier.fillMaxSize()) {
            when {
                activeId == null -> ProfileSelectScreen(vm)
                showParent -> ParentGate(vm, onBack = { showParent = false })
                showGoal -> GoalScreen(state, onBack = { showGoal = false })
                else -> {
                    Scaffold(
                        containerColor = Color.Transparent,
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            ) {
                                Tab.values().forEach { t ->
                                    NavigationBarItem(
                                        selected = tab == t,
                                        onClick = { tab = t },
                                        icon = {
                                            Icon(
                                                when (t) {
                                                    Tab.HOME -> Icons.Default.Home
                                                    Tab.CHORES -> Icons.Default.CheckCircle
                                                    Tab.BANK -> Icons.Default.Savings
                                                    Tab.GROWTH -> Icons.Default.TrendingUp
                                                    Tab.PROFILE -> Icons.Default.Person
                                                },
                                                contentDescription = t.label,
                                            )
                                        },
                                        label = { Text(t.label) },
                                    )
                                }
                            }
                        },
                    ) { pad ->
                        Column(Modifier.padding(pad).fillMaxSize()) {
                            CelebrationBanner(celebration, reduced)
                            when (tab) {
                                Tab.HOME -> HomeScreen(vm, state, onOpenGoal = { showGoal = true })
                                Tab.CHORES -> ChoresScreen(vm, state)
                                Tab.BANK -> RewardBankScreen(vm, state)
                                Tab.GROWTH -> GrowthScreen(vm, state)
                                Tab.PROFILE -> ProfileScreen(
                                    vm, state,
                                    onLeaveChild = { vm.leaveChild() },
                                    onOpenParent = { showParent = true },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
