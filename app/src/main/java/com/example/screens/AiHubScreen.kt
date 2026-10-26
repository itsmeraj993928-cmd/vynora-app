package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.GeminiService
import com.example.ui.theme.VynoraNeonPink
import kotlinx.coroutines.launch

private class AiMsg(val fromUser: Boolean, val text: String)

private class SavedChat(val id: String, val title: String, val updatedAt: Long, val msgs: List<AiMsg>)

/** Chats kept on this phone (last 30). */
private object ChatStore {
    private const val KEY = "chats"
    private fun prefs(ctx: android.content.Context) = ctx.getSharedPreferences("vynora_ai_chats", android.content.Context.MODE_PRIVATE)

    fun load(ctx: android.content.Context): List<SavedChat> = try {
        val arr = org.json.JSONArray(prefs(ctx).getString(KEY, "[]"))
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val ms = o.getJSONArray("m")
            SavedChat(
                o.getString("id"), o.optString("t", "Chat"), o.optLong("u", 0L),
                (0 until ms.length()).map { j -> ms.getJSONObject(j).let { AiMsg(it.getBoolean("u"), it.getString("t")) } }
            )
        }.sortedByDescending { it.updatedAt }
    } catch (e: Exception) { emptyList() }

    fun save(ctx: android.content.Context, chat: SavedChat) {
        try {
            val all = load(ctx).filter { it.id != chat.id } + chat
            val keep = all.sortedByDescending { it.updatedAt }.take(30)
            write(ctx, keep)
        } catch (e: Exception) { }
    }

    fun delete(ctx: android.content.Context, id: String) {
        try { write(ctx, load(ctx).filter { it.id != id }) } catch (e: Exception) { }
    }

    private fun write(ctx: android.content.Context, list: List<SavedChat>) {
        val arr = org.json.JSONArray()
        list.forEach { c ->
            val ms = org.json.JSONArray()
            c.msgs.forEach { ms.put(org.json.JSONObject().put("u", it.fromUser).put("t", it.text)) }
            arr.put(org.json.JSONObject().put("id", c.id).put("t", c.title).put("u", c.updatedAt).put("m", ms))
        }
        prefs(ctx).edit().putString(KEY, arr.toString()).apply()
    }
}

private const val GREETING = "Hi! I'm VYNORA AI ✨ Ask me anything: captions, ideas, replies, translations. Hindi, English, Urdu, any language."

/** AI tab: Chat with AI, or make a 5 second video. */
@Composable
fun AiHubScreen(
    isDarkMode: Boolean,
    onPostAsReel: (android.net.Uri, String) -> Unit,
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by remember { mutableStateOf(0) }
    val bg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)
    val fg = if (isDarkMode) Color.White else Color(0xFF0F172A)
    Column(modifier = modifier.fillMaxSize().background(bg)) {
        TabRow(
            selectedTabIndex = tab,
            containerColor = bg,
            contentColor = VynoraNeonPink,
            modifier = Modifier.statusBarsPadding()
        ) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("AI Chat", color = if (tab == 0) VynoraNeonPink else fg, fontWeight = FontWeight.Bold) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("AI Video", color = if (tab == 1) VynoraNeonPink else fg, fontWeight = FontWeight.Bold) })
        }
        if (tab == 0) AiChatPane(isDarkMode, onToast, Modifier.weight(1f))
        else AiVideoScreen(isDarkMode = isDarkMode, onPostAsReel = onPostAsReel, onToast = onToast, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AiChatPane(isDarkMode: Boolean, onToast: (String) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var chatId by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    val msgs = remember { mutableStateListOf(AiMsg(false, GREETING)) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<List<SavedChat>>(emptyList()) }
    val listState = rememberLazyListState()
    val fg = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val card = if (isDarkMode) Color(0xFF1E2235) else Color(0xFFE2E8F0)

    LaunchedEffect(msgs.size, busy) { if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1) }

    fun saveCurrent() {
        val firstUser = msgs.firstOrNull { it.fromUser } ?: return
        ChatStore.save(ctx, SavedChat(chatId, firstUser.text.take(32), System.currentTimeMillis(), msgs.toList()))
    }

    fun newChat() {
        if (busy) return
        saveCurrent()
        chatId = java.util.UUID.randomUUID().toString()
        msgs.clear()
        msgs.add(AiMsg(false, GREETING))
        input = ""
    }

    fun send() {
        val t = input.trim()
        if (t.isBlank() || busy) return
        input = ""
        msgs.add(AiMsg(true, t))
        val history = msgs.drop(1).map { it.fromUser to it.text } // skip the greeting
        msgs.add(AiMsg(false, "…"))
        val slot = msgs.lastIndex
        busy = true
        scope.launch {
            val r = GeminiService.chatStream(history) { partial -> if (slot < msgs.size) msgs[slot] = AiMsg(false, partial) }
            busy = false
            if (slot < msgs.size) msgs[slot] = AiMsg(false, r.text ?: ("⚠️ " + (r.error ?: "AI isn't available right now.")))
            saveCurrent()
        }
    }

    if (showHistory) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showHistory = false },
            title = { Text("Chat history") },
            text = {
                if (saved.isEmpty()) Text("No saved chats yet.")
                else LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(saved.size) { i ->
                        val c = saved[i]
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (!busy) {
                                    saveCurrent()
                                    chatId = c.id
                                    msgs.clear()
                                    msgs.add(AiMsg(false, GREETING))
                                    msgs.addAll(c.msgs)
                                    showHistory = false
                                }
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(c.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(
                                    java.text.SimpleDateFormat("d MMM, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(c.updatedAt)),
                                    fontSize = 11.sp, color = Color(0xFF94A3B8)
                                )
                            }
                            Text("🗑", fontSize = 18.sp, modifier = Modifier.clickable {
                                ChatStore.delete(ctx, c.id)
                                saved = ChatStore.load(ctx)
                                if (c.id == chatId) {
                                    chatId = java.util.UUID.randomUUID().toString()
                                    msgs.clear(); msgs.add(AiMsg(false, GREETING))
                                }
                            }.padding(8.dp))
                        }
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { showHistory = false }) { Text("Close") } }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp), color = card,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable { saved = ChatStore.load(ctx); showHistory = true }
            ) { Text("🕘 History", color = fg, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) }
            Surface(
                shape = RoundedCornerShape(18.dp), color = VynoraNeonPink,
                modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable { newChat() }
            ) { Text("＋ New chat", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)) }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(msgs.size) { i ->
                val m = msgs[i]
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (m.fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (m.fromUser) VynoraNeonPink else card,
                        modifier = Modifier.widthIn(max = 300.dp)
                    ) {
                        Text(m.text, color = if (m.fromUser) Color.White else fg, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 100.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it.take(1000) },
                placeholder = { Text("Ask VYNORA AI…", fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = VynoraNeonPink)
            )
            IconButton(onClick = { send() }, enabled = !busy) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = VynoraNeonPink)
            }
        }
    }
}
