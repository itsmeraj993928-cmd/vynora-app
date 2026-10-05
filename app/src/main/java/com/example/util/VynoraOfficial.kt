package com.example.util

import com.example.model.User

object VynoraOfficial {
    const val OFFICIAL_USER_ID = "vynora_official_uid"
    const val OFFICIAL_USERNAME = "vynora_official"
    const val OFFICIAL_NAME = "VYNORA OFFICIAL"
    const val OFFICIAL_BIO = "Official account of VYNORA. Share your world."
    const val OFFICIAL_AVATAR = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=400&q=80"

    val RESERVED_KEYWORDS = listOf("vynora", "official", "admin", "support", "staff")

    fun isOfficialAccount(userId: String?, username: String?): Boolean {
        return userId == OFFICIAL_USER_ID || username?.equals(OFFICIAL_USERNAME, ignoreCase = true) == true
    }

    /**
     * Blocks registration of any username containing "vynora", "official", "admin", "support", or "staff",
     * except for the official account itself.
     */
    fun isReservedUsername(username: String): Boolean {
        val lower = username.lowercase().trim()
        if (lower == OFFICIAL_USERNAME) return false
        return RESERVED_KEYWORDS.any { lower.contains(it) }
    }

    fun createOfficialUser(): User {
        return User(
            id = OFFICIAL_USER_ID,
            email = "official@vynora.io",
            username = OFFICIAL_USERNAME,
            fullName = OFFICIAL_NAME,
            avatarUrl = OFFICIAL_AVATAR,
            bio = OFFICIAL_BIO,
            website = "vynora.io",
            followersCount = 1,
            followingCount = 0,
            postsCount = 0,
            isEmailVerified = true,
            isOfficial = true,
            badgeActive = true,
            badgeExpiry = 0L, // Permanent
            badgeSource = "official",
            referralCode = "VYNORA",
            role = "ADMIN"
        )
    }

    fun welcomeMessage(): String {
        return "Welcome to VYNORA! 🌐✨\n\nShare your world through Posts, 24h Moments, and Clips. Invite 10 friends with your referral link to earn 30 days of VYNORA Plus for free, or upgrade anytime for Rs 49/month.\n\nEnjoy the journey!"
    }
}
