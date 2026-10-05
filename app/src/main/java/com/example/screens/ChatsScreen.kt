package com.example.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.ChatConversation
import com.example.model.ChatMessage
import com.example.model.User
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink
import kotlinx.coroutines.launch

@Composable
fun ChatsScreen(
    chats: List<ChatConversation>,
    isDarkMode: Boolean,
    onOpenConversation: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0: Primary, 1: Requests

    val screenBg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF0F121E) else Color(0xFFFFFFFF)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("chats_screen")
    ) {
        // --- 1. Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                }
                Text(
                    text = "Chats",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
            }

            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "New Chat",
                    tint = VynoraNeonPink
                )
            }
        }

        // --- 2. Search Messages ---
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search messages & creators...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = VynoraNeonCyan) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VynoraNeonCyan,
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // --- 3. Tabs (Primary / Requests) ---
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = VynoraNeonPink,
            divider = { HorizontalDivider(color = if (isDarkMode) Color(0x22FFFFFF) else Color(0x1F000000)) }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Primary (${chats.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Requests (2)", fontWeight = FontWeight.Medium, fontSize = 13.sp) }
            )
        }

        // --- 4. Chat List ---
        val filteredChats = if (searchQuery.isBlank()) chats else {
            chats.filter {
                it.participant.username.contains(searchQuery, ignoreCase = true) ||
                it.lastMessage.text.contains(searchQuery, ignoreCase = true)
            }
        }

        if (filteredChats.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = VynoraNeonCyan,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No chats yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Start a direct conversation with creators or friends on VYNORA.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                items(filteredChats, key = { it.id }) { conv ->
                    ChatConversationRow(
                        conversation = conv,
                        isDarkMode = isDarkMode,
                        onClick = { onOpenConversation(conv.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatConversationRow(
    conversation: ChatConversation,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar with online green dot
        Box(modifier = Modifier.size(52.dp)) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
            ) {
                AsyncImage(
                    model = conversation.participant.avatarUrl,
                    contentDescription = conversation.participant.username,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Online status dot
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(13.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF10B981))
                    .border(2.dp, if (isDarkMode) Color(0xFF050509) else Color.White, CircleShape)
            )
        }

        // Message Preview
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.participant.fullName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
                Text(
                    text = conversation.lastMessage.timestamp,
                    fontSize = 11.sp,
                    color = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (conversation.lastMessage.isAudio) "🎤 Voice note (${conversation.lastMessage.audioDuration ?: "0:34"})" else conversation.lastMessage.text,
                    fontSize = 13.sp,
                    color = if (conversation.unreadCount > 0) (if (isDarkMode) Color.White else Color.Black) else (if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)),
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                if (conversation.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(VynoraNeonPink),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = conversation.unreadCount.toString(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatRoomScreen(
    conversation: ChatConversation,
    currentUser: User,
    isDarkMode: Boolean,
    onSendMessage: (text: String, isAudio: Boolean, duration: String?) -> Unit,
    onAddReaction: (messageId: String, emoji: String) -> Unit,
    onBack: () -> Unit,
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showReactionMenuForMessageId by remember { mutableStateOf<String?>(null) }
    var isRecordingAudio by remember { mutableStateOf(false) }

    LaunchedEffect(conversation.messages.size) {
        listState.animateScrollToItem((conversation.messages.size - 1).coerceAtLeast(0))
    }

    val screenBg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF0F121E) else Color(0xFFFFFFFF)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("chat_room_screen")
    ) {
        // --- 1. Top Room Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                ) {
                    AsyncImage(
                        model = conversation.participant.avatarUrl,
                        contentDescription = conversation.participant.username,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column {
                    Text(
                        text = conversation.participant.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Active now",
                        fontSize = 10.sp,
                        color = Color(0xFF10B981)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { onToast("Calling @${conversation.participant.username}...") }) {
                    Icon(imageVector = Icons.Outlined.Call, contentDescription = "Call", tint = VynoraNeonCyan)
                }
                IconButton(onClick = { onToast("Video calling @${conversation.participant.username}...") }) {
                    Icon(imageVector = Icons.Outlined.Videocam, contentDescription = "Video", tint = VynoraNeonPink)
                }
            }
        }

        // --- 2. Messages List ---
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(conversation.messages, key = { it.id }) { msg ->
                val isMe = msg.senderId == currentUser.id
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Column(
                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                        modifier = Modifier.pointerInput(msg.id) {
                            detectTapGestures(
                                onLongPress = { showReactionMenuForMessageId = msg.id }
                            )
                        }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isMe) 16.dp else 4.dp,
                                bottomEnd = if (isMe) 4.dp else 16.dp
                            ),
                            color = if (isMe) VynoraNeonPink else (if (isDarkMode) Color(0xFF1E2235) else Color(0xFFE2E8F0)),
                            tonalElevation = 2.dp
                        ) {
                            if (msg.isAudio) {
                                // Simulated Waveform Audio Player
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = "Play",
                                        tint = if (isMe) Color.White else VynoraNeonCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    // Waveform bars simulation
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                                        listOf(8, 16, 24, 12, 28, 14, 20, 8, 18, 10).forEach { h ->
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(h.dp)
                                                    .clip(RoundedCornerShape(1.5.dp))
                                                    .background(if (isMe) Color.White.copy(alpha = 0.8f) else VynoraNeonCyan)
                                            )
                                        }
                                    }
                                    Text(
                                        text = msg.audioDuration ?: "0:34",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isMe) Color.White else (if (isDarkMode) Color.White else Color.Black)
                                    )
                                }
                            } else {
                                Text(
                                    text = msg.text,
                                    fontSize = 14.sp,
                                    color = if (isMe) Color.White else (if (isDarkMode) Color.White else Color(0xFF0F172A)),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                )
                            }
                        }

                        // Emoji reaction badge if attached
                        if (msg.reactionEmoji != null) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDarkMode) Color(0xFF0F121E) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300D1FF)),
                                modifier = Modifier.offset(y = (-8).dp)
                            ) {
                                Text(
                                    text = msg.reactionEmoji,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = msg.timestamp,
                            fontSize = 9.sp,
                            color = if (isDarkMode) Color(0xFF64748B) else Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // Reaction Bar Modal
        AnimatedVisibility(visible = showReactionMenuForMessageId != null) {
            val msgId = showReactionMenuForMessageId ?: ""
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDarkMode) Color(0xFF1E2235) else Color(0xFFF1F5F9))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("❤️", "🔥", "🚀", "😂", "👏", "😮").forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .clickable {
                                onAddReaction(msgId, emoji)
                                showReactionMenuForMessageId = null
                            }
                            .padding(4.dp)
                    )
                }
            }
        }

        // --- 3. Composer Bottom Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Simulated voice note record button
            IconButton(
                onClick = {
                    isRecordingAudio = true
                    onToast("Recording voice note (0:15)...")
                    onSendMessage("Voice note", true, "0:15")
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = "Voice note",
                    tint = VynoraNeonCyan
                )
            }

            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text("Message @${conversation.participant.username}...", fontSize = 13.sp) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input"),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VynoraNeonPink,
                    focusedContainerColor = if (isDarkMode) Color(0xFF161928) else Color(0xFFF1F5F9),
                    unfocusedContainerColor = if (isDarkMode) Color(0xFF161928) else Color(0xFFF1F5F9)
                )
            )

            IconButton(
                onClick = {
                    if (messageInput.isNotBlank()) {
                        onSendMessage(messageInput, false, null)
                        messageInput = ""
                    }
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
}
