package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.PostCard
import com.example.i18n.AppLanguage
import com.example.model.Post

/** Full post opened from a profile grid: the same card as the home feed (caption, share, menu, delete). */
@Composable
fun PostViewerScreen(
    post: Post?,
    isDarkMode: Boolean,
    currentLanguage: AppLanguage,
    isOwner: Boolean,
    onBack: () -> Unit,
    onLike: () -> Unit,
    onSave: () -> Unit,
    onComments: () -> Unit,
    onShare: () -> Unit,
    onReport: () -> Unit,
    onDelete: () -> Unit,
    onUserClick: (String) -> Unit
) {
    val bg = if (isDarkMode) Color(0xFF050509) else Color.White
    val fg = if (isDarkMode) Color.White else Color(0xFF0F172A)
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this post?") },
            text = { Text("This will remove your post for everyone. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) {
                    Text("Delete", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }

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
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 110.dp)) {
            item {
                PostCard(
                    post = post,
                    isDarkMode = isDarkMode,
                    currentLanguage = currentLanguage,
                    onLikeToggle = onLike,
                    onSaveToggle = onSave,
                    onCommentsClick = onComments,
                    onShareClick = onShare,
                    onReportClick = onReport,
                    onAuthorClick = { onUserClick(post.author.id) },
                    isOwner = isOwner,
                    onDeleteClick = { confirmDelete = true }
                )
            }
        }
    }
}
