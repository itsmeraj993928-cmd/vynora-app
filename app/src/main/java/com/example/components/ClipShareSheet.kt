package com.example.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.User
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink

/**
 * In-app share sheet (Instagram style): search people, pick one or more,
 * and send the reel straight into their chat. Quick actions below for
 * copy link, WhatsApp and the system share menu.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClipShareSheet(
    isDarkMode: Boolean,
    loadTargets: suspend () -> List<User>,
    onSend: (List<User>) -> Unit,
    onCopyLink: () -> Unit,
    onWhatsApp: () -> Unit,
    onMore: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bg = if (isDarkMode) Color(0xFF0F121E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val subColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    var query by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val targets by produceState<List<User>?>(initialValue = null) {
        value = try { loadTargets() } catch (e: Exception) { emptyList() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VynoraNeonCyan,
                    unfocusedBorderColor = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000)
                )
            )
            Spacer(modifier = Modifier.height(12.dp))

            val list = targets
            val filtered = (list ?: emptyList()).filter {
                query.isBlank() ||
                    it.username.contains(query.trim(), ignoreCase = true) ||
                    it.fullName.contains(query.trim(), ignoreCase = true)
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    list == null -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = VynoraNeonCyan
                    )
                    filtered.isEmpty() -> Text(
                        text = if (list.isEmpty()) "Follow people or start a chat to share reels with them."
                        else "No one found",
                        color = subColor,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp)
                    )
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(filtered, key = { it.id }) { u ->
                            val selected = u.id in selectedIds
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedIds = if (selected) selectedIds - u.id else selectedIds + u.id
                                    }
                                    .padding(4.dp)
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(CircleShape)
                                            .then(
                                                if (selected) Modifier.border(BorderStroke(3.dp, VynoraNeonCyan), CircleShape)
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        StoryRingAvatar(
                                            avatarUrl = u.avatarUrl,
                                            size = 66.dp,
                                            onClick = {
                                                selectedIds = if (selected) selectedIds - u.id else selectedIds + u.id
                                            }
                                        )
                                    }
                                    if (selected) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(VynoraNeonCyan),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = u.fullName.ifBlank { u.username },
                                    color = textColor,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedIds.isNotEmpty()) {
                Button(
                    onClick = {
                        val chosen = (targets ?: emptyList()).filter { it.id in selectedIds }
                        onSend(chosen)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink)
                ) {
                    Text(
                        text = if (selectedIds.size == 1) "Send" else "Send separately (${selectedIds.size})",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ShareAction(Icons.Outlined.Link, "Copy link", isDarkMode, onCopyLink)
                    ShareAction(Icons.Outlined.Chat, "WhatsApp", isDarkMode, onWhatsApp)
                    ShareAction(Icons.Outlined.Share, "Share", isDarkMode, onMore)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShareAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isDarkMode) Color(0xFF1E2235) else Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
        )
    }
}
