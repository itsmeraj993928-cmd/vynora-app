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

    private val CHAT_MODELS = listOf("gemini-2.5-flash-lite", "gemini-2.5-flash", "gemini-flash-latest")
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

    /** 3 short reply ideas for a chat message, in the same language as the message. Empty if AI isn't set up. */
    suspend fun suggestReplies(lastMessage: String): List<String> = withContext(Dispatchers.IO) {
        val apiKey = key()
        if (apiKey.isBlank()) return@withContext emptyList()
        val out = callGeminiApi(
            "A friend sent this chat message: \"$lastMessage\". Write 3 short, friendly, natural reply options " +
                    "in the SAME language and script as the message. One reply per line, no numbering, no quotes, max 12 words each.",
            apiKey
        ) ?: return@withContext emptyList()
        out.lines().map { it.trim().trimStart('-', '*', '•', ' ').trim() }.filter { it.isNotBlank() }.take(3)
    }

    class ChatResult(val text: String?, val error: String?)

    /** Chat with the VYNORA AI assistant. history = (isUser, text) pairs, oldest first. */
    suspend fun chat(history: List<Pair<Boolean, String>>): ChatResult = withContext(Dispatchers.IO) {
        val apiKey = key()
        if (apiKey.isBlank()) return@withContext ChatResult(null, "AI key is missing in this app build. Add the GEMINI_API_KEY secret on GitHub and rebuild.")
        if (history.isEmpty()) return@withContext ChatResult(null, "Nothing to send.")
        val contents = JSONArray()
        history.takeLast(20).forEach { (isUser, text) ->
            contents.put(
                JSONObject().put("role", if (isUser) "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", text)))
            )
        }
        val system = "You are VYNORA AI, a warm, funny, friendly buddy inside the VYNORA social app. " +
                "Talk exactly like a real person chatting on WhatsApp: casual, natural and short (1-3 sentences most of the time). " +
                "Copy the user's language, script and style: if they write Hinglish, reply in Hinglish; Hindi in Hindi; English in English; Urdu in Urdu. " +
                "Match their mood: joke when they joke, be gentle when they are sad, be excited when they are excited. " +
                "Use everyday words, like 'yaar', 'arre', 'haha' when it fits, and at most one or two emojis. " +
                "Never sound like a robot: no bullet lists, no headings, no 'As an AI', no long lectures, no repeating the question back. " +
                "Ask a small follow-up question sometimes, like a friend would. " +
                "You can help with captions, ideas, replies, translations and any question, but keep it conversational. " +
                "Be honest: if someone sincerely asks whether you are an AI or a human, say you are VYNORA AI. " +
                "Do not produce harmful, sexual or hateful content, and if someone seems to be in real danger or distress, be kind and suggest talking to someone they trust or a local helpline."
        var lastError = "No answer from AI."
        for (model in CHAT_MODELS) {
            try {
                val body = JSONObject()
                    .put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
                    .put("contents", contents)
                    .put(
                        "generationConfig",
                        JSONObject().put("temperature", 1.0).put("maxOutputTokens", 1024)
                            // no hidden "thinking": faster replies and no cut-off answers
                            .put("thinkingConfig", JSONObject().put("thinkingBudget", 0))
                    )
                val req = Request.Builder()
                    .url("$BASE_URL/$model:generateContent?key=$apiKey")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(req).execute().use { r ->
                    val txt = r.body?.string() ?: ""
                    if (!r.isSuccessful) {
                        val msg = try { JSONObject(txt).optJSONObject("error")?.optString("message") ?: "" } catch (e: Exception) { "" }
                        lastError = when (r.code) {
                            429 -> "AI quota is full for now (too many requests). Try again in a minute."
                            400 -> "AI key problem: " + msg.take(120)
                            403 -> "AI key is blocked or not allowed: " + msg.take(120)
                            else -> "AI error ${r.code}: " + msg.take(120)
                        }
                        return@use
                    }
                    val parts = JSONObject(txt).optJSONArray("candidates")?.optJSONObject(0)
                        ?.optJSONObject("content")?.optJSONArray("parts")
                    val out = parts?.optJSONObject(0)?.optString("text")?.trim()
                    if (!out.isNullOrBlank()) return@withContext ChatResult(out, null)
                    lastError = "AI gave an empty answer. Try rephrasing."
                }
            } catch (e: Exception) {
                lastError = "No internet or AI is unreachable."
            }
        }
        ChatResult(null, lastError)
    }

    /** Any language -> one English sentence (so image generators understand it). Returns the original if AI isn't set up. */
    suspend fun toEnglish(text: String): String = withContext(Dispatchers.IO) {
        val apiKey = key()
        if (apiKey.isBlank()) return@withContext text
        callGeminiApi(
            "Translate this idea into one clear, vivid English sentence describing a beautiful cinematic photo. " +
                    "If it is already English, improve it slightly. Return only the sentence: \"$text\"",
            apiKey
        ) ?: text
    }

    private val imageClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /** Free text -> picture (Pollinations, no key). Returns null if the service is down or refuses. */
    suspend fun textToImage(englishPrompt: String): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = "https://image.pollinations.ai/prompt/" + android.net.Uri.encode(englishPrompt + ", vertical, high quality") +
                    "?width=720&height=1280&nologo=true&seed=" + (System.currentTimeMillis() % 100000)
            imageClient.newCall(Request.Builder().url(url).build()).execute().use { r ->
                if (!r.isSuccessful) return@withContext null
                val bytes = r.body?.bytes() ?: return@withContext null
                android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) { null }
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
