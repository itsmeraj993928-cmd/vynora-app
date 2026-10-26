package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    val msgs = remember { mutableStateListOf(AiMsg(false, "Hi! I'm VYNORA AI ✨ Ask me anything: captions, ideas, replies, translations. Hindi, English, Urdu, any language.")) }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val fg = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val card = if (isDarkMode) Color(0xFF1E2235) else Color(0xFFE2E8F0)

    LaunchedEffect(msgs.size, busy) { if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1) }

    fun send() {
        val t = input.trim()
        if (t.isBlank() || busy) return
        input = ""
        msgs.add(AiMsg(true, t))
        busy = true
        val history = msgs.drop(1).map { it.fromUser to it.text } // skip the greeting
        scope.launch {
            val r = GeminiService.chat(history)
            busy = false
            msgs.add(AiMsg(false, r.text ?: ("⚠️ " + (r.error ?: "AI isn't available right now."))))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
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
            if (busy) {
                item { Text("VYNORA AI is typing…", color = Color(0xFF94A3B8), fontSize = 12.sp, modifier = Modifier.padding(start = 6.dp)) }
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
