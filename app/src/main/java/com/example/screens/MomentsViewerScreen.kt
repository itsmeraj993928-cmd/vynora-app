package com.example.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Moment
import com.example.model.User
import com.example.util.formatCount
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonEmerald
import com.example.ui.theme.VynoraNeonPink
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentsViewerScreen(
    moments: List<Moment>,
    initialIndex: Int,
    currentUser: User,
    onClose: () -> Unit,
    onSendReply: (momentId: String, text: String) -> Unit,
    onToast: (String) -> Unit,
    viewers: List<User> = emptyList(),
    onMomentShown: (Moment) -> Unit = {},
    pollVotes: com.example.model.PollVotes = com.example.model.PollVotes(),
    onVotePoll: (Moment, Int) -> Unit = { _, _ -> },
    onOpenProfile: (String) -> Unit = {},
    onDeleteMoment: (Moment) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (moments.isEmpty()) {
        onClose()
        return
    }

    // Only the tapped person's stories play together; when they end we move to the next person.
    val authorOrder = remember(moments) { moments.map { it.author.id }.distinct() }
    val startMoment = remember { moments[initialIndex.coerceIn(0, moments.lastIndex)] }
    var groupIdx by remember { mutableStateOf(authorOrder.indexOf(startMoment.author.id).coerceAtLeast(0)) }
    val authorId = authorOrder.getOrElse(groupIdx) { authorOrder.first() }
    val group = remember(moments, authorId) {
        moments.filter { it.author.id == authorId }.sortedBy { it.createdAt }
    }
    var currentIndex by remember { mutableStateOf(group.indexOfFirst { it.id == startMoment.id }.coerceAtLeast(0)) }
    val safeIndex = currentIndex.coerceIn(0, group.lastIndex)
    val currentMoment = group[safeIndex]
    var showDeleteStory by remember { mutableStateOf(false) }
    if (showDeleteStory) {
        AlertDialog(
            onDismissRequest = { showDeleteStory = false },
            title = { Text("Delete this story?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { showDeleteStory = false; onDeleteMoment(currentMoment) }) {
                    Text("Delete", color = Color(0xFFEF4444))
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteStory = false }) { Text("Cancel") } }
        )
    }

    fun goNext() {
        if (safeIndex < group.lastIndex) currentIndex = safeIndex + 1
        else if (groupIdx < authorOrder.lastIndex) { groupIdx++; currentIndex = 0 }
        else onClose()
    }
    fun goPrev() {
        if (safeIndex > 0) currentIndex = safeIndex - 1
        else if (groupIdx > 0) { groupIdx--; currentIndex = 0 }
    }
    var replyText by remember { mutableStateOf("") }
    var isLiked by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var showViewersSheet by remember { mutableStateOf(false) }

    val progressAnim = remember { Animatable(0f) }

    // Story music: play the attached song while this story is on screen
    val context = androidx.compose.ui.platform.LocalContext.current
    var songPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    DisposableEffect(currentMoment.id) {
        var mp: android.media.MediaPlayer? = null
        val url = currentMoment.musicUrl
        if (url.isNotBlank()) {
            try {
                mp = android.media.MediaPlayer().apply {
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(url)
                    isLooping = true
                    setOnPreparedListener { it.start() }
                    setOnErrorListener { _, _, _ -> true }
                    prepareAsync()
                }
            } catch (e: Exception) { mp = null }
        }
        songPlayer = mp
        onDispose {
            try { mp?.stop() } catch (e: Exception) { }
            try { mp?.release() } catch (e: Exception) { }
            if (songPlayer === mp) songPlayer = null
        }
    }
    // pause the song when the story is paused (long press) or the viewers list is open
    LaunchedEffect(isPaused, showViewersSheet, songPlayer) {
        val mp = songPlayer ?: return@LaunchedEffect
        try {
            if (isPaused || showViewersSheet) { if (mp.isPlaying) mp.pause() }
            else if (!mp.isPlaying) mp.start()
        } catch (e: Exception) { }
    }

    // Save my view (others' stories) or start loading viewers (my own story)
    LaunchedEffect(currentMoment.id) { onMomentShown(currentMoment) }

    // Auto-advance progress
    LaunchedEffect(currentMoment.id, isPaused, showViewersSheet) {
        progressAnim.snapTo(0f)
        if (!isPaused && !showViewersSheet) {
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 4500, easing = LinearEasing)
            )
            // Move to next story / next person, or close if last
            goNext()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("moments_viewer_screen")
    ) {
        // --- 1. Moment Media ---
        AsyncImage(
            model = currentMoment.mediaUrl,
            contentDescription = "Moment Media",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Gradient vignettes
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // --- 2. Tap zones (Left = Prev, Right = Next, Long Press = Pause) ---
        Row(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(currentMoment.id) {
                    var dragTotal = 0f
                    // swipe up = viewers list (own story), swipe down = close
                    detectVerticalDragGestures(
                        onDragStart = { dragTotal = 0f },
                        onDragEnd = {
                            if (dragTotal < -90f && currentMoment.author.id == currentUser.id) showViewersSheet = true
                            else if (dragTotal > 220f) onClose()
                        },
                        onVerticalDrag = { _, amount -> dragTotal += amount }
                    )
                }
                .pointerInput(currentMoment.id) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            if (offset.x < size.width * 0.35f) goPrev() else goNext()
                        }
                    )
                }
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.weight(1f))
        }

        // --- 3. Top Segments & Header ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Segmented Progress Bars
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                group.forEachIndexed { idx, _ ->
                    val segmentProgress = when {
                        idx < safeIndex -> 1f
                        idx == safeIndex -> progressAnim.value
                        else -> 0f
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(segmentProgress)
                                .background(Color.White)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Author Info Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.clickable { onOpenProfile(currentMoment.author.id) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    com.example.components.StoryRingAvatar(
                        avatarUrl = currentMoment.author.avatarUrl,
                        size = 38.dp,
                        onClick = { onOpenProfile(currentMoment.author.id) }
                    )

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentMoment.author.username,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )

                            if (currentMoment.isCloseFriendsOnly) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = VynoraNeonEmerald.copy(alpha = 0.3f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = "Close Friends",
                                            tint = VynoraNeonEmerald,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Text(
                                            text = "Close Friends",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = VynoraNeonEmerald
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "${currentMoment.timestamp} • Auto-expires in 24h",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }

                if (currentMoment.author.id == currentUser.id || currentUser.role == "ADMIN") {
                    IconButton(
                        onClick = { showDeleteStory = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete story",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close Moment",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Music track badge
            if (currentMoment.musicTrack != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = VynoraNeonCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = currentMoment.musicTrack,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }

        // --- 4. Center Text Overlay ---
        if (currentMoment.textOverlay != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.55f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VynoraNeonPink.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = currentMoment.textOverlay,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                    )
                }
            }
        }

        // --- 4b. Poll sticker ---
        if (currentMoment.pollQuestion.isNotBlank()) {
            val isMine = currentMoment.author.id == currentUser.id
            val showResults = isMine || pollVotes.mine >= 0
            val total = (pollVotes.a + pollVotes.b).coerceAtLeast(1)
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp).padding(bottom = 90.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.95f)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            currentMoment.pollQuestion,
                            color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        listOf(0 to currentMoment.pollOptionA, 1 to currentMoment.pollOptionB).forEach { (idx, label) ->
                            val count = if (idx == 0) pollVotes.a else pollVotes.b
                            val pct = count * 100 / total
                            val chosen = pollVotes.mine == idx
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFE2E8F0))
                                    .clickable(enabled = !isMine) { onVotePoll(currentMoment, idx) }
                            ) {
                                if (showResults) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(pct / 100f)
                                            .background(if (chosen) VynoraNeonPink.copy(alpha = 0.55f) else VynoraNeonCyan.copy(alpha = 0.4f))
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(label, color = Color(0xFF0F172A), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    if (showResults) Text("$pct%", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                        if (showResults) {
                            Text(
                                "${pollVotes.a + pollVotes.b} votes",
                                color = Color(0xFF64748B), fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // --- 5. Bottom Interactive Reply Bar ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Viewers info if own moment
            if (currentMoment.author.id == currentUser.id) {
                Row(
                    modifier = Modifier
                        .clickable { showViewersSheet = true }
                        .padding(bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = "Views",
                        tint = VynoraNeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${formatCount(maxOf(viewers.size, currentMoment.viewersCount))} viewers  •  swipe up",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    placeholder = { Text("Reply to ${currentMoment.author.username}...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = VynoraNeonCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.4f),
                        focusedContainerColor = Color.Black.copy(alpha = 0.4f),
                        unfocusedContainerColor = Color.Black.copy(alpha = 0.4f)
                    ),
                    singleLine = true,
                    trailingIcon = {
                        if (replyText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onSendReply(currentMoment.id, replyText)
                                    onToast("Sent reply to @${currentMoment.author.username} 💌")
                                    replyText = ""
                                }
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = "Send", tint = VynoraNeonPink)
                            }
                        }
                    }
                )

                IconButton(
                    onClick = {
                        isLiked = !isLiked
                        if (isLiked) onToast("Sent ❤️ to @${currentMoment.author.username}")
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) VynoraNeonPink else Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    // --- Viewers list (tap a viewer to open their profile) ---
    if (showViewersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showViewersSheet = false },
            containerColor = Color(0xFF0F121E)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Text(
                    text = "${viewers.size} viewers",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (viewers.isEmpty()) {
                    Text("No views yet", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                }
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(viewers.size) { i ->
                        val v = viewers[i]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    showViewersSheet = false
                                    onOpenProfile(v.id)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            com.example.components.StoryRingAvatar(
                                avatarUrl = v.avatarUrl,
                                size = 42.dp,
                                onClick = {
                                    showViewersSheet = false
                                    onOpenProfile(v.id)
                                }
                            )
                            Column {
                                Text("@${v.username}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                if (v.fullName.isNotBlank()) {
                                    Text(v.fullName, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
