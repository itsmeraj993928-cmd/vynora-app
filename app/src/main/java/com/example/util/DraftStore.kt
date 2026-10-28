package com.example.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Tiny local store for unfinished posts/stories/reels (max 10). */
object DraftStore {
    data class Draft(val uri: String, val isVideo: Boolean, val mode: String, val caption: String, val filter: String, val savedAt: Long)

    private fun prefs(c: Context) = c.getSharedPreferences("vynora_drafts", Context.MODE_PRIVATE)

    fun list(c: Context): List<Draft> = try {
        val arr = JSONArray(prefs(c).getString("items", "[]"))
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Draft(o.getString("uri"), o.optBoolean("video"), o.optString("mode", "POST"), o.optString("caption"), o.optString("filter", "Original"), o.optLong("at"))
        }
    } catch (e: Exception) { emptyList() }

    fun save(c: Context, d: Draft) {
        val all = (listOf(d) + list(c).filter { it.uri != d.uri }).take(10)
        val arr = JSONArray()
        all.forEach {
            arr.put(JSONObject().put("uri", it.uri).put("video", it.isVideo).put("mode", it.mode)
                .put("caption", it.caption).put("filter", it.filter).put("at", it.savedAt))
        }
        try { prefs(c).edit().putString("items", arr.toString()).apply() } catch (e: Exception) { }
    }

    fun remove(c: Context, uri: String) {
        val arr = JSONArray()
        list(c).filter { it.uri != uri }.forEach {
            arr.put(JSONObject().put("uri", it.uri).put("video", it.isVideo).put("mode", it.mode)
                .put("caption", it.caption).put("filter", it.filter).put("at", it.savedAt))
        }
        try { prefs(c).edit().putString("items", arr.toString()).apply() } catch (e: Exception) { }
    }
}
