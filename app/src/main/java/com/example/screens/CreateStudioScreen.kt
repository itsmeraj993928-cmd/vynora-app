package com.example.screens

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.i18n.AppLanguage
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class CreateMode {
    POST, MOMENT, CLIP
}

private data class GalleryItem(val uri: Uri, val isVideo: Boolean, val durationMs: Long, val addedAt: Long)

private fun queryGallery(ctx: Context, videosOnly: Boolean): List<GalleryItem> {
    val out = ArrayList<GalleryItem>()
    try {
        val vProj = arrayOf(MediaStore.Video.Media._ID, MediaStore.Video.Media.DURATION, MediaStore.Video.Media.DATE_ADDED)
        ctx.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, vProj, null, null, "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { c ->
            var n = 0
            while (c.moveToNext() && n < 150) {
                val id = c.getLong(0)
                out.add(
                    GalleryItem(
                        ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id),
                        true, c.getLong(1), c.getLong(2)
                    )
                )
                n++
            }
        }
    } catch (e: Exception) { }
    if (!videosOnly) {
        try {
            val iProj = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED)
            ctx.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, iProj, null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { c ->
                var n = 0
                while (c.moveToNext() && n < 150) {
                    val id = c.getLong(0)
                    out.add(
                        GalleryItem(
                            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                            false, 0L, c.getLong(1)
                        )
                    )
                    n++
                }
            }
        } catch (e: Exception) { }
    }
    return out.sortedByDescending { it.addedAt }
}

@Suppress("DEPRECATION")
private fun loadGalleryThumb(ctx: Context, uri: Uri, isVideo: Boolean, px: Int): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= 29) {
            ctx.contentResolver.loadThumbnail(uri, Size(px, px), null)
        } else {
            val id = ContentUris.parseId(uri)
            if (isVideo) MediaStore.Video.Thumbnails.getThumbnail(ctx.contentResolver, id, MediaStore.Video.Thumbnails.MINI_KIND, null)
            else MediaStore.Images.Thumbnails.getThumbnail(ctx.contentResolver, id, MediaStore.Images.Thumbnails.MINI_KIND, null)
        }
    } catch (e: Exception) { null }
}

private fun fmtDuration(ms: Long): String {
    val s = (ms / 1000).toInt()
    return String.format("%d:%02d", s / 60, s % 60)
}

