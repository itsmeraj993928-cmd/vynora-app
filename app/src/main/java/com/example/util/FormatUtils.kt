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
