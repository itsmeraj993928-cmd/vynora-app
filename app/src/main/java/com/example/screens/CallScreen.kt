package com.example.screens

import android.Manifest
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.view.SurfaceView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.components.StoryRingAvatar
import com.example.model.CallInfo
import com.example.model.User
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.delay

// Agora App ID (public identifier, not a secret). Project is in "App ID only" testing mode.
private const val AGORA_APP_ID = "f55ff47d981a472e91d1f5ce5f4437d3"

private val CallBg = Color(0xFF0B0D17)
private val CallRed = Color(0xFFEF4444)
private val CallGreen = Color(0xFF22C55E)

@Composable
private fun RoundCallButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    bg: Color,
    tint: Color = Color.White,
    size: Int = 58,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.size(size.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size * 0.46f).dp))
    }
}

@Composable
fun CallScreen(
    call: CallInfo,
    peer: User?,
    isCaller: Boolean,
    onEnd: () -> Unit,
    onToast: (String) -> Unit
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    BackHandler(enabled = true) { /* use the End button */ }
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val needed = remember(call.video) {
        if (call.video) arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
        else arrayOf(Manifest.permission.RECORD_AUDIO)
    }
    fun allGranted() = needed.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }
    var permsGranted by remember { mutableStateOf(allGranted()) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permsGranted = allGranted()
        if (!permsGranted) {
            onToast(if (call.video) "Microphone and camera permission are needed for video calls" else "Microphone permission is needed for calls")
            onEnd()
        }
    }
    LaunchedEffect(Unit) { if (!permsGranted) permLauncher.launch(needed) }

    var engine by remember { mutableStateOf<RtcEngine?>(null) }
    var remoteUid by remember { mutableStateOf(0) }
    var remoteLeft by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var speakerOn by remember { mutableStateOf(call.video) }
    var camOff by remember { mutableStateOf(false) }
    var seconds by remember { mutableStateOf(0) }

    DisposableEffect(permsGranted) {
        if (!permsGranted) return@DisposableEffect onDispose { }
        val handler = object : IRtcEngineEventHandler() {
            override fun onUserJoined(uid: Int, elapsed: Int) { remoteUid = uid }
            override fun onUserOffline(uid: Int, reason: Int) { remoteUid = 0; remoteLeft = true }
        }
        val created: RtcEngine? = try {
            val cfg = RtcEngineConfig().apply {
                mContext = ctx.applicationContext
                mAppId = AGORA_APP_ID
                mEventHandler = handler
            }
            RtcEngine.create(cfg)
        } catch (e: Exception) { null }
        if (created == null) {
            onToast("Could not start the call")
            onEnd()
            return@DisposableEffect onDispose { }
        }
        try {
            created.enableAudio()
            if (call.video) { created.enableVideo() }
            created.setEnableSpeakerphone(call.video)
            val opts = ChannelMediaOptions().apply {
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                publishMicrophoneTrack = true
                publishCameraTrack = call.video
                autoSubscribeAudio = true
                autoSubscribeVideo = call.video
            }
            created.joinChannel(null, call.channel, 0, opts)
        } catch (e: Exception) {
            onToast("Could not join the call")
        }
        engine = created
        onDispose {
            try { created.stopPreview() } catch (e: Exception) { }
            try { created.leaveChannel() } catch (e: Exception) { }
            engine = null
            try { RtcEngine.destroy() } catch (e: Exception) { }
        }
    }

    LaunchedEffect(remoteLeft) { if (remoteLeft) onEnd() }
    LaunchedEffect(remoteUid != 0) {
        if (remoteUid != 0) {
            seconds = 0
            while (true) { delay(1000); seconds++ }
        }
    }

    val name = peer?.let { it.fullName.ifBlank { it.username } } ?: "Calling…"
    val status = when {
        remoteUid != 0 -> String.format("%02d:%02d", seconds / 60, seconds % 60)
        call.status == "RINGING" && isCaller -> "Ringing…"
        else -> "Connecting…"
    }
    val eng = engine

    Box(
        modifier = Modifier.fillMaxSize().background(CallBg).pointerInput(Unit) { detectTapGestures { } }
    ) {
        // Remote video (video calls)
        if (call.video && eng != null && remoteUid != 0) {
            key(remoteUid) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { c ->
                        SurfaceView(c).also { sv ->
                            eng.setupRemoteVideo(VideoCanvas(sv, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid))
                        }
                    }
                )
            }
        }

        // Voice call, or video not connected yet: avatar + name
        if (!call.video || remoteUid == 0) {
            Column(
                modifier = Modifier.fillMaxSize().padding(top = 110.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StoryRingAvatar(avatarUrl = peer?.avatarUrl ?: "", size = 128.dp)
                Spacer(modifier = Modifier.height(22.dp))
                Text(name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(status, color = Color(0xFFCBD5E1), fontSize = 15.sp)
                if (call.video) {
                    Text("Video call", color = Color(0xFF94A3B8), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            Column(modifier = Modifier.align(Alignment.TopStart).padding(top = 48.dp, start = 20.dp)) {
                Text(name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(status, color = Color(0xFFE2E8F0), fontSize = 13.sp)
            }
        }

        // My own camera preview (small window, video calls)
        if (call.video && eng != null && !camOff) {
            key(eng) {
                AndroidView(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 48.dp, end = 16.dp)
                        .size(width = 100.dp, height = 140.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    factory = { c ->
                        SurfaceView(c).also { sv ->
                            sv.setZOrderMediaOverlay(true)
                            eng.setupLocalVideo(VideoCanvas(sv, VideoCanvas.RENDER_MODE_HIDDEN, 0))
                            eng.startPreview()
                        }
                    }
                )
            }
        }

        // Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 56.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundCallButton(
                icon = if (muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                bg = if (muted) Color.White else Color(0x33FFFFFF),
                tint = if (muted) Color.Black else Color.White
            ) {
                muted = !muted
                try { eng?.muteLocalAudioStream(muted) } catch (e: Exception) { }
            }
            if (call.video) {
                RoundCallButton(
                    icon = if (camOff) Icons.Filled.VideocamOff else Icons.Filled.Videocam,
                    bg = if (camOff) Color.White else Color(0x33FFFFFF),
                    tint = if (camOff) Color.Black else Color.White
                ) {
                    camOff = !camOff
                    try { eng?.muteLocalVideoStream(camOff) } catch (e: Exception) { }
                }
                RoundCallButton(icon = Icons.Filled.FlipCameraAndroid, bg = Color(0x33FFFFFF)) {
                    try { eng?.switchCamera() } catch (e: Exception) { }
                }
            }
            RoundCallButton(
                icon = if (speakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                bg = if (speakerOn) Color.White else Color(0x33FFFFFF),
                tint = if (speakerOn) Color.Black else Color.White
            ) {
                speakerOn = !speakerOn
                try { eng?.setEnableSpeakerphone(speakerOn) } catch (e: Exception) { }
            }
            RoundCallButton(icon = Icons.Filled.CallEnd, bg = CallRed, size = 66, onClick = onEnd)
        }
    }
}

@Composable
fun IncomingCallScreen(
    call: CallInfo,
    peer: User?,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val ctx = LocalContext.current
    BackHandler(enabled = true) { onDecline() }
    DisposableEffect(Unit) {
        val ringtone = try {
            RingtoneManager.getRingtone(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
                ?.also { it.play() }
        } catch (e: Exception) { null }
        onDispose { try { ringtone?.stop() } catch (e: Exception) { } }
    }
    val name = peer?.let { it.fullName.ifBlank { it.username } } ?: "Someone"
    Box(modifier = Modifier.fillMaxSize().background(CallBg).pointerInput(Unit) { detectTapGestures { } }) {
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StoryRingAvatar(avatarUrl = peer?.avatarUrl ?: "", size = 128.dp)
            Spacer(modifier = Modifier.height(22.dp))
            Text(name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                if (call.video) "Incoming video call…" else "Incoming voice call…",
                color = Color(0xFFCBD5E1), fontSize = 15.sp
            )
        }
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RoundCallButton(icon = Icons.Filled.CallEnd, bg = CallRed, size = 70, onClick = onDecline)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Decline", color = Color.White, fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                RoundCallButton(
                    icon = if (call.video) Icons.Filled.Videocam else Icons.Filled.Call,
                    bg = CallGreen, size = 70, onClick = onAccept
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Accept", color = Color.White, fontSize = 13.sp)
            }
        }
    }
}
