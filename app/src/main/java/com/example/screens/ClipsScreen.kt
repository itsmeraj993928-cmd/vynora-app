package com.example.screens

import android.net.Uri
import android.widget.VideoView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.components.StoryRingAvatar
import com.example.components.VynoraOfficialBadge
import com.example.components.VynoraPlusBadge
import com.example.model.Clip
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink
import com.example.util.formatCount

@Composable
fun ClipsScreen(
    clips: List<Clip>,
    onLikeToggle: (String) -> Unit,
    onSaveToggle: (String) -> Unit,
    onFollowToggle: (String) -> Unit,
    onCommentsClick: (String) -> Unit,
    onShareClick: (Clip) -> Unit,
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier,
    currentUserId: String = "",
    onDeleteClip: (String) -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onBack: (() -> Unit)? = null,
    initialIndex: Int = 0,
    onCreateClick: (() -> Unit)? = null,
    onReport: (clipId: String, username: String) -> Unit = { _, _ -> },
    onView: (String) -> Unit = {}
) {
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, maxOf(0, clips.size - 1)), pageCount = { clips.size })
    LaunchedEffect(pagerState.currentPage, clips.size) {
        clips.getOrNull(pagerState.currentPage)?.let { onView(it.id) }
    }
    var isMuted by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("clips_screen")
    ) {
        if (clips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = null,
                        tint = VynoraNeonPink,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Clips yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Be the first creator to record or upload a short Clip to VYNORA!",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            VerticalPager(
                state = pagerState,
                beyondViewportPageCount = if (com.example.util.DataSaver.enabled) 0 else 1,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val clip = clips[page]
                ClipItemView(
                    clip = clip,
                    isMuted = isMuted,
                    onMuteToggle = { isMuted = !isMuted },
                    onLikeToggle = { onLikeToggle(clip.id) },
                    onSaveToggle = { onSaveToggle(clip.id) },
                    onFollowToggle = { onFollowToggle(clip.author.username) },
                    onCommentsClick = { onCommentsClick(clip.id) },
                    onShareClick = { onShareClick(clip) },
                    isCurrentPage = pagerState.currentPage == page,
                    isOwner = currentUserId.isNotBlank() && clip.author.id == currentUserId,
                    onDelete = { onDeleteClip(clip.id) },
                    onUserClick = { onUserClick(clip.author.id) },
                    onToast = onToast,
                    onReport = { onReport(clip.id, clip.author.username) }
                )
            }
        }

        // Top bar overlay with "Clips" and Mute Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                if (onBack != null) {
                    Text(
                        text = "Reels",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                } else if (onCreateClick != null) {
                    IconButton(
                        onClick = onCreateClick,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .border(1.5.dp, Color.White, CircleShape)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Create", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            IconButton(
                onClick = {
                    isMuted = !isMuted
                    onToast(if (isMuted) "Audio muted 🔇" else "Audio unmuted 🔊")
                },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = "Mute Toggle",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ClipItemView(
    clip: Clip,
    isMuted: Boolean,
    onMuteToggle: () -> Unit,
    onLikeToggle: () -> Unit,
    onSaveToggle: () -> Unit,
    onFollowToggle: () -> Unit,
    onCommentsClick: () -> Unit,
    onShareClick: () -> Unit,
    isCurrentPage: Boolean,
    isOwner: Boolean,
    onDelete: () -> Unit,
    onUserClick: () -> Unit = {},
    onToast: (String) -> Unit = {},
    onReport: () -> Unit = {}
) {
    var captionExpanded by remember { mutableStateOf(false) }
    val clipCtx = androidx.compose.ui.platform.LocalContext.current
    // Data Saver on mobile data: reels wait for a tap instead of playing by themselves
    var isPlaying by remember { mutableStateOf(!(com.example.util.DataSaver.enabled && com.example.util.DataSaver.onMeteredNetwork(clipCtx))) }
    var heartVisible by remember { mutableStateOf(false) }
    LaunchedEffect(heartVisible) {
        if (heartVisible) {
            delay(700)
            heartVisible = false
        }
    }
    val heartAlpha by animateFloatAsState(if (heartVisible) 1f else 0f, tween(250), label = "heart_alpha")
    val heartScale by animateFloatAsState(if (heartVisible) 1.2f else 0.5f, spring(), label = "heart_scale")
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this clip?") },
            text = { Text("This will remove your clip for everyone. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) { Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { isPlaying = !isPlaying },
                    onDoubleTap = {
                        if (!clip.isLiked) onLikeToggle()
                        heartVisible = true
                    }
                )
            }
    ) {
        // Real video player
        val appActive by com.example.util.rememberAppActive()
        ClipVideoPlayer(
            url = clip.mediaUrl,
            shouldPlay = isPlaying && isCurrentPage && appActive,
            isMuted = isMuted,
            musicUrl = clip.musicUrl,
            posterUrl = com.example.util.reelPoster(clip.mediaUrl, clip.thumbnailUrl),
            modifier = Modifier.fillMaxSize()
        )

        // Gradient shadows for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Double-tap heart
        if (heartAlpha > 0.01f) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = VynoraNeonPink,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(110.dp)
                    .graphicsLayer {
                        alpha = heartAlpha
                        scaleX = heartScale
                        scaleY = heartScale
                    }
            )
        }

        // Play/Pause Center Indicator
        if (!isPlaying) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Paused",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // --- Right Action Stack ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Creator Avatar with Follow '+' button
            Box(
                modifier = Modifier.size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape)
                ) {
                    StoryRingAvatar(
                        avatarUrl = clip.author.avatarUrl,
                        size = 46.dp,
                        onClick = onUserClick
                    )
                }

                if (!clip.isFollowing && !isOwner) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 4.dp)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(VynoraNeonPink)
                            .clickable(onClick = onFollowToggle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Follow",
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }

            // Like Action
            ActionIconWithCount(
                icon = if (clip.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                count = if (clip.hideLikeCount && !isOwner) "Like" else formatCount(clip.likesCount),
                tint = if (clip.isLiked) VynoraNeonPink else Color.White,
                onClick = onLikeToggle
            )

            // Comment Action
            ActionIconWithCount(
                icon = Icons.Outlined.ChatBubbleOutline,
                count = if (clip.commentsOff) "Off" else formatCount(clip.commentsCount),
                tint = Color.White,
                onClick = if (clip.commentsOff) ({ onToast("Comments are turned off for this reel") }) else onCommentsClick
            )

            // Save Action
            ActionIconWithCount(
                icon = if (clip.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                count = "Save",
                tint = if (clip.isSaved) VynoraNeonCyan else Color.White,
                onClick = onSaveToggle
            )

            // Share Action
            ActionIconWithCount(
                icon = Icons.Outlined.Send,
                count = if (clip.hideShareCount && !isOwner) "Share" else formatCount(clip.sharesCount),
                tint = Color.White,
                onClick = onShareClick
            )

            // More options (three dots): Delete for the creator, Report for everyone else
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "More",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp).clickable { menuOpen = true }
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (isOwner) {
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                            onClick = { menuOpen = false; showDeleteConfirm = true }
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Report") },
                            leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null) },
                            onClick = { menuOpen = false; onReport() }
                        )
                    }
                }
            }
        }

        // --- Bottom Info Stack ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 28.dp)
        ) {
            // Username & Follow pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StoryRingAvatar(
                    avatarUrl = clip.author.avatarUrl,
                    size = 32.dp,
                    onClick = onUserClick
                )
                Text(
                    text = "@${clip.author.username}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier.clickable(onClick = onUserClick)
                )

                if (clip.author.isOfficial) {
                    VynoraOfficialBadge(size = 15.dp)
                } else if (clip.author.isPlusActive) {
                    VynoraPlusBadge(size = 15.dp)
                }

                if (!isOwner && !clip.isFollowing) Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (clip.isFollowing) Color.White.copy(alpha = 0.2f) else VynoraNeonPink,
                    modifier = Modifier.clickable(onClick = onFollowToggle)
                ) {
                    Text(
                        text = if (clip.isFollowing) "Following" else "Follow",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Caption
            Text(
                text = clip.caption,
                fontSize = 13.sp,
                color = Color.White,
                lineHeight = 17.sp,
                maxLines = if (captionExpanded) Int.MAX_VALUE else 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier
                    .clickable { captionExpanded = !captionExpanded }
                    .then(
                        if (captionExpanded) Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(androidx.compose.foundation.rememberScrollState())
                        else Modifier
                    )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Music Ticker
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = VynoraNeonCyan,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${clip.musicTitle} • ${clip.musicArtist}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ActionIconWithCount(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = count,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

// Cloudinary can deliver any uploaded video as H.264 MP4, which every Android phone can play.
private fun toPlayableVideoUrl(url: String): String {
    if (!url.contains("res.cloudinary.com") || !url.contains("/video/upload/")) return url
    if (url.contains("/upload/f_mp4")) return url
    val lastSlash = url.lastIndexOf('/')
    val lastDot = url.lastIndexOf('.')
    val withMp4Ext = if (lastDot > lastSlash) url.substring(0, lastDot) + ".mp4" else "$url.mp4"
    return withMp4Ext.replaceFirst("/upload/", "/upload/f_mp4,vc_h264,ac_aac/")
}

@Composable
private fun ClipVideoPlayer(
    url: String,
    shouldPlay: Boolean,
    isMuted: Boolean,
    musicUrl: String = "",
    posterUrl: String = "",
    modifier: Modifier = Modifier
) {
    val videoUrl = remember(url) { toPlayableVideoUrl(url) }
    val hasSong = musicUrl.isNotBlank()

    // Song chosen for this reel: it plays instead of the video's own sound
    var song by remember(musicUrl) { mutableStateOf<android.media.MediaPlayer?>(null) }
    DisposableEffect(musicUrl) {
        var mp: android.media.MediaPlayer? = null
        if (hasSong) {
            try {
                mp = android.media.MediaPlayer().apply {
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(musicUrl)
                    isLooping = true
                    setOnPreparedListener { song = it }
                    setOnErrorListener { _, _, _ -> true }
                    prepareAsync()
                }
            } catch (e: Exception) { mp = null }
        }
        onDispose {
            try { mp?.stop() } catch (e: Exception) { }
            try { mp?.release() } catch (e: Exception) { }
            song = null
        }
    }
    LaunchedEffect(song, shouldPlay, isMuted) {
        val sp = song ?: return@LaunchedEffect
        try {
            if (shouldPlay && !isMuted) { if (!sp.isPlaying) sp.start() }
            else if (sp.isPlaying) sp.pause()
        } catch (e: Exception) { }
    }
    var hasError by remember(videoUrl) { mutableStateOf(false) }
    var isReady by remember(videoUrl) { mutableStateOf(false) }
    var player by remember(videoUrl) { mutableStateOf<android.media.MediaPlayer?>(null) }
    // keep the still picture on screen until the video has really started, so scrolling never flashes black
    var showPoster by remember(videoUrl) { mutableStateOf(true) }
    LaunchedEffect(isReady, shouldPlay) {
        if (isReady && shouldPlay) { kotlinx.coroutines.delay(300); showPoster = false }
    }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                VideoView(ctx).apply {
                    setOnPreparedListener { mp ->
                        mp.isLooping = true
                        player = mp
                        isReady = true
                    }
                    setOnErrorListener { _, _, _ ->
                        hasError = true
                        true
                    }
                    setVideoURI(Uri.parse(videoUrl))
                }
            },
            update = { vv ->
                val volume = if (isMuted || hasSong) 0f else 1f
                player?.let {
                    try { it.setVolume(volume, volume) } catch (_: Throwable) { }
                }
                if (shouldPlay) {
                    if (!vv.isPlaying) vv.start()
                } else {
                    if (vv.isPlaying) vv.pause()
                }
            },
            onRelease = { vv ->
                try { vv.stopPlayback() } catch (_: Throwable) { }
            }
        )
        if (showPoster && !hasError && posterUrl.isNotBlank()) {
            coil.compose.AsyncImage(model = posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
        }
        if (hasError) {
            Text(
                text = "Video could not be loaded",
                color = Color.White,
                fontSize = 13.sp
            )
        }
    }
}
