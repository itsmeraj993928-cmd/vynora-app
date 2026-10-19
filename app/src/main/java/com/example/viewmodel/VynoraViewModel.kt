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
    data class ChatRoom(val conversationId: String) : ScreenDestination()
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
    val activeSharePost: Post? = null,
    val activeReportTarget: Triple<String, String, String>? = null,
    val toastMessage: String? = null,
    val isAiGenerating: Boolean = false,
    val keywordFilterActive: Boolean = true,
    val offensiveKeywords: List<String> = listOf("scam", "cheat", "hate", "idiot", "fake", "abuse"),
    val deviceId: String = ""
)

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
    private fun blockedNames(): Set<String> = try { blockedPrefs().getStringSet("names", emptySet()) ?: emptySet() } catch (e: Exception) { emptySet() }

    private fun startDataObservations(userId: String) {
        observeJob?.cancel()
        likedIds = emptySet()
        likedClipIds = emptySet()
        observeJob = viewModelScope.launch {
            followingIds = repository.getFollowingIds(userId)
            launch {
                repository.observePosts().collect { postsList ->
                    val blocked = blockedNames()
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
                    val blocked = blockedNames()
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

    // --- Saved accounts (switch account). Passwords are never stored. ---
    private val accountPrefs = application.getSharedPreferences("vynora_accounts", android.content.Context.MODE_PRIVATE)
    private val _savedAccounts = MutableStateFlow(loadSavedAccounts())
    val savedAccounts: StateFlow<List<SavedAccount>> = _savedAccounts.asStateFlow()
    var prefillIdentifier: String = ""

    private fun loadSavedAccounts(): List<SavedAccount> = try {
        val arr = org.json.JSONArray(accountPrefs.getString("list", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            SavedAccount(o.getString("id"), o.getString("username"), o.getString("identifier"), o.optString("avatarUrl", ""))
        }
    } catch (e: Exception) { emptyList() }

    private fun rememberAccount(user: User, authEmail: String) {
        try {
            val identifier = if (authEmail.endsWith("@phone.vynora.app"))
                authEmail.substringBefore("@").substringBefore(".") else authEmail
            val avatar = if (user.avatarUrl.length < 300_000) user.avatarUrl else ""
            val updated = (_savedAccounts.value.filterNot { it.id == user.id } +
                SavedAccount(user.id, user.username, identifier, avatar)).takeLast(5)
            _savedAccounts.value = updated
            val arr = org.json.JSONArray()
            updated.forEach {
                arr.put(org.json.JSONObject().put("id", it.id).put("username", it.username)
                    .put("identifier", it.identifier).put("avatarUrl", it.avatarUrl))
            }
            accountPrefs.edit().putString("list", arr.toString()).apply()
        } catch (e: Exception) { }
    }

    fun switchAccount(accountId: String, identifier: String) {
        val cred = com.example.util.CredentialVault.get(getApplication(), accountId)
        if (cred == null) {
            // no saved login for this account: go to login screen with the id filled in
            prefillIdentifier = identifier
            signOut()
            return
        }
        signOut()
        signInWithEmail(
            email = cred.first,
            pass = cred.second,
            onSuccess = { },
            onError = {
                prefillIdentifier = identifier
                showToast("Login nahi hua, password dobara dalo")
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

    fun openComments(postId: String) {
        _uiState.update { it.copy(activeCommentsPostId = postId) }
    }

    fun closeComments() {
        _uiState.update { it.copy(activeCommentsPostId = null) }
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
        if (VynoraOfficial.isOfficialAccount(null, username)) {
            showToast("You cannot unfollow the official VYNORA account.")
            return
        }
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
        if (VynoraOfficial.isOfficialAccount(null, username)) {
            showToast("You cannot unfollow the official VYNORA account.")
            return
        }
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
    fun publishMoment(textOverlay: String, mediaUrl: String, musicTrack: String?, isCloseFriends: Boolean, visibility: String = "PUBLIC") {
        val user = _uiState.value.currentUser ?: return
        if (!user.isEmailVerified && !user.isOfficial) {
            showToast("Please verify your email before posting moments.")
            return
        }

        val newMoment = Moment(
            id = "m_${System.currentTimeMillis()}",
            author = user,
            mediaUrl = mediaUrl,
            textOverlay = textOverlay,
            musicTrack = musicTrack,
            timestamp = "Just now",
            isCloseFriendsOnly = isCloseFriends,
            viewersCount = 0,
            visibility = visibility
        )
        _uiState.update { state ->
            state.copy(moments = listOf(newMoment) + state.moments)
        }
        showToast("Moment published! Active for 24 hours 🕒")
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
        visibility: String = "PUBLIC"
    ) {
        val user = _uiState.value.currentUser ?: return
        val newClip = Clip(
            id = "c_${System.currentTimeMillis()}",
            author = user,
            mediaUrl = mediaUrl,
            caption = caption,
            musicTitle = musicTitle,
            musicArtist = musicArtist,
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
                    participant = VynoraOfficial.createOfficialUser(),
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

    fun markAllNotificationsRead() {
        _uiState.update { state ->
            val updated = state.notifications.map { it.copy(isRead = true) }
            state.copy(notifications = updated)
        }
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
