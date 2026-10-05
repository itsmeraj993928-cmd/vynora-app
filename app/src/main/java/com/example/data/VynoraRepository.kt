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
import kotlinx.coroutines.tasks.await
import java.util.UUID

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
                val ownerId = doc.getString("userId")
                currentUserId != null && ownerId == currentUserId
            }
        } catch (e: Exception) {
            true
        }
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
            if (phoneDoc.exists() && phoneDoc.getString("userId") != uid) {
                return Result.failure(IllegalArgumentException("Mobile number is already registered to another account."))
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
                    mapOf("userId" to uid, "registeredAt" to FieldValue.serverTimestamp())
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

        // Auto follow vynora_official
        try {
            ensureOfficialAccountExists()
            db.collection("follows").document("${uid}_${VynoraOfficial.OFFICIAL_USER_ID}").set(
                mapOf(
                    "followerId" to uid,
                    "followedId" to VynoraOfficial.OFFICIAL_USER_ID,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()

            // Send welcome message in Chats from vynora_official
            val welcomeChatId = if (uid < VynoraOfficial.OFFICIAL_USER_ID) "${uid}_${VynoraOfficial.OFFICIAL_USER_ID}" else "${VynoraOfficial.OFFICIAL_USER_ID}_${uid}"
            db.collection("chats").document(welcomeChatId).set(
                mapOf(
                    "id" to welcomeChatId,
                    "participantIds" to listOf(uid, VynoraOfficial.OFFICIAL_USER_ID),
                    "lastMessage" to VynoraOfficial.welcomeMessage().take(60) + "...",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val msgId = "msg_welcome_${System.currentTimeMillis()}"
            db.collection("chats").document(welcomeChatId).collection("messages").document(msgId).set(
                ChatMessage(
                    id = msgId,
                    senderId = VynoraOfficial.OFFICIAL_USER_ID,
                    text = VynoraOfficial.welcomeMessage(),
                    timestamp = "Now",
                    isRead = false
                )
            ).await()
        } catch (ignored: Exception) {}

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
            }
        awaitClose { listener.remove() }
    }

    fun observeChats(userId: String): Flow<List<ChatConversation>> = callbackFlow {
        val listener = db.collection("chats")
            .whereArrayContains("participantIds", userId)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val chats = snapshot?.documents?.mapNotNull { doc ->
                    val id = doc.getString("id") ?: doc.id
                    val participantIds = doc.get("participantIds") as? List<String> ?: listOf()
                    val otherUserId = participantIds.firstOrNull { it != userId } ?: VynoraOfficial.OFFICIAL_USER_ID
                    val lastMessageText = doc.getString("lastMessage") ?: ""

                    ChatConversation(
                        id = id,
                        participant = if (otherUserId == VynoraOfficial.OFFICIAL_USER_ID) VynoraOfficial.createOfficialUser() else User(id = otherUserId, username = otherUserId),
                        lastMessage = ChatMessage(text = lastMessageText, timestamp = "Recent"),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()
                trySend(chats)
            }
        awaitClose { listener.remove() }
    }

    fun observeNotifications(userId: String): Flow<List<NotificationItem>> = callbackFlow {
        val listener = db.collection("notifications")
            .whereEqualTo("recipientId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val notifs = snapshot?.toObjects(NotificationItem::class.java) ?: emptyList()
                trySend(notifs)
            }
        awaitClose { listener.remove() }
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

    suspend fun saveClip(clip: Clip) {
        try {
            db.collection("clips").document(clip.id).set(clip).await()
        } catch (e: Exception) {
            Log.e("VynoraRepository", "Error saving clip to Firestore: ${e.message}", e)
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

    suspend fun deletePost(postId: String) {
        try {
            db.collection("posts").document(postId).delete().await()
        } catch (ignored: Exception) {}
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
            if (phoneDoc.exists() && phoneDoc.getString("userId") != uid) {
                return Result.failure(IllegalArgumentException("Mobile number is already registered to another account."))
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
            usernameLastChangedAt = if (isUsernameChanged) System.currentTimeMillis() else updatedUser.usernameLastChangedAt
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
                    try { db.collection("phones").document(oldCleanPhone).delete().await() } catch (_: Exception) {}
                }
                if (cleanPhone.isNotBlank()) {
                    db.collection("phones").document(cleanPhone).set(
                        mapOf("userId" to uid, "registeredAt" to FieldValue.serverTimestamp())
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
}
