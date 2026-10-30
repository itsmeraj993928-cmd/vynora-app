package com.example.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import com.example.model.hasActiveNote
import com.example.model.activeStreak
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
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.components.StoryRingAvatar
import com.example.util.formatCount
import com.example.util.CloudinaryUploader
import com.example.components.VynoraLogoIcon
import com.example.components.VynoraOfficialBadge
import com.example.components.VynoraPlusBadge
import com.example.model.Clip
import com.example.model.Highlight
import com.example.model.Post
import com.example.model.User
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    user: User,
    posts: List<Post>,
    clips: List<Clip>,
    isDarkMode: Boolean,
    onOpenSettings: () -> Unit,
    onOpenCreatorStudio: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onUpdateProfile: (name: String, bio: String, website: String) -> Unit,
    onOpenEditProfile: () -> Unit = {},
    onAddHighlight: (title: String, coverUrl: String) -> Unit = { _, _ -> },
    onToast: (String) -> Unit,
    savedAccounts: List<com.example.model.SavedAccount> = emptyList(),
    savedFolders: Map<String, String> = emptyMap(),
    onSwitchAccount: (accountId: String, identifier: String) -> Unit = { _, _ -> },
    onOpenFollowList: (tab: Int) -> Unit = {},
    onAddAccount: () -> Unit = {},
    switchPasswordFor: com.example.model.SavedAccount? = null,
    onSubmitSwitchPassword: (String) -> Unit = {},
    onDismissSwitchPassword: () -> Unit = {},
    onSaveNote: (text: String, song: com.example.util.MusicTrack?) -> Unit = { _, _ -> },
    isOwnProfile: Boolean = true,
    isFollowing: Boolean = false,
    onFollowClick: () -> Unit = {},
    followsYou: Boolean = false,
    isMuted: Boolean = false,
    onMessageClick: () -> Unit = {},
    onMuteClick: () -> Unit = {},
    onBlockClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
    onOpenPost: (String) -> Unit = {},
    onOpenClip: (Clip) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showAccountSwitcher by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0: Posts, 1: Clips, 2: Tagged, 3: Saved
    var showQrDialog by remember { mutableStateOf(false) }
    var showFollowOptions by remember { mutableStateOf(false) }
    var showEditProfileSheet by remember { mutableStateOf(false) }

    // Highlights Creation State
    var showAddHighlightDialog by remember { mutableStateOf(false) }
    var highlightTitle by remember { mutableStateOf("") }
    var highlightCoverUri by remember { mutableStateOf<Uri?>(null) }
    var highlightCoverSecureUrl by remember { mutableStateOf<String?>(null) }
    var isUploadingHighlightCover by remember { mutableStateOf(false) }
    var highlightUploadProgress by remember { mutableStateOf(0) }
    var highlightUploadError by remember { mutableStateOf<String?>(null) }

    val highlightCoverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            highlightCoverUri = uri
            highlightUploadError = null
            highlightUploadProgress = 0
            isUploadingHighlightCover = true
            coroutineScope.launch {
                val res = CloudinaryUploader.uploadUri(
                    context = context,
                    uri = uri,
                    onProgress = { progress -> highlightUploadProgress = progress }
                )
                isUploadingHighlightCover = false
                if (res.isSuccess) {
                    highlightCoverSecureUrl = res.getOrNull()?.secureUrl
                    onToast("Highlight cover uploaded! ☁️")
                } else {
                    highlightUploadError = res.exceptionOrNull()?.localizedMessage ?: "Upload failed"
                }
            }
        }
    }

    val userPosts = posts.filter { it.author.id == user.id || it.author.username == user.username }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    val savedPosts = posts.filter { it.isSaved && (selectedFolder == null || savedFolders[it.id] == selectedFolder) }

    val screenBg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)
    val cardBg = if (isDarkMode) Color(0xFF0F121E) else Color(0xFFFFFFFF)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(screenBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("profile_screen")
    ) {
        // --- 1. Top Bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = if (isOwnProfile) Modifier.clickable { showAccountSwitcher = true } else Modifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!isOwnProfile) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                    }
                }
                Text(
                    text = user.username,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
                if (user.isOfficial) {
                    VynoraOfficialBadge(size = 18.dp)
                } else if (user.isPlusActive) {
                    VynoraPlusBadge(size = 18.dp)
                }
                if (isOwnProfile) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = "Switch account",
                        tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              if (isOwnProfile) {
                // Creator Studio
                IconButton(onClick = onOpenCreatorStudio) {
                    Icon(
                        imageVector = Icons.Outlined.Analytics,
                        contentDescription = "Creator Studio",
                        tint = VynoraNeonCyan
                    )
                }

                // Admin Panel button (if admin role)
                if (user.role == "ADMIN") {
                    IconButton(onClick = onOpenAdminPanel) {
                        Icon(
                            imageVector = Icons.Outlined.AdminPanelSettings,
                            contentDescription = "Admin",
                            tint = VynoraNeonPink
                        )
                    }
                }

                // Settings
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                }
              }
            }
        }

        // --- 2. Profile Details Header & Stats ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StoryRingAvatar(
                            avatarUrl = user.avatarUrl,
                            hasStory = false,
                            size = 80.dp,
                            onClick = { if (isOwnProfile && user.avatarUrl.isBlank()) onOpenEditProfile() }
                        )

                        // Stats (Posts, Followers, Following)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(22.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProfileStatItem("Posts", formatCount(userPosts.size), isDarkMode)
                            if (!user.hideFollowersCount) {
                                ProfileStatItem("Followers", formatCount(user.followersCount), isDarkMode, onClick = { onOpenFollowList(0) })
                                ProfileStatItem("Following", formatCount(user.followingCount), isDarkMode, onClick = { onOpenFollowList(1) })
                            } else {
                                ProfileStatItem("Followers", "—", isDarkMode)
                                ProfileStatItem("Following", "—", isDarkMode)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Name, Pronouns & Category
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = user.fullName.replace('\n', ' '),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                        if (user.pronouns.isNotBlank()) {
                            Text(
                                text = "(${user.pronouns})",
                                fontSize = 12.sp,
                                color = if (isDarkMode) Color(0xFFA855F7) else Color(0xFF7E22CE),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (user.activeStreak() > 0) {
                        Text(
                            text = "🔥 ${user.activeStreak()} day streak",
                            fontSize = 11.5.sp,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Note (24h) - tap to edit on my own profile
                    run {
                        var showNote by remember { mutableStateOf(false) }
                        if (showNote) {
                            NoteEditDialog(
                                initialText = if (user.hasActiveNote()) user.note else "",
                                initialSong = if (user.hasActiveNote()) user.noteSong else "",
                                initialSongUrl = user.noteSongUrl,
                                isDarkMode = isDarkMode,
                                onSave = { t, sg -> onSaveNote(t, sg); showNote = false },
                                onDismiss = { showNote = false }
                            )
                        }
                        if (user.hasActiveNote()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            NoteBubble(
                                user.note, user.noteSong, isDarkMode,
                                onClick = if (isOwnProfile) ({ showNote = true }) else null
                            )
                        } else if (isOwnProfile) {
                            TextButton(onClick = { showNote = true }, contentPadding = PaddingValues(0.dp)) {
                                Text("+ Add note", color = VynoraNeonCyan, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    if (user.category.isNotBlank()) {
                        Text(
                            text = user.category,
                            fontSize = 11.5.sp,
                            color = VynoraNeonCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (user.isAiGeneratedProfile) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x33A855F7),
                            border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(12.dp))
                                Text("AI-generated profile", color = Color(0xFFA855F7), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (user.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = user.bio,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF334155)
                        )
                    }

                    // Multiple Links
                    val displayLinks = if (user.links.isNotEmpty()) user.links else if (user.website.isNotBlank()) listOf(user.website) else emptyList()
                    if (displayLinks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            displayLinks.take(3).forEach { link ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.clickable { onToast("Opening $link...") }
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Link,
                                        contentDescription = null,
                                        tint = VynoraNeonCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = link,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = VynoraNeonCyan
                                    )
                                }
                            }
                        }
                    }

                    // Contact Options (Email / Phone if enabled)
                    if (user.showEmailOnProfile || user.showPhoneOnProfile) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (user.showEmailOnProfile && user.email.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { onToast("Email: ${user.email}") },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0x3300D1FF)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Outlined.Email, contentDescription = null, tint = VynoraNeonCyan, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Email", fontSize = 11.5.sp, color = VynoraNeonCyan)
                                }
                            }
                            if (user.showPhoneOnProfile && user.phone.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { onToast("Contact: ${user.phone}") },
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, Color(0x3310B981)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Outlined.Phone, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Contact", fontSize = 11.5.sp, color = Color(0xFF10B981))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons (Edit Profile, Share QR) / Follow for other people
                    if (!isOwnProfile) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { if (isFollowing) showFollowOptions = true else onFollowClick() },
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) (if (isDarkMode) Color(0xFF1E2235) else Color(0xFFE2E8F0)) else VynoraNeonPink
                                )
                            ) {
                                Text(
                                    text = if (isFollowing) "Following" else if (followsYou) "Follow Back" else "Follow",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isFollowing) (if (isDarkMode) Color.White else Color(0xFF0F172A)) else Color.White
                                )
                                if (isFollowing) {
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Button(
                                onClick = onMessageClick,
                                modifier = Modifier.weight(1f).height(40.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isDarkMode) Color(0xFF1E2235) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Text(
                                    text = "Message",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                )
                            }
                        }
                        if (showFollowOptions) {
                            FollowOptionsSheet(
                                username = user.username,
                                isDarkMode = isDarkMode,
                                isMuted = isMuted,
                                onDismiss = { showFollowOptions = false },
                                onMute = { showFollowOptions = false; onMuteClick() },
                                onUnfollow = { showFollowOptions = false; onFollowClick() },
                                onBlock = { showFollowOptions = false; onBlockClick() }
                            )
                        }
                    } else Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onOpenEditProfile,
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("profile_edit_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) Color(0xFF1E2235) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Text(
                                text = "Edit Profile",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                        }

                        Button(
                            onClick = { showQrDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) Color(0xFF1E2235) else Color(0xFFE2E8F0)
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.QrCode,
                                    contentDescription = null,
                                    tint = VynoraNeonCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Share Card",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // --- 3. Story Highlights Row (Real Highlights with "New" Button) ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 12.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // "+ New" Highlight Button (only on my own profile)
                        if (isOwnProfile) item {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clickable {
                                        highlightTitle = ""
                                        highlightCoverUri = null
                                        highlightCoverSecureUrl = null
                                        highlightUploadError = null
                                        highlightUploadProgress = 0
                                        showAddHighlightDialog = true
                                    }
                                    .testTag("add_highlight_button")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .border(
                                            width = 1.5.dp,
                                            brush = Brush.sweepGradient(
                                                listOf(VynoraNeonPink, VynoraNeonCyan, VynoraNeonPink)
                                            ),
                                            shape = CircleShape
                                        )
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(if (isDarkMode) Color(0xFF131726) else Color(0xFFE2E8F0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "New Highlight",
                                        tint = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "New",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
                                )
                            }
                        }

                        // Real User Highlights Only (no fake data)
                        items(user.highlights) { highlight ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { onToast("Highlight: ${highlight.title}") }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .border(
                                            width = 1.5.dp,
                                            color = if (isDarkMode) Color(0x5500D1FF) else Color(0x33000000),
                                            shape = CircleShape
                                        )
                                        .padding(3.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E2235)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (highlight.coverUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = highlight.coverUrl,
                                            contentDescription = highlight.title,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = null,
                                            tint = VynoraNeonCyan,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = highlight.title,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
                                )
                            }
                        }
                    }
                }
            }

            // --- 4. Profile Tabs ---
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = VynoraNeonPink,
                    divider = { HorizontalDivider(color = if (isDarkMode) Color(0x22FFFFFF) else Color(0x1F000000)) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Filled.GridOn, contentDescription = "Posts") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Filled.Movie, contentDescription = "Clips") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Filled.Bookmark, contentDescription = "Saved") }
                    )
                }
            }

            // --- 4b. Saved folders ---
            if (selectedTab == 2 && savedFolders.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(selected = selectedFolder == null, onClick = { selectedFolder = null }, label = { Text("All") })
                        savedFolders.values.toSet().forEach { f ->
                            FilterChip(selected = selectedFolder == f, onClick = { selectedFolder = f }, label = { Text(f) })
                        }
                    }
                }
            }

            // --- 5. Tab Content Grid ---
            item {
                val userClips = clips.filter { it.author.id == user.id || it.author.username == user.username }
                // thumbnail, id, isClip
                val displayList: List<Triple<String, String, Boolean>> = when (selectedTab) {
                    1 -> userClips.map { Triple(profileVideoThumb(it.mediaUrl), it.id, true) }
                    2 -> savedPosts.map { Triple(profilePostThumb(it), it.id, false) }
                    else -> userPosts.map { Triple(profilePostThumb(it), it.id, false) }
                }

                if (displayList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = when (selectedTab) {
                                    1 -> Icons.Filled.Movie
                                    2 -> Icons.Filled.Bookmark
                                    else -> Icons.Filled.GridOn
                                },
                                contentDescription = null,
                                tint = VynoraNeonCyan,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = when (selectedTab) {
                                    1 -> "No Clips yet"
                                    2 -> "No saved items"
                                    else -> "No posts yet"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when (selectedTab) {
                                    1 -> "Create your first short video Clip!"
                                    2 -> "Save posts to review them here later."
                                    else -> "Share your world with your first post or moment."
                                },
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        val rows = displayList.chunked(3)
                        rows.forEach { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                rowItems.forEach { (mediaUrl, itemId, isClipItem) ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(if (isClipItem) 0.6f else 1f)
                                            .background(Color(0xFF1E2235))
                                            .clickable {
                                                if (isClipItem) {
                                                    clips.firstOrNull { it.id == itemId }?.let { onOpenClip(it) }
                                                } else onOpenPost(itemId)
                                            }
                                    ) {
                                        AsyncImage(
                                            model = mediaUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        if (isClipItem) {
                                            val v = clips.firstOrNull { it.id == itemId }?.viewsCount ?: 0
                                            Row(
                                                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Filled.Visibility, contentDescription = "Views", tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(com.example.util.formatCount(v), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                // Empty placeholders to balance row
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- QR Code Dialog ---
    if (showQrDialog) {
        Dialog(onDismissRequest = { showQrDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    VynoraLogoIcon(size = 56.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "@${user.username}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "Scan to follow on VYNORA",
                        fontSize = 12.sp,
                        color = VynoraNeonCyan
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // QR Card Box
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(3.dp, Brush.sweepGradient(listOf(VynoraNeonPink, VynoraNeonCyan, VynoraNeonPink)), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCode2,
                            contentDescription = "QR Code",
                            tint = Color.Black,
                            modifier = Modifier.size(150.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            onToast("Profile link copied! 📋")
                            showQrDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink)
                    ) {
                        Text("Copy Profile URL", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // --- Password prompt for switching to an account that has no saved login yet ---
    switchPasswordFor?.let { acc ->
        var pw by remember(acc.id) { mutableStateOf("") }
        var show by remember(acc.id) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = onDismissSwitchPassword,
            shape = RoundedCornerShape(20.dp),
            containerColor = cardBg,
            title = { Text("Switch to @${acc.username}", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the password once. Next time this account switches with one tap.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    OutlinedTextField(
                        value = pw,
                        onValueChange = { pw = it },
                        singleLine = true,
                        label = { Text("Password") },
                        visualTransformation = if (show) androidx.compose.ui.text.input.VisualTransformation.None
                            else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = { TextButton(onClick = { show = !show }) { Text(if (show) "Hide" else "Show", color = VynoraNeonCyan) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = pw.length >= 6, onClick = { onSubmitSwitchPassword(pw) }) {
                    Text("Switch", color = VynoraNeonCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = onDismissSwitchPassword) { Text("Cancel", color = Color(0xFF94A3B8)) } }
        )
    }

    // --- Account switcher ---
    if (showAccountSwitcher) {
        AlertDialog(
            onDismissRequest = { showAccountSwitcher = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = cardBg,
            title = { Text("Accounts", fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    savedAccounts.forEach { acc ->
                        val isCurrent = acc.id == user.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !isCurrent) {
                                    showAccountSwitcher = false
                                    onSwitchAccount(acc.id, acc.identifier)
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StoryRingAvatar(avatarUrl = acc.avatarUrl, hasStory = false, size = 40.dp)
                            Text(
                                text = "@${acc.username}",
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                            )
                            if (isCurrent) Text("Current", color = VynoraNeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    OutlinedButton(
                        onClick = { showAccountSwitcher = false; onAddAccount() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, tint = VynoraNeonCyan)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add account / Log in to another", color = VynoraNeonCyan, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Accounts you have logged into on this phone can be switched with one tap.",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAccountSwitcher = false }) { Text("Close", color = VynoraNeonCyan) } }
        )
    }

    // --- Edit Profile Sheet ---
    if (showEditProfileSheet) {
        var newFullName by remember { mutableStateOf(user.fullName) }
        var newBio by remember { mutableStateOf(user.bio) }
        var newWebsite by remember { mutableStateOf(user.website) }

        Dialog(onDismissRequest = { showEditProfileSheet = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Edit Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newFullName,
                        onValueChange = { newFullName = it.replace("\n", " ").take(30) },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newBio,
                        onValueChange = { newBio = it },
                        label = { Text("Bio") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newWebsite,
                        onValueChange = { newWebsite = it },
                        label = { Text("Link / Website") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            onUpdateProfile(newFullName, newBio, newWebsite)
                            showEditProfileSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // --- Add Highlight Dialog with Cloudinary Cover Upload ---
    if (showAddHighlightDialog) {
        Dialog(onDismissRequest = { if (!isUploadingHighlightCover) showAddHighlightDialog = false }) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, Color(0x3300D1FF)),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "New Story Highlight",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cover selector
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(if (isDarkMode) Color(0xFF161928) else Color(0xFFE2E8F0))
                            .border(1.5.dp, VynoraNeonCyan, CircleShape)
                            .clickable {
                                highlightCoverPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (highlightCoverSecureUrl != null || highlightCoverUri != null) {
                            AsyncImage(
                                model = highlightCoverSecureUrl ?: highlightCoverUri,
                                contentDescription = "Cover",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.AddAPhoto, contentDescription = null, tint = VynoraNeonCyan, modifier = Modifier.size(24.dp))
                                Text("Cover", fontSize = 10.sp, color = VynoraNeonCyan)
                            }
                        }
                    }

                    // Upload Progress Bar
                    if (isUploadingHighlightCover) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { highlightUploadProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = VynoraNeonCyan,
                            trackColor = Color(0xFF1E2438)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Uploading to Cloudinary... $highlightUploadProgress%",
                            fontSize = 11.5.sp,
                            color = VynoraNeonCyan,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Upload Error with Retry
                    if (highlightUploadError != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Upload failed: $highlightUploadError",
                            fontSize = 11.5.sp,
                            color = Color(0xFFEF4444),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                val uri = highlightCoverUri
                                if (uri != null) {
                                    highlightUploadError = null
                                    highlightUploadProgress = 0
                                    isUploadingHighlightCover = true
                                    coroutineScope.launch {
                                        val res = CloudinaryUploader.uploadUri(
                                            context = context,
                                            uri = uri,
                                            onProgress = { p -> highlightUploadProgress = p }
                                        )
                                        isUploadingHighlightCover = false
                                        if (res.isSuccess) {
                                            highlightCoverSecureUrl = res.getOrNull()?.secureUrl
                                            onToast("Cover uploaded! ☁️")
                                        } else {
                                            highlightUploadError = res.exceptionOrNull()?.localizedMessage ?: "Retry failed"
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Retry Upload", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = highlightTitle,
                        onValueChange = { highlightTitle = it },
                        label = { Text("Highlight Title") },
                        placeholder = { Text("e.g. Vibes, Travel") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAddHighlightDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                val title = highlightTitle.trim()
                                val cover = highlightCoverSecureUrl ?: ""
                                onAddHighlight(title, cover)
                                showAddHighlightDialog = false
                            },
                            enabled = highlightTitle.isNotBlank() && !isUploadingHighlightCover,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink)
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatItem(label: String, count: String, isDarkMode: Boolean, onClick: (() -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = if (onClick != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable { onClick() }.padding(4.dp) else Modifier
    ) {
        Text(
            text = count,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 17.sp,
            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FollowOptionsSheet(
    username: String,
    isDarkMode: Boolean,
    isMuted: Boolean,
    onDismiss: () -> Unit,
    onMute: () -> Unit,
    onUnfollow: () -> Unit,
    onBlock: () -> Unit
) {
    val textColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = if (isDarkMode) Color(0xFF0F121E) else Color.White
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = username,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = textColor,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            HorizontalDivider(color = if (isDarkMode) Color(0x22FFFFFF) else Color(0x1F000000))
            Text(
                text = if (isMuted) "Unmute posts and reels" else "Mute posts and reels",
                fontSize = 15.sp,
                color = textColor,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onMute).padding(horizontal = 20.dp, vertical = 16.dp)
            )
            Text(
                text = "Unfollow",
                fontSize = 15.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onUnfollow).padding(horizontal = 20.dp, vertical = 16.dp)
            )
            Text(
                text = "Block",
                fontSize = 15.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onBlock).padding(horizontal = 20.dp, vertical = 16.dp)
            )
        }
    }
}


private fun profileVideoThumb(videoUrl: String): String {
    val withoutExt = videoUrl.substringBeforeLast('.', videoUrl)
    return if (videoUrl.contains("/video/upload/")) {
        withoutExt.replace("/video/upload/", "/video/upload/so_0,w_480,h_480,c_fill/") + ".jpg"
    } else withoutExt + ".jpg"
}

private fun profilePostThumb(post: Post): String {
    val first = post.mediaUrls.firstOrNull() ?: return ""
    return if (post.isVideo || first.contains("/video/upload/")) profileVideoThumb(first) else first
}
