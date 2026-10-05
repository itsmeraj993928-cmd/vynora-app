package com.example.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
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

@Composable
fun MomentsViewerScreen(
    moments: List<Moment>,
    initialIndex: Int,
    currentUser: User,
    onClose: () -> Unit,
    onSendReply: (momentId: String, text: String) -> Unit,
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (moments.isEmpty()) {
        onClose()
        return
    }

    var currentIndex by remember { mutableStateOf(initialIndex.coerceIn(0, moments.lastIndex)) }
    val currentMoment = moments[currentIndex]
    var replyText by remember { mutableStateOf("") }
    var isLiked by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var showViewersSheet by remember { mutableStateOf(false) }

    val progressAnim = remember { Animatable(0f) }

    // Auto-advance progress
    LaunchedEffect(currentIndex, isPaused) {
        progressAnim.snapTo(0f)
        if (!isPaused) {
            progressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 4500, easing = LinearEasing)
            )
            // Move to next moment or close if last
            if (currentIndex < moments.lastIndex) {
                currentIndex++
            } else {
                onClose()
            }
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
                .pointerInput(currentIndex) {
                    detectTapGestures(
                        onPress = {
                            isPaused = true
                            tryAwaitRelease()
                            isPaused = false
                        },
                        onTap = { offset ->
                            if (offset.x < size.width * 0.35f) {
                                if (currentIndex > 0) currentIndex--
                            } else {
                                if (currentIndex < moments.lastIndex) currentIndex++ else onClose()
                            }
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
                moments.forEachIndexed { idx, _ ->
                    val segmentProgress = when {
                        idx < currentIndex -> 1f
                        idx == currentIndex -> progressAnim.value
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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                    ) {
                        AsyncImage(
                            model = currentMoment.author.avatarUrl,
                            contentDescription = currentMoment.author.username,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

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
                        text = "${formatCount(currentMoment.viewersCount)} viewers",
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
}
