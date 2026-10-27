package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
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
        if (tab == 0) AiChatPane(isDarkMode, onToast, { tab = 1 }, Modifier.weight(1f))
        else AiVideoScreen(isDarkMode = isDarkMode, onPostAsReel = onPostAsReel, onToast = onToast, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AiChatPane(isDarkMode: Boolean, onToast: (String) -> Unit, onOpenVideo: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var chatId by remember { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    val msgs = remember { mutableStateListOf<AiMsg>() }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf<List<SavedChat>>(emptyList()) }
    val drawerState = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val listState = rememberLazyListState()
    val fg = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val sub = Color(0xFF94A3B8)
    val surface = if (isDarkMode) Color(0xFF1E2235) else Color(0xFFEFF1F5)
    val userBubble = if (isDarkMode) Color(0xFF2A2F45) else Color(0xFFE8EAF0)

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
        input = ""
    }

    fun send() {
        val t = input.trim()
        if (t.isBlank() || busy) return
        input = ""
        msgs.add(AiMsg(true, t))
        val history = msgs.map { it.fromUser to it.text }
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

    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        modifier = modifier,
        drawerContent = {
            androidx.compose.material3.ModalDrawerSheet {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("VYNORA AI", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp), color = VynoraNeonPink,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable {
                            newChat(); scope.launch { drawerState.close() }
                        }
                    ) { Text("＋  New chat", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(14.dp)) }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("History", color = sub, fontSize = 12.sp)
                    if (saved.isEmpty()) Text("No saved chats yet.", color = sub, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(saved.size) { i ->
                            val c = saved[i]
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    if (!busy) {
                                        saveCurrent()
                                        chatId = c.id
                                        msgs.clear()
                                        msgs.addAll(c.msgs.filter { it.fromUser || it.text != GREETING })
                                        scope.launch { drawerState.close() }
                                    }
                                }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(c.title, fontWeight = FontWeight.SemiBold, maxLines = 1, fontSize = 14.sp)
                                    Text(
                                        java.text.SimpleDateFormat("d MMM, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(c.updatedAt)),
                                        fontSize = 11.sp, color = sub
                                    )
                                }
                                Text("🗑", fontSize = 18.sp, modifier = Modifier.clickable {
                                    ChatStore.delete(ctx, c.id)
                                    saved = ChatStore.load(ctx)
                                    if (c.id == chatId) { chatId = java.util.UUID.randomUUID().toString(); msgs.clear() }
                                }.padding(8.dp))
                            }
                        }
                    }
                }
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // top bar: menu, title, new chat
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape, color = surface,
                    modifier = Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).clickable {
                        saved = ChatStore.load(ctx); scope.launch { drawerState.open() }
                    }
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.Menu, contentDescription = "History", tint = fg) } }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("VYNORA AI", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = fg)
                }
                Surface(
                    shape = androidx.compose.foundation.shape.CircleShape, color = surface,
                    modifier = Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape).clickable { newChat() }
                ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Edit, contentDescription = "New chat", tint = fg) } }
            }

            if (msgs.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("What can I help with?", fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = fg)
                }
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    @Composable
                    fun Suggestion(label: String, onClick: () -> Unit) {
                        Text(label, color = fg, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 14.dp))
                    }
                    Suggestion("🎬  Create a video") { onOpenVideo() }
                    Suggestion("✍️  Write a caption") { input = "Write a catchy caption for my post about " }
                    Suggestion("🌐  Translate something") { input = "Translate to English: " }
                    Suggestion("💡  Give me ideas") { input = "Give me 5 post ideas about " }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    items(msgs.size) { i ->
                        val m = msgs[i]
                        if (m.fromUser) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                Surface(shape = RoundedCornerShape(20.dp), color = userBubble, modifier = Modifier.widthIn(max = 290.dp)) {
                                    Text(m.text, color = fg, fontSize = 15.sp, lineHeight = 21.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                                }
                            }
                        } else {
                            Text(m.text, color = fg, fontSize = 15.sp, lineHeight = 23.sp, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            // composer pill
            Surface(
                shape = RoundedCornerShape(28.dp), color = surface,
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 100.dp)
            ) {
                Row(modifier = Modifier.padding(start = 4.dp, end = 6.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onOpenVideo() }) { Icon(Icons.Outlined.Add, contentDescription = "Create video", tint = fg) }
                    androidx.compose.foundation.text.BasicTextField(
                        value = input,
                        onValueChange = { input = it.take(1000) },
                        textStyle = androidx.compose.ui.text.TextStyle(color = fg, fontSize = 16.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(VynoraNeonPink),
                        maxLines = 5,
                        modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                        decorationBox = { inner ->
                            Box {
                                if (input.isEmpty()) Text("Ask VYNORA AI", color = sub, fontSize = 16.sp)
                                inner()
                            }
                        }
                    )
                    val canSend = input.isNotBlank() && !busy
                    Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = if (canSend) VynoraNeonPink else (if (isDarkMode) Color(0xFF3A3F55) else Color(0xFFCBD5E1)),
                        modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape).clickable(enabled = canSend) { send() }
                    ) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Filled.ArrowUpward, contentDescription = "Send", tint = Color.White) } }
                }
            }
        }
    }
}
