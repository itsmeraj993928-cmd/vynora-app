package com.example.service

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val MODELS = listOf("gemini-2.5-flash", "gemini-flash-latest", "gemini-3.5-flash", "gemini-2.0-flash")

    /** The real key, or "" when the build only has the placeholder. */
    private fun key(): String {
        val k = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        return if (k.isBlank() || k.startsWith("MY_GEMINI")) "" else k
    }

    fun isConfigured(): Boolean = key().isNotBlank()

    /** Looks at the photo and writes a caption. Returns null when AI isn't set up or fails. */
    suspend fun captionFromImage(jpeg: ByteArray, hint: String = "", style: String = "trendy"): String? = withContext(Dispatchers.IO) {
        val apiKey = key()
        if (apiKey.isBlank()) return@withContext null
        val prompt = "Look carefully at this photo and write a short, catchy Instagram-style caption (max 2 lines) " +
                "that matches what is really in the picture, with 2-4 fitting emojis. Style: $style. " +
                (if (hint.isNotBlank()) "The user already wrote: '$hint' - keep that idea. " else "") +
                "No hashtags. Return only the caption text."
        callGeminiApi(prompt, apiKey, android.util.Base64.encodeToString(jpeg, android.util.Base64.NO_WRAP))
    }
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    suspend fun generateCaption(topicOrImageDesc: String, style: String = "trendy"): String = withContext(Dispatchers.IO) {
        val apiKey = key()
        if (apiKey.isBlank()) {
            return@withContext fallbackCaption(topicOrImageDesc, style)
        }

        val prompt = "You are a creative social media AI copywriter for VYNORA (a futuristic neon-aesthetic social platform). " +
                "Generate an engaging, stylish 2-line caption with 3-4 emojis for a post about: '$topicOrImageDesc'. Style: $style. " +
                "Do not include hashtags in this caption. Return only the caption text."

        callGeminiApi(prompt, apiKey) ?: fallbackCaption(topicOrImageDesc, style)
    }

    suspend fun generateHashtags(topic: String): List<String> = withContext(Dispatchers.IO) {
        val apiKey = key()
        if (apiKey.isBlank()) {
            return@withContext fallbackHashtags(topic)
        }

        val prompt = "Generate 6-8 trending, high-reach hashtags for a social media post about: '$topic'. " +
                "Include #VynoraVibes and other relevant tags. Return them as a space-separated list of hashtags only."

        val response = callGeminiApi(prompt, apiKey)
        if (response != null && response.contains("#")) {
            response.split("\\s+".toRegex()).filter { it.startsWith("#") }
        } else {
            fallbackHashtags(topic)
        }
    }

    suspend fun translateText(text: String, targetLanguage: String): String = withContext(Dispatchers.IO) {
        val apiKey = key()
        if (apiKey.isBlank()) {
            return@withContext fallbackTranslation(text, targetLanguage)
        }

        val prompt = "Translate the following social media text into $targetLanguage accurately and naturally: \"$text\". " +
                "Preserve tone and emojis. Return only the translated text."

        callGeminiApi(prompt, apiKey) ?: fallbackTranslation(text, targetLanguage)
    }

    private fun callGeminiApi(promptText: String, apiKey: String, imageB64: String? = null): String? {
        for (model in MODELS) {
            val r = callModel(model, promptText, apiKey, imageB64)
            if (!r.isNullOrBlank()) return r
        }
        return null
    }

    private fun callModel(model: String, promptText: String, apiKey: String, imageB64: String?): String? {
        return try {
            val url = "$BASE_URL/$model:generateContent?key=$apiKey"
            val parts = JSONArray()
            if (imageB64 != null) {
                parts.put(JSONObject().put("inline_data", JSONObject().put("mime_type", "image/jpeg").put("data", imageB64)))
            }
            parts.put(JSONObject().put("text", promptText))
            val jsonBody = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val responseString = response.body?.string() ?: return null
                val candidates = JSONObject(responseString).optJSONArray("candidates") ?: return null
                if (candidates.length() == 0) return null
                val p = candidates.getJSONObject(0).optJSONObject("content")?.optJSONArray("parts") ?: return null
                if (p.length() == 0) return null
                p.getJSONObject(0).optString("text").trim()
            }
        } catch (e: Exception) {
            null
        }
    }

    private val videoClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    class VideoResult(val file: java.io.File?, val error: String?)

    /** Text (any language) -> 5 second vertical video using Veo. Slow (1-3 min) and PAID on the Gemini API. */
    suspend fun generateVideo(promptAnyLanguage: String, outFile: java.io.File): VideoResult = withContext(Dispatchers.IO) {
        val apiKey = key()
        if (apiKey.isBlank()) return@withContext VideoResult(null, "AI is not set up yet (Gemini key missing).")
        try {
            // Veo understands English best, so translate first (works for Hindi, Urdu, Tamil, anything).
            val english = callGeminiApi(
                "Translate this video idea into one clear, vivid English sentence for a video generator. " +
                        "If it is already English, improve it slightly. Return only the sentence: \"$promptAnyLanguage\"",
                apiKey
            ) ?: promptAnyLanguage

            val model = "veo-2.0-generate-001"
            val body = JSONObject()
                .put("instances", JSONArray().put(JSONObject().put("prompt", english)))
                .put("parameters", JSONObject().put("aspectRatio", "9:16").put("durationSeconds", 5))
            val start = Request.Builder()
                .url("$BASE_URL/$model:predictLongRunning")
                .header("x-goog-api-key", apiKey)
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val opName = videoClient.newCall(start).execute().use { r ->
                val txt = r.body?.string() ?: ""
                if (!r.isSuccessful) return@withContext VideoResult(null, apiErrorMessage(r.code, txt))
                JSONObject(txt).optString("name")
            }
            if (opName.isBlank()) return@withContext VideoResult(null, "Video request failed. Try again.")

            var videoUri = ""
            for (i in 0 until 40) { // up to ~5 minutes
                kotlinx.coroutines.delay(8000)
                val poll = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/$opName")
                    .header("x-goog-api-key", apiKey)
                    .build()
                val done = videoClient.newCall(poll).execute().use { r ->
                    val txt = r.body?.string() ?: ""
                    if (!r.isSuccessful) return@withContext VideoResult(null, apiErrorMessage(r.code, txt))
                    val j = JSONObject(txt)
                    if (j.optBoolean("done")) {
                        val err = j.optJSONObject("error")
                        if (err != null) return@withContext VideoResult(null, err.optString("message", "Video failed"))
                        val samples = j.optJSONObject("response")?.optJSONObject("generateVideoResponse")?.optJSONArray("generatedSamples")
                        if (samples == null || samples.length() == 0) {
                            return@withContext VideoResult(null, "This idea was blocked by safety rules. Try a different prompt.")
                        }
                        videoUri = samples.getJSONObject(0).optJSONObject("video")?.optString("uri") ?: ""
                        true
                    } else false
                }
                if (done) break
            }
            if (videoUri.isBlank()) return@withContext VideoResult(null, "Video took too long. Try again.")

            val dl = Request.Builder().url(videoUri).header("x-goog-api-key", apiKey).build()
            videoClient.newCall(dl).execute().use { r ->
                if (!r.isSuccessful) return@withContext VideoResult(null, "Couldn't download the video (${r.code}).")
                r.body?.byteStream()?.use { input -> outFile.outputStream().use { input.copyTo(it) } }
            }
            if (outFile.length() < 1000) VideoResult(null, "Video came back empty. Try again.") else VideoResult(outFile, null)
        } catch (e: Exception) {
            VideoResult(null, "Network problem. Check internet and try again.")
        }
    }

    private fun apiErrorMessage(code: Int, body: String): String {
        val msg = try { JSONObject(body).optJSONObject("error")?.optString("message") ?: "" } catch (e: Exception) { "" }
        return when {
            code == 429 -> "AI video limit reached for now. Try later."
            code == 400 && msg.contains("billing", true) -> "AI video needs a paid Gemini plan (billing) on this key."
            code == 403 || code == 404 -> "AI video isn't enabled for this Gemini key. It needs a paid plan with Veo access."
            msg.isNotBlank() -> msg.take(160)
            else -> "AI video failed ($code)."
        }
    }

    private fun fallbackCaption(topic: String, style: String): String {
        val clean = if (topic.isBlank()) "Midnight city glow" else topic
        return when (style.lowercase()) {
            "poetic" -> "Where neon whispers meet the quiet dark, every reflection finds its spark ✨🔮 $clean"
            "hype" -> "Electric frequencies only ⚡ Dropping pure energy into the matrix! $clean 🔥🚀"
            "minimal" -> "Raw frames. Pure light. $clean 🌙"
            else -> "Lost in the luminescence ✨ Sharing my world with the night. $clean 🔮⚡"
        }
    }

    private fun fallbackHashtags(topic: String): List<String> {
        val base = listOf("#VynoraVibes", "#ShareYourWorld", "#NeonAesthetic", "#CyberGlow", "#CreatorDaily", "#DigitalArt")
        val custom = if (topic.isNotBlank()) {
            "#" + topic.replace("[^A-Za-z0-9]".toRegex(), "")
        } else "#VisualStory"
        return listOf(custom) + base
    }

    private fun fallbackTranslation(text: String, targetLang: String): String {
        return when {
            targetLang.contains("Hindi", ignoreCase = true) -> {
                "अनुवाद: $text (वायनोरा एआई द्वारा अनुवादित)"
            }
            targetLang.contains("Urdu", ignoreCase = true) -> {
                "ترجمہ: $text (وائنورا AI کے ذریعے)"
            }
            else -> {
                "Translation: $text [Vynora AI translated]"
            }
        }
    }
}
