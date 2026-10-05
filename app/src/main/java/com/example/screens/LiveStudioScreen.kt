package com.example.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FlipCameraAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LiveStudioScreen(
    onEndLive: () -> Unit,
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewerCount by remember { mutableStateOf(1280) }
    var flyingHearts by remember { mutableStateOf(listOf<Int>()) }
    var commentInput by remember { mutableStateOf("") }
    val comments = remember {
        mutableStateListOf(
            "neona_zen" to "The live lighting is insane!! 🔥",
            "kai_sound" to "Bass sounds crisp through the mic 🎧",
            "aria_wander" to "Greetings from Goa! 🌅✨",
            "mira_art" to "Love the glowing neon setup"
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Periodically increment viewer count & auto add comments
    LaunchedEffect(Unit) {
        val sampleLiveComments = listOf(
            "zack_dev" to "VYNORA stream latency is under 100ms ⚡",
            "tanya_fit" to "High energy stream! 💪",
            "cyber_samurai" to "Tokyo checking in 🏮",
            "zoe_glow" to "Send more hearts everyone! 💖💖"
        )
        var idx = 0
        while (true) {
            delay(3500)
            viewerCount += (12..48).random()
            if (idx < sampleLiveComments.size) {
                comments.add(sampleLiveComments[idx])
                idx++
                listState.animateScrollToItem(comments.size - 1)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070913))
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("live_studio_screen")
    ) {
        // --- 1. Camera Preview Placeholder with subtle ambient glow ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF1E1035),
                            Color(0xFF0A0D1A)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Videocam,
                    contentDescription = null,
                    tint = VynoraNeonPink.copy(alpha = 0.4f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "LIVE BROADCAST ACTIVE",
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }

        // --- 2. Top Header Overlay ---
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
                // LIVE Badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFEF4444)
                ) {
                    Text(
                        text = "LIVE",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Viewer Count
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$viewerCount",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Flip camera button
                IconButton(
                    onClick = { onToast("Switched camera") },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FlipCameraAndroid,
                        contentDescription = "Flip Camera",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // End Broadcast Button
                Button(
                    onClick = onEndLive,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("End Live", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }
            }
        }

        // --- 3. Live Comments Stream (Bottom Left) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.75f)
                .padding(start = 16.dp, bottom = 80.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.heightIn(max = 180.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(comments) { (user, comment) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x33FFFFFF))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = user,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = VynoraNeonCyan
                            )
                            Text(
                                text = comment,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // --- 4. Floating Flying Hearts (Bottom Right) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Heart button
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(VynoraNeonPink, Color(0xFFD946EF)))
                    )
                    .clickable {
                        coroutineScope.launch {
                            flyingHearts = flyingHearts + (0..100).random()
                            delay(1800)
                            if (flyingHearts.isNotEmpty()) {
                                flyingHearts = flyingHearts.drop(1)
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Send Heart",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // --- 5. Bottom Live Comment Input ---
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = commentInput,
                onValueChange = { commentInput = it },
                placeholder = { Text("Comment as host...", color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = VynoraNeonPink,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedContainerColor = Color.Black.copy(alpha = 0.5f),
                    unfocusedContainerColor = Color.Black.copy(alpha = 0.5f)
                ),
                trailingIcon = {
                    if (commentInput.isNotBlank()) {
                        IconButton(onClick = {
                            comments.add("alex_vynora (Host)" to commentInput)
                            commentInput = ""
                        }) {
                            Icon(Icons.Filled.Send, contentDescription = "Send", tint = VynoraNeonPink)
                        }
                    }
                }
            )
        }
    }
}
