package com.example.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { clips.size })
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
                    onShareClick = { onShareClick(clip) }
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
            Text(
                text = "CLIPS",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.sp,
                color = Color.White
            )

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
    onShareClick: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }

    // Vinyl spinning rotation
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing)
        ),
        label = "vinyl_rotate"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { isPlaying = !isPlaying }
                )
            }
    ) {
        // Media Image / Simulated Video
        AsyncImage(
            model = clip.mediaUrl,
            contentDescription = "Clip Media",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
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
                    AsyncImage(
                        model = clip.author.avatarUrl,
                        contentDescription = clip.author.username,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (!clip.isFollowing) {
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
                count = formatCount(clip.likesCount),
                tint = if (clip.isLiked) VynoraNeonPink else Color.White,
                onClick = onLikeToggle
            )

            // Comment Action
            ActionIconWithCount(
                icon = Icons.Outlined.ChatBubbleOutline,
                count = formatCount(clip.commentsCount),
                tint = Color.White,
                onClick = onCommentsClick
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
                count = formatCount(clip.sharesCount),
                tint = Color.White,
                onClick = onShareClick
            )

            // Rotating Vinyl Disc
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .rotate(rotation)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2235))
                    .border(2.dp, Brush.sweepGradient(listOf(VynoraNeonPink, VynoraNeonCyan, VynoraNeonPink)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                ) {
                    AsyncImage(
                        model = clip.author.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // --- Bottom Info Stack ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .padding(start = 16.dp, bottom = 90.dp)
        ) {
            // Username & Follow pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "@${clip.author.username}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )

                if (clip.author.isOfficial) {
                    VynoraOfficialBadge(size = 15.dp)
                } else if (clip.author.isPlusActive) {
                    VynoraPlusBadge(size = 15.dp)
                }

                Surface(
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
                maxLines = 2
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
