package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.*
import com.example.util.VynoraOfficial
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

private const val MAX_ACCOUNTS_PER_PHONE = 5

internal data class NotificationDoc(
    val id: String = "",
    val recipientId: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val user: User = User(),
    val contentPreview: String? = null,
    val timeGroup: String = "Today",
    val timestamp: String = "Just now",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val postId: String = ""
)

class VynoraRepository(
    private val context: Context,
    private val db: FirebaseFirestore = run {
        try {
            val dbId = context.getString(R.string.firestore_database_id)
            if (dbId.isNotBlank()) {
                FirebaseFirestore.getInstance(dbId)
            } else {
                FirebaseFirestore.getInstance()
            }
        } catch (e: Exception) {
            FirebaseFirestore.getInstance()
        }
    },
    private val auth: FirebaseAuth = Firebase.auth
) {
    val currentAuthUser: FirebaseUser?
        get() = auth.currentUser

    // --- Authentication & User Management ---

    suspend fun getUserProfile(userId: String): User? {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val user = doc.toObject(User::class.java)
                // Check local expiry if badge is active
                if (user != null && user.badgeActive && user.badgeExpiry > 0L && user.badgeExpiry <= System.currentTimeMillis() && !user.isOfficial) {
                    val expiredUser = user.copy(badgeActive = false, badgeSource = "")
                    db.collection("users").document(userId).update(
                        mapOf("badgeActive" to false, "badgeSource" to "")
                    ).await()
                    expiredUser
                } else {
                    user
                }
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun ensureUserProfile(user: FirebaseUser, deviceId: String): User {
        val existing = getUserProfile(user.uid)
        if (existing != null) return existing

        val baseUsername = (user.email?.substringBefore("@") ?: "user_${user.uid.take(6)}")
            .filter { it.isLetterOrDigit() || it == '_' }
            .lowercase()
            .ifBlank { "user_${user.uid.take(6)}" }

        val safeUsername = if (VynoraOfficial.isReservedUsername(baseUsername)) {
            "${baseUsername}_${user.uid.take(4).lowercase()}"
        } else baseUsername

        val myReferralCode = safeUsername.take(4).uppercase() + (10..99).random()

        val newUser = User(
            id = user.uid,
            email = user.email ?: "",
            username = safeUsername,
            fullName = user.displayName ?: safeUsername,
            avatarUrl = user.photoUrl?.toString() ?: "",
            bio = "",
            isEmailVerified = user.isEmailVerified,
            isOfficial = false,
            badgeActive = false,
            badgeExpiry = 0L,
            badgeSource = "",
            referralCode = myReferralCode,
            referralCount = 0,
            referredBy = null,
            role = "USER",
            deviceId = deviceId,
            createdAt = System.currentTimeMillis()
        )

        try {
            db.collection("usernames").document(safeUsername).set(
                mapOf("userId" to user.uid, "registeredAt" to FieldValue.serverTimestamp())
            ).await()
        } catch (_: Exception) {}

        try {
            db.collection("users").document(user.uid).set(newUser).await()
        } catch (_: Exception) {}

        return newUser
    }

    suspend fun isUsernameAvailable(username: String, currentUserId: String? = null): Boolean {
        val clean = username.lowercase().trim()
        if (clean.isBlank() || VynoraOfficial.isReservedUsername(clean)) return false
        return try {
            val doc = db.collection("usernames").document(clean).get().await()
            if (!doc.exists()) {
                true
            } else {
                val ids = phoneOwnerIds(doc)
                (currentUserId != null && ids.contains(currentUserId)) || ids.size < MAX_ACCOUNTS_PER_PHONE
            }
        } catch (e: Exception) {
            true
        }
    }

    private fun phoneOwnerIds(doc: com.google.firebase.firestore.DocumentSnapshot): List<String> {
        val list = (doc.get("userIds") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val single = doc.getString("userId")
        return if (single != null && !list.contains(single)) list + single else list
    }

    suspend fun isPhoneAvailable(phone: String, currentUserId: String? = null): Boolean {
        val clean = phone.filter { it.isDigit() || it == '+' }
        if (clean.isBlank()) return false
        return try {
            val doc = db.collection("phones").document(clean).get().await()
            if (!doc.exists()) {
                true
            } else {
                val ownerId = doc.getString("userId")
                currentUserId != null && ownerId == currentUserId
            }
        } catch (e: Exception) {
            true
        }
    }

    suspend fun registerUser(
        uid: String,
        email: String,
        username: String,
        fullName: String,
        deviceId: String,
        referralCodeInput: String?,
        gender: String = "",
        dob: String = "",
        phone: String = "",
        phoneVerified: Boolean = false,
        photoBase64: String = "",
        termsVersion: String = "1.0"
    ): Result<User> {
        val cleanUsername = username.lowercase().trim()
        val cleanPhone = phone.filter { it.isDigit() || it == '+' }

        // 1. Reserved usernames check
        if (VynoraOfficial.isReservedUsername(cleanUsername)) {
            return Result.failure(IllegalArgumentException("Username '$username' is reserved and cannot be registered."))
        }

        // 2. Username Uniqueness check
        val usernameDoc = db.collection("usernames").document(cleanUsername).get().await()
        if (usernameDoc.exists() && usernameDoc.getString("userId") != uid) {
            return Result.failure(IllegalArgumentException("Username '$username' is already taken."))
        }

        // 3. Phone Uniqueness check (if provided)
        if (cleanPhone.isNotBlank()) {
            val phoneDoc = db.collection("phones").document(cleanPhone).get().await()
            if (phoneDoc.exists()) {
                val ids = phoneOwnerIds(phoneDoc)
                if (!ids.contains(uid) && ids.size >= MAX_ACCOUNTS_PER_PHONE) {
                    return Result.failure(IllegalArgumentException("Is mobile number se maximum $MAX_ACCOUNTS_PER_PHONE accounts ban sakte hain."))
                }
            }
        }

        // 4. Generate unique referral code for this user
        val myReferralCode = cleanUsername.take(4).uppercase() + (10..99).random()

        val avatarSource = if (photoBase64.isNotBlank()) {
            if (photoBase64.startsWith("data:image")) photoBase64 else "data:image/jpeg;base64,$photoBase64"
        } else ""

        val newUser = User(
            id = uid,
            email = email,
            username = cleanUsername,
            fullName = fullName.trim(),
            avatarUrl = avatarSource,
            photoBase64 = photoBase64,
            bio = "",
            gender = gender,
            dob = dob,
            phone = cleanPhone,
            phoneVerified = phoneVerified,
            termsAccepted = true,
            termsVersion = termsVersion,
            termsAcceptedAt = System.currentTimeMillis(),
            usernameLastChangedAt = System.currentTimeMillis(),
            isEmailVerified = auth.currentUser?.isEmailVerified ?: false,
            isOfficial = false,
            badgeActive = false,
            badgeExpiry = 0L,
            badgeSource = "",
            referralCode = myReferralCode,
            referralCount = 0,
            referredBy = referralCodeInput?.trim()?.uppercase(),
            role = "USER",
            deviceId = deviceId,
            createdAt = System.currentTimeMillis()
        )

        // Save username reservation
        try {
            db.collection("usernames").document(cleanUsername).set(
                mapOf("userId" to uid, "registeredAt" to FieldValue.serverTimestamp())
            ).await()
        } catch (_: Exception) {}

        // Save phone reservation (uniqueness lookup)
        if (cleanPhone.isNotBlank()) {
            try {
                db.collection("phones").document(cleanPhone).set(
                    mapOf(
                        "userId" to uid,
                        "userIds" to FieldValue.arrayUnion(uid),
                        "registeredAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
            } catch (_: Exception) {}
        }

        // Save private user details subcollection
        try {
            db.collection("users").document(uid).collection("private").document("info").set(
                mapOf(
                    "phone" to cleanPhone,
                    "dob" to dob,
                    "email" to email,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {}

        // Save user document
        db.collection("users").document(uid).set(newUser).await()

        // Auto follow founder account (@meraj)
        try {
            val founderId = db.collection("usernames").document("meraj").get().await().getString("userId")
            if (!founderId.isNullOrBlank() && founderId != uid) {
                db.collection("follows").document("${uid}_$founderId").set(
                    mapOf(
                        "followerId" to uid,
                        "followedId" to founderId,
                        "timestamp" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (_: Exception) {}

        return Result.success(newUser)
    }

    suspend fun ensureOfficialAccountExists() {
        val officialDoc = db.collection("users").document(VynoraOfficial.OFFICIAL_USER_ID).get().await()
        if (!officialDoc.exists()) {
            val officialUser = VynoraOfficial.createOfficialUser()
            db.collection("users").document(VynoraOfficial.OFFICIAL_USER_ID).set(officialUser).await()
            db.collection("usernames").document(VynoraOfficial.OFFICIAL_USERNAME).set(
                mapOf("userId" to VynoraOfficial.OFFICIAL_USER_ID, "registeredAt" to FieldValue.serverTimestamp())
            ).await()
        }
    }

    // --- Referral Logic with Anti-Fraud ---

    suspend fun validateAndRecordReferral(newUserId: String, email: String, deviceId: String, referralCode: String?): Boolean {
        if (referralCode.isNullOrBlank()) return false

        val cleanCode = referralCode.trim().uppercase()

        // 1. Find inviter by referralCode
        val inviterQuery = db.collection("users")
            .whereEqualTo("referralCode", cleanCode)
            .limit(1)
            .get()
            .await()

        if (inviterQuery.isEmpty) return false
        val inviterDoc = inviterQuery.documents[0]
        val inviter = inviterDoc.toObject(User::class.java) ?: return false

        // Anti-fraud Rule 1: Block self-referral
        if (inviter.id == newUserId) return false

        // Anti-fraud Rule 2: Block same device
        if (inviter.deviceId.isNotBlank() && inviter.deviceId == deviceId) return false

        // Anti-fraud Rule 3: Block repeated email patterns (e.g. test+1@gmail.com)
        val normalizedInviterEmail = inviter.email.substringBefore("+").substringBefore("@")
        val normalizedNewEmail = email.substringBefore("+").substringBefore("@")
        if (normalizedInviterEmail.isNotBlank() && normalizedInviterEmail == normalizedNewEmail) return false

        // Anti-fraud Rule 4: Count only when verified
        val isVerified = auth.currentUser?.isEmailVerified == true
        if (!isVerified) return false

        // Prevent duplicate referral counting for this user
        val existingRef = db.collection("referrals")
            .whereEqualTo("referredUserId", newUserId)
            .limit(1)
            .get()
            .await()

        if (!existingRef.isEmpty) return false

        // Record referral
        val refId = "ref_${System.currentTimeMillis()}"
        val record = Referral(
            id = refId,
            inviterId = inviter.id,
            inviterUsername = inviter.username,
            referredUserId = newUserId,
            referredUsername = auth.currentUser?.displayName ?: "User",
            referralCode = cleanCode,
            isValidated = true,
            createdAt = System.currentTimeMillis()
        )
        db.collection("referrals").document(refId).set(record).await()

        // Increment inviter count
        val newCount = inviter.referralCount + 1
        val updateMap = mutableMapOf<String, Any>("referralCount" to newCount)

        // Award badge for 30 days if reaches 10 referrals
        if (newCount >= 10 && (!inviter.badgeActive || inviter.badgeSource == "referral")) {
            val thirtyDaysMillis = 30L * 24L * 60L * 60L * 1000L
            val currentExpiry = if (inviter.badgeActive && inviter.badgeExpiry > System.currentTimeMillis()) inviter.badgeExpiry else System.currentTimeMillis()
            updateMap["badgeActive"] = true
            updateMap["badgeExpiry"] = currentExpiry + thirtyDaysMillis
            updateMap["badgeSource"] = "referral"

            // Send notification to inviter
            db.collection("notifications").document("notif_plus_${System.currentTimeMillis()}").set(
                NotificationItem(
                    id = "notif_plus_${System.currentTimeMillis()}",
                    type = NotificationType.SYSTEM,
                    user = VynoraOfficial.createOfficialUser(),
                    contentPreview = "Congratulations! You reached 10 referrals and earned 30 days of free VYNORA Plus! 🌟",
                    timeGroup = "Today",
                    timestamp = "Just now"
                )
            )
        }

        db.collection("users").document(inviter.id).update(updateMap).await()
        return true
    }

    // --- Google Play Billing & Buy Badge (Rs 49 for 1 month) ---

    suspend fun activatePaidBadge(userId: String, orderId: String, purchaseToken: String): Result<User> {
        val userDoc = db.collection("users").document(userId).get().await()
        if (!userDoc.exists()) return Result.failure(IllegalStateException("User not found"))

        val user = userDoc.toObject(User::class.java)!!
        val thirtyDaysMillis = 30L * 24L * 60L * 60L * 1000L
        val currentExpiry = if (user.badgeActive && user.badgeExpiry > System.currentTimeMillis()) user.badgeExpiry else System.currentTimeMillis()
        val newExpiry = currentExpiry + thirtyDaysMillis

        val updateMap = mapOf(
            "badgeActive" to true,
            "badgeExpiry" to newExpiry,
            "badgeSource" to "paid"
        )
        db.collection("users").document(userId).update(updateMap).await()

        // Record payment
        val paymentId = "pay_${System.currentTimeMillis()}"
        val payment = PaymentRecord(
            id = paymentId,
            userId = userId,
            userEmail = user.email,
            username = user.username,
            amount = 49.0,
            currency = "INR",
            purchaseToken = purchaseToken,
            orderId = orderId,
            status = "ACKNOWLEDGED",
            timestamp = System.currentTimeMillis()
        )
        db.collection("payments").document(paymentId).set(payment).await()

        // Notification
        db.collection("notifications").document("notif_pay_${System.currentTimeMillis()}").set(
            NotificationItem(
                id = "notif_pay_${System.currentTimeMillis()}",
                type = NotificationType.SYSTEM,
                user = VynoraOfficial.createOfficialUser(),
                contentPreview = "Your VYNORA Plus subscription is active for 30 days! Thank you for supporting VYNORA.",
                timeGroup = "Today",
                timestamp = "Just now"
            )
        )

        return Result.success(user.copy(badgeActive = true, badgeExpiry = newExpiry, badgeSource = "paid"))
    }

    // --- Realtime Snapshot Flows ---

    fun observePosts(): Flow<List<Post>> = callbackFlow {
        val listener = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val posts = snapshot?.toObjects(Post::class.java) ?: emptyList()
                trySend(posts)
            }
        awaitClose { listener.remove() }
    }

    fun observeClips(): Flow<List<Clip>> = callbackFlow {
        val listener = db.collection("clips")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val clips = snapshot?.toObjects(Clip::class.java) ?: emptyList()
                trySend(clips)
                // reel docs may carry an old/empty copy of the creator's photo: fetch the real one
                launch {
                    val enriched = clips.map { c ->
                        if (c.author.id.isBlank()) c
                        else {
                            val full = loadUserCached(c.author.id)
                            c.copy(
                                author = c.author.copy(
                                    username = full.username.ifBlank { c.author.username },
                                    fullName = full.fullName.ifBlank { c.author.fullName },
                                    avatarUrl = full.avatarUrl.ifBlank { c.author.avatarUrl }
                                )
                            )
                        }
                    }
                    trySend(enriched)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeMoments(): Flow<List<Moment>> = callbackFlow {
        val now = System.currentTimeMillis()
        val listener = db.collection("moments")
            .whereGreaterThan("expiresAt", now)
            .orderBy("expiresAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val moments = snapshot?.toObjects(Moment::class.java) ?: emptyList()
                trySend(moments)
                // fill in each author's real profile photo/name (story docs may carry an old or empty copy)
                launch {
                    val enriched = moments.map { m ->
                        if (m.author.id.isBlank()) m
                        else {
                            val full = loadUserCached(m.author.id)
                            m.copy(
                                author = m.author.copy(
                                    username = full.username.ifBlank { m.author.username },
                                    fullName = full.fullName.ifBlank { m.author.fullName },
                                    avatarUrl = full.avatarUrl.ifBlank { m.author.avatarUrl },
                                    note = full.note,
                                    noteSong = full.noteSong,
                                    noteSongUrl = full.noteSongUrl,
                                    noteAt = full.noteAt,
                                    streakCount = full.streakCount
                                )
                            )
                        }
                    }
                    trySend(enriched)
                }
            }
        awaitClose { listener.remove() }
    }

    private val userCache = mutableMapOf<String, User>()

    private suspend fun loadUserCached(uid: String): User {
        if (uid == VynoraOfficial.OFFICIAL_USER_ID) return VynoraOfficial.createOfficialUser()
        userCache[uid]?.let { return it }
        return try {
            val u = db.collection("users").document(uid).get().await().toObject(User::class.java)
            val user = (u ?: User(id = uid, username = uid)).copy(id = uid)
            userCache[uid] = user
            user
        } catch (e: Exception) {
            User(id = uid, username = uid)
        }
    }

    fun observeChats(userId: String): Flow<List<ChatConversation>> = callbackFlow {
        val listener = db.collection("chats")
            .whereArrayContains("participantIds", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("VynoraRepo", "chats listener error", error)
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: emptyList()
                launch {
                    val chats = docs.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val participantIds = (doc.get("participantIds") as? List<*>)?.filterIsInstance<String>() ?: listOf()
                            val otherUserId = participantIds.firstOrNull { it != userId } ?: VynoraOfficial.OFFICIAL_USER_ID
                            val lastMessageText = doc.getString("lastMessage") ?: ""
                            val msgs = try {
                                doc.reference.collection("messages").get().await()
                                    .documents.mapNotNull { m -> try { m.toObject(ChatMessage::class.java)?.let { cm -> cm.copy(isAudio = (m.getBoolean("isAudio") ?: m.getBoolean("audio") ?: cm.isAudio)) } } catch (e: Exception) { null } }
                                    .sortedBy { it.createdAt }
                            } catch (e: Exception) { emptyList() }
                            val updatedAt = when (val v = doc.get("updatedAt")) {
                                is Number -> v.toLong()
                                is com.google.firebase.Timestamp -> v.toDate().time
                                else -> null
                            } ?: System.currentTimeMillis()
                            ChatConversation(
                                id = id,
                                participant = loadUserCached(otherUserId),
                                messages = msgs,
                                lastMessage = msgs.lastOrNull() ?: ChatMessage(text = lastMessageText, timestamp = "Recent"),
                                updatedAt = updatedAt
                            )
                        } catch (e: Exception) { null }
                    }
                    trySend(chats.sortedByDescending { it.updatedAt })
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeNotifications(userId: String): Flow<List<NotificationItem>> = callbackFlow {
        val listener = db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("VynoraRepo", "notifications listener error", error)
                    return@addSnapshotListener
                }
                val notifs = snapshot?.documents?.mapNotNull { d ->
                    try { d.toObject(NotificationItem::class.java) } catch (e: Exception) { null }
                } ?: emptyList()
                val sorted = notifs.sortedByDescending { it.createdAt }.take(100)
                this@callbackFlow.launch {
                    val enriched = sorted.map { n ->
                        if (n.user.id.isBlank()) n
                        else {
                            val full = loadUserCached(n.user.id)
                            n.copy(user = n.user.copy(avatarUrl = full.avatarUrl.ifBlank { n.user.avatarUrl }))
                        }
                    }
                    trySend(enriched)
                }
            }
        awaitClose { listener.remove() }
    }

    fun observeLikedPostIds(userId: String): Flow<Set<String>> = callbackFlow {
        val listener = db.collection("users").document(userId).collection("likedPosts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("VynoraRepo", "likedPosts listener error", error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.map { it.id }?.toSet() ?: emptySet())
            }
        awaitClose { listener.remove() }
    }

    suspend fun setPostLike(postId: String, me: User, authorId: String, liked: Boolean, preview: String) {
        try {
            val likedRef = db.collection("users").document(me.id).collection("likedPosts").document(postId)
            val postRef = db.collection("posts").document(postId)
            if (liked) {
                likedRef.set(mapOf("postId" to postId, "at" to FieldValue.serverTimestamp())).await()
                postRef.update("likesCount", FieldValue.increment(1)).await()
            } else {
                likedRef.delete().await()
                postRef.update("likesCount", FieldValue.increment(-1)).await()
            }
        } catch (e: Exception) {
            Log.w("VynoraRepo", "setPostLike failed", e)
        }
        try {
            if (authorId.isNotBlank() && authorId != me.id) {
                val nid = "like_${postId}_${me.id}"
                val ref = db.collection("notifications").document(nid)
                if (liked) {
                    val smallUser = me.copy(
                        avatarUrl = if (me.avatarUrl.startsWith("data:")) "" else me.avatarUrl,
                        photoBase64 = ""
                    )
                    ref.set(
                        NotificationDoc(
                            id = nid,
                            recipientId = authorId,
                            type = NotificationType.LIKE,
                            user = smallUser,
                            contentPreview = preview,
                            postId = postId
                        )
                    ).await()
                } else {
                    ref.delete().await()
                }
            }
        } catch (e: Exception) {
            Log.w("VynoraRepo", "like notification failed", e)
        }
    }

    // --- Admin Operations ---

    suspend fun getAllBadgeHolders(): List<User> {
        return try {
            val query = db.collection("users")
                .whereEqualTo("badgeActive", true)
                .get()
                .await()
            query.toObjects(User::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllPayments(): List<PaymentRecord> {
        return try {
            val query = db.collection("payments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            query.toObjects(PaymentRecord::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllReports(): List<ReportItem> {
        return try {
            val query = db.collection("reports")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
            query.toObjects(ReportItem::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun revokeBadgeAdmin(userId: String) {
        db.collection("users").document(userId).update(
            mapOf("badgeActive" to false, "badgeSource" to "", "badgeExpiry" to 0L)
        ).await()
    }

    suspend fun grantBadgeAdmin(userId: String, days: Int = 30) {
        val expiry = System.currentTimeMillis() + (days.toLong() * 24L * 60L * 60L * 1000L)
        db.collection("users").document(userId).update(
            mapOf("badgeActive" to true, "badgeSource" to "admin_grant", "badgeExpiry" to expiry)
        ).await()
    }

    suspend fun banUserAdmin(userId: String, isBanned: Boolean) {
        db.collection("users").document(userId).update(
            mapOf("isBanned" to isBanned, "badgeActive" to false)
        ).await()
    }

    suspend fun getFollowingIds(userId: String): Set<String> {
        return try {
            db.collection("follows").whereEqualTo("followerId", userId).get().await()
                .documents.mapNotNull { it.getString("followedId") }.toSet()
        } catch (e: Exception) {
            Log.e("VynoraRepository", "Error loading following list: ${e.message}", e)
            emptySet()
        }
    }

    suspend fun getFollowerIds(userId: String): List<String> {
        return try {
            db.collection("follows").whereEqualTo("followedId", userId).get().await()
                .documents.mapNotNull { it.getString("followerId") }.distinct()
        } catch (e: Exception) {
            Log.e("VynoraRepository", "Error loading followers: ${e.message}", e)
            emptyList()
        }
    }

    /** Finds accounts whose username starts with [prefix] (case-insensitive, usernames are stored lowercase). */
    suspend fun searchUsersByPrefix(prefix: String, limit: Long = 20): List<User> {
        val q = prefix.trim().removePrefix("@").lowercase()
        if (q.isBlank()) return emptyList()
        return try {
            db.collection("users")
                .orderBy("username")
                .startAt(q)
                .endAt(q + "\uf8ff")
                .limit(limit)
                .get().await()
                .toObjects(User::class.java)
                .filter { it.username.isNotBlank() }
        } catch (e: Exception) {
            Log.w("VynoraRepo", "searchUsersByPrefix failed", e)
            emptyList()
        }
    }

    suspend fun getUsersByIds(ids: List<String>): List<User> {
        val list = ids.distinct().take(300)
        val result = ArrayList<User>()
        for (id in list) {
            result.add(loadUserCached(id))
        }
        return result
    }

    suspend fun savePost(post: Post) {
        try {
            db.collection("posts").document(post.id).set(post).await()
            if (post.author.id.isNotBlank()) {
                db.collection("users").document(post.author.id).update(
                    "postsCount", FieldValue.increment(1)
                ).await()
            }
        } catch (e: Exception) {
            Log.e("VynoraRepository", "Error saving post to Firestore: ${e.message}", e)
        }
    }

    /** Returns null on success, or a short reason when saving failed. */
    suspend fun saveClip(clip: Clip): String? {
        return try {
            db.collection("clips").document(clip.id).set(clip).await()
            null
        } catch (e: Exception) {
            Log.e("VynoraRepository", "Error saving clip to Firestore: ${e.message}", e)
            (e.message ?: "unknown error").take(120)
        }
    }

    suspend fun addHighlight(userId: String, highlight: Highlight): Result<List<Highlight>> {
        return try {
            val doc = db.collection("users").document(userId).get().await()
            val existing = doc.toObject(User::class.java) ?: return Result.failure(IllegalStateException("User not found"))
            val updated = existing.highlights + highlight
            db.collection("users").document(userId).update("highlights", updated).await()
            Result.success(updated)
        } catch (e: Exception) {
            Log.e("VynoraRepository", "Error adding highlight: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateUserAvatar(userId: String, avatarUrl: String) {
        try {
            db.collection("users").document(userId).update("avatarUrl", avatarUrl).await()
        } catch (e: Exception) {
            Log.e("VynoraRepository", "Error updating avatarUrl: ${e.message}", e)
        }
    }

    suspend fun deletePost(postId: String, authorId: String = "") {
        try {
            db.collection("posts").document(postId).delete().await()
            if (authorId.isNotBlank()) {
                try {
                    db.collection("users").document(authorId).update(
                        "postsCount", FieldValue.increment(-1)
                    ).await()
                } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            Log.e("VynoraRepository", "Error deleting post: ${e.message}", e)
        }
    }

    suspend fun deleteMoment(momentId: String) {
        try {
            db.collection("moments").document(momentId).delete().await()
        } catch (e: Exception) {
            Log.e("VynoraRepository", "Error deleting moment: ${e.message}", e)
        }
    }

    suspend fun deleteClip(clipId: String) {
        try {
            db.collection("clips").document(clipId).delete().await()
        } catch (e: Exception) {
            Log.e("VynoraRepository", "Error deleting clip: ${e.message}", e)
        }
    }

    suspend fun promoteToOfficialAccount(email: String): Result<Boolean> {
        return try {
            val query = db.collection("users").whereEqualTo("email", email).limit(1).get().await()
            if (query.isEmpty) return Result.failure(IllegalArgumentException("User with email $email not found"))
            val doc = query.documents[0]
            doc.reference.update(
                mapOf(
                    "isOfficial" to true,
                    "badgeActive" to true,
                    "badgeExpiry" to 0L,
                    "badgeSource" to "official",
                    "role" to "ADMIN"
                )
            ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(updatedUser: User, oldUser: User? = null): Result<User> {
        val uid = updatedUser.id.ifBlank { auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated")) }
        val cleanUsername = updatedUser.username.lowercase().trim()
        val cleanPhone = updatedUser.phone.filter { it.isDigit() || it == '+' }

        // 0. Full name can be changed only once every 14 days
        val isNameChanged = oldUser != null && oldUser.fullName.trim() != updatedUser.fullName.trim()
        if (isNameChanged) {
            val dayMs = 24L * 60L * 60L * 1000L
            val elapsedName = System.currentTimeMillis() - oldUser!!.nameLastChangedAt
            if (oldUser.nameLastChangedAt > 0L && elapsedName < 14L * dayMs) {
                val daysLeft = ((14L * dayMs - elapsedName) / dayMs).toInt() + 1
                return Result.failure(IllegalStateException("Name can only be changed once every 14 days. $daysLeft days remaining."))
            }
        }

        // 1. If username changed, verify 14-day limit and uniqueness
        val isUsernameChanged = oldUser != null && oldUser.username.isNotBlank() && oldUser.username.lowercase().trim() != cleanUsername
        if (isUsernameChanged) {
            val fourteenDaysMillis = 14L * 24L * 60L * 60L * 1000L
            val elapsed = System.currentTimeMillis() - oldUser!!.usernameLastChangedAt
            if (oldUser.usernameLastChangedAt > 0L && elapsed < fourteenDaysMillis) {
                val daysRemaining = ((fourteenDaysMillis - elapsed) / (24L * 60L * 60L * 1000L)).toInt() + 1
                return Result.failure(IllegalStateException("Username can only be changed once every 14 days. $daysRemaining days remaining."))
            }

            if (VynoraOfficial.isReservedUsername(cleanUsername)) {
                return Result.failure(IllegalArgumentException("Username '$cleanUsername' is reserved."))
            }

            val usernameDoc = db.collection("usernames").document(cleanUsername).get().await()
            if (usernameDoc.exists() && usernameDoc.getString("userId") != uid) {
                return Result.failure(IllegalArgumentException("Username '$cleanUsername' is already taken."))
            }
        }

        // 2. If phone changed, verify uniqueness
        val isPhoneChanged = oldUser != null && oldUser.phone.filter { it.isDigit() || it == '+' } != cleanPhone
        if (isPhoneChanged && cleanPhone.isNotBlank()) {
            val phoneDoc = db.collection("phones").document(cleanPhone).get().await()
            if (phoneDoc.exists()) {
                val ids = phoneOwnerIds(phoneDoc)
                if (!ids.contains(uid) && ids.size >= MAX_ACCOUNTS_PER_PHONE) {
                    return Result.failure(IllegalArgumentException("Is mobile number se maximum $MAX_ACCOUNTS_PER_PHONE accounts ban sakte hain."))
                }
            }
        }

        val avatarSource = if (updatedUser.photoBase64.isNotBlank()) {
            if (updatedUser.photoBase64.startsWith("data:image")) updatedUser.photoBase64 else "data:image/jpeg;base64,${updatedUser.photoBase64}"
        } else if (updatedUser.avatarUrl.isNotBlank()) {
            updatedUser.avatarUrl
        } else {
            ""
        }

        val finalUser = updatedUser.copy(
            id = uid,
            username = cleanUsername,
            phone = cleanPhone,
            avatarUrl = avatarSource,
            usernameLastChangedAt = if (isUsernameChanged) System.currentTimeMillis() else updatedUser.usernameLastChangedAt,
            nameLastChangedAt = if (isNameChanged) System.currentTimeMillis() else (oldUser?.nameLastChangedAt ?: updatedUser.nameLastChangedAt)
        )

        return try {
            // Update old vs new username reservation
            if (isUsernameChanged) {
                if (oldUser!!.username.isNotBlank()) {
                    try { db.collection("usernames").document(oldUser.username.lowercase().trim()).delete().await() } catch (_: Exception) {}
                }
                db.collection("usernames").document(cleanUsername).set(
                    mapOf("userId" to uid, "registeredAt" to FieldValue.serverTimestamp())
                ).await()
            }

            // Update old vs new phone reservation
            if (isPhoneChanged) {
                val oldCleanPhone = oldUser!!.phone.filter { it.isDigit() || it == '+' }
                if (oldCleanPhone.isNotBlank()) {
                    try { db.collection("phones").document(oldCleanPhone).update("userIds", FieldValue.arrayRemove(uid)).await() } catch (_: Exception) {}
                }
                if (cleanPhone.isNotBlank()) {
                    db.collection("phones").document(cleanPhone).set(
                        mapOf(
                            "userId" to uid,
                            "userIds" to FieldValue.arrayUnion(uid),
                            "registeredAt" to FieldValue.serverTimestamp()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()
                }
            }

            // Update private subcollection for DOB and phone
            try {
                db.collection("users").document(uid).collection("private").document("info").set(
                    mapOf(
                        "phone" to cleanPhone,
                        "dob" to finalUser.dob,
                        "email" to finalUser.email,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (_: Exception) {}

            // Update user document
            db.collection("users").document(uid).set(finalUser).await()

            Result.success(finalUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- Social actions (all persisted in Firestore) ----------

    private fun slimUser(u: User): User = u.copy(
        avatarUrl = if (u.avatarUrl.startsWith("data:")) "" else u.avatarUrl,
        photoBase64 = ""
    )

    private suspend fun writeNotification(nid: String, recipientId: String, type: NotificationType, me: User, preview: String?, postId: String = "") {
        try {
            if (recipientId.isBlank() || recipientId == me.id) return
            db.collection("notifications").document(nid).set(
                NotificationDoc(
                    id = nid,
                    recipientId = recipientId,
                    type = type,
                    user = slimUser(me),
                    contentPreview = preview,
                    postId = postId
                )
            ).await()
        } catch (e: Exception) {
            Log.w("VynoraRepo", "notification write failed", e)
        }
    }

    suspend fun getUserAvatar(userId: String): String = loadUserCached(userId).avatarUrl

    suspend fun markNotificationsRead(ids: List<String>) {
        for (id in ids) {
            try { db.collection("notifications").document(id).update("isRead", true).await() } catch (e: Exception) { }
        }
    }

    private suspend fun removeNotification(nid: String) {
        try { db.collection("notifications").document(nid).delete().await() } catch (e: Exception) { }
    }

    suspend fun setFollow(me: User, targetId: String, follow: Boolean) {
        if (targetId.isBlank() || targetId == me.id) return
        try {
            val ref = db.collection("follows").document("${me.id}_$targetId")
            if (follow) {
                ref.set(mapOf("followerId" to me.id, "followedId" to targetId, "createdAt" to System.currentTimeMillis())).await()
            } else {
                ref.delete().await()
            }
            val d = if (follow) 1L else -1L
            try { db.collection("users").document(me.id).update("followingCount", FieldValue.increment(d)).await() } catch (e: Exception) { }
            try { db.collection("users").document(targetId).update("followersCount", FieldValue.increment(d)).await() } catch (e: Exception) { }
        } catch (e: Exception) {
            Log.w("VynoraRepo", "setFollow failed", e)
        }
        val nid = "follow_${me.id}_$targetId"
        if (follow) writeNotification(nid, targetId, NotificationType.FOLLOW, me, null) else removeNotification(nid)
    }

    fun observeLikedClipIds(userId: String): Flow<Set<String>> = callbackFlow {
        val listener = db.collection("users").document(userId).collection("likedClips")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("VynoraRepo", "likedClips listener error", error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.map { it.id }?.toSet() ?: emptySet())
            }
        awaitClose { listener.remove() }
    }

    suspend fun setClipLike(clipId: String, me: User, authorId: String, liked: Boolean, preview: String) {
        try {
            val likedRef = db.collection("users").document(me.id).collection("likedClips").document(clipId)
            val clipRef = db.collection("clips").document(clipId)
            if (liked) {
                likedRef.set(mapOf("clipId" to clipId, "at" to FieldValue.serverTimestamp())).await()
                clipRef.update("likesCount", FieldValue.increment(1)).await()
            } else {
                likedRef.delete().await()
                clipRef.update("likesCount", FieldValue.increment(-1)).await()
            }
        } catch (e: Exception) {
            Log.w("VynoraRepo", "setClipLike failed", e)
        }
        val nid = "cliplike_${clipId}_${me.id}"
        if (liked) writeNotification(nid, authorId, NotificationType.LIKE, me, preview) else removeNotification(nid)
    }

    suspend fun saveComment(postId: String, comment: Comment, postAuthorId: String) {
        try {
            val c = comment.copy(author = slimUser(comment.author))
            db.collection("posts").document(postId).update("comments", FieldValue.arrayUnion(c)).await()
        } catch (e: Exception) {
            Log.w("VynoraRepo", "saveComment failed", e)
        }
        writeNotification("comment_${comment.id}", postAuthorId, NotificationType.COMMENT, comment.author, comment.text.take(60), postId)
    }

    suspend fun saveClipComment(clipId: String, comment: Comment, clipAuthorId: String) {
        try {
            val c = comment.copy(author = slimUser(comment.author))
            db.collection("clips").document(clipId).update(
                mapOf(
                    "comments" to FieldValue.arrayUnion(c),
                    "commentsCount" to FieldValue.increment(1)
                )
            ).await()
        } catch (e: Exception) {
            Log.w("VynoraRepo", "saveClipComment failed", e)
        }
        writeNotification("comment_${comment.id}", clipAuthorId, NotificationType.COMMENT, comment.author, comment.text.take(60), clipId)
    }

    suspend fun setClipComments(clipId: String, comments: List<Comment>) {
        try {
            db.collection("clips").document(clipId).update(
                mapOf(
                    "comments" to comments.map { it.copy(author = slimUser(it.author)) },
                    "commentsCount" to comments.size
                )
            ).await()
        } catch (e: Exception) {
            Log.w("VynoraRepo", "setClipComments failed", e)
        }
    }

    /** Records that [viewer] watched a story. One doc per viewer, so repeats never double count. */
    suspend fun recordMomentView(momentId: String, viewer: User) {
        try {
            db.collection("moments").document(momentId)
                .collection("viewers").document(viewer.id)
                .set(
                    mapOf(
                        "userId" to viewer.id,
                        "username" to viewer.username,
                        "viewedAt" to System.currentTimeMillis()
                    )
                ).await()
        } catch (e: Exception) {
            Log.w("VynoraRepo", "recordMomentView failed", e)
        }
    }

    /** Live list of people who viewed a story (newest first), with their full profiles. */
    fun observeMomentViewers(momentId: String): Flow<List<User>> = callbackFlow {
        val listener = db.collection("moments").document(momentId)
            .collection("viewers")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("VynoraRepo", "viewers listener error", error)
                    return@addSnapshotListener
                }
                val ids = (snapshot?.documents ?: emptyList())
                    .sortedByDescending { it.getLong("viewedAt") ?: 0L }
                    .map { it.id }
                launch { trySend(ids.map { loadUserCached(it) }) }
            }
        awaitClose { listener.remove() }
    }

    suspend fun setStreak(uid: String, count: Int, day: String) {
        try {
            db.collection("users").document(uid).update(mapOf("streakCount" to count, "lastStreakDay" to day)).await()
            userCache.remove(uid)
        } catch (e: Exception) {
            Log.w("VynoraRepo", "setStreak failed", e)
        }
    }

    /** Sets (or clears when [note] is blank) the profile note. */
    suspend fun setNote(uid: String, note: String, song: String, songUrl: String): Boolean {
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "note" to note,
                    "noteSong" to song,
                    "noteSongUrl" to songUrl,
                    "noteAt" to if (note.isBlank()) 0L else System.currentTimeMillis()
                )
            ).await()
            userCache.remove(uid)
            true
        } catch (e: Exception) {
            Log.w("VynoraRepo", "setNote failed", e)
            false
        }
    }

    /** One vote per person; voting again just changes the choice. option: 0 = A, 1 = B */
    suspend fun voteMomentPoll(momentId: String, voterId: String, option: Int) {
        try {
            db.collection("moments").document(momentId)
                .collection("votes").document(voterId)
                .set(mapOf("userId" to voterId, "option" to option, "votedAt" to System.currentTimeMillis()))
                .await()
        } catch (e: Exception) {
            Log.w("VynoraRepo", "voteMomentPoll failed", e)
        }
    }

    fun observeMomentPollVotes(momentId: String, myId: String): Flow<PollVotes> = callbackFlow {
        val listener = db.collection("moments").document(momentId)
            .collection("votes")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("VynoraRepo", "votes listener error", error)
                    return@addSnapshotListener
                }
                var a = 0; var b = 0; var mine = -1
                snapshot?.documents?.forEach { d ->
                    val o = (d.getLong("option") ?: -1L).toInt()
                    if (o == 0) a++ else if (o == 1) b++
                    if (d.id == myId) mine = o
                }
                trySend(PollVotes(a, b, mine))
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveMoment(moment: Moment) {
        try {
            db.collection("moments").document(moment.id).set(moment).await()
        } catch (e: Exception) {
            Log.w("VynoraRepo", "saveMoment failed", e)
        }
    }

    suspend fun createCall(call: CallInfo) {
        try {
            db.collection("calls").document(call.id).set(call).await()
        } catch (e: Exception) {
            Log.w("VynoraRepo", "createCall failed", e)
        }
    }

    suspend fun updateCallStatus(callId: String, status: String) {
        try {
            db.collection("calls").document(callId).update("status", status).await()
        } catch (e: Exception) {
            Log.w("VynoraRepo", "updateCallStatus failed", e)
        }
    }

    suspend fun markCallDelivered(callId: String) {
        try {
            db.collection("calls").document(callId).update("delivered", true).await()
        } catch (e: Exception) {
            Log.w("VynoraRepo", "markCallDelivered failed", e)
        }
    }

    fun observeCall(callId: String): Flow<CallInfo?> = callbackFlow {
        val listener = db.collection("calls").document(callId).addSnapshotListener { snap, error ->
            if (error != null) return@addSnapshotListener
            trySend(snap?.toObject(CallInfo::class.java))
        }
        awaitClose { listener.remove() }
    }

    /** Emits the newest call that is ringing for this user (started in the last minute), or null. */
    fun observeIncomingCalls(userId: String): Flow<CallInfo?> = callbackFlow {
        val listener = db.collection("calls")
            .whereEqualTo("calleeId", userId)
            .whereEqualTo("status", "RINGING")
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.w("VynoraRepo", "incoming calls listener error", error)
                    return@addSnapshotListener
                }
                val now = System.currentTimeMillis()
                val fresh = snap?.toObjects(CallInfo::class.java)
                    ?.filter { Math.abs(now - it.createdAt) < 10 * 60_000L }
                    ?.maxByOrNull { it.createdAt }
                trySend(fresh)
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendChatMessage(conversationId: String, me: User, otherId: String, msg: ChatMessage) {
        try {
            val chatRef = db.collection("chats").document(conversationId)
            chatRef.set(
                mapOf(
                    "id" to conversationId,
                    "participantIds" to listOf(me.id, otherId),
                    "lastMessage" to msg.text.take(60),
                    "updatedAt" to System.currentTimeMillis()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            ).await()
            chatRef.collection("messages").document(msg.id).set(msg).await()
        } catch (e: Exception) {
            Log.w("VynoraRepo", "sendChatMessage failed", e)
        }
    }

    suspend fun deleteChatMessage(conversationId: String, messageId: String, newLast: ChatMessage?) {
        try {
            val chatRef = db.collection("chats").document(conversationId)
            chatRef.collection("messages").document(messageId).delete().await()
            val label = when {
                newLast == null -> ""
                newLast.isAudio -> "Voice note"
                else -> newLast.text.take(60)
            }
            chatRef.update("lastMessage", label).await()
        } catch (e: Exception) {
            Log.w("VynoraRepo", "deleteChatMessage failed", e)
        }
    }

    /** +1 on the reported post / reel / story so it can be hidden once enough people report it. */
    suspend fun bumpReportCount(targetType: String, targetId: String) {
        val col = when (targetType.lowercase()) {
            "post" -> "posts"
            "clip", "reel" -> "clips"
            "moment", "story" -> "moments"
            else -> return
        }
        try {
            db.collection(col).document(targetId).update("reportCount", com.google.firebase.firestore.FieldValue.increment(1)).await()
        } catch (e: Exception) {
            Log.w("VynoraRepo", "bumpReportCount failed", e)
        }
    }

    suspend fun saveReport(report: ReportItem) {
        try {
            db.collection("reports").document(report.id).set(report).await()
        } catch (e: Exception) {
            Log.w("VynoraRepo", "saveReport failed", e)
        }
    }

    suspend fun updateReportStatus(reportId: String, status: String) {
        try {
            db.collection("reports").document(reportId).update("status", status).await()
        } catch (e: Exception) {
            Log.w("VynoraRepo", "updateReportStatus failed", e)
        }
    }
}
