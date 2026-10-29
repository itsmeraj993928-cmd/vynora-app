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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import com.example.model.hasActiveNote
import com.example.model.activeStreak
import com.example.model.postedToday
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    posts: List<Post>,
    clips: List<com.example.model.Clip> = emptyList(),
    onOpenClips: (com.example.model.Clip) -> Unit = {},
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
    onFollowAuthor: (id: String, username: String) -> Unit = { _, _ -> },
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onSaveNote: (text: String, song: com.example.util.MusicTrack?) -> Unit = { _, _ -> },
    onNotInterested: (String) -> Unit = {},
    onRepost: (Post) -> Unit = {},
    onHashtagClick: (String) -> Unit = {},
    savedFolders: Map<String, String> = emptyMap(),
    onSaveToFolder: (String, String) -> Unit = { _, _ -> },
    onMute: (String) -> Unit = {},
    onPostView: (String) -> Unit = {}
) {
    var folderForPost by remember { mutableStateOf<String?>(null) }
    var newFolder by remember { mutableStateOf("") }
    if (folderForPost != null) {
        AlertDialog(
            onDismissRequest = { folderForPost = null; newFolder = "" },
            title = { Text("Save to folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val names = (savedFolders.values.toSet() + "Favorites").toList()
                    names.forEach { n ->
                        TextButton(onClick = { onSaveToFolder(folderForPost!!, n); folderForPost = null }) { Text(n) }
                    }
                    OutlinedTextField(value = newFolder, onValueChange = { newFolder = it }, label = { Text("New folder") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(enabled = newFolder.isNotBlank(), onClick = { onSaveToFolder(folderForPost!!, newFolder); folderForPost = null; newFolder = "" }) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { folderForPost = null; newFolder = "" }) { Text("Cancel") } }
        )
    }
    var followingOnly by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    if (showNoteDialog) {
        NoteEditDialog(
            initialText = if (currentUser.hasActiveNote()) currentUser.note else "",
            initialSong = if (currentUser.hasActiveNote()) currentUser.noteSong else "",
            initialSongUrl = currentUser.noteSongUrl,
            isDarkMode = isDarkMode,
            onSave = { t, sg -> onSaveNote(t, sg); showNoteDialog = false },
            onDismiss = { showNoteDialog = false }
        )
    }
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
    val feed = remember(posts, clips, followingOnly) {
        val ps = if (followingOnly) posts.filter { it.author.isFollowing || it.author.id == currentUser.id } else posts
        val cs = if (followingOnly) clips.filter { it.isFollowing || it.author.id == currentUser.id } else clips
        // keep the algorithm's order inside posts and inside reels, then interleave by relative rank
        val a = ps.mapIndexed { i, p -> (i + 0.25f) / ps.size.coerceAtLeast(1) to (FeedItem.P(p) as FeedItem) }
        val b = cs.mapIndexed { i, c -> (i + 0.75f) / cs.size.coerceAtLeast(1) to (FeedItem.C(c) as FeedItem) }
        (a + b).sortedBy { it.first }.map { it.second }
    }
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
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
                    onCreateMoment = onCreateMoment,
                    onEditNote = { showNoteDialog = true }
                )
            }

            // --- 1b. Daily challenge + streak ---
            item {
                DailyChallengeCard(
                    user = currentUser,
                    isDarkMode = isDarkMode,
                    onPostNow = onCreateMoment
                )
            }

            // --- 1c. For You / Following ---
            item {
                Row(modifier = Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !followingOnly, onClick = { followingOnly = false }, label = { Text("For You") })
                    FilterChip(selected = followingOnly, onClick = { followingOnly = true }, label = { Text("Following") })
                }
            }

            // --- 2. Posts Feed ---
            if (feed.isEmpty()) {
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
                items(feed, key = { it.key }) { item ->
                    if (item is FeedItem.C) {
                        val clip = item.clip
                        ReelFeedCard(
                            clip = clip,
                            isDarkMode = isDarkMode,
                            isOwner = currentUser.id.isNotBlank() && clip.author.id == currentUser.id,
                            onOpen = { onOpenClips(clip) },
                            onAuthorClick = { onAuthorClick(clip.author.username) },
                            onFollow = { onFollowAuthor(clip.author.id, clip.author.username) },
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        return@items
                    }
                    val post = (item as FeedItem.P).post
                    LaunchedEffect(post.id) { onPostView(post.id) }
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
                            isOwner = currentUser.id.isNotBlank() && post.author.id == currentUser.id,
                            onDeleteClick = { postToDelete = post.id },
                            onAuthorClick = { onAuthorClick(post.author.username) },
                            onNotInterested = { onNotInterested(post.id) },
                            onRepost = { onRepost(post) },
                            onHashtagClick = onHashtagClick,
                            onSaveToFolder = { folderForPost = post.id },
                            onMute = { onMute(post.author.username) },
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        if (post.author.id.isNotBlank() && post.author.id != currentUser.id && !post.author.isOfficial && !post.author.isFollowing) {
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
    onCreateMoment: () -> Unit,
    onEditNote: () -> Unit = {}
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
            val myIndex = moments.indexOfFirst { it.author.id.isNotBlank() && it.author.id == currentUser.id }
            val hasMine = myIndex >= 0
            val openMine: () -> Unit = { if (hasMine) onOpenMoment(myIndex) else onCreateMoment() }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable(onClick = openMine)
                    .testTag("add_moment_button")
            ) {
                // note bubble slot (tap to write / change my note)
                Box(modifier = Modifier.height(30.dp), contentAlignment = Alignment.BottomCenter) {
                    if (currentUser.hasActiveNote()) {
                        NoteBubble(currentUser.note, currentUser.noteSong, isDarkMode, onClick = onEditNote)
                    } else {
                        Text(
                            "+ Note",
                            fontSize = 10.sp,
                            color = VynoraNeonCyan,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onEditNote).padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
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
                                hasStory = hasMine,
                                size = 62.dp,
                                onClick = openMine
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
                            )
                            .clickable(onClick = onCreateMoment),
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
        // one ring per person (not one per story)
        val others = moments.withIndex()
            .filter { !(it.value.author.id.isNotBlank() && it.value.author.id == currentUser.id) }
            .distinctBy { it.value.author.id.ifBlank { it.value.id } }
        items(others.size) { k ->
            val index = others[k].index
            val moment = others[k].value
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onOpenMoment(index) }
                    .testTag("moment_item_${moment.id}")
            ) {
                Box(modifier = Modifier.height(30.dp), contentAlignment = Alignment.BottomCenter) {
                    if (moment.author.hasActiveNote()) {
                        NoteBubble(moment.author.note, moment.author.noteSong, isDarkMode)
                    }
                }
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


@Composable
private fun DailyChallengeCard(user: User, isDarkMode: Boolean, onPostNow: () -> Unit) {
    val (tag, prompt) = remember { com.example.util.DailyChallenge.today() }
    val streak = user.activeStreak()
    val done = user.postedToday()
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isDarkMode) Color(0xFF131728) else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, VynoraNeonPink.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Today's challenge  $tag", color = VynoraNeonPink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(prompt, color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E293B), fontSize = 13.sp)
                Text(
                    when {
                        done && streak > 0 -> "Done for today ✅  🔥 $streak day streak"
                        streak > 0 -> "🔥 $streak day streak - post today to keep it"
                        else -> "Post today and start your streak 🔥"
                    },
                    color = Color(0xFF94A3B8), fontSize = 11.sp
                )
            }
            if (!done) {
                Button(
                    onClick = onPostNow,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink)
                ) { Text("Post", color = Color.White, fontSize = 13.sp) }
            }
        }
    }
}


