package com.example.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.util.formatCount
import com.example.i18n.AppLanguage
import com.example.i18n.VynoraStrings
import com.example.model.Comment
import com.example.model.Post
import com.example.model.User
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: Post,
    currentUser: User,
    isDarkMode: Boolean,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onAddComment: (postId: String, text: String, replyToId: String?) -> Unit,
    onLikeComment: (commentId: String) -> Unit,
    onPinComment: (commentId: String) -> Unit,
    onDeleteComment: (commentId: String) -> Unit,
    onTranslateComment: (text: String, onResult: (String) -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentText by remember { mutableStateOf("") }
    var replyingTo by remember { mutableStateOf<Comment?>(null) }

    val sheetBg = if (isDarkMode) Color(0xFF0F121E) else Color(0xFFFFFFFF)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBg,
        tonalElevation = 16.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isDarkMode) Color(0xFF475569) else Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comments (${post.comments.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
            }

            HorizontalDivider(color = if (isDarkMode) Color(0x22FFFFFF) else Color(0x1F000000))

            // Comments List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (post.comments.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = null,
                                tint = VynoraNeonCyan.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No comments yet. Start the conversation!",
                                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(post.comments, key = { it.id }) { comment ->
                        CommentItemRow(
                            comment = comment,
                            isPostAuthor = post.author.id == currentUser.id,
                            isCommentAuthor = comment.author.id == currentUser.id,
                            isDarkMode = isDarkMode,
                            currentLanguage = currentLanguage,
                            onReplyClick = { replyingTo = comment },
                            onLike = { onLikeComment(comment.id) },
                            onPin = { onPinComment(comment.id) },
                            onDelete = { onDeleteComment(comment.id) },
                            onTranslate = onTranslateComment
                        )
                    }
                }
            }

            // Quick Emoji Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("❤️", "🔥", "✨", "👏", "😂", "🚀", "😍").forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .clickable { commentText += emoji }
                            .padding(4.dp)
                    )
                }
            }

            // Replying Indicator
            AnimatedVisibility(visible = replyingTo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDarkMode) Color(0x3300D1FF) else Color(0x1F0284C7))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Replying to @${replyingTo?.author?.username}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = VynoraNeonCyan
                    )
                    IconButton(
                        onClick = { replyingTo = null },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel reply",
                            tint = if (isDarkMode) Color.White else Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = 12.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = currentUser.avatarUrl,
                    contentDescription = "My Avatar",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                )

                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = {
                        Text(
                            text = replyingTo?.let { "Reply to @${it.author.username}..." }
                                ?: VynoraStrings.addComment(currentLanguage),
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VynoraNeonCyan,
                        unfocusedBorderColor = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000),
                        focusedContainerColor = if (isDarkMode) Color(0xFF161928) else Color(0xFFF1F5F9),
                        unfocusedContainerColor = if (isDarkMode) Color(0xFF161928) else Color(0xFFF1F5F9)
                    ),
                    singleLine = true,
                    trailingIcon = {
                        if (commentText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onAddComment(post.id, commentText, replyingTo?.id)
                                    commentText = ""
                                    replyingTo = null
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Send,
                                    contentDescription = "Send",
                                    tint = VynoraNeonPink
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun CommentItemRow(
    comment: Comment,
    isPostAuthor: Boolean,
    isCommentAuthor: Boolean,
    isDarkMode: Boolean,
    currentLanguage: AppLanguage,
    onReplyClick: () -> Unit,
    onLike: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit,
    onTranslate: (text: String, onResult: (String) -> Unit) -> Unit
) {
    var translatedText by remember { mutableStateOf<String?>(null) }
    var isTranslating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                model = comment.author.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
            )

            Column(modifier = Modifier.weight(1f)) {
                // Header with Pinned badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = comment.author.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                    if (comment.isPinned) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = VynoraNeonPink.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "PINNED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = VynoraNeonPink,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Text
                Text(
                    text = translatedText ?: comment.text,
                    fontSize = 13.sp,
                    color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                    lineHeight = 17.sp
                )

                // Actions: timestamp, reply, translate, delete
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = comment.timestamp,
                        fontSize = 11.sp,
                        color = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)
                    )
                    Text(
                        text = "Reply",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
                        modifier = Modifier.clickable(onClick = onReplyClick)
                    )
                    // AI Translate Action
                    Text(
                        text = if (isTranslating) "Translating..." else if (translatedText == null) VynoraStrings.autoTranslate(currentLanguage) else "Original",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = VynoraNeonCyan,
                        modifier = Modifier.clickable {
                            if (translatedText != null) {
                                translatedText = null
                            } else {
                                isTranslating = true
                                onTranslate(comment.text) { result ->
                                    isTranslating = false
                                    translatedText = result
                                }
                            }
                        }
                    )

                    if (isPostAuthor) {
                        Text(
                            text = if (comment.isPinned) "Unpin" else "Pin",
                            fontSize = 11.sp,
                            color = VynoraNeonPink,
                            modifier = Modifier.clickable(onClick = onPin)
                        )
                    }

                    if (isCommentAuthor) {
                        Text(
                            text = "Delete",
                            fontSize = 11.sp,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.clickable(onClick = onDelete)
                        )
                    }
                }
            }

            // Like Comment Icon
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = onLike,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (comment.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (comment.isLiked) VynoraNeonPink else (if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)),
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (comment.likesCount > 0) {
                    Text(
                        text = formatCount(comment.likesCount),
                        fontSize = 10.sp,
                        color = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)
                    )
                }
            }
        }

        // Nested Replies
        if (comment.replies.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(start = 44.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                comment.replies.forEach { reply ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AsyncImage(
                            model = reply.author.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                        )
                        Column {
                            Text(
                                text = reply.author.username,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                            Text(
                                text = reply.text,
                                fontSize = 12.sp,
                                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
                            )
                        }
                    }
                }
            }
        }
    }
}
