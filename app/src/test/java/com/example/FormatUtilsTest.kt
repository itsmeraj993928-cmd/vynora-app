package com.example

import com.example.model.Highlight
import com.example.model.User
import com.example.util.CloudinaryConfig
import com.example.util.formatCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun testFormatCountSpecifications() {
        // 0 shows "0"
        assertEquals("0", formatCount(0))
        assertEquals("0", formatCount(0L))

        // below 1000 shows exact number
        assertEquals("1", formatCount(1))
        assertEquals("42", formatCount(42))
        assertEquals("500", formatCount(500))
        assertEquals("999", formatCount(999))

        // 1000 = 1K, 1100 = 1.1K
        assertEquals("1K", formatCount(1000))
        assertEquals("1.1K", formatCount(1100))

        // Truncate, never round up, no trailing ".0"
        assertEquals("1K", formatCount(1099))
        assertEquals("1.1K", formatCount(1199))

        // 12500 = 12.5K
        assertEquals("12.5K", formatCount(12500))

        // 1000000 = 1M
        assertEquals("1M", formatCount(1000000))
        assertEquals("1M", formatCount(1099999))
        assertEquals("1.1M", formatCount(1100000))
        assertEquals("2.5M", formatCount(2500000))

        // Billions
        assertEquals("1B", formatCount(1000000000L))
        assertEquals("1.2B", formatCount(1250000000L))
    }

    @Test
    fun testCloudinaryConfigConstants() {
        assertEquals("x5zwfz5y", CloudinaryConfig.CLOUD_NAME)
        assertEquals("vynora_unsigned", CloudinaryConfig.UPLOAD_PRESET)
        assertTrue(CloudinaryConfig.UPLOAD_URL.contains("x5zwfz5y"))
        assertTrue(CloudinaryConfig.UPLOAD_URL.contains("auto/upload"))
    }

    @Test
    fun testNewUserDefaultStatsAreZeroAndHighlightsEmpty() {
        val newUser = User(
            id = "test_123",
            username = "testuser",
            fullName = "Test User"
        )

        // New users must start with 0 posts, 0 followers, 0 following
        assertEquals(0, newUser.postsCount)
        assertEquals(0, newUser.followersCount)
        assertEquals(0, newUser.followingCount)
        assertEquals("", newUser.bio)
        assertEquals("", newUser.avatarUrl)
        assertTrue(newUser.highlights.isEmpty())

        // Adding real highlight
        val highlight = Highlight(
            id = "hl_1",
            title = "Travel",
            coverUrl = "https://res.cloudinary.com/x5zwfz5y/image/upload/sample.jpg"
        )
        val userWithHighlight = newUser.copy(highlights = listOf(highlight))
        assertEquals(1, userWithHighlight.highlights.size)
        assertEquals("Travel", userWithHighlight.highlights[0].title)
    }
}
