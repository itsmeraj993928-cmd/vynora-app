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
    onDismiss: () -> Unit,
    /** Optional: search ANY account by username (not only people you follow) */
    search: (suspend (String) -> List<User>)? = null,
    /** Optional: shows an "Add to story" button */
    onAddToStory: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bg = if (isDarkMode) Color(0xFF0F121E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val subColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    var query by remember { mutableStateOf("") }
    val selectedUsers = remember { androidx.compose.runtime.mutableStateMapOf<String, User>() }
    val selectedIds = selectedUsers.keys
    fun toggle(u: User) { if (selectedUsers.containsKey(u.id)) selectedUsers.remove(u.id) else selectedUsers[u.id] = u }
    var remoteResults by remember { mutableStateOf<List<User>>(emptyList()) }
    val queryNow = query
    val targets by produceState<List<User>?>(initialValue = null) {
        value = try { loadTargets() } catch (e: Exception) { emptyList() }
    }

    LaunchedEffect(queryNow) {
        remoteResults = emptyList()
        val q = queryNow.trim()
        if (q.length >= 2 && search != null) {
            kotlinx.coroutines.delay(350)
            remoteResults = try { search(q) } catch (e: Exception) { emptyList() }
        }
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(23.dp))
                    .background(if (isDarkMode) Color(0xFF1C1F2B) else Color(0xFFEFF1F5))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = subColor, modifier = Modifier.size(20.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, color = textColor),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(VynoraNeonCyan),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (query.isEmpty()) Text("Search", fontSize = 15.sp, color = subColor)
                            inner()
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            val list = targets
            val localMatches = (list ?: emptyList()).filter {
                query.isBlank() ||
                    it.username.contains(query.trim(), ignoreCase = true) ||
                    it.fullName.contains(query.trim(), ignoreCase = true)
            }
            val filtered = (localMatches + remoteResults).distinctBy { it.id }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    list == null && filtered.isEmpty() -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = VynoraNeonCyan
                    )
                    filtered.isEmpty() -> Text(
                        text = if (query.isBlank()) "Follow people or start a chat to share with them."
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
                                        toggle(u)
                                    }
                                    .padding(4.dp)
                            ) {
                                Box(contentAlignment = Alignment.BottomEnd) {
                                    Box(
                                        modifier = Modifier
                                            .size(84.dp)
                                            .clip(CircleShape)
                                            .then(
                                                if (selected) Modifier.border(BorderStroke(3.dp, VynoraNeonCyan), CircleShape)
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        StoryRingAvatar(
                                            avatarUrl = u.avatarUrl,
                                            size = 78.dp,
                                            onClick = { toggle(u) }
                                        )
                                    }
                                    val ago = System.currentTimeMillis() - u.lastActiveAt
                                    if (!selected && u.lastActiveAt > 0L) {
                                        if (ago < 150_000L) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(4.dp)
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(bg)
                                                    .padding(3.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF22C55E))
                                            )
                                        } else if (ago < 86_400_000L) {
                                            Text(
                                                text = if (ago < 3_600_000L) "${ago / 60_000L}m" else "${ago / 3_600_000L}h",
                                                color = Color(0xFF16A34A),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (isDarkMode) Color(0xFF14301F) else Color(0xFFE7F8EC))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
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
                        onSend(selectedUsers.values.toList())
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
                    if (onAddToStory != null) ShareAction(Icons.Outlined.AddCircleOutline, "Add to story", isDarkMode, onAddToStory)
                    ShareAction(Icons.Outlined.Link, "Copy link", isDarkMode, onCopyLink)
                    ShareAction(Icons.Outlined.Chat, "WhatsApp", isDarkMode, onWhatsApp, circleColor = Color(0xFF25D366), iconTint = Color.White)
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
    onClick: () -> Unit,
    circleColor: Color? = null,
    iconTint: Color? = null
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
                .background(circleColor ?: if (isDarkMode) Color(0xFF1E2235) else Color(0xFFE2E8F0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint ?: if (isDarkMode) Color.White else Color(0xFF0F172A)
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
