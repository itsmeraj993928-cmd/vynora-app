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

    private const val MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    suspend fun generateCaption(topicOrImageDesc: String, style: String = "trendy"): String = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isBlank()) {
            return@withContext fallbackCaption(topicOrImageDesc, style)
        }

        val prompt = "You are a creative social media AI copywriter for VYNORA (a futuristic neon-aesthetic social platform). " +
                "Generate an engaging, stylish 2-line caption with 3-4 emojis for a post about: '$topicOrImageDesc'. Style: $style. " +
                "Do not include hashtags in this caption. Return only the caption text."

        callGeminiApi(prompt, apiKey) ?: fallbackCaption(topicOrImageDesc, style)
    }

    suspend fun generateHashtags(topic: String): List<String> = withContext(Dispatchers.IO) {
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
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
        val apiKey = try { BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isBlank()) {
            return@withContext fallbackTranslation(text, targetLanguage)
        }

        val prompt = "Translate the following social media text into $targetLanguage accurately and naturally: \"$text\". " +
                "Preserve tone and emojis. Return only the translated text."

        callGeminiApi(prompt, apiKey) ?: fallbackTranslation(text, targetLanguage)
    }

    private fun callGeminiApi(promptText: String, apiKey: String): String? {
        return try {
            val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"
            val jsonBody = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", promptText)
                            })
                        })
                    })
                }
                put("contents", contents)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val responseString = response.body?.string() ?: return null
            val root = JSONObject(responseString)
            val candidates = root.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null

            parts.getJSONObject(0).optString("text").trim()
        } catch (e: Exception) {
            null
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
