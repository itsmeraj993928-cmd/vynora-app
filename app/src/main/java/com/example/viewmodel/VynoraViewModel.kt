package com.example.viewmodel

import android.app.Activity
import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthResult
import com.example.auth.AuthService
import com.example.auth.FirebaseAuthService
import com.example.billing.BillingService
import com.example.data.VynoraRepository
import com.example.i18n.AppLanguage
import com.example.model.*
import com.example.service.GeminiService
import com.example.util.VynoraOfficial
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

sealed class ScreenDestination {
    object Splash : ScreenDestination()
    object Onboarding : ScreenDestination()
    object Auth : ScreenDestination()
    data class Main(val tab: MainTab = MainTab.HOME) : ScreenDestination()
    data class MomentsViewer(val initialMomentIndex: Int = 0) : ScreenDestination()
    object ChatsList : ScreenDestination()
    data class ChatRoom(val conversationId: String, val participant: User? = null) : ScreenDestination()
    object Notifications : ScreenDestination()
    object LiveStudio : ScreenDestination()
    object CreatorAnalytics : ScreenDestination()
    object Settings : ScreenDestination()
    object AdminPanel : ScreenDestination()
    object GetBadge : ScreenDestination()
    object EditProfile : ScreenDestination()
    data class Legal(val docType: String) : ScreenDestination()
    data class UserProfile(val userId: String) : ScreenDestination()
    data class FollowList(val userId: String, val username: String, val tab: Int) : ScreenDestination()
    data class PostViewer(val postId: String) : ScreenDestination()
    data class ClipViewer(val clipId: String, val authorId: String) : ScreenDestination()
}

enum class MainTab {
    HOME, EXPLORE, CREATE, CLIPS, PROFILE
}

data class VynoraUiState(
    val currentScreen: ScreenDestination = ScreenDestination.Auth,
    val selectedTab: MainTab = MainTab.HOME,
    val navigationStack: List<ScreenDestination> = emptyList(),
    val isDarkMode: Boolean = true,
    val currentLanguage: AppLanguage = AppLanguage.ENGLISH,
    val isLoggedIn: Boolean = false,
    val currentUser: User? = null,
    val posts: List<Post> = emptyList(),
    val moments: List<Moment> = emptyList(),
    val clips: List<Clip> = emptyList(),
    val chats: List<ChatConversation> = emptyList(),
    val notifications: List<NotificationItem> = emptyList(),
    val reports: List<ReportItem> = emptyList(),
    val badgeHolders: List<User> = emptyList(),
    val payments: List<PaymentRecord> = emptyList(),
    val creatorStats: CreatorStats = CreatorStats(),
    val activeCommentsPostId: String? = null,
    val activeCall: CallInfo? = null,
    val incomingCall: CallInfo? = null,
    val callPeer: User? = null,
    val highlightCommentId: String? = null,
    val activeSharePost: Post? = null,
    val activeReportTarget: Triple<String, String, String>? = null,
    val toastMessage: String? = null,
    val isAiGenerating: Boolean = false,
    val keywordFilterActive: Boolean = true,
    val offensiveKeywords: List<String> = listOf("scam", "cheat", "hate", "idiot", "fake", "abuse"),
    val deviceId: String = ""
)

private const val CALL_SERVER_BASE = "https://meet.jit.si"

class VynoraViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = VynoraRepository(application.applicationContext)
    val authService: AuthService = FirebaseAuthService(application.applicationContext)
    private val auth: FirebaseAuth = Firebase.auth

    private val _uiState = MutableStateFlow(
        VynoraUiState(
            deviceId = Settings.Secure.getString(application.contentResolver, Settings.Secure.ANDROID_ID) ?: "device_default"
        )
    )
    val uiState: StateFlow<VynoraUiState> = _uiState.asStateFlow()

    private var billingService: BillingService? = null

    init {
        // Initialize billing service safely
        try {
            billingService = BillingService(
                context = application.applicationContext,
                scope = viewModelScope,
                onPurchaseConfirmed = { orderId, token ->
                    onBadgePurchaseConfirmed(orderId, token)
                },
                onError = { error ->
                    showToast("Purchase error: $error")
                }
            )
        } catch (e: Exception) {
            billingService = null
        }

        // Keep the user logged in: if Firebase already has a signed-in user, restore the session
        val existingUser = try { auth.currentUser } catch (e: Exception) { null }
        if (existingUser == null) {
            _uiState.update { it.copy(currentScreen = ScreenDestination.Auth, isLoggedIn = false, currentUser = null) }
        } else {
            _uiState.update { it.copy(currentScreen = ScreenDestination.Splash) }
            viewModelScope.launch {
                try {
                    val profile = withTimeoutOrNull(15000L) {
                        repository.getUserProfile(existingUser.uid)
                            ?: repository.ensureUserProfile(existingUser, _uiState.value.deviceId)
                    }
                    if (profile != null) {
                        _uiState.update {
                            it.copy(
                                isLoggedIn = true,
                                currentUser = profile,
                                currentScreen = ScreenDestination.Main(MainTab.HOME)
                            )
                        }
                        startDataObservations(existingUser.uid)
                    } else {
                        _uiState.update { it.copy(currentScreen = ScreenDestination.Auth, isLoggedIn = false, currentUser = null) }
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(currentScreen = ScreenDestination.Auth, isLoggedIn = false, currentUser = null) }
                }
            }
        }
    }

    private var followingIds: Set<String> = emptySet()

    private fun isVisibleTo(visibility: String, authorId: String, viewerId: String): Boolean =
        when (visibility.uppercase()) {
            "PRIVATE" -> authorId == viewerId
            "FOLLOWERS" -> authorId == viewerId || authorId in followingIds
            else -> true
        }

    private var observeJob: kotlinx.coroutines.Job? = null
    private var likedIds: Set<String> = emptySet()
    private var likedClipIds: Set<String> = emptySet()

    private fun blockedPrefs() = getApplication<Application>().getSharedPreferences("vynora_blocked", android.content.Context.MODE_PRIVATE)
    private fun mutedPrefs() = getApplication<Application>().getSharedPreferences("vynora_muted", android.content.Context.MODE_PRIVATE)
    private fun mutedNames(): Set<String> = try { mutedPrefs().getStringSet("names", emptySet()) ?: emptySet() } catch (e: Exception) { emptySet() }
    private val pendingChatParticipants = HashMap<String, User>()
    private fun blockedNames(): Set<String> = try { blockedPrefs().getStringSet("names", emptySet()) ?: emptySet() } catch (e: Exception) { emptySet() }

    private fun startDataObservations(userId: String) {
        observeJob?.cancel()
        likedIds = emptySet()
        likedClipIds = emptySet()
        observeJob = viewModelScope.launch {
            followingIds = repository.getFollowingIds(userId)
            launch {
                repository.observePosts().collect { postsList ->
                    val blocked = blockedNames() + mutedNames()
                    val visible = postsList
                        .filter { isVisibleTo(it.visibility, it.author.id, userId) && it.author.username !in blocked }
                        .map {
                            it.copy(
                                isLiked = it.id in likedIds,
                                author = it.author.copy(isFollowing = it.author.id in followingIds),
                                comments = it.comments.sortedByDescending { c -> c.createdAt }
                            )
                        }
                    _uiState.update { it.copy(posts = visible) }
                }
            }
            launch {
                repository.observeLikedPostIds(userId).collect { ids ->
                    likedIds = ids
                    _uiState.update { s -> s.copy(posts = s.posts.map { p -> p.copy(isLiked = p.id in ids) }) }
                }
            }
            launch {
                repository.observeLikedClipIds(userId).collect { ids ->
                    likedClipIds = ids
                    _uiState.update { s -> s.copy(clips = s.clips.map { c -> c.copy(isLiked = c.id in ids) }) }
                }
            }
            launch {
                repository.observeClips().collect { clipsList ->
                    val blocked = blockedNames() + mutedNames()
                    val visible = clipsList
                        .filter { isVisibleTo(it.visibility, it.author.id, userId) && it.author.username !in blocked }
                        .map {
                            val f = it.author.id in followingIds
                            it.copy(isLiked = it.id in likedClipIds, isFollowing = f, author = it.author.copy(isFollowing = f))
                        }
                    _uiState.update { it.copy(clips = visible) }
                }
            }
            launch {
                repository.observeMoments().collect { momentsList ->
                    val visible = momentsList.filter { isVisibleTo(it.visibility, it.author.id, userId) }
                    _uiState.update { it.copy(moments = visible) }
                }
            }
            launch {
                repository.observeChats(userId).collect { chatsList ->
                    _uiState.update { it.copy(chats = chatsList) }
                }
            }
            launch {
                repository.observeIncomingCalls(userId).collect { call ->
                    val st = _uiState.value
                    if (call == null) {
                        if (st.incomingCall != null && st.activeCall == null) {
                            _uiState.update { it.copy(incomingCall = null, callPeer = null) }
                            incomingTimeoutJob?.cancel()
                            cancelCallNotification()
                        }
                        return@collect
                    }
                    if (st.activeCall != null) {
                        repository.updateCallStatus(call.id, "DECLINED") // already on a call
                        return@collect
                    }
                    if (st.incomingCall?.id == call.id) return@collect
                    val peer = try { repository.getUserProfile(call.callerId) } catch (e: Exception) { null }
                        ?: User(id = call.callerId, username = "Someone")
                    _uiState.update { it.copy(incomingCall = call, callPeer = peer.copy(id = call.callerId)) }
                    viewModelScope.launch { repository.markCallDelivered(call.id) }
                    showIncomingCallNotification(call, peer)
                    incomingTimeoutJob?.cancel()
                    incomingTimeoutJob = viewModelScope.launch {
                        delay(45_000)
                        if (_uiState.value.incomingCall?.id == call.id) {
                            repository.updateCallStatus(call.id, "MISSED")
                            _uiState.update { it.copy(incomingCall = null, callPeer = null) }
                            cancelCallNotification()
                        }
                    }
                }
            }
            launch {
                repository.observeNotifications(userId).collect { notifsList ->
                    _uiState.update { it.copy(notifications = notifsList) }
                }
            }
            launch {
                val st = _uiState.first { it.currentUser?.id == userId }
                st.currentUser?.let { rememberAccount(it, auth.currentUser?.email ?: it.email) }
            }
        }
    }

    // --- Pull to refresh ---
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    /** Pull down at the top of a list: reload feed, stories, follows and notifications. */
    fun refreshAll() {
        val uid = _uiState.value.currentUser?.id ?: return
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                startDataObservations(uid)
                // let the first results arrive so the spinner does not vanish before the list updates
                delay(1200)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // --- Saved accounts (switch account). Passwords are never stored. ---
    private val accountPrefs = application.getSharedPreferences("vynora_accounts", android.content.Context.MODE_PRIVATE)
    private val _savedAccounts = MutableStateFlow(loadSavedAccounts())
    val savedAccounts: StateFlow<List<SavedAccount>> = _savedAccounts.asStateFlow()
    var prefillIdentifier: String = ""

    private fun loadSavedAccounts(): List<SavedAccount> = try {
        val arr = org.json.JSONArray(accountPrefs.getString("list", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            SavedAccount(o.getString("id"), o.getString("username"), o.getString("identifier"), o.optString("avatarUrl", ""), o.optBoolean("isGoogle", false), o.optString("authEmail", ""))
        }
    } catch (e: Exception) { emptyList() }

    private fun rememberAccount(user: User, authEmail: String) {
        try {
            val identifier = if (authEmail.endsWith("@phone.vynora.app"))
                authEmail.substringBefore("@").substringBefore(".") else authEmail
            val avatar = if (user.avatarUrl.length < 300_000) user.avatarUrl else ""
            val isGoogle = auth.currentUser?.providerData?.any { it.providerId == "google.com" } == true &&
                auth.currentUser?.providerData?.none { it.providerId == "password" } == true
            val updated = (_savedAccounts.value.filterNot { it.id == user.id } +
                SavedAccount(user.id, user.username, identifier, avatar, isGoogle, authEmail)).takeLast(5)
            _savedAccounts.value = updated
            val arr = org.json.JSONArray()
            updated.forEach {
                arr.put(org.json.JSONObject().put("id", it.id).put("username", it.username)
                    .put("identifier", it.identifier).put("avatarUrl", it.avatarUrl).put("isGoogle", it.isGoogle).put("authEmail", it.authEmail))
            }
            accountPrefs.edit().putString("list", arr.toString()).apply()
        } catch (e: Exception) { }
    }

    /** Account whose password we must ask for (small dialog) because no saved login exists yet. */
    private val _switchPasswordFor = MutableStateFlow<SavedAccount?>(null)
    val switchPasswordFor: StateFlow<SavedAccount?> = _switchPasswordFor.asStateFlow()

    fun dismissSwitchPassword() { _switchPasswordFor.value = null }

    /**
     * One-tap switch. If a saved login exists we sign in directly (the current account stays
     * logged in until the new one succeeds). If not, we ask only for the password in a small
     * dialog instead of sending the user to the full Sign In screen.
     */
    fun switchAccount(accountId: String, identifier: String) {
        val acc = _savedAccounts.value.firstOrNull { it.id == accountId }
            ?: SavedAccount(id = accountId, identifier = identifier)
        val cred = com.example.util.CredentialVault.get(getApplication(), accountId)
        when {
            cred != null -> signInForSwitch(acc, cred.first, cred.second)
            acc.isGoogle -> {
                // Google accounts have no password: open the login screen so "Sign in with Google" can be used
                prefillIdentifier = identifier
                showToast("Use \"Sign in with Google\" for @${acc.username}")
                signOut()
            }
            else -> _switchPasswordFor.value = acc
        }
    }

    fun submitSwitchPassword(password: String) {
        val acc = _switchPasswordFor.value ?: return
        val loginId = acc.authEmail.ifBlank {
            val id = acc.identifier.trim()
            if (id.contains("@")) id else {
                // old saved phone accounts only kept the number: rebuild the hidden login email
                var d = id.filter { it.isDigit() }
                if (d.length == 10) d = "91$d"
                "$d@phone.vynora.app"
            }
        }
        signInForSwitch(acc, loginId, password)
    }

    private fun signInForSwitch(acc: SavedAccount, loginId: String, password: String) {
        signInWithEmail(
            email = loginId,
            pass = password,
            onSuccess = { _switchPasswordFor.value = null },
            onError = {
                // saved password is missing/outdated (or no internet): ask for it in the small dialog
                _switchPasswordFor.value = acc
                showToast("Could not log in to @${acc.username}. Check password and internet")
            }
        )
    }

    fun addAccount() {
        prefillIdentifier = ""
        signOut()
    }

    // Navigation
    fun navigateTo(destination: ScreenDestination) {
        _uiState.update { current ->
            current.copy(
                navigationStack = current.navigationStack + current.currentScreen,
                currentScreen = destination,
                selectedTab = if (destination is ScreenDestination.Main) destination.tab else current.selectedTab
            )
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(selectedTab = tab, currentScreen = ScreenDestination.Main(tab)) }
    }

    fun navigateBack(): Boolean {
        val stack = _uiState.value.navigationStack
        if (stack.isNotEmpty()) {
            val previous = stack.last()
            val newStack = stack.dropLast(1)
            _uiState.update { it.copy(currentScreen = previous, navigationStack = newStack) }
            return true
        } else if (_uiState.value.currentScreen !is ScreenDestination.Main || _uiState.value.selectedTab != MainTab.HOME) {
            _uiState.update { it.copy(currentScreen = ScreenDestination.Main(MainTab.HOME), selectedTab = MainTab.HOME) }
            return true
        }
        return false
    }

    // Language & Theme
    fun setLanguage(lang: AppLanguage) {
        _uiState.update { it.copy(currentLanguage = lang) }
        showToast("Language changed to ${lang.displayName}")
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun showToast(message: String) {
        _uiState.update { it.copy(toastMessage = message) }
        viewModelScope.launch {
            delay(2500)
            if (_uiState.value.toastMessage == message) {
                _uiState.update { it.copy(toastMessage = null) }
            }
        }
    }

    // --- Authentication & User Lifecycle (Google Sign-In & Email) ---

    fun signInWithGoogle(
        activity: Activity,
        onProfileSetupRequired: (FirebaseUser) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = authService.signInWithGoogle(activity)) {
                is AuthResult.Success -> {
                    val user = result.user
                    val profile = repository.getUserProfile(user.uid)
                    if (profile != null) {
                        _uiState.update {
                            it.copy(
                                isLoggedIn = true,
                                currentUser = profile,
                                currentScreen = ScreenDestination.Main(MainTab.HOME)
                            )
                        }
                        startDataObservations(user.uid)
                        showToast("Welcome back, @${profile.username}! ✨")
                        onSuccess()
                    } else {
                        // New user needs profile setup (choose unique username, referral code)
                        onProfileSetupRequired(user)
                    }
                }
                is AuthResult.Error -> {
                    onError(result.message)
                }
                is AuthResult.Cancelled -> {
                    // User dismissed prompt, no error to show
                }
            }
        }
    }

    fun handleGoogleSignInSuccess(
        idToken: String,
        onProfileSetupRequired: (FirebaseUser) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = authService.signInWithGoogleIdToken(idToken)) {
                is AuthResult.Success -> {
                    val user = result.user
                    val profile = repository.getUserProfile(user.uid)
                    if (profile != null) {
                        _uiState.update {
                            it.copy(
                                isLoggedIn = true,
                                currentUser = profile,
                                currentScreen = ScreenDestination.Main(MainTab.HOME)
                            )
                        }
                        startDataObservations(user.uid)
                        showToast("Welcome back, @${profile.username}! ✨")
                        onSuccess()
                    } else {
                        // New user needs profile setup (choose unique username, referral code)
                        onProfileSetupRequired(user)
                    }
                }
                is AuthResult.Error -> {
                    onError(result.message)
                }
                is AuthResult.Cancelled -> {
                    // No-op
                }
            }
        }
    }

    fun completeGoogleProfileSetup(
        username: String,
        fullName: String,
        referralCode: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = authService.currentUser
        if (user == null) {
            onError("Please authenticate with Google first")
            return
        }
        val cleanUsername = username.lowercase().trim()
        if (VynoraOfficial.isReservedUsername(cleanUsername)) {
            onError("Username '$username' is reserved by VYNORA.")
            return
        }

        viewModelScope.launch {
            try {
                val regResult = repository.registerUser(
                    uid = user.uid,
                    email = user.email ?: "",
                    username = cleanUsername,
                    fullName = fullName.trim().ifEmpty { user.displayName ?: cleanUsername },
                    deviceId = _uiState.value.deviceId,
                    referralCodeInput = referralCode
                )
                if (regResult.isSuccess) {
                    val profile = regResult.getOrThrow()
                    _uiState.update {
                        it.copy(
                            isLoggedIn = true,
                            currentUser = profile,
                            currentScreen = ScreenDestination.Main(MainTab.HOME)
                        )
                    }
                    startDataObservations(user.uid)
                    // If referred, validate referral immediately
                    if (!referralCode.isNullOrBlank()) {
                        repository.validateAndRecordReferral(
                            newUserId = profile.id,
                            email = profile.email,
                            deviceId = profile.deviceId,
                            referralCode = referralCode
                        )
                    }
                    showToast("Welcome to VYNORA, @${profile.username}! 🌟")
                    onSuccess()
                } else {
                    onError(regResult.exceptionOrNull()?.localizedMessage ?: "Registration failed")
                }
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Sign up failed")
            }
        }
    }

    fun signInWithEmail(
        email: String,
        pass: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (email.isBlank() || pass.isBlank()) {
            onError("Please enter both email and password")
            return
        }
        viewModelScope.launch {
            when (val result = authService.signInWithEmail(email, pass)) {
                is AuthResult.Success -> {
                    val user = result.user
                    val profile = repository.ensureUserProfile(user, _uiState.value.deviceId)
                    com.example.util.CredentialVault.save(getApplication(), user.uid, email.trim(), pass)
                    _uiState.update {
                        it.copy(
                            isLoggedIn = true,
                            currentUser = profile,
                            currentScreen = ScreenDestination.Main(MainTab.HOME)
                        )
                    }
                    startDataObservations(user.uid)
                    showToast("Welcome back, @${profile.username}! ✨")
                    onSuccess()
                }
                is AuthResult.Error -> {
                    onError(result.message)
                }
                is AuthResult.Cancelled -> Unit
            }
        }
    }

    fun signUpWithEmail(
        email: String,
        pass: String,
        username: String,
        fullName: String,
        referralCode: String?,
        gender: String = "",
        dob: String = "",
        phone: String = "",
        phoneVerified: Boolean = false,
        photoBase64: String = "",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        val cleanUsername = username.lowercase().trim()
        if (cleanEmail.isBlank() || pass.length < 6) {
            onError("Valid email and password (min 6 chars) required")
            return
        }
        if (cleanUsername.isBlank()) {
            onError("Please enter a username")
            return
        }
        if (VynoraOfficial.isReservedUsername(cleanUsername)) {
            onError("Username '$username' is reserved by VYNORA.")
            return
        }

        viewModelScope.launch {
            when (val result = authService.signUpWithEmail(cleanEmail, pass)) {
                is AuthResult.Success -> {
                    val user = result.user
                    val regResult = repository.registerUser(
                        uid = user.uid,
                        email = cleanEmail,
                        username = cleanUsername,
                        fullName = fullName.trim().ifEmpty { cleanUsername },
                        deviceId = _uiState.value.deviceId,
                        referralCodeInput = referralCode,
                        gender = gender,
                        dob = dob,
                        phone = phone,
                        phoneVerified = phoneVerified,
                        photoBase64 = photoBase64
                    )
                    if (regResult.isSuccess) {
                        val profile = regResult.getOrThrow()
                        com.example.util.CredentialVault.save(getApplication(), user.uid, cleanEmail, pass)
                        _uiState.update {
                            it.copy(
                                isLoggedIn = true,
                                currentUser = profile,
                                currentScreen = ScreenDestination.Main(MainTab.HOME)
                            )
                        }
                        startDataObservations(user.uid)
                        if (!referralCode.isNullOrBlank()) {
                            repository.validateAndRecordReferral(
                                newUserId = profile.id,
                                email = profile.email,
                                deviceId = profile.deviceId,
                                referralCode = referralCode
                            )
                        }
                        showToast("Welcome to VYNORA! Account created ✨")
                        onSuccess()
                    } else {
                        onError(regResult.exceptionOrNull()?.localizedMessage ?: "Registration failed")
                    }
                }
                is AuthResult.Error -> {
                    onError(result.message)
                }
                is AuthResult.Cancelled -> Unit
            }
        }
    }

    suspend fun checkUsernameAvailability(username: String): Boolean {
        return repository.isUsernameAvailable(username, _uiState.value.currentUser?.id)
    }

    suspend fun checkPhoneAvailability(phone: String): Boolean {
        return repository.isPhoneAvailable(phone, _uiState.value.currentUser?.id)
    }

    fun saveFullUserProfile(
        updatedUser: User,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val oldUser = _uiState.value.currentUser
            val result = repository.updateUserProfile(updatedUser, oldUser)
            if (result.isSuccess) {
                val savedUser = result.getOrThrow()
                _uiState.update { it.copy(currentUser = savedUser) }
                showToast("Profile updated successfully ✨")
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.localizedMessage ?: "Failed to save profile")
            }
        }
    }

    fun signOut() {
        observeJob?.cancel()
        likedIds = emptySet()
        authService.signOut()
        _uiState.update {
            it.copy(
                isLoggedIn = false,
                currentUser = null,
                currentScreen = ScreenDestination.Auth,
                navigationStack = emptyList(),
                posts = emptyList(),
                moments = emptyList(),
                clips = emptyList(),
                chats = emptyList(),
                notifications = emptyList()
            )
        }
    }

    fun sendPasswordReset(
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            onError("Please enter your email address")
            return
        }
        viewModelScope.launch {
            val result = authService.sendPasswordReset(cleanEmail)
            if (result.isSuccess) {
                onSuccess()
            } else {
                val err = result.exceptionOrNull()?.localizedMessage ?: "Failed to send reset link"
                val friendlyError = when {
                    err.contains("user-not-found", ignoreCase = true) || err.contains("no user record", ignoreCase = true) ->
                        "No account found with this email"
                    err.contains("invalid-email", ignoreCase = true) || err.contains("badly formatted", ignoreCase = true) ->
                        "Invalid email address"
                    err.contains("too-many-requests", ignoreCase = true) ->
                        "Too many requests. Please wait a moment and try again"
                    else -> err
                }
                onError(friendlyError)
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            authService.sendEmailVerification()
                .onSuccess {
                    showToast("Verification email sent! ✉️")
                }
                .onFailure {
                    showToast("Failed to send verification email: ${it.localizedMessage}")
                }
        }
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            val isVerified = authService.reloadUser().getOrDefault(false)
            _uiState.update { state ->
                val updatedUser = state.currentUser?.copy(isEmailVerified = isVerified)
                state.copy(currentUser = updatedUser)
            }
            if (isVerified) {
                showToast("Email successfully verified! ✨")
                // Check if this user was referred by someone and validate referral
                val currentUser = _uiState.value.currentUser
                if (currentUser != null && !currentUser.referredBy.isNullOrBlank()) {
                    repository.validateAndRecordReferral(
                        newUserId = currentUser.id,
                        email = currentUser.email,
                        deviceId = currentUser.deviceId,
                        referralCode = currentUser.referredBy
                    )
                }
            } else {
                showToast("Email not verified yet. Please check your inbox.")
            }
        }
    }

    fun logout() {
        observeJob?.cancel()
        likedIds = emptySet()
        auth.signOut()
        _uiState.update {
            it.copy(
                isLoggedIn = false,
                currentUser = null,
                currentScreen = ScreenDestination.Auth,
                posts = emptyList(),
                clips = emptyList(),
                moments = emptyList(),
                chats = emptyList(),
                notifications = emptyList()
            )
        }
        showToast("Logged out")
    }

    // --- VYNORA Plus & Google Play Billing ---

    fun launchBuyBadgeFlow(activity: android.app.Activity) {
        val user = _uiState.value.currentUser ?: return
        if (!user.isEmailVerified && !user.isOfficial) {
            showToast("Please verify your email before buying the VYNORA Plus badge.")
            return
        }
        billingService?.launchBillingFlow(activity)
    }

    private fun onBadgePurchaseConfirmed(orderId: String, token: String) {
        val user = _uiState.value.currentUser ?: return
        viewModelScope.launch {
            val result = repository.activatePaidBadge(user.id, orderId, token)
            if (result.isSuccess) {
                val updated = result.getOrThrow()
                _uiState.update { it.copy(currentUser = updated) }
                showToast("VYNORA Plus activated for 30 days! ⭐")
            } else {
                showToast("Badge activation failed: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    // --- Posts & Feed Actions ---

    fun toggleLikePost(postId: String) {
        val me = _uiState.value.currentUser ?: return
        val post = _uiState.value.posts.firstOrNull { it.id == postId } ?: return
        val newLiked = !post.isLiked
        _uiState.update { state ->
            state.copy(posts = state.posts.map { p ->
                if (p.id == postId) p.copy(
                    isLiked = newLiked,
                    likesCount = if (newLiked) p.likesCount + 1 else maxOf(0, p.likesCount - 1)
                ) else p
            })
        }
        likedIds = if (newLiked) likedIds + postId else likedIds - postId
        viewModelScope.launch {
            repository.setPostLike(postId, me, post.author.id, newLiked, post.caption.take(60))
        }
    }

    fun toggleSavePost(postId: String) {
        _uiState.update { state ->
            val updated = state.posts.map { post ->
                if (post.id == postId) post.copy(isSaved = !post.isSaved) else post
            }
            val wasSaved = updated.firstOrNull { it.id == postId }?.isSaved == true
            showToast(if (wasSaved) "Saved to your bookmarks ✨" else "Removed from saved")
            state.copy(posts = updated)
        }
    }

    fun openComments(postId: String, highlightCommentId: String? = null) {
        _uiState.update { it.copy(activeCommentsPostId = postId, highlightCommentId = highlightCommentId) }
    }

    fun closeComments() {
        _uiState.update { it.copy(activeCommentsPostId = null, highlightCommentId = null) }
    }

    private fun updateClipComments(clipId: String, transform: (List<Comment>) -> List<Comment>): List<Comment>? {
        var result: List<Comment>? = null
        _uiState.update { state ->
            state.copy(clips = state.clips.map { c ->
                if (c.id == clipId) {
                    val list = transform(c.comments)
                    result = list
                    c.copy(comments = list, commentsCount = list.size)
                } else c
            })
        }
        return result
    }

    fun openClipViewer(clip: Clip) {
        navigateTo(ScreenDestination.ClipViewer(clip.id, clip.author.id))
    }

    fun openPostViewer(postId: String) {
        navigateTo(ScreenDestination.PostViewer(postId))
    }

    fun addComment(postId: String, text: String, replyToId: String? = null) {
        if (text.isBlank()) return
        val user = _uiState.value.currentUser ?: return

        // Keyword filter check
        if (_uiState.value.keywordFilterActive) {
            val containsOffensive = _uiState.value.offensiveKeywords.any { text.contains(it, ignoreCase = true) }
            if (containsOffensive) {
                showToast("Comment hidden: contains restricted keywords (Safety filter)")
                return
            }
        }

        val newComment = Comment(
            id = "c_${System.currentTimeMillis()}",
            postId = postId,
            author = user,
            text = text.trim(),
            timestamp = "Just now",
            likesCount = 0,
            isLiked = false
        )

        val targetClip = _uiState.value.clips.firstOrNull { it.id == postId }
        if (targetClip != null) {
            updateClipComments(postId) { listOf(newComment) + it }
            viewModelScope.launch { repository.saveClipComment(postId, newComment, targetClip.author.id) }
            return
        }

        _uiState.update { state ->
            val updated = state.posts.map { post ->
                if (post.id == postId) {
                    post.copy(comments = listOf(newComment) + post.comments)
                } else post
            }
            state.copy(posts = updated)
        }
        val postAuthorId = _uiState.value.posts.firstOrNull { it.id == postId }?.author?.id ?: ""
        viewModelScope.launch { repository.saveComment(postId, newComment, postAuthorId) }
    }

    fun toggleLikeComment(postId: String, commentId: String) {
        if (_uiState.value.clips.any { it.id == postId }) {
            updateClipComments(postId) { list ->
                list.map { c ->
                    if (c.id == commentId) {
                        val liked = !c.isLiked
                        c.copy(isLiked = liked, likesCount = if (liked) c.likesCount + 1 else maxOf(0, c.likesCount - 1))
                    } else c
                }
            }
            return
        }
        _uiState.update { state ->
            val updated = state.posts.map { post ->
                if (post.id == postId) {
                    val updatedComments = post.comments.map { comment ->
                        if (comment.id == commentId) {
                            val liked = !comment.isLiked
                            comment.copy(
                                isLiked = liked,
                                likesCount = if (liked) comment.likesCount + 1 else maxOf(0, comment.likesCount - 1)
                            )
                        } else comment
                    }
                    post.copy(comments = updatedComments)
                } else post
            }
            state.copy(posts = updated)
        }
    }

    fun pinComment(postId: String, commentId: String) {
        if (_uiState.value.clips.any { it.id == postId }) {
            updateClipComments(postId) { list ->
                list.map { c -> if (c.id == commentId) c.copy(isPinned = !c.isPinned) else c.copy(isPinned = false) }
                    .sortedByDescending { it.isPinned }
            }
            return
        }
        _uiState.update { state ->
            val updated = state.posts.map { post ->
                if (post.id == postId) {
                    val updatedComments = post.comments.map { comment ->
                        if (comment.id == commentId) comment.copy(isPinned = !comment.isPinned)
                        else comment.copy(isPinned = false)
                    }
                    post.copy(comments = updatedComments.sortedByDescending { it.isPinned })
                } else post
            }
            state.copy(posts = updated)
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        if (_uiState.value.clips.any { it.id == postId }) {
            val left = updateClipComments(postId) { list -> list.filterNot { it.id == commentId } }
            if (left != null) viewModelScope.launch { repository.setClipComments(postId, left) }
            showToast("Comment deleted")
            return
        }
        _uiState.update { state ->
            val updated = state.posts.map { post ->
                if (post.id == postId) post.copy(comments = post.comments.filterNot { it.id == commentId })
                else post
            }
            state.copy(posts = updated)
        }
        showToast("Comment deleted")
    }

    // Share & Report
    fun openShare(post: Post) {
        _uiState.update { it.copy(activeSharePost = post) }
    }

    fun closeShare() {
        _uiState.update { it.copy(activeSharePost = null) }
    }

    fun openReportDialog(targetType: String, targetId: String, username: String) {
        // Rule: Official account cannot be reported
        if (VynoraOfficial.isOfficialAccount(null, username)) {
            showToast("The official VYNORA account cannot be reported.")
            return
        }
        _uiState.update { it.copy(activeReportTarget = Triple(targetType, targetId, username)) }
    }

    fun closeReportDialog() {
        _uiState.update { it.copy(activeReportTarget = null) }
    }

    fun submitReport(reason: String, details: String) {
        val target = _uiState.value.activeReportTarget ?: return
        val user = _uiState.value.currentUser ?: return
        val newReport = ReportItem(
            id = "rep_${System.currentTimeMillis()}",
            targetType = target.first,
            targetId = target.second,
            reportedUsername = target.third,
            reporterUsername = user.username,
            reason = reason,
            details = details,
            timestamp = "Just now",
            status = "PENDING"
        )
        _uiState.update { state ->
            state.copy(
                reports = listOf(newReport) + state.reports,
                activeReportTarget = null
            )
        }
        viewModelScope.launch { repository.saveReport(newReport) }
        showToast("Report submitted to moderation team. Thank you for keeping VYNORA safe.")
    }

    /**
     * People you can send things to: recent chats first, then mutuals (follow + follow back),
     * then people you follow, then your followers.
     */
    suspend fun loadShareTargets(): List<User> {
        val me = _uiState.value.currentUser ?: return emptyList()
        val chatIds = _uiState.value.chats.filter { !it.isGroup }.map { it.participant.id }
        val following = try { repository.getFollowingIds(me.id) } catch (e: Exception) { emptySet() }
        val followers = try { repository.getFollowerIds(me.id) } catch (e: Exception) { emptyList() }
        val mutuals = followers.filter { it in following }
        val ids = (chatIds + mutuals + following.toList() + followers)
            .filter { it.isNotBlank() && it != me.id }.distinct().take(100)
        return try { repository.getUsersByIds(ids).filter { it.username.isNotBlank() } } catch (e: Exception) { emptyList() }
    }

    /** Search any account by username (for the share sheet search box). */
    suspend fun searchShareUsers(query: String): List<User> {
        val me = _uiState.value.currentUser?.id
        return repository.searchUsersByPrefix(query).filter { it.id != me }
    }

    private fun sendToUsers(targets: List<User>, text: String, doneLabel: String) {
        val me = _uiState.value.currentUser ?: return
        var sent = 0
        for (target in targets) {
            if (target.id.isBlank() || target.id == me.id) continue
            val existing = _uiState.value.chats.firstOrNull { !it.isGroup && it.participant.id == target.id }
            val convId = existing?.id ?: if (me.id < target.id) "${me.id}_${target.id}" else "${target.id}_${me.id}"
            pendingChatParticipants[convId] = target
            sendMessage(convId, text)
            sent++
        }
        if (sent > 0) showToast(if (sent == 1) "$doneLabel sent" else "$doneLabel sent to $sent people")
    }

    fun sharePostToUsers(post: Post, targets: List<User>) {
        sendToUsers(targets, "Shared post: https://vynora.io/p/${post.id}", "Post")
    }

    /** Re-share a photo post to my own story. */
    fun addPostToStory(post: Post) {
        val url = post.mediaUrls.firstOrNull()
        if (url.isNullOrBlank() || post.isVideo) {
            showToast("Only photo posts can be added to your story")
            return
        }
        publishMoment("", url, null, false)
    }

    fun shareClipToUsers(clip: Clip, targets: List<User>) {
        val me = _uiState.value.currentUser ?: return
        var sent = 0
        for (target in targets) {
            if (target.id.isBlank() || target.id == me.id) continue
            val existing = _uiState.value.chats.firstOrNull { !it.isGroup && it.participant.id == target.id }
            val convId = existing?.id ?: if (me.id < target.id) "${me.id}_${target.id}" else "${target.id}_${me.id}"
            pendingChatParticipants[convId] = target
            sendMessage(convId, "Shared a reel by @${clip.author.username}: ${clip.mediaUrl}")
            sent++
        }
        if (sent > 0) showToast(if (sent == 1) "Reel sent" else "Reel sent to $sent people")
    }

    fun sendVoiceNote(conversationId: String, file: java.io.File, duration: String) {
        showToast("Sending voice note…")
        viewModelScope.launch {
            val res = com.example.util.CloudinaryUploader.uploadUri(
                context = getApplication<Application>(),
                uri = android.net.Uri.fromFile(file)
            )
            try { file.delete() } catch (e: Exception) { }
            val url = res.getOrNull()?.secureUrl
            if (res.isSuccess && !url.isNullOrBlank()) {
                sendMessage(conversationId, url, true, duration)
            } else {
                showToast("Voice note could not be sent. Please try again.")
            }
        }
    }

    /** Creates a call room, posts the invite into the chat and returns the room link. */
    fun startCall(conversationId: String, video: Boolean): String {
        val room = "vynora" + conversationId.filter { it.isLetterOrDigit() }.takeLast(12) +
            java.lang.Long.toString(System.currentTimeMillis(), 36)
        val link = "$CALL_SERVER_BASE/$room" + if (video) "" else "#config.startWithVideoMuted=true"
        sendMessage(conversationId, (if (video) "Started a video call: " else "Started a voice call: ") + link)
        return link
    }

    // ---- In-app voice / video calls (Agora for media, Firestore "calls" for ringing) ----
    private var callWatchJob: kotlinx.coroutines.Job? = null
    private var incomingTimeoutJob: kotlinx.coroutines.Job? = null
    private var callAnsweredAt = 0L
    private val loggedCallIds = HashSet<String>()

    /** The caller's phone writes one line into the chat: duration, missed, declined... */
    private fun logCall(call: CallInfo, finalStatus: String) {
        val me = _uiState.value.currentUser?.id
        if (call.callerId != me || call.conversationId.isBlank()) return
        if (!loggedCallIds.add(call.id)) return
        val secs = if (finalStatus == "ENDED" && callAnsweredAt > 0L)
            ((System.currentTimeMillis() - callAnsweredAt) / 1000L).toInt() else 0
        sendMessage(call.conversationId, "[call]${if (call.video) "video" else "voice"}|$finalStatus|$secs")
    }

    private fun cancelCallNotification() {
        try {
            getApplication<Application>().getSystemService(android.app.NotificationManager::class.java).cancel(4242)
        } catch (e: Exception) { }
    }

    /** When the app is in the background (but still running), show a heads-up "incoming call" notification. */
    private fun showIncomingCallNotification(call: CallInfo, peer: User) {
        if (AppForeground.inForeground) return
        val app = getApplication<Application>()
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33 &&
                androidx.core.content.ContextCompat.checkSelfPermission(app, android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) return
            val nm = app.getSystemService(android.app.NotificationManager::class.java)
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                val ch = android.app.NotificationChannel("calls_v1", "Calls", android.app.NotificationManager.IMPORTANCE_HIGH)
                ch.setSound(
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE),
                    android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                ch.enableVibration(true)
                nm.createNotificationChannel(ch)
            }
            val launch = app.packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
            } ?: return
            val pi = android.app.PendingIntent.getActivity(
                app, 0, launch,
                android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
            val name = peer.fullName.ifBlank { peer.username }
            val n = androidx.core.app.NotificationCompat.Builder(app, "calls_v1")
                .setSmallIcon(android.R.drawable.sym_call_incoming)
                .setContentTitle(name)
                .setContentText(if (call.video) "Incoming video call" else "Incoming voice call")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setTimeoutAfter(45_000)
                .setContentIntent(pi)
                .setFullScreenIntent(pi, true)
                .build()
            nm.notify(4242, n)
        } catch (e: Exception) { }
    }
    private var ringTimeoutJob: kotlinx.coroutines.Job? = null

    private fun clearCallLocally() {
        callAnsweredAt = 0L
        incomingTimeoutJob?.cancel(); incomingTimeoutJob = null
        cancelCallNotification()
        callWatchJob?.cancel(); callWatchJob = null
        ringTimeoutJob?.cancel(); ringTimeoutJob = null
        _uiState.update { it.copy(activeCall = null, incomingCall = null, callPeer = null) }
    }

    private fun watchCall(callId: String) {
        callWatchJob?.cancel()
        callWatchJob = viewModelScope.launch {
            repository.observeCall(callId).collect { c ->
                if (c == null) return@collect
                val me = _uiState.value.currentUser?.id
                if (c.status in listOf("ENDED", "DECLINED", "CANCELED", "MISSED")) {
                    logCall(c, c.status)
                    clearCallLocally()
                } else {
                    if (c.status == "ACCEPTED" && callAnsweredAt == 0L) callAnsweredAt = System.currentTimeMillis()
                    _uiState.update { st -> if (st.activeCall?.id == c.id) st.copy(activeCall = c) else st }
                }
            }
        }
    }

    fun placeCall(peer: User, video: Boolean, conversationId: String = "") {
        val me = _uiState.value.currentUser ?: return
        if (peer.id.isBlank() || peer.id == me.id) return
        if (_uiState.value.activeCall != null || _uiState.value.incomingCall != null) {
            showToast("You are already in a call")
            return
        }
        val now = System.currentTimeMillis()
        val call = CallInfo(
            id = "call_${now}_${me.id.takeLast(4)}",
            callerId = me.id,
            calleeId = peer.id,
            video = video,
            status = "RINGING",
            channel = "call_${now}_${me.id.takeLast(4)}",
            conversationId = conversationId,
            createdAt = now
        )
        if (conversationId.isNotBlank()) pendingChatParticipants[conversationId] = peer
        _uiState.update { it.copy(activeCall = call, callPeer = peer) }
        viewModelScope.launch { repository.createCall(call) }
        watchCall(call.id)
        ringTimeoutJob?.cancel()
        ringTimeoutJob = viewModelScope.launch {
            delay(45_000)
            val cur = _uiState.value.activeCall
            if (cur?.id == call.id && cur.status == "RINGING") {
                repository.updateCallStatus(call.id, "MISSED")
                logCall(cur, "MISSED")
                clearCallLocally()
                showToast("No answer")
            }
        }
    }

    fun acceptCall() {
        val c = _uiState.value.incomingCall ?: return
        incomingTimeoutJob?.cancel(); cancelCallNotification()
        _uiState.update { it.copy(activeCall = c.copy(status = "ACCEPTED"), incomingCall = null) }
        viewModelScope.launch { repository.updateCallStatus(c.id, "ACCEPTED") }
        watchCall(c.id)
    }

    fun declineCall() {
        val c = _uiState.value.incomingCall ?: return
        incomingTimeoutJob?.cancel(); cancelCallNotification()
        _uiState.update { it.copy(incomingCall = null, callPeer = null) }
        viewModelScope.launch { repository.updateCallStatus(c.id, "DECLINED") }
    }

    fun endCall() {
        val c = _uiState.value.activeCall ?: return
        val me = _uiState.value.currentUser?.id
        val status = if (c.status == "RINGING" && c.callerId == me) "CANCELED" else "ENDED"
        logCall(c, status)
        clearCallLocally()
        viewModelScope.launch { repository.updateCallStatus(c.id, status) }
    }

    fun isMutedUser(username: String): Boolean = username in mutedNames()

    fun toggleMuteUser(username: String) {
        val names = mutedNames()
        val nowMuted = username !in names
        try {
            mutedPrefs().edit().putStringSet("names", if (nowMuted) names + username else names - username).apply()
        } catch (e: Exception) { }
        if (nowMuted) {
            _uiState.update { state ->
                state.copy(
                    posts = state.posts.filterNot { it.author.username == username },
                    clips = state.clips.filterNot { it.author.username == username }
                )
            }
        }
        showToast(if (nowMuted) "Muted @$username. Their posts and reels are hidden from your feed." else "Unmuted @$username. Their posts will show again after you reopen the app.")
    }

    suspend fun followsMe(userId: String): Boolean {
        val me = _uiState.value.currentUser ?: return false
        return try { userId in repository.getFollowerIds(me.id) } catch (e: Exception) { false }
    }

    fun openChatWith(target: User) {
        val me = _uiState.value.currentUser ?: return
        if (target.id.isBlank() || target.id == me.id) return
        val existing = _uiState.value.chats.firstOrNull { it.participant.id == target.id }
        val convId = existing?.id ?: if (me.id < target.id) "${me.id}_${target.id}" else "${target.id}_${me.id}"
        pendingChatParticipants[convId] = target
        navigateTo(ScreenDestination.ChatRoom(convId, target))
    }

    fun blockUser(username: String) {
        if (VynoraOfficial.isOfficialAccount(null, username)) {
            showToast("The official VYNORA account cannot be blocked.")
            return
        }
        try {
            val names = blockedNames() + username
            blockedPrefs().edit().putStringSet("names", names).apply()
        } catch (e: Exception) { }
        _uiState.update { state ->
            val filteredPosts = state.posts.filterNot { it.author.username == username }
            val filteredClips = state.clips.filterNot { it.author.username == username }
            showToast("Blocked @$username")
            state.copy(posts = filteredPosts, clips = filteredClips)
        }
    }

    // Clips interactions
    fun toggleLikeClip(clipId: String) {
        val me = _uiState.value.currentUser ?: return
        val clip = _uiState.value.clips.firstOrNull { it.id == clipId } ?: return
        val liked = !clip.isLiked
        _uiState.update { state ->
            state.copy(clips = state.clips.map { c ->
                if (c.id == clipId) c.copy(
                    isLiked = liked,
                    likesCount = if (liked) c.likesCount + 1 else maxOf(0, c.likesCount - 1)
                ) else c
            })
        }
        likedClipIds = if (liked) likedClipIds + clipId else likedClipIds - clipId
        viewModelScope.launch { repository.setClipLike(clipId, me, clip.author.id, liked, clip.caption.take(60)) }
    }

    fun toggleSaveClip(clipId: String) {
        _uiState.update { state ->
            val updated = state.clips.map { clip ->
                if (clip.id == clipId) clip.copy(isSaved = !clip.isSaved) else clip
            }
            state.copy(clips = updated)
        }
        showToast("Clip saved to collection")
    }

    // ---- Other people's profiles ----
    fun openUserProfile(userId: String) {
        val me = _uiState.value.currentUser
        if (userId.isBlank() || userId == me?.id) {
            selectTab(MainTab.PROFILE)
        } else {
            navigateTo(ScreenDestination.UserProfile(userId))
        }
    }

    fun openUserProfileByUsername(username: String) {
        val st = _uiState.value
        val id = st.posts.firstOrNull { it.author.username == username }?.author?.id
            ?: st.clips.firstOrNull { it.author.username == username }?.author?.id
        if (id == null) selectTab(MainTab.PROFILE) else openUserProfile(id)
    }

    suspend fun loadUserProfile(userId: String): User? {
        val fromDb = try { repository.getUserProfile(userId) } catch (e: Exception) { null }
        if (fromDb != null) return fromDb.copy(id = userId)
        val st = _uiState.value
        return st.posts.firstOrNull { it.author.id == userId }?.author
            ?: st.clips.firstOrNull { it.author.id == userId }?.author
    }

    fun isFollowingUser(userId: String): Boolean = userId in followingIds

    fun openFollowList(userId: String, username: String, tab: Int) {
        navigateTo(ScreenDestination.FollowList(userId, username, tab))
    }

    suspend fun loadFollowers(userId: String): List<User> =
        repository.getUsersByIds(repository.getFollowerIds(userId))

    suspend fun loadFollowing(userId: String): List<User> =
        repository.getUsersByIds(repository.getFollowingIds(userId).toList())

    fun toggleFollowById(targetId: String, username: String) {
        val me = _uiState.value.currentUser ?: return
        if (targetId.isBlank() || targetId == me.id) return
        val nowFollowing = targetId !in followingIds
        followingIds = if (nowFollowing) followingIds + targetId else followingIds - targetId
        _uiState.update { state ->
            val d = if (nowFollowing) 1 else -1
            state.copy(
                posts = state.posts.map { p -> if (p.author.id == targetId) p.copy(author = p.author.copy(isFollowing = nowFollowing)) else p },
                clips = state.clips.map { c -> if (c.author.id == targetId) c.copy(isFollowing = nowFollowing, author = c.author.copy(isFollowing = nowFollowing)) else c },
                currentUser = state.currentUser?.let { cu -> cu.copy(followingCount = maxOf(0, cu.followingCount + d)) }
            )
        }
        showToast(if (nowFollowing) "Following @$username" else "Unfollowed @$username")
        viewModelScope.launch { repository.setFollow(me, targetId, nowFollowing) }
    }

    fun toggleFollowCreator(username: String) {
        // Rule: Official account cannot be unfollowed
        val me = _uiState.value.currentUser ?: return
        val targetId = _uiState.value.posts.firstOrNull { it.author.username == username }?.author?.id
            ?: _uiState.value.clips.firstOrNull { it.author.username == username }?.author?.id
            ?: return
        if (targetId == me.id) return
        val nowFollowing = targetId !in followingIds
        followingIds = if (nowFollowing) followingIds + targetId else followingIds - targetId

        _uiState.update { state ->
            val updatedPosts = state.posts.map { post ->
                if (post.author.id == targetId) post.copy(author = post.author.copy(isFollowing = nowFollowing)) else post
            }
            val updatedClips = state.clips.map { clip ->
                if (clip.author.id == targetId) clip.copy(isFollowing = nowFollowing, author = clip.author.copy(isFollowing = nowFollowing)) else clip
            }
            val d = if (nowFollowing) 1 else -1
            state.copy(
                posts = updatedPosts,
                clips = updatedClips,
                currentUser = state.currentUser?.let { cu -> cu.copy(followingCount = maxOf(0, cu.followingCount + d)) }
            )
        }
        showToast(if (nowFollowing) "Following @$username" else "Unfollowed @$username")
        viewModelScope.launch { repository.setFollow(me, targetId, nowFollowing) }
    }

    // Moments
    // ---- Background upload (continues while the user browses the app) ----
    data class UploadStatus(
        val label: String,
        val progress: Int = 0,
        val sizeMb: Float = 0f,
        val failed: Boolean = false,
        val message: String = ""
    )

    private val _uploadStatus = MutableStateFlow<UploadStatus?>(null)
    val uploadStatus: StateFlow<UploadStatus?> = _uploadStatus.asStateFlow()
    private var lastUploadJob: (() -> Unit)? = null

    fun dismissUpload() {
        _uploadStatus.value = null
        lastUploadJob = null
    }

    fun retryUpload() { lastUploadJob?.invoke() }

    fun startBackgroundUpload(
        kind: String,
        uri: android.net.Uri,
        caption: String,
        hashtags: List<String>,
        location: String?,
        filterName: String?,
        visibility: String,
        music: String,
        closeFriends: Boolean,
        flags: Set<String> = emptySet()
    ) {
        val current = _uploadStatus.value
        if (current != null && !current.failed) {
            showToast("Another upload is still in progress. Please wait.")
            return
        }
        val app = getApplication<Application>()
        val label = when (kind) { "CLIP" -> "reel"; "MOMENT" -> "moment"; else -> "post" }
        val sizeMb = try {
            val bytes = app.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
            if (bytes > 0L) bytes / 1048576f else 0f
        } catch (e: Exception) { 0f }
        val isVideo = (app.contentResolver.getType(uri) ?: "").startsWith("video")

        val job: () -> Unit = {
            _uploadStatus.value = UploadStatus(label = label, progress = 0, sizeMb = sizeMb)
            viewModelScope.launch {
                val res = com.example.util.CloudinaryUploader.uploadUri(
                    context = app,
                    uri = uri,
                    onProgress = { p -> _uploadStatus.update { st -> st?.copy(progress = p) } }
                )
                if (res.isSuccess) {
                    val result = res.getOrNull()
                    val url = result?.secureUrl
                    if (url.isNullOrBlank()) {
                        _uploadStatus.value = UploadStatus(label, 0, sizeMb, true, "Upload failed. Please try again.")
                    } else {
                        val publicId = result?.publicId ?: ""
                        when (kind) {
                            "CLIP" -> publishClip(
                                caption, url, music, cloudinaryPublicId = publicId, visibility = visibility,
                                hideLikes = "HIDE_LIKES" in flags, hideShares = "HIDE_SHARES" in flags, commentsOff = "NO_COMMENTS" in flags
                            )
                            "MOMENT" -> {
                                // poll travels inside flags as "POLL:question|optionA|optionB"
                                val poll = flags.firstOrNull { it.startsWith("POLL:") }?.removePrefix("POLL:")?.split("|") ?: emptyList()
                                publishMoment(
                                    caption, url, music, closeFriends, visibility,
                                    pollQuestion = poll.getOrElse(0) { "" },
                                    pollOptionA = poll.getOrElse(1) { "" },
                                    pollOptionB = poll.getOrElse(2) { "" }
                                )
                            }
                            else -> publishPost(
                                caption, hashtags, listOf(url), location, filterName,
                                cloudinaryPublicId = publicId,
                                cloudinaryResourceType = if (isVideo) "video" else "image",
                                visibility = visibility
                            )
                        }
                        _uploadStatus.value = null
                        lastUploadJob = null
                        showToast("Your $label is live! 🎉")
                    }
                } else {
                    _uploadStatus.value = UploadStatus(
                        label, 0, sizeMb, true,
                        res.exceptionOrNull()?.localizedMessage ?: "Upload failed. Please try again."
                    )
                }
            }
        }
        lastUploadJob = job
        job()
    }

    // --- Posting streak: +1 for every day with a post, reel or story ---
    private fun bumpStreak() {
        val me = _uiState.value.currentUser ?: return
        val today = com.example.util.DailyChallenge.todayKey()
        if (me.lastStreakDay == today) return
        val count = if (me.lastStreakDay == com.example.util.DailyChallenge.yesterdayKey()) me.streakCount + 1 else 1
        _uiState.update { st -> st.copy(currentUser = st.currentUser?.copy(streakCount = count, lastStreakDay = today)) }
        viewModelScope.launch { repository.setStreak(me.id, count, today) }
        if (count > 1) showToast("🔥 $count day streak!")
    }

    // --- Profile note ---
    fun saveNote(text: String, song: com.example.util.MusicTrack?) {
        val me = _uiState.value.currentUser ?: return
        val clean = text.trim().take(60)
        viewModelScope.launch {
            val ok = repository.setNote(me.id, clean, song?.label ?: "", song?.previewUrl ?: "")
            if (ok) {
                _uiState.update { st ->
                    st.copy(
                        currentUser = st.currentUser?.copy(
                            note = clean,
                            noteSong = if (clean.isBlank()) "" else song?.label ?: "",
                            noteSongUrl = if (clean.isBlank()) "" else song?.previewUrl ?: "",
                            noteAt = if (clean.isBlank()) 0L else System.currentTimeMillis()
                        )
                    )
                }
                showToast(if (clean.isBlank()) "Note removed" else "Note shared for 24 hours")
            } else showToast("Could not save note")
        }
    }

    // --- Story viewers ---
    private val _momentViewers = MutableStateFlow<List<User>>(emptyList())
    val momentViewers: StateFlow<List<User>> = _momentViewers.asStateFlow()
    private var viewersJob: Job? = null
    private val viewedMomentIds = mutableSetOf<String>()

    private val _pollVotes = MutableStateFlow(PollVotes())
    val pollVotes: StateFlow<PollVotes> = _pollVotes.asStateFlow()
    private var pollJob: Job? = null

    fun voteMomentPoll(moment: Moment, option: Int) {
        val me = _uiState.value.currentUser ?: return
        // show the choice immediately, the live listener then confirms it
        val cur = _pollVotes.value
        _pollVotes.value = when {
            cur.mine == option -> cur
            option == 0 -> PollVotes(cur.a + 1, if (cur.mine == 1) cur.b - 1 else cur.b, 0)
            else -> PollVotes(if (cur.mine == 0) cur.a - 1 else cur.a, cur.b + 1, 1)
        }
        viewModelScope.launch { repository.voteMomentPoll(moment.id, me.id, option) }
    }

    /** Call when a story is shown. Saves a view for others' stories, or live-loads viewers for my own. */
    fun onMomentShown(moment: Moment) {
        val me = _uiState.value.currentUser ?: return
        pollJob?.cancel()
        _pollVotes.value = PollVotes()
        if (moment.pollQuestion.isNotBlank()) {
            pollJob = viewModelScope.launch {
                repository.observeMomentPollVotes(moment.id, me.id).collect { _pollVotes.value = it }
            }
        }
        viewersJob?.cancel()
        _momentViewers.value = emptyList()
        if (moment.author.id == me.id) {
            viewersJob = viewModelScope.launch {
                repository.observeMomentViewers(moment.id).collect { _momentViewers.value = it }
            }
        } else if (viewedMomentIds.add(moment.id)) {
            viewModelScope.launch { repository.recordMomentView(moment.id, me) }
        }
    }

    fun publishMoment(
        textOverlay: String,
        mediaUrl: String,
        musicTrack: String?,
        isCloseFriends: Boolean,
        visibility: String = "PUBLIC",
        pollQuestion: String = "",
        pollOptionA: String = "",
        pollOptionB: String = ""
    ) {
        val user = _uiState.value.currentUser ?: return
        bumpStreak()
        val (musicLabel, musicUrl) = com.example.util.MusicTrack.unpack(musicTrack)
        val newMoment = Moment(
            id = "m_${System.currentTimeMillis()}",
            author = user,
            mediaUrl = mediaUrl,
            textOverlay = textOverlay,
            musicTrack = musicLabel.ifBlank { null },
            musicUrl = musicUrl,
            pollQuestion = pollQuestion,
            pollOptionA = pollOptionA,
            pollOptionB = pollOptionB,
            timestamp = "Just now",
            isCloseFriendsOnly = isCloseFriends,
            viewersCount = 0,
            visibility = visibility
        )
        _uiState.update { state ->
            state.copy(moments = listOf(newMoment) + state.moments)
        }
        viewModelScope.launch { repository.saveMoment(newMoment) }
        showToast("Your story is live for 24 hours")
    }

    // Create Post
    fun publishPost(
        caption: String,
        hashtags: List<String>,
        mediaUrls: List<String>,
        location: String?,
        filterName: String?,
        cloudinaryPublicId: String = "",
        cloudinaryResourceType: String = "image",
        visibility: String = "PUBLIC"
    ) {
        val user = _uiState.value.currentUser ?: return
        bumpStreak()

        val newPost = Post(
            id = "p_${System.currentTimeMillis()}",
            author = user,
            mediaUrls = mediaUrls,
            caption = caption,
            hashtags = hashtags,
            location = location,
            timestamp = "Just now",
            likesCount = 0,
            isLiked = false,
            filterApplied = filterName,
            cloudinaryPublicId = cloudinaryPublicId,
            cloudinaryResourceType = cloudinaryResourceType,
            visibility = visibility
        )

        viewModelScope.launch {
            repository.savePost(newPost)
        }

        _uiState.update { state ->
            state.copy(
                posts = listOf(newPost) + state.posts,
                selectedTab = MainTab.HOME,
                currentScreen = ScreenDestination.Main(MainTab.HOME)
            )
        }
        showToast("Post shared to your world! 🚀")
    }

    // Publish Clip (Short Video)
    fun publishClip(
        caption: String,
        mediaUrl: String,
        musicTitle: String = "Cyber Drift - Ambient Wave",
        musicArtist: String = "VYNORA Audio",
        cloudinaryPublicId: String = "",
        visibility: String = "PUBLIC",
        hideLikes: Boolean = false,
        hideShares: Boolean = false,
        commentsOff: Boolean = false
    ) {
        val user = _uiState.value.currentUser ?: return
        bumpStreak()
        // a song picked in the studio arrives as "Title • Artist" + preview url
        val (pickedLabel, pickedUrl) = com.example.util.MusicTrack.unpack(musicTitle)
        val labelParts = pickedLabel.split(" • ", limit = 2)
        val finalTitle = labelParts[0]
        val finalArtist = if (pickedUrl.isNotBlank() && labelParts.size > 1) labelParts[1] else musicArtist
        val newClip = Clip(
            id = "c_${System.currentTimeMillis()}",
            author = user,
            mediaUrl = mediaUrl,
            caption = caption,
            musicTitle = finalTitle,
            musicUrl = pickedUrl,
            musicArtist = if (finalTitle == "Original audio") user.username else finalArtist,
            hideLikeCount = hideLikes,
            hideShareCount = hideShares,
            commentsOff = commentsOff,
            likesCount = 0,
            commentsCount = 0,
            sharesCount = 0,
            cloudinaryPublicId = cloudinaryPublicId,
            createdAt = System.currentTimeMillis(),
            visibility = visibility
        )

        viewModelScope.launch {
            repository.saveClip(newClip)
        }

        _uiState.update { state ->
            state.copy(
                clips = listOf(newClip) + state.clips,
                selectedTab = MainTab.CLIPS,
                currentScreen = ScreenDestination.Main(MainTab.CLIPS)
            )
        }
        showToast("Clip shared! 🎬")
    }

    // Highlights
    fun addHighlight(
        title: String,
        coverUrl: String,
        mediaUrls: List<String> = emptyList(),
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val user = _uiState.value.currentUser ?: return
        val newHighlight = Highlight(
            id = "hl_${System.currentTimeMillis()}",
            title = title,
            coverUrl = coverUrl,
            mediaUrls = mediaUrls.ifEmpty { listOf(coverUrl) },
            createdAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            val result = repository.addHighlight(user.id, newHighlight)
            if (result.isSuccess) {
                val updatedHighlights = result.getOrNull() ?: (user.highlights + newHighlight)
                _uiState.update { it.copy(currentUser = user.copy(highlights = updatedHighlights)) }
                showToast("Highlight '$title' added! ✨")
                onSuccess()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to save highlight"
                showToast(err)
                onError(err)
            }
        }
    }

    fun updateUserAvatar(url: String) {
        val user = _uiState.value.currentUser ?: return
        _uiState.update { it.copy(currentUser = user.copy(avatarUrl = url)) }
        viewModelScope.launch {
            repository.updateUserAvatar(user.id, url)
        }
    }

    // Chats
    fun deleteChatMessage(conversationId: String, messageId: String) {
        var newLast: ChatMessage? = null
        _uiState.update { state ->
            state.copy(chats = state.chats.map { conv ->
                if (conv.id == conversationId) {
                    val left = conv.messages.filterNot { it.id == messageId }
                    newLast = left.lastOrNull()
                    conv.copy(messages = left, lastMessage = newLast ?: ChatMessage(text = "", timestamp = ""))
                } else conv
            })
        }
        viewModelScope.launch { repository.deleteChatMessage(conversationId, messageId, newLast) }
    }

    fun sendMessage(conversationId: String, text: String, isAudio: Boolean = false, audioDuration: String? = null) {
        if (text.isBlank() && !isAudio) return
        val user = _uiState.value.currentUser ?: return

        val newMessage = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            senderId = user.id,
            text = text,
            timestamp = "Now",
            isAudio = isAudio,
            audioDuration = audioDuration,
            isRead = true
        )

        _uiState.update { state ->
            val conversationExists = state.chats.any { it.id == conversationId }
            val updatedChats = if (conversationExists) {
                state.chats.map { conv ->
                    if (conv.id == conversationId) {
                        conv.copy(
                            messages = conv.messages + newMessage,
                            lastMessage = newMessage
                        )
                    } else conv
                }
            } else {
                state.chats + ChatConversation(
                    id = conversationId,
                    participant = pendingChatParticipants[conversationId] ?: VynoraOfficial.createOfficialUser(),
                    messages = listOf(newMessage),
                    lastMessage = newMessage
                )
            }
            state.copy(chats = updatedChats)
        }
        val otherId = _uiState.value.chats.firstOrNull { it.id == conversationId }?.participant?.id
            ?: VynoraOfficial.OFFICIAL_USER_ID
        viewModelScope.launch { repository.sendChatMessage(conversationId, user, otherId, newMessage) }
    }

    fun addChatReaction(conversationId: String, messageId: String, emoji: String) {
        _uiState.update { state ->
            val updatedChats = state.chats.map { conv ->
                if (conv.id == conversationId) {
                    val updatedMsgs = conv.messages.map { msg ->
                        if (msg.id == messageId) msg.copy(reactionEmoji = emoji) else msg
                    }
                    conv.copy(messages = updatedMsgs)
                } else conv
            }
            state.copy(chats = updatedChats)
        }
    }

    // AI Helpers (Gemini API)
    fun requestAiCaption(topic: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiGenerating = true) }
            val caption = GeminiService.generateCaption(topic)
            _uiState.update { it.copy(isAiGenerating = false) }
            onResult(caption)
        }
    }

    fun requestAiHashtags(topic: String, onResult: (List<String>) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiGenerating = true) }
            val tags = GeminiService.generateHashtags(topic)
            _uiState.update { it.copy(isAiGenerating = false) }
            onResult(tags)
        }
    }

    fun requestTranslation(text: String, targetLang: String, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val translated = GeminiService.translateText(text, targetLang)
            onResult(translated)
        }
    }

    // --- Admin Operations ---

    fun loadAdminData() {
        viewModelScope.launch {
            val holders = repository.getAllBadgeHolders()
            val pmts = repository.getAllPayments()
            val reps = repository.getAllReports()
            _uiState.update {
                it.copy(badgeHolders = holders, payments = pmts, reports = reps)
            }
        }
    }

    fun adminRevokeBadge(userId: String) {
        viewModelScope.launch {
            repository.revokeBadgeAdmin(userId)
            showToast("Badge revoked")
            loadAdminData()
        }
    }

    fun adminGrantBadge(userId: String) {
        viewModelScope.launch {
            repository.grantBadgeAdmin(userId, 30)
            showToast("VYNORA Plus granted for 30 days")
            loadAdminData()
        }
    }

    fun adminBanUser(userId: String) {
        viewModelScope.launch {
            repository.banUserAdmin(userId, true)
            showToast("User banned for referral fraud")
            loadAdminData()
        }
    }

    fun adminPromoteToOfficial(email: String) {
        viewModelScope.launch {
            val result = repository.promoteToOfficialAccount(email)
            if (result.isSuccess) {
                showToast("Account $email promoted to official account! 👑")
                loadAdminData()
            } else {
                showToast("Failed to promote: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    suspend fun loadUserAvatar(userId: String): String = try { repository.getUserAvatar(userId) } catch (e: Exception) { "" }

    fun markNotificationsSeen() {
        val unread = _uiState.value.notifications.filter { !it.isRead }.map { it.id }
        if (unread.isEmpty()) return
        _uiState.update { state ->
            state.copy(notifications = state.notifications.map { it.copy(isRead = true) })
        }
        viewModelScope.launch { repository.markNotificationsRead(unread) }
    }

    fun markAllNotificationsRead() {
        markNotificationsSeen()
        showToast("All notifications marked as read")
    }

    fun updateProfile(fullName: String, bio: String, website: String) {
        _uiState.update { state ->
            val updatedUser = state.currentUser?.copy(
                fullName = fullName,
                bio = bio,
                website = website
            )
            state.copy(currentUser = updatedUser)
        }
        showToast("Profile updated successfully ✨")
    }

    fun resolveReport(id: String, action: String) {
        _uiState.update { st -> st.copy(reports = st.reports.map { if (it.id == id) it.copy(status = action) else it }) }
        viewModelScope.launch { repository.updateReportStatus(id, action) }
        showToast("Report marked as $action")
    }

    fun deleteMyPost(postId: String) {
        val user = _uiState.value.currentUser ?: return
        val post = _uiState.value.posts.firstOrNull { it.id == postId } ?: return
        if (post.author.id != user.id && user.role != "ADMIN") return
        _uiState.update { state -> state.copy(posts = state.posts.filterNot { it.id == postId }) }
        viewModelScope.launch { repository.deletePost(postId, post.author.id) }
        showToast("Post deleted")
    }

    fun deleteMyClip(clipId: String) {
        val user = _uiState.value.currentUser ?: return
        val clip = _uiState.value.clips.firstOrNull { it.id == clipId } ?: return
        if (clip.author.id != user.id && user.role != "ADMIN") return
        _uiState.update { state -> state.copy(clips = state.clips.filterNot { it.id == clipId }) }
        viewModelScope.launch { repository.deleteClip(clipId) }
        showToast("Clip deleted")
    }

    fun removePostAsAdmin(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId)
            showToast("Post removed by Admin")
        }
    }
}

/** True while the app screen is visible (used to decide if a call notification is needed). */
object AppForeground {
    @Volatile var inForeground: Boolean = true
}