private sealed class FeedItem(val key: String, val createdAt: Long) {
    class P(val post: Post) : FeedItem("p_" + post.id, post.createdAt)
    class C(val clip: com.example.model.Clip) : FeedItem("c_" + clip.id, clip.createdAt)
}

/** A reel shown inside the home feed. Tap opens the Clips tab. */
@Composable
private fun ReelFeedCard(
    clip: com.example.model.Clip,
    isDarkMode: Boolean,
    isOwner: Boolean,
    onOpen: () -> Unit,
    onAuthorClick: () -> Unit,
    onFollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fg = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val card = if (isDarkMode) Color(0xFF0F121E) else Color.White
    val thumb = remember(clip.mediaUrl, clip.thumbnailUrl) {
        val u = clip.mediaUrl
        com.example.util.reelPoster(u, clip.thumbnailUrl)
    }
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(20.dp)).background(card)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StoryRingAvatar(avatarUrl = clip.author.avatarUrl, size = 40.dp, onClick = onAuthorClick)
            Text(clip.author.username, color = fg, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            if (!isOwner && !clip.author.isFollowing && !clip.author.isOfficial && clip.author.id.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = VynoraNeonPink,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onFollow)
                ) {
                    Text("Follow", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().height(380.dp).background(Color(0xFF111827)).clickable(onClick = onOpen),
            contentAlignment = Alignment.Center
        ) {
            if (thumb.isNotBlank() && (!com.example.util.DataSaver.enabled || clip.thumbnailUrl.isNotBlank())) {
                AsyncImage(model = thumb, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            }
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play reel", tint = Color.White, modifier = Modifier.size(38.dp))
            }
            Text(
                "REEL",
                color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart).padding(12.dp).clip(RoundedCornerShape(8.dp)).background(Color(0x99000000)).padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        if (clip.caption.isNotBlank()) {
            Text(clip.caption, color = fg, fontSize = 14.sp, maxLines = 2, modifier = Modifier.padding(12.dp))
        } else Spacer(modifier = Modifier.height(8.dp))
    }
}
