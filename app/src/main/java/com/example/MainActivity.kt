package com.example

import android.os.Bundle
import android.app.AlertDialog
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.components.CommentsBottomSheet
import com.example.components.ClipShareSheet
import com.example.components.ReportDialog
import com.example.components.ShareDialog
import com.example.components.VynoraBottomNav
import android.util.Log
import com.google.firebase.FirebaseApp
import com.example.screens.*
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink
import com.example.ui.theme.VynoraTheme
import com.example.viewmodel.MainTab
import com.example.viewmodel.ScreenDestination
import com.example.viewmodel.VynoraViewModel

open class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashCatcher()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "FirebaseApp init check: ${e.message}", e)
        }
        enableEdgeToEdge()
        setContent {
            val viewModel: VynoraViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            CompositionLocalProvider(LocalLayoutDirection provides uiState.currentLanguage.layoutDirection) {
                VynoraTheme(darkTheme = uiState.isDarkMode) {
                    VynoraApp(
                        viewModel = viewModel
                    )
                }
            }
        }
        showLastCrashIfAny()
    }

    override fun onStart() {
        super.onStart()
        com.example.viewmodel.AppForeground.inForeground = true
    }

    override fun onStop() {
        com.example.viewmodel.AppForeground.inForeground = false
        super.onStop()
    }

    // --- Crash catcher: saves the real error so it can be shown on next launch ---
    private fun installCrashCatcher() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val text = Log.getStackTraceString(throwable)
                getSharedPreferences("vynora_crash", Context.MODE_PRIVATE)
                    .edit().putString("last_crash", text).commit()
            } catch (_: Throwable) { }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun showLastCrashIfAny() {
        try {
            val prefs = getSharedPreferences("vynora_crash", Context.MODE_PRIVATE)
            val crash = prefs.getString("last_crash", null) ?: return
            prefs.edit().remove("last_crash").apply()
            val lines = crash.lines()
            val root = lines.lastOrNull { it.startsWith("Caused by:") } ?: lines.firstOrNull().orEmpty()
            val msg = "ROOT: $root\n\n" + crash.take(2500)
            AlertDialog.Builder(this)
                .setTitle("Last crash (screenshot bhejo)")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show()
        } catch (_: Throwable) { }
    }
}

