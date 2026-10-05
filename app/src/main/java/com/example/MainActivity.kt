package com.example

import android.os.Bundle
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
    }
}

@Composable
fun VynoraApp(viewModel: VynoraViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Handle Android system back press
    BackHandler(enabled = uiState.currentScreen !is ScreenDestination.Splash) {
        viewModel.navigateBack()
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
                                        onLikeToggle = { postId -> viewModel.toggleLikePost(postId) },
                                        onSaveToggle = { postId -> viewModel.toggleSavePost(postId) },
                                        onCommentsClick = { postId -> viewModel.openComments(postId) },
                                        onShareClick = { post -> viewModel.openShare(post) },
                                        onReportClick = { type, id, username -> viewModel.openReportDialog(type, id, username) },
                                        onAuthorClick = { viewModel.selectTab(MainTab.PROFILE) }
                                    )
                                }
                                MainTab.EXPLORE -> {
                                    ExploreScreen(
                                        posts = uiState.posts,
                                        isDarkMode = uiState.isDarkMode,
                                        onUserClick = { viewModel.selectTab(MainTab.PROFILE) },
                                        onFollowToggle = { username -> viewModel.toggleFollowCreator(username) },
                                        onPostClick = { post -> viewModel.openComments(post.id) }
                                    )
                                }
                                MainTab.CREATE -> {
                                    CreateStudioScreen(
                                        isDarkMode = uiState.isDarkMode,
                                        currentLanguage = uiState.currentLanguage,
                                        onPublishPost = { caption, hashtags, mediaUrls, loc, filter ->
                                            viewModel.publishPost(caption, hashtags, mediaUrls, loc, filter)
                                        },
                                        onPublishMoment = { textOverlay, mediaUrl, music, isCloseFriends ->
                                            viewModel.publishMoment(textOverlay, mediaUrl, music, isCloseFriends)
                                            viewModel.selectTab(MainTab.HOME)
                                        },
                                        onPublishClip = { caption, mediaUrl, music ->
                                            viewModel.publishClip(caption, mediaUrl, music)
                                        },
                                        onRequestAiCaption = { topic, onResult ->
                                            viewModel.requestAiCaption(topic, onResult)
                                        },
                                        onRequestAiHashtags = { topic, onResult ->
                                            viewModel.requestAiHashtags(topic, onResult)
                                        },
                                        onToast = { msg -> viewModel.showToast(msg) }
                                    )
                                }
                                MainTab.CLIPS -> {
                                    ClipsScreen(
                                        clips = uiState.clips,
                                        onLikeToggle = { clipId -> viewModel.toggleLikeClip(clipId) },
                                        onSaveToggle = { clipId -> viewModel.toggleSaveClip(clipId) },
                                        onFollowToggle = { username -> viewModel.toggleFollowCreator(username) },
                                        onCommentsClick = { clipId ->
                                            val relatedPost = uiState.posts.firstOrNull()
                                            if (relatedPost != null) viewModel.openComments(relatedPost.id)
                                        },
                                        onShareClick = { clip ->
                                            viewModel.showToast("Clip shared: ${clip.musicTitle}")
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
                                        onToast = { msg -> viewModel.showToast(msg) }
                                    )
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
                    onToast = { msg -> viewModel.showToast(msg) }
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
                    onToast = { msg -> viewModel.showToast(msg) }
                )
            }
            is ScreenDestination.Notifications -> {
                NotificationsScreen(
                    notifications = uiState.notifications,
                    isDarkMode = uiState.isDarkMode,
                    onMarkAllAsRead = { viewModel.markAllNotificationsRead() },
                    onBack = { viewModel.navigateBack() },
                    onUserClick = { viewModel.selectTab(MainTab.PROFILE) }
                )
            }
            is ScreenDestination.LiveStudio -> {
                LiveStudioScreen(
                    onEndLive = {
                        viewModel.showToast("Live broadcast ended. Total viewers: 1,420")
                        viewModel.navigateBack()
                    },
                    onToast = { msg -> viewModel.showToast(msg) }
                )
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
                    onToast = { msg -> viewModel.showToast(msg) }
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

        // --- Modals & Sheets ---

        // 1. Comments Bottom Sheet
        if (uiState.activeCommentsPostId != null) {
            val post = uiState.posts.firstOrNull { it.id == uiState.activeCommentsPostId }
            if (post != null) {
                CommentsBottomSheet(
                    post = post,
                    currentUser = uiState.currentUser ?: com.example.model.User(),
                    isDarkMode = uiState.isDarkMode,
                    currentLanguage = uiState.currentLanguage,
                    onDismiss = { viewModel.closeComments() },
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

        // 2. Share Dialog
        if (uiState.activeSharePost != null) {
            ShareDialog(
                post = uiState.activeSharePost!!,
                isDarkMode = uiState.isDarkMode,
                onDismiss = { viewModel.closeShare() },
                onSendToUser = { username ->
                    val targetChatId = uiState.chats.firstOrNull { it.participant.username == username }?.id
                        ?: uiState.chats.firstOrNull()?.id
                        ?: "chat_${com.example.util.VynoraOfficial.OFFICIAL_USER_ID}"
                    viewModel.sendMessage(targetChatId, "Shared post: https://vynora.io/p/${uiState.activeSharePost!!.id}")
                },
                onToast = { msg -> viewModel.showToast(msg) }
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
