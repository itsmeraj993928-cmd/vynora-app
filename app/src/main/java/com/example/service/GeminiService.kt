package com.example.service

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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

    private val CHAT_MODELS = listOf("gemini-2.5-flash-lite", "gemini-2.5-flash", "gemini-2.0-flash", "gemini-2.0-flash-lite", "gemini-flash-latest")
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
    /** true = adult/graphic image, false = fine, null = could not check (AI off or busy). */
    fun canCheckImages(): Boolean = key().isNotBlank()

    suspend fun isUnsafeImage(jpeg: ByteArray): Boolean? = withContext(Dispatchers.IO) {
        val apiKey = key()
        if (apiKey.isBlank()) return@withContext null
        val b64 = android.util.Base64.encodeToString(jpeg, android.util.Base64.NO_WRAP)
        val prompt = "You are a strict content moderator for a social app used by teenagers. Look at this image. " +
                "Does it show nudity or partial nudity, a sexual act, sexually explicit or suggestive content, " +
                "lingerie/underwear-only or very revealing sexual poses, or extreme gore/violence? " +
                "When in doubt, say UNSAFE. Answer with exactly one word: UNSAFE or SAFE."
        for (model in listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-2.5-flash-lite")) {
            try {
                val parts = JSONArray()
                    .put(JSONObject().put("inline_data", JSONObject().put("mime_type", "image/jpeg").put("data", b64)))
                    .put(JSONObject().put("text", prompt))
                val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", parts)))
                    .put("generationConfig", JSONObject().put("maxOutputTokens", 10).put("thinkingConfig", JSONObject().put("thinkingBudget", 0)))
                val req = Request.Builder().url("$BASE_URL/$model:generateContent?key=$apiKey")
                    .post(body.toString().toRequestBody("application/json".toMediaType())).build()
                val verdict: Boolean? = client.newBuilder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS).build().newCall(req).execute().use { r ->
                    if (!r.isSuccessful) {
                        val err = r.body?.string().orEmpty().lowercase()
                        // the AI refused the picture for safety / policy reasons
                        return@use if (Regex("safety|blocked|prohibit|policy|sexual|explicit").containsMatchIn(err)) true else null
                    }
                    val json = JSONObject(r.body?.string() ?: return@use null)
                    // Google refusing to look at the picture is itself a strong warning sign
                    if (json.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty().isNotBlank()) return@use true
                    val cand = json.optJSONArray("candidates")?.optJSONObject(0) ?: return@use true // no answer at all = Google refused the picture
                    val text = cand.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text").orEmpty().uppercase()
                    val reason = cand.optString("finishReason")
                    when {
                        text.contains("UNSAFE") -> true
                        text.contains("SAFE") -> false
                        reason.isNotBlank() && reason != "STOP" && reason != "MAX_TOKENS" -> true // SAFETY, PROHIBITED_CONTENT, IMAGE_SAFETY ...
                        else -> null
                    }
                }
                if (verdict != null) return@withContext verdict
            } catch (e: Exception) { }
        }
        null
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

    /** Chat with the VYNORA AI assistant, streaming: onPartial gets the full text so far, many times. history = (isUser, text), oldest first. */
    suspend fun chatStream(history: List<Pair<Boolean, String>>, onPartial: (String) -> Unit): ChatResult = withContext(Dispatchers.IO) {
        val apiKey = key()
        if (apiKey.isBlank()) return@withContext ChatResult(null, "AI key is missing in this app build. Add the GEMINI_API_KEY secret on GitHub and rebuild.")
        if (history.isEmpty()) return@withContext ChatResult(null, "Nothing to send.")
        val contents = JSONArray()
        history.takeLast(16).forEach { (isUser, text) ->
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
        // Backup AI runs at the same time as Gemini, so the wait is the faster of the two, not the sum
        val backupDef = async(Dispatchers.IO) { backupChat(history, system) }
        var retryable = true
        val t0 = System.currentTimeMillis()
        val chatClient = client.newBuilder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(15, TimeUnit.SECONDS).build()
        for (round in 0 until 1) {
        if (round > 0) { if (!retryable) break; kotlinx.coroutines.delay(1000L) }
        retryable = false
        for (model in CHAT_MODELS) {
            if (System.currentTimeMillis() - t0 > 20000) break
            if (backupDef.isCompleted) {
                val b = backupDef.await()
                if (b != null) { withContext(Dispatchers.Main) { onPartial(b) }; return@withContext ChatResult(b, null) }
            }
            try {
                val body = JSONObject()
                    .put("system_instruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
                    .put("contents", contents)
                    .put(
                        "generationConfig",
                        JSONObject().put("temperature", 1.0).put("maxOutputTokens", 600)
                            .put("thinkingConfig", JSONObject().put("thinkingBudget", 0))
                    )
                val req = Request.Builder()
                    .url("$BASE_URL/$model:streamGenerateContent?alt=sse&key=$apiKey")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                var gotAny = false
                chatClient.newCall(req).execute().use { r ->
                    if (!r.isSuccessful) {
                        val txt = r.body?.string() ?: ""
                        val msg = try { JSONObject(txt).optJSONObject("error")?.optString("message") ?: "" } catch (e: Exception) { "" }
                        if (r.code == 429 || r.code == 500 || r.code == 502 || r.code == 503 || r.code == 504) retryable = true
                        lastError = when (r.code) {
                            503, 500, 502, 504 -> "AI is very busy right now. Please send again in a few seconds."
                            429 -> "AI quota is full for now (too many requests). Try again in a minute."
                            400 -> "AI key problem: " + msg.take(120)
                            403 -> "AI key is blocked or not allowed: " + msg.take(120)
                            else -> "AI error ${r.code}: " + msg.take(120)
                        }
                        return@use
                    }
                    val source = r.body?.source() ?: return@use
                    val acc = StringBuilder()
                    while (true) {
                        val line = source.readUtf8Line() ?: break
                        if (!line.startsWith("data:")) continue
                        val json = line.removePrefix("data:").trim()
                        if (json.isEmpty()) continue
                        val parts = try {
                            JSONObject(json).optJSONArray("candidates")?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                        } catch (e: Exception) { null } ?: continue
                        for (i in 0 until parts.length()) acc.append(parts.optJSONObject(i)?.optString("text") ?: "")
                        if (acc.isNotEmpty()) {
                            gotAny = true
                            val snapshot = acc.toString()
                            withContext(Dispatchers.Main) { onPartial(snapshot) }
                        }
                    }
                    if (gotAny) return@withContext ChatResult(acc.toString().trim(), null)
                    lastError = "AI gave an empty answer. Try rephrasing."
                }
            } catch (e: Exception) {
                lastError = "No internet or AI is unreachable (" + (e.javaClass.simpleName) + ")."
                retryable = true
            }
        }
        }
        // Backup AI (no key needed) when Gemini is busy/blocked/unreachable
        val backup = backupDef.await()
        if (backup != null) {
            withContext(Dispatchers.Main) { onPartial(backup) }
            return@withContext ChatResult(backup, null)
        }
        ChatResult(null, lastError)
    }

    /** Free backup chat provider (OpenAI-compatible, no API key). Returns null on any failure. */
    private fun backupChat(history: List<Pair<Boolean, String>>, system: String): String? {
        return try {
            val msgs = JSONArray().put(JSONObject().put("role", "system").put("content", system))
            history.takeLast(12).forEach { (isUser, text) ->
                msgs.put(JSONObject().put("role", if (isUser) "user" else "assistant").put("content", text))
            }
            val body = JSONObject().put("model", "openai").put("messages", msgs).put("temperature", 1.0)
            val req = Request.Builder()
                .url("https://text.pollinations.ai/openai")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
            val c = client.newBuilder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS).build()
            c.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return null
                val txt = JSONObject(r.body?.string() ?: return null)
                    .optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.optString("content")?.trim()
                if (txt.isNullOrBlank()) null else txt
            }
        } catch (e: Exception) { null }
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