@Composable
fun VynoraApp(viewModel: VynoraViewModel) {
    val appCtx = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.remember {
        com.example.util.DataSaver.init(appCtx)
        com.example.util.NetworkMonitor.start(appCtx)
        true
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedAccountsList by viewModel.savedAccounts.collectAsStateWithLifecycle()
    val switchPasswordFor by viewModel.switchPasswordFor.collectAsStateWithLifecycle()
    val momentViewers by viewModel.momentViewers.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pollVotes by viewModel.pollVotes.collectAsStateWithLifecycle()
    var shareClip by remember { mutableStateOf<com.example.model.Clip?>(null) }

    // Handle Android system back press
    val hostContext = androidx.compose.ui.platform.LocalContext.current

    // Ask for the microphone once, right after the account is created / first login,
    // so voice notes work without a surprise prompt later.
    val micPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { _ -> }
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) {
            val prefs = hostContext.getSharedPreferences("vynora_prefs", android.content.Context.MODE_PRIVATE)
            if (!prefs.getBoolean("asked_mic_permission", false)) {
                prefs.edit().putBoolean("asked_mic_permission", true).apply()
                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    hostContext, android.Manifest.permission.RECORD_AUDIO
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!granted) micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    // Notifications (Android 13+): needed to show "incoming call" when the app is in the background.
    val notifPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { _ -> }
    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn && android.os.Build.VERSION.SDK_INT >= 33) {
            val prefs = hostContext.getSharedPreferences("vynora_prefs", android.content.Context.MODE_PRIVATE)
            if (!prefs.getBoolean("asked_notif_permission", false)) {
                prefs.edit().putBoolean("asked_notif_permission", true).apply()
                val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                    hostContext, android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (!granted) notifPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    BackHandler(enabled = uiState.currentScreen !is ScreenDestination.Splash) {
        // If there is nowhere left to go back to (Home screen), close the app normally
        if (uiState.currentScreen is ScreenDestination.Auth || !viewModel.navigateBack()) {
            (hostContext as? android.app.Activity)?.finish()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- Main Screen Switching ---
        when (val screen = uiState.currentScreen) {
            is ScreenDestination.Splash -> {
                SplashScreen()
            }
            is ScreenDestination.Onboarding -> {
                OnboardingScreen(
                    currentLanguage = uiState.currentLanguage,
                    onComplete = {
                        viewModel.navigateTo(ScreenDestination.Main(MainTab.HOME))
                    }
                )
            }
            is ScreenDestination.Auth -> {
                AuthScreen(
                    currentLanguage = uiState.currentLanguage,
                    viewModel = viewModel,
                    onAuthSuccess = {
                        viewModel.navigateTo(ScreenDestination.Main(MainTab.HOME))
                    }
                )
            }
            is ScreenDestination.Main -> {
                if (!uiState.isLoggedIn || uiState.currentUser == null) {
                    AuthScreen(
                        currentLanguage = uiState.currentLanguage,
                        viewModel = viewModel,
                        onAuthSuccess = {
                            viewModel.navigateTo(ScreenDestination.Main(MainTab.HOME))
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = Color.Transparent,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            VynoraBottomNav(
                                selectedTab = uiState.selectedTab,
                                onTabSelected = { tab -> viewModel.selectTab(tab) },
                                userAvatarUrl = uiState.currentUser?.avatarUrl ?: "",
                                currentLanguage = uiState.currentLanguage,
                                isDarkMode = uiState.isDarkMode
                            )
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = innerPadding.calculateBottomPadding())
                        ) {
                            when (uiState.selectedTab) {
                                MainTab.HOME -> {
                                    HomeScreen(
                                        posts = uiState.posts,
                                        clips = uiState.clips,
                                        onOpenClips = { c -> viewModel.navigateTo(ScreenDestination.ClipViewer(c.id, "")) },
                                        moments = uiState.moments,
                                        currentUser = uiState.currentUser ?: com.example.model.User(),
                                        unreadNotificationsCount = uiState.notifications.count { !it.isRead },
                                        unreadChatsCount = uiState.chats.sumOf { it.unreadCount },
                                        isDarkMode = uiState.isDarkMode,
                                        currentLanguage = uiState.currentLanguage,
                                        onOpenMomentsViewer = { idx -> viewModel.navigateTo(ScreenDestination.MomentsViewer(idx)) },
                                        onCreateMoment = { viewModel.selectTab(MainTab.CREATE) },
                                        onOpenNotifications = { viewModel.navigateTo(ScreenDestination.Notifications) },
                                        onOpenChats = { viewModel.navigateTo(ScreenDestination.ChatsList) },
                                        onGoLiveClick = { viewModel.navigateTo(ScreenDestination.LiveStudio) },
                                        onDeletePost = { postId -> viewModel.deleteMyPost(postId) },
                                        onLikeToggle = { postId -> viewModel.toggleLikePost(postId) },
                                        onSaveToggle = { postId -> viewModel.toggleSavePost(postId) },
                                        onCommentsClick = { postId -> viewModel.openComments(postId) },
                                        onShareClick = { post -> viewModel.openShare(post) },
                                        onReportClick = { type, id, username -> viewModel.openReportDialog(type, id, username) },
                                        onAuthorClick = { username -> viewModel.openUserProfileByUsername(username) },
                                        onFollowAuthor = { id, username -> viewModel.toggleFollowById(id, username) },
                                        isRefreshing = isRefreshing,
                                        onRefresh = { viewModel.refreshAll() },
                                        onSaveNote = { t, sg -> viewModel.saveNote(t, sg) }
                                    )
                                }
                                MainTab.EXPLORE -> {
                                    ExploreScreen(
                                        isRefreshing = isRefreshing,
                                        onRefresh = { viewModel.refreshAll() },
                                        posts = uiState.posts.filter { it.author.id != uiState.currentUser?.id },
                                        isDarkMode = uiState.isDarkMode,
                                        onUserClick = { username -> viewModel.openUserProfileByUsername(username) },
                                        onFollowToggle = { username -> viewModel.toggleFollowCreator(username) },
                                        onPostClick = { post -> viewModel.openPostViewer(post.id) }
                                    )
                                }
                                MainTab.CREATE -> {
                                    CreateStudioScreen(
                                        isDarkMode = uiState.isDarkMode,
                                        currentLanguage = uiState.currentLanguage,
                                        onPublishPost = { caption, hashtags, mediaUrls, loc, filter, visibility ->
                                            viewModel.publishPost(caption, hashtags, mediaUrls, loc, filter, visibility = visibility)
                                        },
                                        onPublishMoment = { textOverlay, mediaUrl, music, isCloseFriends, visibility ->
                                            viewModel.publishMoment(textOverlay, mediaUrl, music, isCloseFriends, visibility)
                                            viewModel.selectTab(MainTab.HOME)
                                        },
                                        onPublishClip = { caption, mediaUrl, music, visibility ->
                                            viewModel.publishClip(caption, mediaUrl, music, visibility = visibility)
                                        },
                                        onClose = { viewModel.selectTab(MainTab.HOME) },
                                        onStartBackgroundUpload = { kind, uri, cap, tags, loc, filter, vis, music, close, flags ->
                                            viewModel.startBackgroundUpload(kind, uri, cap, tags, loc, filter, vis, music, close, flags)
                                            viewModel.selectTab(if (kind == "CLIP") MainTab.CLIPS else MainTab.HOME)
                                        },
                                        onRequestAiCaption = { topic, onResult ->
                                            viewModel.requestAiCaption(topic, onResult)
                                        },
                                        onRequestAiImageCaption = { jpeg, hint, onResult ->
                                            viewModel.requestAiImageCaption(jpeg, hint, onResult)
                                        },
                                        onRequestAiHashtags = { topic, onResult ->
                                            viewModel.requestAiHashtags(topic, onResult)
                                        },
                                        onToast = { msg -> viewModel.showToast(msg) }
                                    )
                                }
                                MainTab.CLIPS -> {
                                    ClipsScreen(
                                        currentUserId = uiState.currentUser?.id ?: "",
                                        onDeleteClip = { clipId -> viewModel.deleteMyClip(clipId) },
                                        clips = uiState.clips,
                                        onLikeToggle = { clipId -> viewModel.toggleLikeClip(clipId) },
                                        onSaveToggle = { clipId -> viewModel.toggleSaveClip(clipId) },
                                        onFollowToggle = { username -> viewModel.toggleFollowCreator(username) },
                                        onCommentsClick = { clipId -> viewModel.openComments(clipId) },
                                        onUserClick = { uid -> viewModel.openUserProfile(uid) },
                                        onShareClick = { clip -> shareClip = clip },
                                        onToast = { msg -> viewModel.showToast(msg) },
                                        onReport = { id, uname -> viewModel.openReportDialog("Clip", id, uname) },
                                        onCreateClick = { viewModel.selectTab(MainTab.CREATE) }
                                    )
                                }
                                MainTab.AI -> {
                                    com.example.screens.AiHubScreen(
                                        isDarkMode = uiState.isDarkMode,
                                        onPostAsReel = { uri, cap ->
                                            viewModel.startBackgroundUpload("CLIP", uri, cap, emptyList(), null, null, "PUBLIC", "Original audio", false, emptySet())
                                            viewModel.selectTab(MainTab.CLIPS)
                                        },
                                        onToast = { msg -> viewModel.showToast(msg) }
                                    )
                                }
                                MainTab.PROFILE -> {
                                    ProfileScreen(
                                        user = uiState.currentUser ?: com.example.model.User(),
                                        posts = uiState.posts,
                                        clips = uiState.clips,
                                        isDarkMode = uiState.isDarkMode,
                                        onOpenPost = { pid -> viewModel.openPostViewer(pid) },
                                        onOpenClip = { c -> viewModel.openClipViewer(c) },
                                        onOpenSettings = { viewModel.navigateTo(ScreenDestination.Settings) },
                                        onOpenCreatorStudio = { viewModel.navigateTo(ScreenDestination.CreatorAnalytics) },
                                        onOpenAdminPanel = { viewModel.navigateTo(ScreenDestination.AdminPanel) },
                                        onOpenEditProfile = { viewModel.navigateTo(ScreenDestination.EditProfile) },
                                        onUpdateProfile = { name, bio, site ->
                                            viewModel.updateProfile(name, bio, site)
                                        },
                                        onAddHighlight = { title, coverUrl ->
                                            viewModel.addHighlight(title, coverUrl)
                                        },
                                        savedAccounts = savedAccountsList,
                                        onSwitchAccount = { accId, ident -> viewModel.switchAccount(accId, ident) },
                                        onOpenFollowList = { tab -> uiState.currentUser?.let { viewModel.openFollowList(it.id, it.username, tab) } },
                                        onAddAccount = { viewModel.addAccount() },
                                        onSaveNote = { t, sg -> viewModel.saveNote(t, sg) },
                                        switchPasswordFor = switchPasswordFor,
                                        onSubmitSwitchPassword = { pw -> viewModel.submitSwitchPassword(pw) },
                                        onDismissSwitchPassword = { viewModel.dismissSwitchPassword() },
                                        onToast = { msg -> viewModel.showToast(msg) }
                                    )
                                }
                            }
                            val upStatus by viewModel.uploadStatus.collectAsStateWithLifecycle()
                            upStatus?.let { st ->
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .windowInsetsPadding(WindowInsets.statusBars)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF0F121E),
                                    tonalElevation = 6.dp
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        if (st.failed) {
                                            Text(
                                                text = "Your ${st.label} could not be uploaded",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(text = st.message, color = Color(0xFF94A3B8), fontSize = 12.sp)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                TextButton(onClick = { viewModel.retryUpload() }) { Text("Retry") }
                                                TextButton(onClick = { viewModel.dismissUpload() }) { Text("Dismiss") }
                                            }
                                        } else {
                                            val mbText = if (st.sizeMb > 0f) " (" + String.format("%.1f", st.sizeMb) + " MB)" else ""
                                            Text(
                                                text = "Uploading your ${st.label}$mbText… ${st.progress}%",
                                                color = Color.White,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            LinearProgressIndicator(
                                                progress = { st.progress / 100f },
                                                modifier = Modifier.fillMaxWidth().height(5.dp),
                                                color = Color(0xFF00D1FF),
                                                trackColor = Color(0xFF1E2438)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is ScreenDestination.MomentsViewer -> {
                MomentsViewerScreen(
                    moments = uiState.moments,
                    initialIndex = screen.initialMomentIndex,
                    currentUser = uiState.currentUser ?: com.example.model.User(),
                    onClose = { viewModel.navigateBack() },
                    onSendReply = { momentId, text ->
                        val targetChatId = uiState.chats.firstOrNull()?.id ?: "chat_${com.example.util.VynoraOfficial.OFFICIAL_USER_ID}"
                        viewModel.sendMessage(targetChatId, "Replied to moment: $text")
                    },
                    onToast = { msg -> viewModel.showToast(msg) },
                    viewers = momentViewers,
                    onMomentShown = { m -> viewModel.onMomentShown(m) },
                    pollVotes = pollVotes,
                    onVotePoll = { m, opt -> viewModel.voteMomentPoll(m, opt) },
                    onOpenProfile = { uid -> viewModel.openUserProfile(uid) }
                )
            }
            is ScreenDestination.ChatsList -> {
                ChatsScreen(
                    chats = uiState.chats,
                    isDarkMode = uiState.isDarkMode,
                    onOpenConversation = { id -> viewModel.navigateTo(ScreenDestination.ChatRoom(id)) },
                    onBack = { viewModel.navigateBack() }
                )
            }
            is ScreenDestination.ChatRoom -> {
                val conversation = uiState.chats.firstOrNull { it.id == screen.conversationId }
                    ?: screen.participant?.let { p ->
                        com.example.model.ChatConversation(
                            id = screen.conversationId,
                            participant = p,
                            lastMessage = com.example.model.ChatMessage(text = "", timestamp = "Now")
                        )
                    }
                    ?: uiState.chats.firstOrNull()
                    ?: com.example.model.ChatConversation(
                        id = screen.conversationId.ifEmpty { "chat_${com.example.util.VynoraOfficial.OFFICIAL_USER_ID}" },
                        participant = com.example.util.VynoraOfficial.createOfficialUser(),
                        lastMessage = com.example.model.ChatMessage(text = "Welcome to VYNORA!", timestamp = "Just now")
                    )
                ChatRoomScreen(
                    conversation = conversation,
                    currentUser = uiState.currentUser ?: com.example.model.User(),
                    isDarkMode = uiState.isDarkMode,
                    onSendMessage = { text, isAudio, duration ->
                        viewModel.sendMessage(conversation.id, text, isAudio, duration)
                    },
                    onAddReaction = { messageId, emoji ->
                        viewModel.addChatReaction(conversation.id, messageId, emoji)
                    },
                    onBack = { viewModel.navigateBack() },
                    onToast = { msg -> viewModel.showToast(msg) },
                    onDeleteMessage = { mid -> viewModel.deleteChatMessage(conversation.id, mid) },
                    onSendVoiceNote = { file, dur -> viewModel.sendVoiceNote(conversation.id, file, dur) },
                    onStartCall = { video ->
                        if (conversation.isGroup) viewModel.showToast("Calls work in one-to-one chats")
                        else viewModel.placeCall(conversation.participant, video, conversation.id)
                    }
                )
            }
            is ScreenDestination.Notifications -> {
                DisposableEffect(Unit) {
                    onDispose { viewModel.markNotificationsSeen() }
                }
                NotificationsScreen(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refreshAll() },
                    notifications = uiState.notifications,
                    isDarkMode = uiState.isDarkMode,
                    onMarkAllAsRead = { viewModel.markAllNotificationsRead() },
                    onBack = { viewModel.navigateBack() },
                    onUserClick = { uid, uname ->
                        if (uid.isNotBlank()) viewModel.openUserProfile(uid)
                        else viewModel.openUserProfileByUsername(uname)
                    },
                    onFollowBack = { uid, uname -> viewModel.toggleFollowById(uid, uname) },
                    onPostClick = { pid, cid -> viewModel.openComments(pid, cid) },
                    isFollowing = { uid -> viewModel.isFollowingUser(uid) }
                )
            }
            is ScreenDestination.LiveStudio -> {
                // Live streaming is not available yet: no fake viewers or comments are shown.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF050509))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Live is coming soon",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Live video is not available yet. We will turn it on in a future update.",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { viewModel.navigateBack() }) {
                        Text("Go back")
                    }
                }
            }
            is ScreenDestination.CreatorAnalytics -> {
                CreatorAnalyticsScreen(
                    stats = uiState.creatorStats,
                    topPosts = uiState.posts,
                    isDarkMode = uiState.isDarkMode,
                    onBack = { viewModel.navigateBack() },
                    onToast = { msg -> viewModel.showToast(msg) }
                )
            }
            is ScreenDestination.Settings -> {
                SettingsScreen(
                    isDarkMode = uiState.isDarkMode,
                    currentLanguage = uiState.currentLanguage,
                    onToggleTheme = { viewModel.toggleTheme() },
                    onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
                    onOpenLegalDoc = { doc -> viewModel.navigateTo(ScreenDestination.Legal(doc)) },
                    onBack = { viewModel.navigateBack() },
                    onToast = { msg ->
                        if (msg.contains("Logged out", ignoreCase = true)) {
                            viewModel.signOut()
                        } else {
                            viewModel.showToast(msg)
                        }
                    }
                )
            }
            is ScreenDestination.AdminPanel -> {
                AdminPanelScreen(
                    reports = uiState.reports,
                    isDarkMode = uiState.isDarkMode,
                    onResolveReport = { id, action -> viewModel.resolveReport(id, action) },
                    onRemovePost = { postId -> viewModel.removePostAsAdmin(postId) },
                    onBanUser = { username -> viewModel.blockUser(username) },
                    onBack = { viewModel.navigateBack() }
                )
            }
            is ScreenDestination.Legal -> {
                LegalScreen(
                    docType = screen.docType,
                    isDarkMode = uiState.isDarkMode,
                    onBack = { viewModel.navigateBack() },
                    onToast = { msg -> viewModel.showToast(msg) }
                )
            }
            is ScreenDestination.GetBadge -> {
                val context = androidx.compose.ui.platform.LocalContext.current
                GetBadgeScreen(
                    user = uiState.currentUser ?: com.example.model.User(),
                    isDarkMode = uiState.isDarkMode,
                    currentLanguage = uiState.currentLanguage,
                    onBuyClicked = {
                        viewModel.launchBuyBadgeFlow(context as android.app.Activity)
                    },
                    onBack = { viewModel.navigateBack() },
                    onToast = { msg -> viewModel.showToast(msg) }
                )
            }
            is ScreenDestination.FollowList -> {
                FollowListScreen(
                    username = screen.username,
                    initialTab = screen.tab,
                    isDarkMode = uiState.isDarkMode,
                    myId = uiState.currentUser?.id ?: "",
                    loadFollowers = { viewModel.loadFollowers(screen.userId) },
                    loadFollowing = { viewModel.loadFollowing(screen.userId) },
                    isFollowing = { id -> viewModel.isFollowingUser(id) },
                    onFollowToggle = { u -> viewModel.toggleFollowById(u.id, u.username) },
                    onUserClick = { id -> viewModel.openUserProfile(id) },
                    onBack = { viewModel.navigateBack() }
                )
            }
            is ScreenDestination.UserProfile -> {
                val otherId = screen.userId
                val other by produceState<com.example.model.User?>(initialValue = null, otherId) {
                    value = viewModel.loadUserProfile(otherId)
                }
                var following by remember(otherId) { mutableStateOf(viewModel.isFollowingUser(otherId)) }
                val theyFollowMe by produceState(initialValue = false, otherId) { value = viewModel.followsMe(otherId) }
                var muted by remember(otherId) { mutableStateOf(false) }
                val shown = other
                if (shown == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    ProfileScreen(
                        user = shown,
                        posts = uiState.posts,
                        clips = uiState.clips,
                        isDarkMode = uiState.isDarkMode,
                        onOpenPost = { pid -> viewModel.openPostViewer(pid) },
                        onOpenClip = { c -> viewModel.openClipViewer(c) },
                        onOpenSettings = {},
                        onOpenCreatorStudio = {},
                        onOpenAdminPanel = {},
                        onUpdateProfile = { _, _, _ -> },
                        onToast = { msg -> viewModel.showToast(msg) },
                        isOwnProfile = false,
                        onOpenFollowList = { tab -> viewModel.openFollowList(shown.id, shown.username, tab) },
                        isFollowing = following,
                        followsYou = theyFollowMe,
                        isMuted = muted || viewModel.isMutedUser(shown.username),
                        onFollowClick = {
                            viewModel.toggleFollowById(otherId, shown.username)
                            following = viewModel.isFollowingUser(otherId)
                        },
                        onMessageClick = { viewModel.openChatWith(shown) },
                        onMuteClick = {
                            viewModel.toggleMuteUser(shown.username)
                            muted = viewModel.isMutedUser(shown.username)
                        },
                        onBlockClick = {
                            viewModel.blockUser(shown.username)
                            viewModel.navigateBack()
                        },
                        onBackClick = { viewModel.navigateBack() }
                    )
                }
            }
            is ScreenDestination.PostViewer -> {
                PostViewerScreen(
                    post = uiState.posts.firstOrNull { it.id == screen.postId },
                    isDarkMode = uiState.isDarkMode,
                    onBack = { viewModel.navigateBack() },
                    onLike = { viewModel.toggleLikePost(screen.postId) },
                    onSave = { viewModel.toggleSavePost(screen.postId) },
                    onComments = { viewModel.openComments(screen.postId) },
                    onUserClick = { uid -> viewModel.openUserProfile(uid) }
                )
            }
            is ScreenDestination.ClipViewer -> {
                val authorClips = if (screen.authorId.isBlank()) uiState.clips else uiState.clips.filter { it.author.id == screen.authorId }
                val startAt = authorClips.indexOfFirst { it.id == screen.clipId }.coerceAtLeast(0)
                ClipsScreen(
                    currentUserId = uiState.currentUser?.id ?: "",
                    onDeleteClip = { clipId -> viewModel.deleteMyClip(clipId); viewModel.navigateBack() },
                    clips = authorClips,
                    onLikeToggle = { clipId -> viewModel.toggleLikeClip(clipId) },
                    onSaveToggle = { clipId -> viewModel.toggleSaveClip(clipId) },
                    onFollowToggle = { username -> viewModel.toggleFollowCreator(username) },
                    onCommentsClick = { clipId -> viewModel.openComments(clipId) },
                    onUserClick = { uid -> viewModel.openUserProfile(uid) },
                    onShareClick = { clip -> shareClip = clip },
                    onToast = { msg -> viewModel.showToast(msg) },
                    onBack = { viewModel.navigateBack() },
                    onReport = { id, uname -> viewModel.openReportDialog("Clip", id, uname) },
                    initialIndex = startAt
                )
            }
            is ScreenDestination.EditProfile -> {
                EditProfileScreen(
                    user = uiState.currentUser ?: com.example.model.User(),
                    isDarkMode = uiState.isDarkMode,
                    currentLanguage = uiState.currentLanguage,
                    viewModel = viewModel,
                    onBack = { viewModel.navigateBack() },
                    onToast = { msg -> viewModel.showToast(msg) }
                )
            }
        }

        if (!com.example.util.NetworkMonitor.online) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Color(0xFFB91C1C))
                    .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Text("No internet connection", color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp)
            }
        }

        // --- Modals & Sheets ---

        // 1. Comments Bottom Sheet
        if (uiState.activeCommentsPostId != null) {
            val post = uiState.posts.firstOrNull { it.id == uiState.activeCommentsPostId }
                ?: uiState.clips.firstOrNull { it.id == uiState.activeCommentsPostId }?.let { c ->
                    com.example.model.Post(
                        id = c.id, author = c.author, mediaUrls = listOf(c.mediaUrl),
                        caption = c.caption, comments = c.comments, isVideo = true
                    )
                }
            if (post != null) {
                CommentsBottomSheet(
                    post = post,
                    currentUser = uiState.currentUser ?: com.example.model.User(),
                    isDarkMode = uiState.isDarkMode,
                    currentLanguage = uiState.currentLanguage,
                    onDismiss = { viewModel.closeComments() },
                    highlightCommentId = uiState.highlightCommentId,
                    loadAvatar = { id -> viewModel.loadUserAvatar(id) },
                    onAddComment = { pId, text, replyId -> viewModel.addComment(pId, text, replyId) },
                    onLikeComment = { cId -> viewModel.toggleLikeComment(post.id, cId) },
                    onPinComment = { cId -> viewModel.pinComment(post.id, cId) },
                    onDeleteComment = { cId -> viewModel.deleteComment(post.id, cId) },
                    onTranslateComment = { text, onResult ->
                        viewModel.requestTranslation(text, uiState.currentLanguage.displayName, onResult)
                    }
                )
            }
        }

        // Calls (full screen, above everything)
        val activeCall = uiState.activeCall
        val incomingCall = uiState.incomingCall
        if (activeCall != null) {
            CallScreen(
                call = activeCall,
                peer = uiState.callPeer,
                isCaller = activeCall.callerId == uiState.currentUser?.id,
                onEnd = { viewModel.endCall() },
                onToast = { msg -> viewModel.showToast(msg) }
            )
        } else if (incomingCall != null) {
            IncomingCallScreen(
                call = incomingCall,
                peer = uiState.callPeer,
                onAccept = { viewModel.acceptCall() },
                onDecline = { viewModel.declineCall() }
            )
        }

        // 1b. Reel share sheet (in-app, Instagram style)
        shareClip?.let { clipToShare ->
            val shareText = "Watch this reel by @${clipToShare.author.username} on VYNORA: ${clipToShare.mediaUrl}"
            ClipShareSheet(
                isDarkMode = uiState.isDarkMode,
                loadTargets = { viewModel.loadShareTargets() },
                search = { q -> viewModel.searchShareUsers(q) },
                onSend = { users ->
                    viewModel.shareClipToUsers(clipToShare, users)
                    shareClip = null
                },
                onCopyLink = {
                    try {
                        val cm = hostContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("VYNORA reel", clipToShare.mediaUrl))
                        viewModel.showToast("Link copied")
                    } catch (e: Exception) {
                        viewModel.showToast("Could not copy link")
                    }
                    shareClip = null
                },
                onWhatsApp = {
                    try {
                        val i = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            setPackage("com.whatsapp")
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        }
                        hostContext.startActivity(i)
                    } catch (e: Exception) {
                        viewModel.showToast("WhatsApp is not installed")
                    }
                    shareClip = null
                },
                onMore = {
                    try {
                        val i = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        }
                        hostContext.startActivity(android.content.Intent.createChooser(i, "Share reel"))
                    } catch (e: Exception) {
                        viewModel.showToast("Could not open share options")
                    }
                    shareClip = null
                },
                onDismiss = { shareClip = null }
            )
        }

        // 2. Share sheet for posts (Instagram style: search + people grid + quick actions)
        uiState.activeSharePost?.let { sharePost ->
            val postLink = "https://vynora.io/p/${sharePost.id}"
            val postShareText = "Check out this post by @${sharePost.author.username} on VYNORA: $postLink"
            ClipShareSheet(
                isDarkMode = uiState.isDarkMode,
                loadTargets = { viewModel.loadShareTargets() },
                search = { q -> viewModel.searchShareUsers(q) },
                onSend = { users ->
                    viewModel.sharePostToUsers(sharePost, users)
                    viewModel.closeShare()
                },
                onAddToStory = {
                    viewModel.addPostToStory(sharePost)
                    viewModel.closeShare()
                },
                onCopyLink = {
                    try {
                        val cm = hostContext.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("VYNORA post", postLink))
                        viewModel.showToast("Link copied")
                    } catch (e: Exception) {
                        viewModel.showToast("Could not copy link")
                    }
                    viewModel.closeShare()
                },
                onWhatsApp = {
                    try {
                        val i = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            setPackage("com.whatsapp")
                            putExtra(android.content.Intent.EXTRA_TEXT, postShareText)
                        }
                        hostContext.startActivity(i)
                    } catch (e: Exception) {
                        viewModel.showToast("WhatsApp is not installed")
                    }
                    viewModel.closeShare()
                },
                onMore = {
                    try {
                        val i = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, postShareText)
                        }
                        hostContext.startActivity(android.content.Intent.createChooser(i, "Share post"))
                    } catch (e: Exception) {
                        viewModel.showToast("Could not open share options")
                    }
                    viewModel.closeShare()
                },
                onDismiss = { viewModel.closeShare() }
            )
        }

        // 3. Report Dialog
        if (uiState.activeReportTarget != null) {
            val target = uiState.activeReportTarget!!
            ReportDialog(
                targetType = target.first,
                targetId = target.second,
                username = target.third,
                isDarkMode = uiState.isDarkMode,
                onDismiss = { viewModel.closeReportDialog() },
                onSubmit = { reason, details -> viewModel.submitReport(reason, details) }
            )
        }

        // --- Toast SnackBar Overlay ---
        AnimatedVisibility(
            visible = uiState.toastMessage != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 85.dp, start = 24.dp, end = 24.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (uiState.isDarkMode) Color(0xFF1E2235) else Color(0xFF0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, VynoraNeonCyan),
                tonalElevation = 10.dp
            ) {
                Text(
                    text = uiState.toastMessage ?: "",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
        }
    }
}
