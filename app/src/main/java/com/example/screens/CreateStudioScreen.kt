package com.example.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.i18n.AppLanguage
import com.example.i18n.VynoraStrings
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink
import com.example.util.CloudinaryUploader
import kotlinx.coroutines.launch

enum class CreateMode {
    POST, MOMENT, CLIP
}

@Composable
fun CreateStudioScreen(
    isDarkMode: Boolean,
    currentLanguage: AppLanguage,
    onPublishPost: (caption: String, hashtags: List<String>, mediaUrls: List<String>, location: String?, filterName: String?, visibility: String) -> Unit,
    onPublishMoment: (textOverlay: String, mediaUrl: String, musicTrack: String?, isCloseFriends: Boolean, visibility: String) -> Unit,
    onPublishClip: (caption: String, mediaUrl: String, musicTitle: String, visibility: String) -> Unit = { _, _, _, _ -> },
    onRequestAiCaption: (topic: String, onResult: (String) -> Unit) -> Unit,
    onRequestAiHashtags: (topic: String, onResult: (List<String>) -> Unit) -> Unit,
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(CreateMode.POST) }
    var caption by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Original") }
    var selectedRatio by remember { mutableStateOf("1:1") }
    var isCloseFriends by remember { mutableStateOf(false) }
    var isScheduled by remember { mutableStateOf(false) }
    var visibility by remember { mutableStateOf("PUBLIC") }
    var confirmSafeContent by remember { mutableStateOf(false) }
    var isGeneratingAi by remember { mutableStateOf(false) }
    var selectedMusicTrack by remember { mutableStateOf("Cyber Drift - Ambient Wave") }

    // Cloudinary media state
    var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
    var uploadedSecureUrl by remember { mutableStateOf<String?>(null) }
    var uploadedPublicId by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    fun startUpload(uri: Uri) {
        selectedMediaUri = uri
        uploadedSecureUrl = null
        uploadedPublicId = null
        uploadError = null
        uploadProgress = 0
        isUploading = true
        coroutineScope.launch {
            val res = CloudinaryUploader.uploadUri(
                context = context,
                uri = uri,
                onProgress = { p -> uploadProgress = p }
            )
            isUploading = false
            if (res.isSuccess) {
                val result = res.getOrNull()
                uploadedSecureUrl = result?.secureUrl
                uploadedPublicId = result?.publicId
                onToast("Uploaded to Cloudinary! ☁️")
            } else {
                uploadError = res.exceptionOrNull()?.localizedMessage ?: "Upload failed"
                onToast("Upload failed: $uploadError")
            }
        }
    }

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            var blocked = false
            if (mode == CreateMode.CLIP) {
                val durMs: Long = try {
                    val r = android.media.MediaMetadataRetriever()
                    r.setDataSource(context, uri)
                    val d = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    r.release()
                    d
                } catch (e: Exception) { 0L }
                if (durMs in 1..2999) {
                    onToast("Reels must be at least 3 seconds long")
                    blocked = true
                } else if (durMs > 90_000L) {
                    onToast("Reels can be at most 90 seconds long")
                    blocked = true
                }
            }
            if (!blocked) startUpload(uri)
        }
    }

    val filters = listOf("Original", "Neon Glow", "Cyberpunk", "Sunset Warm", "Noir", "Vibrant")

    val screenBg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF0F121E) else Color(0xFFFFFFFF)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 110.dp)
    ) {
        // --- 1. Mode Switcher (Post / Moment / Clip) ---
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(
                CreateMode.POST to VynoraStrings.createPost(currentLanguage),
                CreateMode.MOMENT to VynoraStrings.createMoment(currentLanguage),
                CreateMode.CLIP to VynoraStrings.createClip(currentLanguage)
            ).forEach { (m, label) ->
                val isSelected = mode == m
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) VynoraNeonPink else Color.Transparent)
                        .clickable { mode = m }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (isSelected) Color.White else (if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. Media Preview & Upload Card ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (selectedRatio == "9:16" || mode == CreateMode.CLIP) 0.9f else 1.1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Black)
                .border(
                    1.dp,
                    if (uploadedSecureUrl != null) VynoraNeonCyan else (if (isDarkMode) Color(0x3300D1FF) else Color(0x1F000000)),
                    RoundedCornerShape(20.dp)
                )
                .clickable {
                    val requestType = if (mode == CreateMode.CLIP) {
                        ActivityResultContracts.PickVisualMedia.VideoOnly
                    } else {
                        ActivityResultContracts.PickVisualMedia.ImageAndVideo
                    }
                    mediaPickerLauncher.launch(PickVisualMediaRequest(requestType))
                },
            contentAlignment = Alignment.Center
        ) {
            if (uploadedSecureUrl != null || selectedMediaUri != null) {
                AsyncImage(
                    model = uploadedSecureUrl ?: selectedMediaUri,
                    contentDescription = "Selected Media",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Filter Color overlay
                when (selectedFilter) {
                    "Neon Glow" -> Box(modifier = Modifier.fillMaxSize().background(VynoraNeonCyan.copy(alpha = 0.18f)))
                    "Cyberpunk" -> Box(modifier = Modifier.fillMaxSize().background(VynoraNeonPink.copy(alpha = 0.18f)))
                    "Sunset Warm" -> Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF59E0B).copy(alpha = 0.2f)))
                    "Noir" -> Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                    "Vibrant" -> Box(modifier = Modifier.fillMaxSize().background(Color(0xFFA855F7).copy(alpha = 0.15f)))
                }

                // Change Media Overlay Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xAA000000),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = VynoraNeonCyan, modifier = Modifier.size(14.dp))
                        Text("Change", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Empty state: prompt to select media
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0x3300D1FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (mode == CreateMode.CLIP) Icons.Outlined.Videocam else Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "Add Media",
                            tint = VynoraNeonCyan,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (mode == CreateMode.CLIP) "Select Video Clip from Device" else "Select Photo / Video from Device",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Uploads to Cloudinary (cloud: x5zwfz5y)",
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }

        // --- Upload Progress Bar & Error Retry UI ---
        if (isUploading) {
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { uploadProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = VynoraNeonCyan,
                trackColor = Color(0xFF1E2438)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Uploading to Cloudinary...",
                    fontSize = 12.sp,
                    color = VynoraNeonCyan,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$uploadProgress%",
                    fontSize = 12.sp,
                    color = VynoraNeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (uploadError != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x33EF4444),
                border = BorderStroke(1.dp, Color(0xFFEF4444)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Upload failed: $uploadError",
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        val uri = selectedMediaUri
                        if (uri != null) startUpload(uri)
                    }) {
                        Text("Retry", color = VynoraNeonCyan, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (uploadedSecureUrl != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                Text("Ready to publish via Cloudinary", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. Filter Carousel ---
        if (mode != CreateMode.CLIP) {
            Text(
                text = "Apply Filter",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filters) { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VynoraNeonCyan,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- 4. Caption & AI Tools ---
        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            placeholder = { Text("Write a caption or describe your world...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .testTag("caption_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg,
                focusedBorderColor = VynoraNeonPink
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        // AI Assistant Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    isGeneratingAi = true
                    onRequestAiCaption("vibrant evening cyber") { result ->
                        caption = result
                        isGeneratingAi = false
                    }
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFFA855F7)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI Caption", fontSize = 12.sp, color = Color(0xFFA855F7))
            }

            OutlinedButton(
                onClick = {
                    onRequestAiHashtags("travel neon futuristic") { tags ->
                        caption += "\n" + tags.joinToString(" ")
                    }
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, VynoraNeonCyan),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Outlined.Tag, contentDescription = null, tint = VynoraNeonCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("AI Tags", fontSize = 12.sp, color = VynoraNeonCyan)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Location & Options
        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            placeholder = { Text("Add Location (e.g. Tokyo, Shinjuku)") },
            leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = VynoraNeonCyan) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- Who can see this? ---
        Text(
            text = "Who can see this?",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "PUBLIC" to "🌍 Public",
                "FOLLOWERS" to "👥 Followers",
                "PRIVATE" to "🔒 Only me"
            ).forEach { (value, label) ->
                FilterChip(
                    selected = visibility == value,
                    onClick = { visibility = value },
                    label = { Text(label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VynoraNeonCyan,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- Content safety confirmation (no 18+ / nudity) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { confirmSafeContent = !confirmSafeContent },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = confirmSafeContent,
                onCheckedChange = { confirmSafeContent = it }
            )
            Text(
                text = "I confirm this has no nudity, sexual (18+), violent or illegal content, and I own the rights to it. Rule-breaking content is removed and accounts can be banned.",
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- 5. Publish / Share CTA Button ---
        Button(
            onClick = {
                val mediaUrl = uploadedSecureUrl
                if (mediaUrl.isNullOrBlank()) {
                    if (isUploading) {
                        onToast("Please wait for media upload to finish ⏳")
                    } else {
                        onToast("Please select a photo or video to share! 📸")
                    }
                    return@Button
                }

                if (!confirmSafeContent) {
                    onToast("Please confirm your content follows the rules (no 18+ / nudity) ✅")
                    return@Button
                }

                if (mode == CreateMode.MOMENT) {
                    onPublishMoment(
                        caption.ifBlank { "Late night vibes ✨" },
                        mediaUrl,
                        selectedMusicTrack,
                        isCloseFriends,
                        visibility
                    )
                } else if (mode == CreateMode.CLIP) {
                    onPublishClip(
                        caption.ifBlank { "VYNORA Clip ✨" },
                        mediaUrl,
                        selectedMusicTrack,
                        visibility
                    )
                } else {
                    onPublishPost(
                        caption.ifBlank { "Sharing my world with @VYNORA ✨" },
                        listOf("#VynoraVibes", "#ShareYourWorld"),
                        listOf(mediaUrl),
                        location.ifBlank { null },
                        selectedFilter,
                        visibility
                    )
                }
            },
            enabled = !isUploading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("publish_button"),
            shape = RoundedCornerShape(27.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (mode == CreateMode.MOMENT) "Publish Moment (24h)" else if (mode == CreateMode.CLIP) "Share Clip" else "Share Post",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }
}
