package com.example.util

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Free stock videos from Pexels (royalty-free). Credit to the creator is added in each caption. */
object PexelsService {
    class Video(val id: Long, val link: String, val creator: String)

    val TOPICS = listOf(
        "nature", "city night", "ocean waves", "mountains", "street food", "dance", "fitness", "sunset",
        "travel", "cars", "animals", "fashion", "flowers", "rain", "technology", "cooking", "sports", "festival"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    private fun key(): String {
        val k = try { BuildConfig.PEXELS_API_KEY } catch (e: Throwable) { "" }
        return if (k.isBlank() || k.startsWith("MY_PEXELS")) "" else k
    }

    fun isConfigured(): Boolean = key().isNotBlank()

    /** One page (up to 80) of portrait videos for a topic. Empty list on any problem. */
    suspend fun search(query: String, page: Int): List<Video> = withContext(Dispatchers.IO) {
        val k = key()
        if (k.isBlank()) return@withContext emptyList()
        try {
            val url = "https://api.pexels.com/videos/search?query=" + URLEncoder.encode(query, "UTF-8") +
                    "&orientation=portrait&size=medium&per_page=80&page=$page"
            val req = Request.Builder().url(url).header("Authorization", k).build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@withContext emptyList()
                val arr = JSONObject(r.body?.string() ?: return@withContext emptyList()).optJSONArray("videos")
                    ?: return@withContext emptyList()
                val out = ArrayList<Video>()
                for (i in 0 until arr.length()) {
                    val v = arr.optJSONObject(i) ?: continue
                    val files = v.optJSONArray("video_files") ?: continue
                    var best: String? = null
                    var bestScore = Int.MAX_VALUE
                    for (j in 0 until files.length()) {
                        val f = files.optJSONObject(j) ?: continue
                        if (f.optString("file_type") != "video/mp4") continue
                        val w = f.optInt("width"); val h = f.optInt("height")
                        if (w <= 0 || h < w || w > 1080) continue
                        val score = kotlin.math.abs(w - 720)
                        if (score < bestScore) { bestScore = score; best = f.optString("link") }
                    }
                    if (best.isNullOrBlank()) continue
                    out.add(Video(v.optLong("id"), best, v.optJSONObject("user")?.optString("name") ?: "Pexels"))
                }
                out
            }
        } catch (e: Exception) { emptyList() }
    }
}
