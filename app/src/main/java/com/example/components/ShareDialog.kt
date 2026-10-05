package com.example.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.model.Post
import com.example.util.VynoraOfficial
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink

@Composable
fun ShareDialog(
    post: Post,
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onSendToUser: (username: String) -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val dialogBg = if (isDarkMode) Color(0xFF131726) else Color.White

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Share Post",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Send via Direct Chat",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Recent contacts row
                val officialAccount = remember { VynoraOfficial.createOfficialUser() }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    onSendToUser(officialAccount.username)
                                    onToast("Sent post to @${officialAccount.username} in Chat")
                                    onDismiss()
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                            ) {
                                AsyncImage(
                                    model = officialAccount.avatarUrl,
                                    contentDescription = officialAccount.username,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Official",
                                fontSize = 11.sp,
                                maxLines = 1,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(color = if (isDarkMode) Color(0x22FFFFFF) else Color(0x1F000000))

                Spacer(modifier = Modifier.height(14.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Copy Link
                    ShareActionPill(
                        icon = Icons.Outlined.ContentCopy,
                        label = "Copy Link",
                        isDarkMode = isDarkMode,
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Vynora Post Link", "https://vynora.io/p/${post.id}")
                            clipboard.setPrimaryClip(clip)
                            onToast("Link copied to clipboard! 📋")
                            onDismiss()
                        }
                    )

                    // Add to Moment / Story
                    ShareActionPill(
                        icon = Icons.Outlined.AddCircleOutline,
                        label = "To Moment",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onToast("Post reposted to your Moment! 🕒")
                            onDismiss()
                        }
                    )

                    // External Share
                    ShareActionPill(
                        icon = Icons.Outlined.Share,
                        label = "Other Apps",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onToast("Sharing sheet opened")
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(if (isDarkMode) Color(0xFF1E2235) else Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = VynoraNeonCyan,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
        )
    }
}
