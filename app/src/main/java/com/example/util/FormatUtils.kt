package com.example.util

import java.util.Locale
import kotlin.math.floor

/**
 * Reusable count formatter according to VYNORA specifications:
 * - 0 shows "0"
 * - below 1000 shows the exact number
 * - 1000 = 1K, 1100 = 1.1K, 12500 = 12.5K, 1000000 = 1M
 * - Truncate, never round up, no trailing ".0"
 */
fun formatCount(count: Long): String {
    if (count <= 0L) return "0"
    if (count < 1000L) return count.toString()

    fun truncateToSingleDecimal(value: Double, suffix: String): String {
        // Truncate to 1 decimal place: e.g. 1.099 -> 1.0, 1.19 -> 1.1, 12.59 -> 12.5
        val truncated = floor(value * 10.0) / 10.0
        val longVal = truncated.toLong()
        return if (truncated == longVal.toDouble()) {
            "$longVal$suffix"
        } else {
            String.format(Locale.US, "%.1f%s", truncated, suffix)
        }
    }

    return when {
        count >= 1_000_000_000L -> truncateToSingleDecimal(count / 1_000_000_000.0, "B")
        count >= 1_000_000L -> truncateToSingleDecimal(count / 1_000_000.0, "M")
        else -> truncateToSingleDecimal(count / 1000.0, "K")
    }
}

fun formatCount(count: Int): String = formatCount(count.toLong())

/** "Just now", "5m", "3h", "2d" or "12 Oct" from a millisecond timestamp. */
fun timeAgo(createdAt: Long, now: Long = System.currentTimeMillis()): String {
    val diff = (now - createdAt).coerceAtLeast(0L)
    val min = diff / 60_000L
    return when {
        min < 1L -> "Just now"
        min < 60L -> "${min}m ago"
        min < 24L * 60L -> "${min / 60L}h ago"
        min < 7L * 24L * 60L -> "${min / (24L * 60L)}d ago"
        else -> java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(createdAt))
    }
}

/** "Today", "Yesterday" or "Earlier" (by calendar day). */
fun timeGroupOf(createdAt: Long, now: Long = System.currentTimeMillis()): String {
    val a = java.util.Calendar.getInstance().apply { timeInMillis = createdAt }
    val b = java.util.Calendar.getInstance().apply { timeInMillis = now }
    val days = ((b.get(java.util.Calendar.YEAR) - a.get(java.util.Calendar.YEAR)) * 366 + (b.get(java.util.Calendar.DAY_OF_YEAR) - a.get(java.util.Calendar.DAY_OF_YEAR)))
    return when {
        days <= 0 -> "Today"
        days == 1 -> "Yesterday"
        else -> "Earlier"
    }
}
