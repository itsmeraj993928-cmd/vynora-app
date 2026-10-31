package com.example.components

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Clip
import com.example.model.User

/**
 * "Add to story" for a reel (Instagram style): the reel plays inside a card,
 * you can write a caption, then share to "Your story" or "Close Friends".
 */
@Composable
fun ReelStoryComposer(
    clip: Clip,
    me: User?,
    onShare: (caption: String, closeFriends: Boolean, music: com.example.util.MusicTrack?) -> Unit,
    onCancel: () -> Unit
) {
    var caption by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<String>() }          // text, stickers, mentions put on the story
    var song by remember { mutableStateOf<com.example.util.MusicTrack?>(null) }
    var dialog by remember { mutableStateOf("") }                    // "", "text", "sticker", "mention"
    var typed by remember { mutableStateOf("") }
    var showMusic by remember { mutableStateOf(false) }
    val bgTop = remember(clip.id) { listOf(Color(0xFF7C8DB5), Color(0xFF8B6FA8), Color(0xFF5E9C9A), Color(0xFFB57C7C))[(clip.id.hashCode() and 0x7fffffff) % 4] }
    fun overlayText(): String = (items.toList() + caption.trim()).filter { it.isNotBlank() }.joinToString("  ")
    val appActive by com.example.util.rememberAppActive()
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(bgTop, Color(0xFF3A2E2E))))
                .systemBarsPadding()
                .imePadding()
                .padding(bottom = 18.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(6.dp), verticalAlignment = Alignment.Top) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    @Composable
                    fun Tool(label: String, onClick: () -> Unit, content: @Composable () -> Unit) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x55000000)).clickable(onClick = onClick),
                            contentAlignment = Alignment.Center
                        ) { content() }
                    }
                    Tool("Text", { typed = ""; dialog = "text" }) { Text("Aa", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
                    Tool("Sticker", { dialog = "sticker" }) { Text("😊", fontSize = 20.sp) }
                    Tool("Music", { showMusic = true }) { Text("♫", color = Color.White, fontSize = 20.sp) }
                    Tool("Mention", { typed = ""; dialog = "mention" }) { Text("@", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                }
            }

            BoxWithConstraints(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                // 9:16 card that always fits inside the free space
                val cardH = minOf(maxHeight, maxWidth * 16f / 9f)
                val cardW = cardH * 9f / 16f
                Box(
                    modifier = Modifier
                        .size(cardW, cardH)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.Black)
                ) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            VideoView(ctx).apply {
                                setOnPreparedListener { mp -> mp.isLooping = true; mp.setVolume(0f, 0f); start() }
                                setOnErrorListener { _, _, _ -> true }
                                setVideoURI(Uri.parse(clip.mediaUrl))
                            }
                        },
                        update = { vv -> if (appActive) { if (!vv.isPlaying) vv.start() } else if (vv.isPlaying) vv.pause() },
                        onRelease = { vv -> try { vv.stopPlayback() } catch (_: Throwable) { } }
                    )
                    Text(
                        text = "@" + clip.author.username,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                    )
                    // things I added (tap to remove)
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items.forEachIndexed { i, t ->
                            Text(
                                text = t, color = Color.White, fontSize = if (t.length <= 2) 44.sp else 20.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (t.length <= 2) Color.Transparent else Color(0x66000000))
                                    .clickable { items.removeAt(i) }.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    song?.let { sg ->
                        Text(
                            text = "♫ " + sg.label, color = Color.White, fontSize = 12.sp, maxLines = 1,
                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp).clip(RoundedCornerShape(12.dp))
                                .background(Color(0x77000000)).clickable { song = null }.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // caption
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                androidx.compose.foundation.text.BasicTextField(
                    value = caption,
                    onValueChange = { caption = it.take(120) },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 15.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (caption.isEmpty()) Text("Add a caption...", color = Color(0xFFB0B3BD), fontSize = 15.sp)
                        inner()
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF23242B))
                        .clickable { onShare(overlayText(), false, song) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StoryRingAvatar(avatarUrl = me?.avatarUrl ?: "", size = 24.dp)
                    Text("Your story", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF23242B))
                        .clickable { onShare(overlayText(), true, song) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape).background(Color(0xFF22C55E)),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp)) }
                    Text("Close Friends", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (showMusic) {
            com.example.screens.MusicPickerSheet(
                isDarkMode = true,
                onPick = { song = it; showMusic = false },
                onDismiss = { showMusic = false }
            )
        }
        if (dialog == "text" || dialog == "mention") {
            AlertDialog(
                onDismissRequest = { dialog = "" },
                title = { Text(if (dialog == "text") "Add text" else "Mention someone") },
                text = {
                    OutlinedTextField(
                        value = typed, onValueChange = { typed = it.take(40) }, singleLine = true,
                        placeholder = { Text(if (dialog == "text") "Type something" else "username") }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val t = typed.trim().removePrefix("@")
                        if (t.isNotBlank()) items.add(if (dialog == "mention") "@$t" else t)
                        dialog = ""
                    }) { Text("Add") }
                },
                dismissButton = { TextButton(onClick = { dialog = "" }) { Text("Cancel") } }
            )
        }
        if (dialog == "sticker") {
            AlertDialog(
                onDismissRequest = { dialog = "" },
                title = { Text("Stickers") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("😂😍🔥❤️😎", "🥳👏💯✨🙌", "😭😡🤩😮🤔", "🎉🎶⭐💥🚀").forEach { rowEmojis ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowEmojis.codePoints().toArray().let { cps ->
                                    // split into emoji glyphs (variation selector stays with its emoji)
                                    val glyphs = mutableListOf<String>()
                                    cps.forEach { cp ->
                                        val ch = String(Character.toChars(cp))
                                        if (cp == 0xFE0F && glyphs.isNotEmpty()) glyphs[glyphs.size - 1] = glyphs.last() + ch else glyphs.add(ch)
                                    }
                                    glyphs.forEach { g ->
                                        Text(g, fontSize = 30.sp, modifier = Modifier.clickable { items.add(g); dialog = "" }.padding(4.dp))
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { dialog = "" }) { Text("Close") } }
            )
        }
    }
}
