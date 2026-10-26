package com.example.screens

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.service.GeminiService
import com.example.ui.theme.VynoraNeonCyan
import com.example.ui.theme.VynoraNeonPink
import kotlinx.coroutines.launch
import java.io.File

private const val DAILY_LIMIT = 5

private fun todayKey(): String = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())

/** AI tab: write an idea in any language, get a 5 second vertical video, then post it as a reel. */
@Composable
fun AiVideoScreen(
    isDarkMode: Boolean,
    onPostAsReel: (uri: Uri, caption: String) -> Unit,
    onToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val bg = if (isDarkMode) Color(0xFF050509) else Color(0xFFF8FAFC)
    val fg = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val sub = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val prefs = remember { ctx.getSharedPreferences("vynora_ai_video", android.content.Context.MODE_PRIVATE) }

    var prompt by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<File?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val photoPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { u: Uri? -> if (u != null) photoUri = u }

    fun usedToday(): Int = if (prefs.getString("day", "") == todayKey()) prefs.getInt("count", 0) else 0

    fun generate() {
        val pu = photoUri
        if (pu == null && prompt.isBlank()) { onToast("Write an idea or pick a photo first"); return }
        if (usedToday() >= DAILY_LIMIT) { onToast("Daily limit reached ($DAILY_LIMIT videos per day). Try tomorrow."); return }
        busy = true
        error = null
        result = null
        scope.launch {
            val bmp: android.graphics.Bitmap? = if (pu != null) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { decodeScaled(ctx, pu, 1600) }
            } else {
                val en = GeminiService.toEnglish(prompt.trim())
                GeminiService.textToImage(en)
            }
            if (bmp == null) {
                busy = false
                error = if (pu != null) "Couldn't read this photo." else "AI picture service is busy right now. Try again, or pick your own photo."
                return@launch
            }
            val out = File(ctx.cacheDir, "vn_ai_${System.currentTimeMillis()}.mp4")
            val ok = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                com.example.util.KenBurnsVideo.render(bmp, out, (System.currentTimeMillis() % 4).toInt())
            }
            busy = false
            if (ok) {
                prefs.edit().putString("day", todayKey()).putInt("count", usedToday() + 1).apply()
                result = out
            } else error = "Couldn't make the video on this phone. Try another photo."
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(bg)
            .verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("AI Video ✨", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = fg)
        Text(
            "Write an idea in any language (Hindi, English, Urdu, Tamil…) and AI makes a picture, or pick your own photo. It becomes a 5 second vertical video with smooth motion.",
            color = sub, fontSize = 13.sp, lineHeight = 18.sp
        )
        OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it.take(300) },
            placeholder = { Text("e.g. बारिश में चाय पीता हुआ लड़का, sunset, cinematic") },
            modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
            enabled = !busy,
            shape = RoundedCornerShape(14.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = {
                photoPicker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
            }, enabled = !busy) { Text(if (photoUri == null) "Pick my photo" else "Change photo", color = fg) }
            if (photoUri != null) {
                Text("Photo selected ✓", color = VynoraNeonCyan, fontSize = 13.sp)
                androidx.compose.material3.TextButton(onClick = { photoUri = null }, enabled = !busy) { Text("Remove", color = sub) }
            }
        }
        Button(
            onClick = { generate() },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink)
        ) { Text(if (busy) "Making your video…" else "Generate video", color = Color.White, fontWeight = FontWeight.Bold) }
        Text("Today: ${usedToday()}/$DAILY_LIMIT videos used", color = sub, fontSize = 12.sp)

        if (busy) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = VynoraNeonCyan, strokeWidth = 3.dp)
                Text("About 10-30 seconds. Keep this screen open…", color = sub, fontSize = 13.sp)
            }
        }
        error?.let { Text(it, color = Color(0xFFF87171), fontSize = 13.sp) }

        result?.let { f ->
            Box(
                modifier = Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { c ->
                        VideoView(c).apply {
                            setVideoPath(f.absolutePath)
                            setOnPreparedListener { mp -> mp.isLooping = true; start() }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        onPostAsReel(cacheFileUri(ctx, f), prompt.trim())
                        result = null
                        prompt = ""
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VynoraNeonPink)
                ) { Text("Post as reel", color = Color.White, fontWeight = FontWeight.Bold) }
                OutlinedButton(
                    onClick = { result = null },
                    modifier = Modifier.weight(1f).height(48.dp)
                ) { Text("Discard", color = fg) }
            }
        }
        Spacer(modifier = Modifier.height(110.dp))
    }
}
