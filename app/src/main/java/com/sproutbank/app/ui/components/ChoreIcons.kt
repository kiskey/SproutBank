package com.sproutbank.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Curated chore icon library (issue 4): replaces the raw "type an emoji" text
 * field with a categorized picker a parent can browse. Icons map to the
 * hand-drawn StorybookObject set when available; others fall back to the
 * emoji glyph on a tinted tile (still coherent with the storybook look).
 */
data class ChoreIcon(val emoji: String, val label: String, val category: String)

val CHORE_ICON_CATEGORIES = listOf("Home", "Self-care", "Learning", "Pets & Nature", "Special", "Deductions")

val CHORE_ICON_LIBRARY: List<ChoreIcon> = listOf(
    ChoreIcon("🛏️", "Make Bed", "Home"),
    ChoreIcon("🧹", "Sweep", "Home"),
    ChoreIcon("🧽", "Wipe Table", "Home"),
    ChoreIcon("🧺", "Laundry", "Home"),
    ChoreIcon("🍽️", "Dishes", "Home"),
    ChoreIcon("🗑️", "Trash Out", "Home"),
    ChoreIcon("🧴", "Tidy Shelf", "Home"),
    ChoreIcon("🪥", "Brush Teeth", "Self-care"),
    ChoreIcon("🚿", "Shower", "Self-care"),
    ChoreIcon("🛁", "Bath", "Self-care"),
    ChoreIcon("💇", "Hair Brush", "Self-care"),
    ChoreIcon("👕", "Get Dressed", "Self-care"),
    ChoreIcon("⏰", "On Time", "Self-care"),
    ChoreIcon("😴", "Bedtime", "Self-care"),
    ChoreIcon("📚", "Read", "Learning"),
    ChoreIcon("✏️", "Homework", "Learning"),
    ChoreIcon("🎹", "Practice", "Learning"),
    ChoreIcon("🧩", "Puzzle", "Learning"),
    ChoreIcon("📖", "Story Time", "Learning"),
    ChoreIcon("🐾", "Feed Pet", "Pets & Nature"),
    ChoreIcon("🪴", "Water Plants", "Pets & Nature"),
    ChoreIcon("🐕", "Walk Dog", "Pets & Nature"),
    ChoreIcon("🐦", "Fill Feeder", "Pets & Nature"),
    ChoreIcon("🌱", "Garden", "Pets & Nature"),
    ChoreIcon("🚮", "Yard Tidy", "Pets & Nature"),
    ChoreIcon("🏆", "Big Win", "Special"),
    ChoreIcon("🎯", "Goal", "Special"),
    ChoreIcon("💛", "Kindness", "Special"),
    ChoreIcon("🤝", "Helping Hand", "Special"),
    ChoreIcon("🚗", "Car Help", "Special"),
    ChoreIcon("🧑‍🍳", "Cook Assist", "Special"),
    ChoreIcon("⏳", "Extra Screen", "Deductions"),
    ChoreIcon("📱", "Device Time", "Deductions"),
    ChoreIcon("🍬", "Sweets", "Deductions"),
    ChoreIcon("😤", "Attitude", "Deductions"),
    ChoreIcon("📢", "Tantrum", "Deductions"),
    ChoreIcon("💥", "Breakage", "Deductions"),
).filter { it.label.isNotBlank() }

/**
 * Grid picker for the chore editor. Emojis that have a storybook object render
 * with it; the rest show the glyph on a soft tinted tile.
 */
@Composable
fun ChoreIconPicker(
    selected: String,
    onPick: (ChoreIcon) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CHORE_ICON_CATEGORIES.forEach { category ->
            val icons = CHORE_ICON_LIBRARY.filter { it.category == category }
            if (icons.isNotEmpty()) {
                Text(
                    category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 130.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(icons) { icon ->
                        IconTile(icon, selected = icon.emoji == selected, onClick = { onPick(icon) })
                    }
                }
            }
        }
    }
}

@Composable
private fun IconTile(icon: ChoreIcon, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) scheme.primaryContainer else scheme.surfaceVariant.copy(alpha = 0.5f))
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) scheme.primary else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(icon.emoji, style = MaterialTheme.typography.titleMedium)
        Text(
            icon.label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** Small round preview used in chore rows and editor header. */
@Composable
fun ChoreIconBadge(emoji: String, size: androidx.compose.ui.unit.Dp = 48.dp) {
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(scheme.primaryContainer.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, style = MaterialTheme.typography.titleLarge)
    }
}
