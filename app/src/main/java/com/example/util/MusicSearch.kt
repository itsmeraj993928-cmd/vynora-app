package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** A song the user can attach to a story. [previewUrl] is a 30 second online preview clip. */
data class MusicTrack(
    val title: String,
    val artist: String,
    val previewUrl: String,
    val artworkUrl: String = ""
) {
    val label: String get() = if (artist.isBlank()) title else "$title • $artist"

    /** Packed into the single "music" string that travels through the upload pipeline. */
    fun pack(): String = "$label$SEP$previewUrl"

    companion object {
        private const val SEP = "\u001F"

        /** Returns Pair(display label, preview url or ""). Works for plain labels like "Original audio" too. */
        fun unpack(packed: String?): Pair<String, String> {
            if (packed.isNullOrBlank()) return "" to ""
            val i = packed.indexOf(SEP)
            return if (i < 0) packed to "" else packed.substring(0, i) to packed.substring(i + 1)
        }
    }
}

/** Online song search using the free iTunes Search API (no API key needed). */
object MusicSearch {
    suspend fun search(query: String, limit: Int = 25): Result<List<MusicTrack>> = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext Result.success(emptyList())
        try {
            val url = URL(
                "https://itunes.apple.com/search?media=music&entity=song&limit=$limit&country=IN&term=" +
                    URLEncoder.encode(q, "UTF-8")
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                requestMethod = "GET"
            }
            try {
                if (conn.responseCode != 200) return@withContext Result.failure(Exception("Search failed (${conn.responseCode})"))
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONObject(body).optJSONArray("results")
                val out = ArrayList<MusicTrack>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val preview = o.optString("previewUrl", "")
                        if (preview.isBlank()) continue
                        out.add(
                            MusicTrack(
                                title = o.optString("trackName", "Unknown"),
                                artist = o.optString("artistName", ""),
                                previewUrl = preview,
                                artworkUrl = o.optString("artworkUrl100", "")
                            )
                        )
                    }
                }
                Result.success(out)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Quick filter chips shown above the suggestions. */
    val moods = listOf("Trending", "Bollywood", "Punjabi", "Romantic", "Lofi", "Devotional", "Party", "Sad")

    private fun httpGet(u: String): String? {
        val conn = (URL(u).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000; readTimeout = 10_000; requestMethod = "GET"
        }
        return try {
            if (conn.responseCode != 200) null else conn.inputStream.bufferedReader().use { it.readText() }
        } finally { conn.disconnect() }
    }

    /**
     * Songs shown before the user types anything.
     * Uses the iTunes "Top songs in India" chart; falls back to a few popular searches.
     */
    suspend fun suggestions(): List<MusicTrack> = withContext(Dispatchers.IO) {
        val fromChart = try { topChartIndia() } catch (e: Exception) { emptyList() }
        if (fromChart.isNotEmpty()) return@withContext fromChart
        val out = LinkedHashMap<String, MusicTrack>()
        for (q in listOf("Arijit Singh", "Bollywood hits", "Punjabi hits", "Hindi romantic")) {
            val r = search(q, 10).getOrNull() ?: continue
            r.forEach { out.putIfAbsent(it.previewUrl, it) }
        }
        out.values.toList()
    }

    private fun topChartIndia(): List<MusicTrack> {
        val body = httpGet("https://itunes.apple.com/in/rss/topsongs/limit=30/json") ?: return emptyList()
        val entries = JSONObject(body).optJSONObject("feed")?.optJSONArray("entry") ?: return emptyList()
        val out = ArrayList<MusicTrack>()
        for (i in 0 until entries.length()) {
            val e = entries.getJSONObject(i)
            val title = e.optJSONObject("im:name")?.optString("label") ?: continue
            val artist = e.optJSONObject("im:artist")?.optString("label") ?: ""
            val images = e.optJSONArray("im:image")
            val art = if (images != null && images.length() > 0) images.getJSONObject(images.length() - 1).optString("label") else ""
            var preview = ""
            val links = e.opt("link")
            val linkList = when (links) {
                is org.json.JSONArray -> (0 until links.length()).mapNotNull { links.optJSONObject(it) }
                is JSONObject -> listOf(links)
                else -> emptyList()
            }
            for (l in linkList) {
                val a = l.optJSONObject("attributes") ?: continue
                if (a.optString("im:assetType") == "preview" || a.optString("type").startsWith("audio")) {
                    preview = a.optString("href"); break
                }
            }
            if (preview.isNotBlank()) out.add(MusicTrack(title, artist, preview, art))
        }
        return out
    }
}
