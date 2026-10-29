package com.example.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.i18n.AppLanguage
import com.example.i18n.VynoraStrings
import com.example.model.Post
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PostCard(
    post: Post,
    isDarkMode: Boolean,
    currentLanguage: AppLanguage,
    onLikeToggle: () -> Unit,
    onSaveToggle: () -> Unit,
    onCommentsClick: () -> Unit,
    onShareClick: () -> Unit,
    onReportClick: () -> Unit,
    onAuthorClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOwner: Boolean = false,
    onDeleteClick: () -> Unit = {},
    onNotInterested: (() -> Unit)? = null,
    onRepost: (() -> Unit)? = null,
    onHashtagClick: (String) -> Unit = {},
    onSaveToFolder: (() -> Unit)? = null
) {
    var showBigHeart by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var isCaptionExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val cardBg = if (isDarkMode) Color(0xFF0F121E) else Color(0xFFFFFFFF)
    val cardBorder = if (isDarkMode) Color(0x2800D1FF) else Color(0x1F000000)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
            .padding(bottom = 14.dp)
            .testTag("post_card_${post.id}")
    ) {
        // --- 1. Post Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .clickable(onClick = onAuthorClick)
                    .weight(1f)
            ) {
                StoryRingAvatar(
                    avatarUrl = post.author.avatarUrl,
                    hasStory = true,
                    size = 42.dp,
                    onClick = onAuthorClick
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = post.author.username,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                        if (post.author.isOfficial) {
                            VynoraOfficialBadge(size = 15.dp)
                        } else if (post.author.isPlusActive) {
                            VynoraPlusBadge(size = 15.dp)
                        }
                    }

                    if (post.location != null) {
                        Text(
                            text = post.location,
                            fontSize = 11.sp,
                            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                        )
                    }
                }
            }

            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "Options",
                        tint = if (isDarkMode) Color.White else Color.Black
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(if (isDarkMode) Color(0xFF1E2235) else Color.White)
                ) {
                    if (isOwner) {
                        DropdownMenuItem(
                            text = { Text("Delete Post", color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            }
                        )
                    } else {
                        if (onRepost != null) {
                            DropdownMenuItem(
                                text = { Text("Repost") },
                                leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onRepost()
                                }
                            )
                        }
                        if (onNotInterested != null) {
                            DropdownMenuItem(
                                text = { Text("Not interested") },
                                leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onNotInterested()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Report Post", color = Color(0xFFEF4444)) },
                            leadingIcon = { Icon(Icons.Outlined.Flag, contentDescription = null, tint = Color(0xFFEF4444)) },
                            onClick = {
                                menuExpanded = false
                                onReportClick()
                            }
                        )
                    }
                    if (onSaveToFolder != null) {
                        DropdownMenuItem(
                            text = { Text("Save to folder") },
                            leadingIcon = { Icon(Icons.Outlined.BookmarkBorder, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onSaveToFolder()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Share Link") },
                        leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onShareClick()
                        }
                    )
                }
            }
        }

        // --- 2. Media Carousel with Double-Tap to Like ---
        val pagerState = rememberPagerState(pageCount = { post.mediaUrls.size })

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.05f)
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (!post.isLiked) onLikeToggle()
                            showBigHeart = true
                            coroutineScope.launch {
                                delay(900)
                                showBigHeart = false
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                AsyncImage(
                    model = post.mediaUrls[page],
                    contentDescription = post.altText.ifBlank { "Photo by ${post.author.username}" },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Big Double-Tap Heart Animation
            androidx.compose.animation.AnimatedVisibility(
                visible = showBigHeart,
                enter = scaleIn(initialScale = 0.3f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut(targetScale = 1.3f) + fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Liked",
                    tint = VynoraNeonPink,
                    modifier = Modifier.size(100.dp)
                )
            }

            // Carousel dots indicator if multiple images
            if (post.mediaUrls.size > 1) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(post.mediaUrls.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 7.dp else 5.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) VynoraNeonCyan else Color.White.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }

            // Audio track badge if present
            if (post.audioTrack != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = VynoraNeonCyan,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = post.audioTrack,
                        fontSize = 11.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }

        // --- 3. Action Buttons Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Like Button
                IconButton(
                    onClick = onLikeToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (post.isLiked) VynoraNeonPink else (if (isDarkMode) Color.White else Color.Black),
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Comment Button
                IconButton(
                    onClick = onCommentsClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = if (isDarkMode) Color.White else Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Share Button
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Send,
                        contentDescription = "Share",
                        tint = if (isDarkMode) Color.White else Color.Black,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            // Bookmark / Save
            IconButton(
                onClick = onSaveToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save",
                    tint = if (post.isSaved) VynoraNeonCyan else (if (isDarkMode) Color.White else Color.Black),
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // --- 4. Likes & Caption ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
        ) {
            Text(
                text = VynoraStrings.likes(currentLanguage, post.likesCount),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Caption
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                val textColor = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E293B)
                Column {
                    Text(
                        text = buildString {
                            append("${post.author.username} ")
                            append(post.caption)
                        },
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = textColor,
                        maxLines = if (isCaptionExpanded) Int.MAX_VALUE else 2
                    )

                    if (post.caption.length > 80 && !isCaptionExpanded) {
                        Text(
                            text = "...more",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = VynoraNeonCyan,
                            modifier = Modifier
                                .clickable { isCaptionExpanded = true }
                                .padding(top = 2.dp)
                        )
                    }
                }
            }

            // Hashtags
            if (post.hashtags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    post.hashtags.forEach { tag ->
                        Text(
                            text = tag,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = VynoraNeonCyan,
                            lineHeight = 16.sp,
                            modifier = Modifier.clickable { onHashtagClick(tag) }
                        )
                    }
                }
            }

            // Comments Preview
            if (post.comments.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = VynoraStrings.comments(currentLanguage, post.comments.size),
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                    modifier = Modifier.clickable(onClick = onCommentsClick)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = post.timestamp,
                fontSize = 10.sp,
                color = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)
            )
        }
    }
}
