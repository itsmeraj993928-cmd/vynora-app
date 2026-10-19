package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.components.PostCard
import com.example.components.StoryRingAvatar
import com.example.components.VynoraBrandHeader
import com.example.i18n.AppLanguage
import com.example.i18n.VynoraStrings
import com.example.model.Moment
import com.example.model.Post
import com.example.model.User
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink

@Composable
fun HomeScreen(
    posts: List<Post>,
    moments: List<Moment>,
    currentUser: User,
    unreadNotificationsCount: Int,
    unreadChatsCount: Int,
    isDarkMode: Boolean,
    currentLanguage: AppLanguage,
    onOpenMomentsViewer: (initialIndex: Int) -> Unit,
    onCreateMoment: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenChats: () -> Unit,
    onGoLiveClick: () -> Unit,
    onLikeToggle: (String) -> Unit,
    onSaveToggle: (String) -> Unit,
    onCommentsClick: (String) -> Unit,
    onShareClick: (Post) -> Unit,
    onReportClick: (type: String, id: String, username: String) -> Unit,
    onAuthorClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onDeletePost: (String) -> Unit = {},
    onFollowAuthor: (id: String, username: String) -> Unit = { _, _ -> }
) {
    var postToDelete by remember { mutableStateOf<String?>(null) }
    if (postToDelete != null) {
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("Delete this post?") },
            text = { Text("This will remove your post for everyone. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    postToDelete?.let { onDeletePost(it) }
                    postToDelete = null
                }) { Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) { Text("Cancel") }
            }
        )
    }

    val screenBg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = screenBg,
        topBar = {
            HomeTopBar(
                isDarkMode = isDarkMode,
                unreadNotificationsCount = unreadNotificationsCount,
                unreadChatsCount = unreadChatsCount,
                onOpenNotifications = onOpenNotifications,
                onOpenChats = onOpenChats,
                onGoLiveClick = onGoLiveClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_feed_list"),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- 1. Moments / Stories Row ---
            item {
                MomentsBar(
                    moments = moments,
                    currentUser = currentUser,
                    isDarkMode = isDarkMode,
                    currentLanguage = currentLanguage,
                    onOpenMoment = onOpenMomentsViewer,
                    onCreateMoment = onCreateMoment
                )
            }

            // --- 2. Posts Feed ---
            if (posts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                            .testTag("home_empty_state_card"),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) Color(0xFF131728) else Color(0xFFFFFFFF)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isDarkMode) Color(0x3300D1FF) else Color(0x1F000000)
                        )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                VynoraNeonPink.copy(alpha = 0.2f),
                                                VynoraNeonCyan.copy(alpha = 0.2f)
                                            )
                                        )
                                    )
                                    .border(
                                        1.dp,
                                        Brush.linearGradient(listOf(VynoraNeonPink, VynoraNeonCyan)),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✨",
                                    fontSize = 32.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "No posts yet. Follow people or create your first post.",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 22.sp
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Share your photos, carousels, and creative moments with the VYNORA community.",
                                fontSize = 13.sp,
                                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(22.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = onCreateMoment,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.White
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Create Post",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                items(posts, key = { it.id }) { post ->
                    Column {
                        Box {
                        PostCard(
                            post = post,
                            isDarkMode = isDarkMode,
                            currentLanguage = currentLanguage,
                            onLikeToggle = { onLikeToggle(post.id) },
                            onSaveToggle = { onSaveToggle(post.id) },
                            onCommentsClick = { onCommentsClick(post.id) },
                            onShareClick = { onShareClick(post) },
                            onReportClick = { onReportClick("Post", post.id, post.author.username) },
                            onAuthorClick = { onAuthorClick(post.author.username) },
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        if (post.author.id.isNotBlank() && post.author.id != currentUser.id && !post.author.isOfficial) {
                            val following = post.author.isFollowing
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (following) Color(0x331E2235) else VynoraNeonPink,
                                border = if (following) BorderStroke(1.dp, Color(0x6694A3B8)) else null,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 22.dp, end = 52.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onFollowAuthor(post.author.id, post.author.username) }
                            ) {
                                Text(
                                    text = if (following) "Following" else "Follow",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        }
                        if (currentUser.id.isNotBlank() && post.author.id == currentUser.id) {
                            TextButton(
                                onClick = { postToDelete = post.id },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(horizontal = 14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete my post", color = Color(0xFFEF4444), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(
    isDarkMode: Boolean,
    unreadNotificationsCount: Int,
    unreadChatsCount: Int,
    onOpenNotifications: () -> Unit,
    onOpenChats: () -> Unit,
    onGoLiveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        VynoraBrandHeader(textColor = if (isDarkMode) Color.White else Color(0xFF0F172A))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Go Live Button
            IconButton(
                onClick = onGoLiveClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color(0x22FF2E93) else Color(0x1FFF2E93))
            ) {
                Icon(
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = "Go Live",
                    tint = VynoraNeonPink
                )
            }

            // Notifications with badge
            Box {
                IconButton(onClick = onOpenNotifications) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                }
                if (unreadNotificationsCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(VynoraNeonPink)
                    )
                }
            }

            // Chats with badge
            Box {
                IconButton(onClick = onOpenChats) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Chats",
                        tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                }
                if (unreadChatsCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(VynoraNeonCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadChatsCount.toString(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MomentsBar(
    moments: List<Moment>,
    currentUser: User,
    isDarkMode: Boolean,
    currentLanguage: AppLanguage,
    onOpenMoment: (index: Int) -> Unit,
    onCreateMoment: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. My Moment Item
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(onClick = onCreateMoment)
                    .testTag("add_moment_button")
            ) {
                Box(
                    modifier = Modifier.size(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color(0x33FFFFFF), CircleShape)
                            .background(Color(0xFF1E2235)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentUser.avatarUrl.isNotBlank()) {
                            StoryRingAvatar(
                                avatarUrl = currentUser.avatarUrl,
                                size = 62.dp,
                                onClick = onCreateMoment
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "My Avatar",
                                tint = VynoraNeonCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Plus icon badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(VynoraNeonPink, VynoraNeonCyan))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add Moment",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = VynoraStrings.yourMoment(currentLanguage),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                    maxLines = 1
                )
            }
        }

        // 2. Friends' Moments
        itemsIndexed(moments) { index, moment ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onOpenMoment(index) }
                    .testTag("moment_item_${moment.id}")
            ) {
                StoryRingAvatar(
                    avatarUrl = moment.author.avatarUrl,
                    hasStory = true,
                    isCloseFriends = moment.isCloseFriendsOnly,
                    isViewed = moment.isViewed,
                    size = 64.dp,
                    onClick = { onOpenMoment(index) }
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = moment.author.username.take(9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                    maxLines = 1
                )
            }
        }
    }
}
