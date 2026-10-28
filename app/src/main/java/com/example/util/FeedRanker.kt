package com.example.util

import com.example.model.Clip
import com.example.model.Post
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

/**
 * Simple on-device feed ranking ("algorithm").
 * score = engagement * freshness * (follow boost) * (interest boost) * (small stable jitter)
 * Then a diversity pass so the same creator does not appear many times in a row.
 */
object FeedRanker {
    private val tagRegex = Regex("#[\\p{L}\\p{N}_]+")

    private fun score(
        createdAt: Long, now: Long, likes: Int, comments: Int, shares: Int,
        following: Boolean, interestOverlap: Int, id: String, seed: Int
    ): Double {
        val ageH = ((now - createdAt).coerceAtLeast(0L)) / 3_600_000.0
        val eng = 1.0 + ln(1.0 + likes + 2.0 * comments + 3.0 * shares)
        val fresh = 1.0 / (ageH + 2.0).pow(1.2)
        val follow = if (following) 1.6 else 1.0
        val interest = 1.0 + 0.25 * interestOverlap.coerceAtMost(3)
        val jitter = 0.9 + 0.2 * ((abs((id + seed).hashCode()) % 1000) / 1000.0)
        return eng * fresh * follow * interest * jitter
    }

    private fun <T> diversify(sorted: List<T>, authorOf: (T) -> String): List<T> {
        val rest = sorted.toMutableList()
        val out = ArrayList<T>(sorted.size)
        while (rest.isNotEmpty()) {
            val recent = out.takeLast(2).map(authorOf)
            val idx = rest.indexOfFirst { authorOf(it) !in recent }.let { if (it in 0..6) it else 0 }
            out.add(rest.removeAt(idx))
        }
        return out
    }

    fun rankPosts(posts: List<Post>, followingIds: Set<String>, likedIds: Set<String>, seed: Int): List<Post> {
        if (posts.size < 2) return posts
        val now = System.currentTimeMillis()
        val liked = posts.filter { it.id in likedIds }
        val tags = liked.flatMap { it.hashtags }.map { it.lowercase() }.toHashSet()
        val sorted = posts.sortedByDescending { p ->
            val overlap = if (tags.isEmpty()) 0 else p.hashtags.count { it.lowercase() in tags }
            score(p.createdAt, now, p.likesCount, p.comments.size, 0, p.author.id in followingIds, overlap, p.id, seed)
        }
        return diversify(sorted) { it.author.id }
    }

    fun rankClips(clips: List<Clip>, followingIds: Set<String>, likedIds: Set<String>, seed: Int): List<Clip> {
        if (clips.size < 2) return clips
        val now = System.currentTimeMillis()
        val tags = clips.filter { it.id in likedIds }
            .flatMap { tagRegex.findAll(it.caption).map { m -> m.value.lowercase() }.toList() }.toHashSet()
        val sorted = clips.sortedByDescending { c ->
            val overlap = if (tags.isEmpty()) 0 else tagRegex.findAll(c.caption).count { it.value.lowercase() in tags }
            score(c.createdAt, now, c.likesCount, c.commentsCount, c.sharesCount, c.author.id in followingIds, overlap, c.id, seed)
        }
        return diversify(sorted) { it.author.id }
    }
}
