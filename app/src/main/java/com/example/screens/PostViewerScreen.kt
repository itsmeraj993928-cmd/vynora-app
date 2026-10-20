package com.example.screens

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.components.StoryRingAvatar
import com.example.model.Post
import com.example.ui.theme.VynoraNeonPink
import com.example.ui.theme.VynoraNeonCyan
import com.example.util.formatCount

/** Full post view opened from a profile grid: media, author, like/comment/save and the whole caption. */
@Composable
fun PostViewerScreen(
    post: Post?,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onComments: () -> Unit,
    onUserClick: (String) -> Unit
) {
    val bg = if (isDarkMode) Color(0xFF050509) else Color.White
    val fg = if (isDarkMode) Color.White else Color(0xFF0F172A)
    Column(modifier = Modifier.fillMaxSize().background(bg).windowInsetsPadding(WindowInsets.statusBars)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = fg)
            }
            Text("Post", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = fg)
        }
        if (post == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("This post is no longer available", color = Color.Gray)
            }
            return@Column
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUserClick(post.author.id) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StoryRingAvatar(avatarUrl = post.author.avatarUrl, size = 38.dp, onClick = { onUserClick(post.author.id) })
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(post.author.username, fontWeight = FontWeight.Bold, color = fg, fontSize = 14.sp)
                        post.location?.takeIf { it.isNotBlank() }?.let { Text(it, color = Color.Gray, fontSize = 11.sp) }
                    }
                }
            }
            item {
                val urls = post.mediaUrls.filter { it.isNotBlank() }
                if (urls.isNotEmpty()) {
                    val pager = rememberPagerState(pageCount = { urls.size })
                    Box {
                        HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth()) { page ->
                            val url = urls[page]
                            if (post.isVideo || url.contains("/video/upload/")) {
                                AndroidView(
                                    modifier = Modifier.fillMaxWidth().height(420.dp),
                                    factory = { ctx ->
                                        VideoView(ctx).apply {
                                            val mc = MediaController(ctx)
                                            mc.setAnchorView(this)
                                            setMediaController(mc)
                                            setVideoURI(Uri.parse(url))
                                            setOnPreparedListener { it.isLooping = true; start() }
                                        }
                                    },
                                    onRelease = { try { it.stopPlayback() } catch (_: Throwable) { } }
                                )
                            } else {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.FillWidth
                                )
                            }
                        }
                        if (urls.size > 1) {
                            Text(
                                text = "${pager.currentPage + 1}/${urls.size}",
                                color = Color.White,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onLike) {
                        Icon(
                            if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (post.isLiked) VynoraNeonPink else fg
                        )
                    }
                    IconButton(onClick = onComments) {
                        Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comments", tint = fg)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onSave) {
                        Icon(
                            if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save",
                            tint = if (post.isSaved) VynoraNeonCyan else fg
                        )
                    }
                }
                Text(
                    "${formatCount(post.likesCount)} likes",
                    fontWeight = FontWeight.Bold, color = fg, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                if (post.caption.isNotBlank()) {
                    Text(
                        text = "${post.author.username}  ${post.caption}",
                        color = fg, fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                if (post.hashtags.isNotEmpty()) {
                    Text(
                        text = post.hashtags.joinToString(" ") { if (it.startsWith("#")) it else "#$it" },
                        color = VynoraNeonCyan, fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
                Text(
                    text = if (post.comments.isEmpty()) "Add a comment…" else "View all ${post.comments.size} comments",
                    color = Color.Gray, fontSize = 13.sp,
                    modifier = Modifier.clickable(onClick = onComments).padding(14.dp)
                )
            }
        }
    }
}
