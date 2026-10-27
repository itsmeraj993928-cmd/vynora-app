package com.example.util

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Free stock videos from Pixabay (free to use). Credit to the creator is added in each caption. */
object StockVideoService {
    class Video(val id: Long, val link: String, val creator: String, val thumb: String = "")

    val TOPICS = listOf(
        "nature", "city night", "ocean waves", "mountains", "street food", "dance", "fitness", "sunset",
        "travel", "cars", "animals", "fashion", "flowers", "rain", "technology", "cooking", "sports", "festival"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    private fun key(): String {
        val k = try { BuildConfig.PIXABAY_API_KEY } catch (e: Throwable) { "" }
        return if (k.isBlank() || k.startsWith("MY_PIXABAY")) "" else k
    }

    fun isConfigured(): Boolean = key().isNotBlank()

    /** One page (up to 100) of videos for a topic, portrait ones first. Empty list on any problem. */
    suspend fun search(query: String, page: Int): List<Video> = withContext(Dispatchers.IO) {
        val k = key()
        if (k.isBlank()) return@withContext emptyList()
        try {
            val url = "https://pixabay.com/api/videos/?key=$k&q=" + URLEncoder.encode(query, "UTF-8") +
                    "&safesearch=true&per_page=100&page=$page"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return@withContext emptyList()
                val arr = JSONObject(r.body?.string() ?: return@withContext emptyList()).optJSONArray("hits")
                    ?: return@withContext emptyList()
                val portrait = ArrayList<Video>()
                val other = ArrayList<Video>()
                for (i in 0 until arr.length()) {
                    val v = arr.optJSONObject(i) ?: continue
                    val vids = v.optJSONObject("videos") ?: continue
                    val pick = vids.optJSONObject("small") ?: vids.optJSONObject("medium") ?: vids.optJSONObject("tiny") ?: continue
                    val link = pick.optString("url")
                    if (link.isBlank()) continue
                    val item = Video(v.optLong("id"), link, v.optString("user", "Pixabay"), pick.optString("thumbnail"))
                    if (pick.optInt("height") >= pick.optInt("width")) portrait.add(item) else other.add(item)
                }
                portrait + other
            }
        } catch (e: Exception) { emptyList() }
    }
}
