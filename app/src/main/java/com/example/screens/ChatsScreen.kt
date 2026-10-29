package com.example.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.components.StoryRingAvatar
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

        HorizontalDivider(color = if (isDarkMode) Color(0x22FFFFFF) else Color(0x1F000000))

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
        StoryRingAvatar(
            avatarUrl = conversation.participant.avatarUrl,
            size = 52.dp
        )

        // Message Preview
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = conversation.participant.fullName.ifBlank { conversation.participant.username },
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
                    text = if (conversation.lastMessage.isAudio) "🎤 Voice note (${conversation.lastMessage.audioDuration ?: "0:34"})" else if (parseSharedReel(conversation.lastMessage.text) != null) "🎬 Reel" else parseCallLog(conversation.lastMessage.text)?.let { if (it.video) "🎥 Video call" else "📞 Voice call" } ?: parseCallInvite(conversation.lastMessage.text)?.let { if (it.first) "🎥 Video call" else "📞 Voice call" } ?: conversation.lastMessage.text,
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
    onStartCall: (video: Boolean) -> Unit = {},
    onSendVoiceNote: (File, String) -> Unit = { _, _ -> },
    onDeleteMessage: (String) -> Unit = {},
    onMarkSeen: () -> Unit = {},
    onEditMessage: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var editingId by remember { mutableStateOf<String?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val callContext = androidx.compose.ui.platform.LocalContext.current
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showReactionMenuForMessageId by remember { mutableStateOf<String?>(null) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordFile by remember { mutableStateOf<File?>(null) }
    var recordStartMs by remember { mutableStateOf(0L) }
    var recordSeconds by remember { mutableStateOf(0) }
    var playingMessageId by remember { mutableStateOf<String?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var aiReplies by remember { mutableStateOf<List<String>>(emptyList()) }
    var aiBusy by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            try { player?.release() } catch (e: Exception) { }
            try { recorder?.release() } catch (e: Exception) { }
        }
    }

    fun formatDuration(totalSeconds: Int): String = String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60)

    fun startRecording() {
        try {
            val f = File(callContext.cacheDir, "vn_${System.currentTimeMillis()}.m4a")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(callContext) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64000)
            r.setAudioSamplingRate(44100)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            recordFile = f
            recordStartMs = System.currentTimeMillis()
            recordSeconds = 0
            isRecordingAudio = true
        } catch (e: Exception) {
            onToast("Could not start recording")
        }
    }

    fun stopRecording(send: Boolean) {
        val r = recorder
        val f = recordFile
        val seconds = ((System.currentTimeMillis() - recordStartMs) / 1000).toInt()
        isRecordingAudio = false
        recorder = null
        recordFile = null
        var ok = true
        try { r?.stop() } catch (e: Exception) { ok = false }
        try { r?.release() } catch (e: Exception) { }
        if (send && ok && f != null && seconds >= 1) {
            onSendVoiceNote(f, formatDuration(seconds))
        } else {
            try { f?.delete() } catch (e: Exception) { }
            if (send) onToast("Recording was too short")
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording() else onToast("Microphone permission is needed to record voice notes")
    }

    LaunchedEffect(isRecordingAudio) {
        while (isRecordingAudio) {
            delay(500)
            recordSeconds = ((System.currentTimeMillis() - recordStartMs) / 1000).toInt()
            if (recordSeconds >= 300) stopRecording(true)
        }
    }

    fun togglePlay(messageId: String, url: String) {
        if (playingMessageId == messageId) {
            try { player?.release() } catch (e: Exception) { }
            player = null
            playingMessageId = null
            return
        }
        try { player?.release() } catch (e: Exception) { }
        try {
            val mp = MediaPlayer()
            mp.setDataSource(url)
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener {
                try { it.release() } catch (e: Exception) { }
                if (playingMessageId == messageId) {
                    playingMessageId = null
                    player = null
                }
            }
            mp.setOnErrorListener { _, _, _ ->
                playingMessageId = null
                player = null
                onToast("Could not play this voice note")
                true
            }
            player = mp
            playingMessageId = messageId
            mp.prepareAsync()
        } catch (e: Exception) {
            playingMessageId = null
            player = null
            onToast("Could not play this voice note")
        }
    }
    var previewReelUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(conversation.messages.size) {
        listState.animateScrollToItem((conversation.messages.size - 1).coerceAtLeast(0))
    }
    LaunchedEffect(conversation.id, conversation.messages.lastOrNull()?.id) { onMarkSeen() }
    val shownMessages = if (searchQuery.isBlank()) conversation.messages
        else conversation.messages.filter { it.text.contains(searchQuery.trim(), ignoreCase = true) }
    val lastOwnId = conversation.messages.lastOrNull { it.senderId == currentUser.id }?.id

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
        previewReelUrl?.let { u -> ReelPreviewDialog(url = u, onDismiss = { previewReelUrl = null }) }

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

                StoryRingAvatar(
                    avatarUrl = conversation.participant.avatarUrl,
                    size = 38.dp
                )

                Column {
                    Text(
                        text = conversation.participant.username,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = { searchOpen = !searchOpen; if (!searchOpen) searchQuery = "" }) {
                    Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search", tint = if (isDarkMode) Color.White else Color(0xFF0F172A))
                }
                IconButton(onClick = { onStartCall(false) }) {
                    Icon(imageVector = Icons.Outlined.Call, contentDescription = "Call", tint = VynoraNeonCyan)
                }
                IconButton(onClick = { onStartCall(true) }) {
                    Icon(imageVector = Icons.Outlined.Videocam, contentDescription = "Video", tint = VynoraNeonPink)
                }
            }
        }

        if (searchOpen) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                placeholder = { Text("Search in this chat…", fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(20.dp)
            )
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
            items(shownMessages, key = { it.id }) { msg ->
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
                        val sharedReel = parseSharedReel(msg.text)
                        val callInvite = parseCallInvite(msg.text)
                        val callLog = parseCallLog(msg.text)
                        if (callLog != null) {
                            CallLogPill(log = callLog, isMe = isMe, isDarkMode = isDarkMode)
                        } else if (callInvite != null) {
                            CallInviteCard(
                                isVideo = callInvite.first,
                                isMe = isMe,
                                isDarkMode = isDarkMode,
                                onJoin = {
                                    try {
                                        callContext.startActivity(
                                            android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(callInvite.second))
                                        )
                                    } catch (e: Exception) {
                                        onToast("Could not open the call")
                                    }
                                }
                            )
                        } else if (sharedReel != null) {
                            SharedReelCard(
                                author = sharedReel.first,
                                url = sharedReel.second,
                                onClick = { previewReelUrl = sharedReel.second }
                            )
                        } else Surface(
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
                                        imageVector = if (playingMessageId == msg.id) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "Play",
                                        tint = if (isMe) Color.White else VynoraNeonCyan,
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clickable(enabled = msg.text.startsWith("http")) { togglePlay(msg.id, msg.text) }
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
                            text = msg.timestamp + (if (msg.edited) " • edited" else "") +
                                (if (msg.id == lastOwnId && isMe && conversation.otherSeenAt >= msg.createdAt) " • Seen" else ""),
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
                val menuMsg = conversation.messages.firstOrNull { it.id == msgId }
                if (menuMsg != null && menuMsg.senderId == currentUser.id && !menuMsg.isAudio && menuMsg.text.isNotBlank()) {
                    Text(
                        text = "✏️ Edit",
                        fontSize = 14.sp,
                        modifier = Modifier
                            .clickable {
                                editingId = menuMsg.id
                                messageInput = menuMsg.text
                                showReactionMenuForMessageId = null
                            }
                            .padding(6.dp),
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                }
                if (menuMsg != null && menuMsg.senderId == currentUser.id) {
                    Text(
                        text = "🗑 Delete",
                        fontSize = 14.sp,
                        color = Color(0xFFEF4444),
                        modifier = Modifier
                            .clickable {
                                onDeleteMessage(msgId)
                                showReactionMenuForMessageId = null
                            }
                            .padding(6.dp)
                    )
                }
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

        if (aiReplies.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                aiReplies.forEach { r ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isDarkMode) Color(0xFF1E2235) else Color(0xFFE2E8F0),
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { messageInput = r; aiReplies = emptyList() }
                    ) {
                        Text(r, fontSize = 13.sp, color = if (isDarkMode) Color.White else Color(0xFF0F172A), modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                    }
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
                    if (isRecordingAudio) {
                        stopRecording(true)
                    } else if (ContextCompat.checkSelfPermission(callContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        startRecording()
                    } else {
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            ) {
                Icon(
                    imageVector = if (isRecordingAudio) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = "Voice note",
                    tint = if (isRecordingAudio) Color(0xFFEF4444) else VynoraNeonCyan
                )
            }

            if (isRecordingAudio) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Text(
                        text = "Recording " + formatDuration(recordSeconds) + "  (tap ■ to send)",
                        fontSize = 13.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { stopRecording(false) }) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Cancel", tint = Color(0xFFEF4444))
                    }
                }
            } else {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text(if (editingId != null) "Edit message…" else "Message @${conversation.participant.username}...", fontSize = 13.sp) },
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

            }

            if (!isRecordingAudio) {
                IconButton(
                    enabled = !aiBusy,
                    onClick = {
                        val last = conversation.messages.lastOrNull { it.senderId != currentUser.id && !it.isAudio && it.text.isNotBlank() }
                        if (last == null) { onToast("No message from them to reply to yet"); return@IconButton }
                        aiBusy = true
                        coroutineScope.launch {
                            val r = com.example.service.GeminiService.suggestReplies(last.text)
                            aiBusy = false
                            if (r.isEmpty()) onToast("AI reply isn't available right now") else aiReplies = r
                        }
                    }
                ) {
                    Text(if (aiBusy) "…" else "✨", fontSize = 20.sp)
                }
            }

            IconButton(
                onClick = {
                    if (isRecordingAudio) {
                        stopRecording(true)
                    } else if (messageInput.isNotBlank()) {
                        val eid = editingId
                        if (eid != null) { onEditMessage(eid, messageInput); editingId = null }
                        else onSendMessage(messageInput, false, null)
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


/** Parses "Shared a reel by @author: https://..." into (author, url). */
private fun parseSharedReel(text: String): Pair<String, String>? {
    val prefix = "Shared a reel by @"
    if (!text.startsWith(prefix)) return null
    val rest = text.removePrefix(prefix)
    val idx = rest.indexOf(": ")
    if (idx <= 0) return null
    val author = rest.substring(0, idx)
    val url = rest.substring(idx + 2).trim()
    if (!url.startsWith("http")) return null
    return author to url
}

private fun reelThumbnailUrl(videoUrl: String): String {
    val withoutExt = videoUrl.substringBeforeLast('.', videoUrl)
    return if (videoUrl.contains("/video/upload/")) {
        withoutExt.replace("/video/upload/", "/video/upload/so_0,w_480,h_720,c_fill/") + ".jpg"
    } else {
        withoutExt + ".jpg"
    }
}

@Composable
private fun SharedReelCard(author: String, url: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(210.dp)
            .height(300.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = reelThumbnailUrl(url),
            contentDescription = "Reel",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
                .padding(10.dp)
        ) {
            Text(
                text = "@$author",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(52.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        Icon(
            imageVector = Icons.Outlined.Videocam,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .size(20.dp)
        )
    }
}

@Composable
private fun ReelPreviewDialog(url: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setVideoURI(Uri.parse(url))
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            start()
                        }
                    }
                }
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }
    }
}


/** Parses "Started a video call: <url>" / "Started a voice call: <url>" into (isVideo, url). */
private data class CallLogInfo(val video: Boolean, val status: String, val seconds: Int)

private fun parseCallLog(text: String): CallLogInfo? {
    if (!text.startsWith("[call]")) return null
    val parts = text.removePrefix("[call]").split("|")
    if (parts.size < 3) return null
    return CallLogInfo(parts[0] == "video", parts[1], parts[2].toIntOrNull() ?: 0)
}

@Composable
private fun CallLogPill(log: CallLogInfo, isMe: Boolean, isDarkMode: Boolean) {
    val kind = if (log.video) "video call" else "voice call"
    val missedForMe = !isMe && (log.status == "MISSED" || log.status == "CANCELED")
    val label = when (log.status) {
        "ENDED" -> "${if (log.video) "Video" else "Voice"} call · " + String.format("%d:%02d", log.seconds / 60, log.seconds % 60)
        "DECLINED" -> "Call declined"
        "MISSED" -> if (isMe) "No answer" else "Missed $kind"
        "CANCELED" -> if (isMe) "Call cancelled" else "Missed $kind"
        else -> "Call"
    }
    val tint = if (missedForMe) Color(0xFFEF4444) else (if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E293B))
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (isDarkMode) Color(0xFF1E2235) else Color(0xFFE2E8F0)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (log.video) Icons.Filled.Videocam else Icons.Filled.Call,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Text(label, color = tint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun parseCallInvite(text: String): Pair<Boolean, String>? {
    val video = "Started a video call: "
    val voice = "Started a voice call: "
    return when {
        text.startsWith(video) && text.removePrefix(video).trim().startsWith("http") ->
            true to text.removePrefix(video).trim()
        text.startsWith(voice) && text.removePrefix(voice).trim().startsWith("http") ->
            false to text.removePrefix(voice).trim()
        else -> null
    }
}

@Composable
private fun CallInviteCard(isVideo: Boolean, isMe: Boolean, isDarkMode: Boolean, onJoin: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkMode) Color(0xFF1E2235) else Color(0xFFE2E8F0)
    ) {
        Column(modifier = Modifier.padding(14.dp).width(200.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (isVideo) Icons.Outlined.Videocam else Icons.Outlined.Call,
                    contentDescription = null,
                    tint = if (isVideo) VynoraNeonPink else VynoraNeonCyan
                )
                Text(
                    text = if (isVideo) "Video call" else "Voice call",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isMe) "You started a call" else "Started a call",
                fontSize = 12.sp,
                color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = onJoin,
                modifier = Modifier.fillMaxWidth().height(38.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonCyan)
            ) {
                Text("Join", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    }
}