@Composable
private fun GalleryThumb(uri: Uri, isVideo: Boolean, modifier: Modifier = Modifier, px: Int = 320) {
    val ctx = LocalContext.current
    val bmp by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { loadGalleryThumb(ctx, uri, isVideo, px) }
    }
    Box(modifier = modifier.background(Color(0xFF1E2235))) {
        val b = bmp
        if (b != null) {
            Image(
                bitmap = b.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun CreateStudioScreen(
    isDarkMode: Boolean,
    currentLanguage: AppLanguage,
    onPublishPost: (caption: String, hashtags: List<String>, mediaUrls: List<String>, location: String?, filterName: String?, visibility: String) -> Unit,
    onPublishMoment: (textOverlay: String, mediaUrl: String, musicTrack: String?, isCloseFriends: Boolean, visibility: String) -> Unit,
    onPublishClip: (caption: String, mediaUrl: String, musicTitle: String, visibility: String) -> Unit = { _, _, _, _ -> },
    onStartBackgroundUpload: (kind: String, uri: Uri, caption: String, hashtags: List<String>, location: String?, filter: String?, visibility: String, music: String, closeFriends: Boolean, flags: Set<String>) -> Unit = { _, _, _, _, _, _, _, _, _, _ -> },
    onRequestAiCaption: (topic: String, onResult: (String) -> Unit) -> Unit,
    onRequestAiHashtags: (topic: String, onResult: (List<String>) -> Unit) -> Unit,
    onToast: (String) -> Unit,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var mode by remember { mutableStateOf(CreateMode.CLIP) }
    var step by remember { mutableStateOf(0) } // 0 = pick, 1 = details, 2 = more options
    var caption by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Original") }
    var isCloseFriends by remember { mutableStateOf(false) }
    var visibility by remember { mutableStateOf("PUBLIC") }
    var confirmSafeContent by remember { mutableStateOf(false) }
    var hideLikes by remember { mutableStateOf(false) }
    var hideShares by remember { mutableStateOf(false) }
    var commentsOff by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showAudienceDialog by remember { mutableStateOf(false) }

    var picked by remember { mutableStateOf<GalleryItem?>(null) }
    var pickedSizeMb by remember { mutableStateOf(0f) }

    val screenBg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF0F121E) else Color(0xFFFFFFFF)
    val fg = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val sub = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    BackHandler(enabled = step > 0) { step -= 1 }

    // ---- Gallery access ----
    val perms: Array<String> = if (Build.VERSION.SDK_INT >= 33)
        arrayOf(Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_IMAGES)
    else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    fun hasAccess(): Boolean = perms.any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED } ||
        (Build.VERSION.SDK_INT >= 34 && ContextCompat.checkSelfPermission(context, "android.permission.READ_MEDIA_VISUAL_USER_SELECTED") == PackageManager.PERMISSION_GRANTED)
    var galleryAccess by remember { mutableStateOf(hasAccess()) }
    var askedOnce by remember { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        galleryAccess = hasAccess()
    }
    LaunchedEffect(Unit) {
        if (!galleryAccess && !askedOnce) {
            askedOnce = true
            val req = if (Build.VERSION.SDK_INT >= 34) perms + "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" else perms
            permLauncher.launch(req)
        }
    }
    val videosOnly = mode == CreateMode.CLIP
    val gallery by produceState<List<GalleryItem>?>(initialValue = null, galleryAccess, videosOnly) {
        value = if (galleryAccess) withContext(Dispatchers.IO) { queryGallery(context, videosOnly) } else emptyList()
    }

    fun pickItem(uri: Uri, isVideo: Boolean, knownDurationMs: Long) {
        var dur = knownDurationMs
        if (isVideo && dur <= 0L) {
            dur = try {
                val r = android.media.MediaMetadataRetriever()
                r.setDataSource(context, uri)
                val d = r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                r.release()
                d
            } catch (e: Exception) { 0L }
        }
        if (mode == CreateMode.CLIP) {
            if (dur in 1..2999) { onToast("Reels must be at least 3 seconds long"); return }
            if (dur > 90_000L) { onToast("Reels can be at most 90 seconds long"); return }
        }
        val bytes = try {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
        } catch (e: Exception) { 0L }
        if (bytes > 100L * 1024L * 1024L) { onToast("File is too large. Maximum size is 100 MB"); return }
        picked = GalleryItem(uri, isVideo, dur, 0L)
        pickedSizeMb = if (bytes > 0L) bytes / 1048576f else 0f
        step = 1
    }

    val browseLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            val isVideo = (context.contentResolver.getType(uri) ?: "").startsWith("video")
            pickItem(uri, isVideo, 0L)
        }
    }
    fun launchBrowse() {
        val t = if (mode == CreateMode.CLIP) ActivityResultContracts.PickVisualMedia.VideoOnly
        else ActivityResultContracts.PickVisualMedia.ImageAndVideo
        browseLauncher.launch(PickVisualMediaRequest(t))
    }

    val title = when (mode) { CreateMode.CLIP -> "New reel"; CreateMode.MOMENT -> "New story"; else -> "New post" }

    Box(modifier = modifier.fillMaxSize().background(screenBg).windowInsetsPadding(WindowInsets.statusBars)) {
        when (step) {
            // ------------------------------------------------------------ PICK
            0 -> Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close", tint = fg) }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = fg)
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(48.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recents", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = fg)
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cardBg,
                        modifier = Modifier.clickable { launchBrowse() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                            Text("Browse", color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                val list = gallery
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        !galleryAccess -> Column(
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Allow access to your photos and videos to pick from your gallery.", color = sub, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val req = if (Build.VERSION.SDK_INT >= 34) perms + "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" else perms
                                    permLauncher.launch(req)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink)
                            ) { Text("Allow access", color = Color.White) }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(onClick = { launchBrowse() }) { Text("Browse files instead", color = VynoraNeonCyan) }
                        }
                        list == null -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = VynoraNeonCyan)
                        list.isEmpty() -> Text(
                            if (videosOnly) "No videos found on this phone." else "No photos or videos found.",
                            color = sub, fontSize = 13.sp, modifier = Modifier.align(Alignment.Center)
                        )
                        else -> LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            contentPadding = PaddingValues(bottom = 170.dp)
                        ) {
                            items(list, key = { it.uri.toString() }) { g ->
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(if (videosOnly) 0.75f else 1f)
                                        .clickable { pickItem(g.uri, g.isVideo, g.durationMs) }
                                ) {
                                    GalleryThumb(g.uri, g.isVideo, Modifier.fillMaxSize())
                                    if (g.isVideo && g.durationMs > 0) {
                                        Text(
                                            text = fmtDuration(g.durationMs),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ---------------------------------------------------------- DETAILS
            1 -> Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { step = 0 }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = fg) }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = fg)
                    Spacer(modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(48.dp))
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
                ) {
                    val p = picked
                    if (p != null) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .width(if (mode == CreateMode.CLIP) 110.dp else 130.dp)
                                    .height(if (mode == CreateMode.CLIP) 160.dp else 130.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            ) {
                                GalleryThumb(p.uri, p.isVideo, Modifier.fillMaxSize(), px = 480)
                                when (selectedFilter) {
                                    "Neon Glow" -> Box(Modifier.fillMaxSize().background(VynoraNeonCyan.copy(alpha = 0.18f)))
                                    "Cyberpunk" -> Box(Modifier.fillMaxSize().background(VynoraNeonPink.copy(alpha = 0.18f)))
                                    "Sunset Warm" -> Box(Modifier.fillMaxSize().background(Color(0xFFF59E0B).copy(alpha = 0.2f)))
                                    "Noir" -> Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
                                    "Vibrant" -> Box(Modifier.fillMaxSize().background(Color(0xFFA855F7).copy(alpha = 0.15f)))
                                }
                                val info = buildString {
                                    if (p.isVideo && p.durationMs > 0) append(fmtDuration(p.durationMs))
                                    if (pickedSizeMb > 0f) { if (isNotEmpty()) append(" • "); append(String.format("%.1f MB", pickedSizeMb)) }
                                }
                                if (info.isNotEmpty()) {
                                    Text(
                                        info, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.BottomCenter).background(Color(0x99000000)).fillMaxWidth().padding(4.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it },
                        placeholder = { Text("Write a caption and add hashtags…") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp).testTag("caption_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg,
                            focusedBorderColor = VynoraNeonPink
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = { caption = if (caption.isEmpty() || caption.endsWith(" ")) "$caption#" else "$caption #" }, label = { Text("# Hashtags", fontSize = 12.sp) })
                        AssistChip(onClick = {
                            if (caption.isBlank()) onToast("Write a few words first, then AI can improve it")
                            else onRequestAiCaption(caption) { r -> if (r.isNotBlank()) caption = r }
                        }, label = { Text("AI Caption", fontSize = 12.sp) })
                        AssistChip(onClick = {
                            if (caption.isBlank()) onToast("Write a few words first, then AI can suggest tags")
                            else onRequestAiHashtags(caption) { tags -> if (tags.isNotEmpty()) caption = caption.trimEnd() + "\n" + tags.joinToString(" ") }
                        }, label = { Text("AI Tags", fontSize = 12.sp) })
                    }

                    if (mode == CreateMode.POST) {
                        Spacer(modifier = Modifier.height(14.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("Original", "Neon Glow", "Cyberpunk", "Sunset Warm", "Noir", "Vibrant").size) { i ->
                                val f = listOf("Original", "Neon Glow", "Cyberpunk", "Sunset Warm", "Noir", "Vibrant")[i]
                                FilterChip(
                                    selected = selectedFilter == f,
                                    onClick = { selectedFilter = f },
                                    label = { Text(f, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VynoraNeonCyan, selectedLabelColor = Color.Black)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    SettingRow(Icons.Outlined.LocationOn, if (location.isBlank()) "Add location" else location, null, fg, sub) { showLocationDialog = true }
                    HorizontalDivider(color = if (isDarkMode) Color(0x22FFFFFF) else Color(0x1F000000))
                    SettingRow(
                        Icons.Outlined.Visibility, "Audience",
                        when (visibility) { "FOLLOWERS" -> "Followers"; "PRIVATE" -> "Only me"; else -> "Public" },
                        fg, sub
                    ) { showAudienceDialog = true }
                    if (mode == CreateMode.MOMENT) {
                        HorizontalDivider(color = if (isDarkMode) Color(0x22FFFFFF) else Color(0x1F000000))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Star, contentDescription = null, tint = fg)
                            Spacer(modifier = Modifier.width(14.dp))
                            Text("Close friends only", color = fg, fontSize = 15.sp, modifier = Modifier.weight(1f))
                            Switch(checked = isCloseFriends, onCheckedChange = { isCloseFriends = it })
                        }
                    }
                    if (mode == CreateMode.CLIP) {
                        HorizontalDivider(color = if (isDarkMode) Color(0x22FFFFFF) else Color(0x1F000000))
                        SettingRow(Icons.Outlined.MoreHoriz, "More options", null, fg, sub) { step = 2 }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { confirmSafeContent = !confirmSafeContent },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = confirmSafeContent, onCheckedChange = { confirmSafeContent = it })
                        Text(
                            text = "I confirm this has no nudity, sexual (18+), violent or illegal content, and I own the rights to it. Rule-breaking content is removed and accounts can be banned.",
                            fontSize = 11.5.sp, lineHeight = 16.sp, color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
                        )
                    }
                    if (pickedSizeMb > 0f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "This ${if (mode == CreateMode.CLIP) "reel" else "upload"} will use about ${String.format("%.1f", pickedSizeMb)} MB of data. It uploads in the background, so you can keep using the app.",
                            fontSize = 12.sp, color = sub
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Button(
                    onClick = {
                        val p = picked
                        if (p == null) { onToast("Please select a photo or video first"); return@Button }
                        if (!confirmSafeContent) { onToast("Please confirm your content follows the rules (no 18+ / nudity) ✅"); return@Button }
                        val kindName = when (mode) { CreateMode.MOMENT -> "MOMENT"; CreateMode.CLIP -> "CLIP"; else -> "POST" }
                        val finalCaption = caption.trim()
                        val finalTags = Regex("#[\\p{L}\\p{N}_]+").findAll(finalCaption).map { it.value }.distinct().toList()
                        val flags = buildSet {
                            if (hideLikes) add("HIDE_LIKES")
                            if (hideShares) add("HIDE_SHARES")
                            if (commentsOff) add("NO_COMMENTS")
                        }
                        onStartBackgroundUpload(
                            kindName, p.uri, finalCaption, finalTags, location.ifBlank { null },
                            selectedFilter, visibility, "Original audio", isCloseFriends, flags
                        )
                        picked = null; pickedSizeMb = 0f; caption = ""; location = ""; selectedFilter = "Original"
                        hideLikes = false; hideShares = false; commentsOff = false; confirmSafeContent = false
                        step = 0
                        onToast("Upload started. You can keep using the app.")
                    },
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 96.dp).height(50.dp).testTag("publish_button"),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink)
                ) { Text("Share", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White) }
            }

            // ----------------------------------------------------- MORE OPTIONS
            else -> Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { step = 1 }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = fg) }
                    Text("More options", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = fg)
                }
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("How others can interact with your reel", color = sub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    ToggleRow(Icons.Outlined.FavoriteBorder, "Hide like count on this reel", hideLikes, fg) { hideLikes = it }
                    ToggleRow(Icons.Outlined.Send, "Hide share count on this reel", hideShares, fg) { hideShares = it }
                    ToggleRow(Icons.Outlined.ChatBubbleOutline, "Turn off commenting", commentsOff, fg) { commentsOff = it }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Only you will see the number of likes and shares on this reel. People can't comment if commenting is turned off.",
                        color = sub, fontSize = 12.sp, lineHeight = 17.sp
                    )
                }
            }
        }

        // Post / Story / Reel selector (pick step only)
        if (step == 0) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = if (isDarkMode) Color(0xE6101423) else Color(0xE6FFFFFF),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 104.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    listOf(CreateMode.POST to "POST", CreateMode.MOMENT to "STORY", CreateMode.CLIP to "REEL").forEach { (m, label) ->
                        Text(
                            text = label,
                            letterSpacing = 3.sp,
                            fontSize = 15.sp,
                            fontWeight = if (mode == m) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (mode == m) fg else sub,
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).clickable { mode = m }.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }

    if (showLocationDialog) {
        var tmp by remember { mutableStateOf(location) }
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Add location") },
            text = {
                OutlinedTextField(value = tmp, onValueChange = { tmp = it }, singleLine = true, placeholder = { Text("e.g. Bangalore, India") })
            },
            confirmButton = { TextButton(onClick = { location = tmp.trim(); showLocationDialog = false }) { Text("Done") } },
            dismissButton = { TextButton(onClick = { location = ""; showLocationDialog = false }) { Text("Remove") } }
        )
    }
    if (showAudienceDialog) {
        AlertDialog(
            onDismissRequest = { showAudienceDialog = false },
            title = { Text("Audience") },
            text = {
                Column {
                    listOf("PUBLIC" to "Public", "FOLLOWERS" to "Followers", "PRIVATE" to "Only me").forEach { (v, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { visibility = v; showAudienceDialog = false }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = visibility == v, onClick = { visibility = v; showAudienceDialog = false })
                            Text(label, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAudienceDialog = false }) { Text("Close") } }
        )
    }
}

@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String?,
    fg: Color,
    sub: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg)
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = fg, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (value != null) Text(value, color = sub, fontSize = 14.sp)
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = sub)
    }
}

@Composable
private fun ToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    checked: Boolean,
    fg: Color,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = fg)
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = fg, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
