package com.sproutbank.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sproutbank.app.data.ChildEntity
import com.sproutbank.app.data.Repository
import com.sproutbank.app.ui.components.AvatarCircle
import com.sproutbank.app.ui.components.Sprout
import com.sproutbank.app.ui.components.SproutMood
import com.sproutbank.app.ui.components.ThemedCard
import com.sproutbank.app.ui.components.tactileClickable
import com.sproutbank.app.vm.SproutViewModel

@Composable
fun ProfileSelectScreen(vm: SproutViewModel) {
    val children by vm.children.collectAsState()
    val activeId by vm.activeChildId.collectAsState()
    val activeState by vm.uiState.collectAsState()
    val reduced by vm.reducedAnimations.collectAsState()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Sprout(mood = SproutMood.HAPPY, size = 96.dp, reduced = reduced)
        Spacer(Modifier.height(12.dp))
        Text("SproutBank", style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary)
        Text("Little Steps. Big Dreams.", style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Text("Who's playing?", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(children) { child ->
                ProfileCard(
                    child = child,
                    balancePreview = if (activeId == child.id) activeState.balanceCents else null,
                    reduced = reduced,
                    onClick = { vm.enterChild(child.id) },
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    child: ChildEntity,
    balancePreview: Long?,
    reduced: Boolean,
    onClick: () -> Unit,
) {
    ThemedCard(
        modifier = Modifier
            .fillMaxWidth()
            .tactileClickable(reduced = reduced, onClick = onClick),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AvatarCircle(
                emoji = child.emoji,
                bg = Color(android.graphics.Color.parseColor(child.avatarBg)),
                ring = Color(android.graphics.Color.parseColor(child.avatarRing)),
                size = 88.dp,
            )
            Spacer(Modifier.height(12.dp))
            Text(child.name, style = MaterialTheme.typography.titleMedium)
            if (balancePreview != null) {
                Text(Repository.money(balancePreview),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}




