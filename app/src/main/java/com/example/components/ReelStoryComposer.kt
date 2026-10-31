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
    onShare: (caption: String, closeFriends: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var caption by remember { mutableStateOf("") }
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0B0B0F))
                .systemBarsPadding()
                .imePadding()
                .padding(bottom = 18.dp)
        ) {
            IconButton(onClick = onCancel, modifier = Modifier.padding(6.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
                        onRelease = { vv -> try { vv.stopPlayback() } catch (_: Throwable) { } }
                    )
                    Text(
                        text = "@" + clip.author.username,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                    )
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
                        .clickable { onShare(caption.trim(), false) }
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
                        .clickable { onShare(caption.trim(), true) }
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
    }
}
