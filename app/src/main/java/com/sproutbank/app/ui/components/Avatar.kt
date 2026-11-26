package com.sproutbank.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class Avatar(
    val emoji: String,
    val bgColor: Color,
    val ringColor: Color,
    val name: String,
)

val AVATAR_BACKGROUNDS = listOf(
    Color(0xFFDDF5EC), Color(0xFFFBE4D7), Color(0xFFEAE4FC), Color(0xFFDFEDFC),
    Color(0xFFFFF0C9), Color(0xFFF0E5D8), Color(0xFFE0F7FA), Color(0xFFFCE4EC),
)
val AVATAR_RINGS = listOf(
    Color(0xFF0D9488), Color(0xFFC8644B), Color(0xFF6D5BD0),
    Color(0xFF2B6CB0), Color(0xFFD98A1A), Color(0xFF3F7D58),
)

val AVATAR_LIBRARY: List<Avatar> = run {
    val entries = listOf(
        "🐼" to "Panda", "🦊" to "Fox", "🐰" to "Bunny", "🐻" to "Bear",
        "🐱" to "Cat", "🐶" to "Dog", "🦁" to "Lion", "🐯" to "Tiger",
        "🐸" to "Frog", "🦄" to "Unicorn", "🐨" to "Koala", "🐷" to "Pig",
        "🐵" to "Monkey", "🦉" to "Owl", "🐢" to "Turtle", "🐙" to "Octopus",
        "🦋" to "Butterfly", "🐝" to "Bee", "🐳" to "Whale", "🐧" to "Penguin",
        "🐲" to "Dragon", "🧙" to "Wizard", "🧚" to "Fairy", "🧜" to "Mermaid",
        "🤖" to "Robot", "🦕" to "Dinosaur", "🦸" to "Hero", "🧝" to "Elf",
        "👨‍🚀" to "Astronaut", "🦹" to "Villain",
        "🔬" to "Scientist", "🎨" to "Artist", "⚽" to "Athlete", "🎮" to "Gamer",
        "🚀" to "Explorer", "🎸" to "Musician", "👨‍🍳" to "Chef", "📚" to "Reader",
        "🏆" to "Champion", "🧑‍🚒" to "Firefighter",
    )
    entries.mapIndexed { i, (emoji, name) ->
        Avatar(emoji, AVATAR_BACKGROUNDS[i % AVATAR_BACKGROUNDS.size],
               AVATAR_RINGS[i % AVATAR_RINGS.size], name)
    }
}

@Composable
fun AvatarCircle(
    emoji: String,
    bg: Color,
    ring: Color,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg)
            .border(width = 3.dp, color = ring, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = (size.value * 0.52).sp)
    }
}

@Composable
fun AvatarPickerGrid(
    selectedEmoji: String,
    onPick: (Avatar) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(AVATAR_LIBRARY) { a ->
            Box(
                Modifier
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onPick(a) },
                contentAlignment = Alignment.Center,
            ) {
                AvatarCircle(
                    a.emoji, a.bgColor, a.ringColor,
                    size = if (a.emoji == selectedEmoji) 60.dp else 52.dp,
                )
            }
        }
    }
}
