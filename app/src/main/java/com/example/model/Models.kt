package com.example.model

data class Highlight(
    val id: String = "",
    val title: String = "",
    val coverUrl: String = "",
    val mediaUrls: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val fullName: String = "",
    val avatarUrl: String = "",
    val photoBase64: String = "",
    val bio: String = "",
    val website: String = "",
    val links: List<String> = emptyList(),
    val gender: String = "", // Male, Female, Other, Prefer not to say
    val dob: String = "", // YYYY-MM-DD (private)
    val phone: String = "", // Mobile number with country code (private)
    val phoneVerified: Boolean = false,
    val pronouns: String = "", // e.g. they/them, she/her
    val category: String = "", // e.g. Creator, Writer, Business
    val isPrivateAccount: Boolean = false,
    val showEmailOnProfile: Boolean = false,
    val showPhoneOnProfile: Boolean = false,
    val hideFollowersCount: Boolean = false,
    val isAiGeneratedProfile: Boolean = false,
    val usernameLastChangedAt: Long = 0L,
    val termsAccepted: Boolean = true,
    val termsVersion: String = "1.0",
    val termsAcceptedAt: Long = 0L,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
    val highlights: List<Highlight> = emptyList(),
    val isEmailVerified: Boolean = false,
    val isOfficial: Boolean = false, // Permanent Official badge (gold/white)
    val badgeActive: Boolean = false, // VYNORA Plus badge (blue)
    val badgeExpiry: Long = 0L, // Timestamp millis
    val badgeSource: String = "", // "referral", "paid", "official", ""
    val referralCode: String = "",
    val referralCount: Int = 0, // e.g. 6/10
    val referredBy: String? = null,
    val role: String = "USER", // "USER" or "ADMIN"
    val isFollowing: Boolean = false,
    val isBlocked: Boolean = false,
    val isMuted: Boolean = false,
    val isCloseFriend: Boolean = false,
    val isBanned: Boolean = false,
    val deviceId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    // Check if VYNORA Plus (blue badge) is currently active
    val isPlusActive: Boolean
        get() = !isBanned && (isOfficial || (badgeActive && (badgeExpiry == 0L || badgeExpiry > System.currentTimeMillis())))

    val daysRemaining: Int
        get() {
            if (badgeExpiry == 0L) return 0
            val diff = badgeExpiry - System.currentTimeMillis()
            return if (diff > 0) (diff / (1000 * 60 * 60 * 24)).toInt() + 1 else 0
        }
}

data class Referral(
    val id: String = "",
    val inviterId: String = "",
    val inviterUsername: String = "",
    val referredUserId: String = "",
    val referredUsername: String = "",
    val referralCode: String = "",
    val isValidated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class PaymentRecord(
    val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val username: String = "",
    val productId: String = "vynora_blue_badge_1m",
    val amount: Double = 49.0,
    val currency: String = "INR",
    val purchaseToken: String = "",
    val orderId: String = "",
    val status: String = "ACKNOWLEDGED",
    val timestamp: Long = System.currentTimeMillis()
)

data class Comment(
    val id: String = "",
    val postId: String = "",
    val author: User = User(),
    val text: String = "",
    val timestamp: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val isPinned: Boolean = false,
    val replies: List<Comment> = emptyList()
)

data class Post(
    val id: String = "",
    val author: User = User(),
    val mediaUrls: List<String> = emptyList(),
    val caption: String = "",
    val hashtags: List<String> = emptyList(),
    val location: String? = null,
    val timestamp: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val filterApplied: String? = null,
    val isVideo: Boolean = false,
    val audioTrack: String? = null,
    val cloudinaryPublicId: String = "",
    val cloudinaryResourceType: String = "image",
    val visibility: String = "PUBLIC" // PUBLIC, FOLLOWERS, PRIVATE
)

data class Moment(
    val id: String = "",
    val author: User = User(),
    val mediaUrl: String = "",
    val textOverlay: String? = null,
    val musicTrack: String? = null,
    val timestamp: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000L),
    val isViewed: Boolean = false,
    val isCloseFriendsOnly: Boolean = false,
    val viewersCount: Int = 0,
    val viewers: List<User> = emptyList(),
    val visibility: String = "PUBLIC" // PUBLIC, FOLLOWERS, PRIVATE
)

data class Clip(
    val id: String = "",
    val author: User = User(),
    val mediaUrl: String = "",
    val thumbnailGradient: List<Long> = listOf(0xFF0D0F18, 0xFFFF2E93, 0xFF00D1FF),
    val caption: String = "",
    val musicTitle: String = "",
    val musicArtist: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val sharesCount: Int = 0,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isFollowing: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val cloudinaryPublicId: String = "",
    val visibility: String = "PUBLIC", // PUBLIC, FOLLOWERS, PRIVATE
    val comments: List<Comment> = emptyList(),
    val hideLikeCount: Boolean = false,
    val hideShareCount: Boolean = false,
    val commentsOff: Boolean = false
)

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: String = "",
    @get:com.google.firebase.firestore.PropertyName("isAudio") @set:com.google.firebase.firestore.PropertyName("isAudio") var isAudio: Boolean = false,
    val audioDuration: String? = null,
    val mediaUrl: String? = null,
    val reactionEmoji: String? = null,
    @get:com.google.firebase.firestore.PropertyName("isRead") @set:com.google.firebase.firestore.PropertyName("isRead") var isRead: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class ChatConversation(
    val id: String = "",
    val participant: User = User(),
    val messages: List<ChatMessage> = emptyList(),
    val lastMessage: ChatMessage = ChatMessage(),
    val unreadCount: Int = 0,
    val isGroup: Boolean = false,
    val groupName: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class NotificationType {
    LIKE, COMMENT, FOLLOW, MENTION, BADGE_EXPIRY, SYSTEM
}

data class SavedAccount(
    val id: String = "",
    val username: String = "",
    val identifier: String = "",
    val avatarUrl: String = ""
)

data class NotificationItem(
    val id: String = "",
    val type: NotificationType = NotificationType.SYSTEM,
    val user: User = User(),
    val contentPreview: String? = null,
    val timeGroup: String = "Today",
    val timestamp: String = "",
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val postId: String = ""
)

data class ReportItem(
    val id: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val reportedUsername: String = "",
    val reporterUsername: String = "",
    val reason: String = "",
    val details: String = "",
    val timestamp: String = "",
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
)

data class CreatorStats(
    val totalImpressions: String = "0",
    val impressionsGrowth: String = "0%",
    val totalReach: String = "0",
    val reachGrowth: String = "0%",
    val engagementRate: String = "0%",
    val engagementGrowth: String = "0%",
    val totalTipsEarned: String = "Rs 0",
    val subscriberCount: Int = 0,
    val weeklyGrowth: List<Pair<String, Int>> = listOf(
        "Mon" to 0, "Tue" to 0, "Wed" to 0,
        "Thu" to 0, "Fri" to 0, "Sat" to 0, "Sun" to 0
    )
)


/** A voice/video call between two people (stored in Firestore "calls"). */
data class CallInfo(
    val id: String = "",
    val callerId: String = "",
    val calleeId: String = "",
    val video: Boolean = false,
    val status: String = "RINGING", // RINGING, ACCEPTED, DECLINED, CANCELED, MISSED, ENDED
    val channel: String = "",
    val conversationId: String = "",
    val delivered: Boolean = false, // set by the other phone when it received the call (shows "Ringing")
    val createdAt: Long = 0L
)
