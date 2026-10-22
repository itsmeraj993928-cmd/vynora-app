package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink
import com.example.util.MusicTrack

/** Small speech-bubble that shows a profile note (and the song, if any). */
@Composable
fun NoteBubble(
    text: String,
    song: String,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val bg = if (isDarkMode) Color(0xFF1E2235) else Color(0xFFFFFFFF)
    val fg = if (isDarkMode) Color.White else Color(0xFF0F172A)
    Column(
        modifier = modifier
            .widthIn(max = 96.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, VynoraNeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, color = fg, fontSize = 10.5.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (song.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = VynoraNeonPink, modifier = Modifier.size(10.dp))
                Text(song, color = VynoraNeonPink, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Write / change / remove my note (max 60 characters) and optionally attach a song. */
@Composable
fun NoteEditDialog(
    initialText: String,
    initialSong: String,
    initialSongUrl: String = "",
    isDarkMode: Boolean,
    onSave: (text: String, song: MusicTrack?) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var song by remember { mutableStateOf<MusicTrack?>(null) }
    var songLabel by remember { mutableStateOf(initialSong) }
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        MusicPickerSheet(
            isDarkMode = isDarkMode,
            onPick = { song = it; songLabel = it.label; showPicker = false },
            onDismiss = { showPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Your note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(60) },
                    placeholder = { Text("What's on your mind?") },
                    supportingText = { Text("${text.length}/60  •  visible for 24 hours") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.MusicNote, contentDescription = null, tint = VynoraNeonCyan, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (songLabel.isBlank()) "Add a song" else songLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (songLabel.isNotBlank()) {
                    TextButton(onClick = { song = null; songLabel = "" }) { Text("Remove song", color = VynoraNeonPink) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // keep the song that is already on the note unless a new one was picked or it was removed
                val finalSong = song ?: if (songLabel.isNotBlank()) MusicTrack(songLabel, "", initialSongUrl) else null
                onSave(text, finalSong)
            }) { Text("Share", color = VynoraNeonPink, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Row {
                if (initialText.isNotBlank()) TextButton(onClick = { onSave("", null) }) { Text("Delete note") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
