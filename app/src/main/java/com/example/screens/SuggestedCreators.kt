package com.example.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.StoryRingAvatar
import com.example.model.User
import com.example.ui.theme.VynoraNeonPink

/** One-time "follow a few creators" card for new accounts, so the feed is never empty. */
@Composable
fun SuggestedCreatorsDialog(
    users: List<User>,
    isDarkMode: Boolean,
    onFollow: (User) -> Unit,
    onDismiss: () -> Unit
) {
    val fg = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val sub = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    var followed by remember { mutableStateOf(setOf<String>()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Follow some creators", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Follow a few people to fill your feed with reels and posts.", fontSize = 13.sp, color = sub)
                LazyColumn(modifier = Modifier.heightIn(max = 340.dp).padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(users) { u ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            StoryRingAvatar(avatarUrl = u.avatarUrl, size = 40.dp, onClick = {})
                            Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(u.fullName.ifBlank { u.username }, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = fg, maxLines = 1)
                                Text("@" + u.username + "  •  " + u.followersCount + " followers", fontSize = 11.sp, color = sub, maxLines = 1)
                            }
                            val isF = u.id in followed
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isF) Color.Transparent else VynoraNeonPink,
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(enabled = !isF) { followed = followed + u.id; onFollow(u) }
                            ) {
                                Text(if (isF) "Following" else "Follow", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    color = if (isF) sub else Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done", color = VynoraNeonPink, fontWeight = FontWeight.Bold) } }
    )
}
