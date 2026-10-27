package com.example.util

/** Simple word filter for captions and comments (English + Hinglish). Not perfect, but stops the obvious. */
object ContentFilter {
    private val blocked = listOf(
        "porn", "pornhub", "xxx", "nude", "nudes", "naked", "sex video", "sexvideo", "onlyfans", "18+", "blowjob",
        "handjob", "hardcore sex", "fuck", "fucking", "bitch", "pussy", "dick pic", "boobs", "horny",
        "chut", "lund", "randi", "bhosd", "madarchod", "behenchod", "chodna", "gand mara", "mms leak", "desi sex",
        "sexy video", "hot video", "call girl", "escort"
    )

    private fun norm(s: String): String =
        s.lowercase().replace(Regex("[^a-z0-9+ ]"), " ").replace(Regex("\\s+"), " ").trim()

    /** True when the text contains a blocked word or phrase. */
    fun isBlocked(text: String): Boolean {
        if (text.isBlank()) return false
        val t = " " + norm(text) + " "
        return blocked.any { t.contains(" $it ") || t.contains(" $it") && it.length >= 5 }
    }
}
