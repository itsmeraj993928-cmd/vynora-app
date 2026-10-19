package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.NotificationItem
import com.example.model.NotificationType
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    isDarkMode: Boolean,
    onMarkAllAsRead: () -> Unit,
    onBack: () -> Unit,
    onUserClick: (String, String) -> Unit,
    onFollowBack: (String, String) -> Unit = { _, _ -> },
    onPostClick: (String) -> Unit = {},
    isFollowing: (String) -> Boolean = { false },
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "Likes", "Comments", "Follows", "Mentions")

    val screenBg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF0F121E) else Color(0xFFFFFFFF)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("notifications_screen")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                }
                Text(
                    text = "Notifications",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
            }

            TextButton(onClick = onMarkAllAsRead) {
                Text(
                    text = "Mark all read",
                    color = VynoraNeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Filter Pills
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { f ->
                val isSelected = selectedFilter == f
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) VynoraNeonPink else cardBg,
                    modifier = Modifier.clickable { selectedFilter = f }
                ) {
                    Text(
                        text = f,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else (if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Notifications List Grouped
        val filtered = notifications.filter {
            when (selectedFilter) {
                "Likes" -> it.type == NotificationType.LIKE
                "Comments" -> it.type == NotificationType.COMMENT
                "Follows" -> it.type == NotificationType.FOLLOW
                "Mentions" -> it.type == NotificationType.MENTION
                else -> true
            }
        }

        val groups = listOf("Today", "Yesterday", "Earlier")

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = null,
                        tint = VynoraNeonCyan,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No notifications yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Likes, comments, follows, mentions and badge alerts will appear here.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                groups.forEach { groupName ->
                    val itemsInGroup = filtered.filter { it.timeGroup == groupName }
                    if (itemsInGroup.isNotEmpty()) {
                        item {
                            Text(
                                text = groupName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(itemsInGroup, key = { it.id }) { item ->
                            NotificationRow(
                                item = item,
                                isDarkMode = isDarkMode,
                                onUserClick = {
                                    if (item.postId.isNotBlank() && (item.type == NotificationType.LIKE || item.type == NotificationType.COMMENT)) onPostClick(item.postId)
                                    else onUserClick(item.user.id, item.user.username)
                                },
                                onFollowBack = { onFollowBack(item.user.id, item.user.username) },
                                alreadyFollowing = isFollowing(item.user.id)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    item: NotificationItem,
    isDarkMode: Boolean,
    onUserClick: () -> Unit,
    onFollowBack: () -> Unit,
    alreadyFollowing: Boolean
) {
    var isFollowingBack by remember(item.id, alreadyFollowing) { mutableStateOf(alreadyFollowing) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar with type badge
        Box(modifier = Modifier.size(46.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
            ) {
                com.example.components.StoryRingAvatar(
                    avatarUrl = item.user.avatarUrl,
                    size = 46.dp
                )
            }

            // Type icon badge
            val (badgeIcon, badgeColor) = when (item.type) {
                NotificationType.LIKE -> Icons.Filled.Favorite to VynoraNeonPink
                NotificationType.COMMENT -> Icons.Filled.ChatBubble to VynoraNeonCyan
                NotificationType.FOLLOW -> Icons.Filled.PersonAdd to Color(0xFF10B981)
                NotificationType.MENTION -> Icons.Filled.AlternateEmail to Color(0xFFA855F7)
                NotificationType.BADGE_EXPIRY -> Icons.Filled.Star to Color(0xFF00D1FF)
                NotificationType.SYSTEM -> Icons.Filled.Notifications to Color(0xFFFBBF24)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(11.dp)
                )
            }
        }

        // Notification Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildString {
                    append(item.user.username)
                    append(" ")
                    val preview = item.contentPreview ?: ""
                    if (item.type == NotificationType.FOLLOW && preview.isBlank()) append("started following you")
                    else if (item.type == NotificationType.LIKE && preview.isBlank()) append("liked your post")
                    else append(preview)
                },
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.timestamp,
                fontSize = 11.sp,
                color = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)
            )
        }

        // Follow back button if FOLLOW type
        if (item.type == NotificationType.FOLLOW) {
            Button(
                onClick = { isFollowingBack = !isFollowingBack; onFollowBack() },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowingBack) Color.Gray.copy(alpha = 0.4f) else VynoraNeonCyan
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(
                    text = if (isFollowingBack) "Following" else "Follow Back",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFollowingBack) Color.White else Color.Black
                )
            }
        }

        // Unread dot
        if (!item.isRead) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(VynoraNeonPink)
            )
        }
    }
}
