package com.example.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.VynoraNeonPink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private class DrawStroke(val color: Int, val pts: MutableList<Offset> = mutableListOf())

private val EditorColors = listOf(0xFFFFFFFF, 0xFF000000, 0xFFFF2E93, 0xFF06B6D4, 0xFFFACC15, 0xFFEF4444, 0xFF22C55E).map { it.toInt() }

/** Draw and write text on a photo. onDone gets a new JPEG (with the edits baked in). */
@Composable
internal fun StoryEditorDialog(uri: Uri, onDone: (Uri) -> Unit, onCancel: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val bmp by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { decodeScaled(ctx, uri, 1600) }
    }
    val strokes = remember { mutableStateListOf<DrawStroke>() }
    var tick by remember { mutableStateOf(0) } // forces redraw while a stroke grows
    var color by remember { mutableStateOf(EditorColors[0]) }
    var textMode by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var textPos by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
    var showTextDialog by remember { mutableStateOf(false) }
    var box by remember { mutableStateOf(IntSize(1, 1)) }
    var saving by remember { mutableStateOf(false) }

    if (showTextDialog) {
        var t by remember { mutableStateOf(text) }
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("Add text") },
            text = { OutlinedTextField(value = t, onValueChange = { t = it.take(40) }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { text = t.trim(); showTextDialog = false; textMode = true }) { Text("Done", color = VynoraNeonPink) } },
            dismissButton = { TextButton(onClick = { showTextDialog = false }) { Text("Cancel") } }
        )
    }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(modifier = Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Cancel", color = Color.White, fontSize = 15.sp, modifier = Modifier.clickable(onClick = onCancel).padding(8.dp))
                Box(modifier = Modifier.weight(1f))
                Text("Undo", color = Color.White, fontSize = 15.sp, modifier = Modifier.clickable { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }.padding(8.dp))
                Text(
                    if (saving) "Saving…" else "Done", color = VynoraNeonPink, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(enabled = !saving && bmp != null) {
                        saving = true
                        val src = bmp!!
                        val strokeCopy = strokes.map { DrawStroke(it.color, it.pts.toMutableList()) }
                        val tx = text; val tp = textPos; val tc = color
                        scope.launch {
                            val out = withContext(Dispatchers.IO) { renderEdits(ctx, src, strokeCopy, tx, tp, tc) }
                            saving = false
                            if (out != null) onDone(out) else onCancel()
                        }
                    }.padding(8.dp)
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                val b = bmp
                if (b != null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().aspectRatio(b.width.toFloat() / b.height).onSizeChanged { box = it }
                    ) {
                        Image(b.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
                        Canvas(
                            modifier = Modifier.fillMaxSize().pointerInput(textMode) {
                                detectDragGestures(
                                    onDragStart = { p ->
                                        if (!textMode) {
                                            strokes.add(DrawStroke(color, mutableListOf(Offset(p.x / box.width, p.y / box.height))))
                                            tick++
                                        }
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val n = Offset((change.position.x / box.width).coerceIn(0f, 1f), (change.position.y / box.height).coerceIn(0f, 1f))
                                        if (textMode) textPos = n
                                        else { strokes.lastOrNull()?.pts?.add(n); tick++ }
                                    }
                                )
                            }
                        ) {
                            @Suppress("UNUSED_EXPRESSION") tick
                            strokes.forEach { s ->
                                if (s.pts.size > 1) {
                                    val path = androidx.compose.ui.graphics.Path()
                                    path.moveTo(s.pts[0].x * size.width, s.pts[0].y * size.height)
                                    for (i in 1 until s.pts.size) path.lineTo(s.pts[i].x * size.width, s.pts[i].y * size.height)
                                    drawPath(path, Color(s.color), style = Stroke(width = size.width * 0.012f, cap = StrokeCap.Round))
                                }
                            }
                            if (text.isNotBlank()) {
                                val paint = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                                    this.color = color; textSize = size.width * 0.07f; typeface = Typeface.DEFAULT_BOLD
                                    textAlign = AndroidPaint.Align.CENTER; setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
                                }
                                drawContext.canvas.nativeCanvas.drawText(text, textPos.x * size.width, textPos.y * size.height, paint)
                            }
                        }
                    }
                } else Text("Loading…", color = Color.White)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 12.dp).padding(bottom = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EditorColors.forEach { c ->
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(c))
                            .border(if (c == color) 3.dp else 1.dp, if (c == color) VynoraNeonPink else Color.Gray, CircleShape)
                            .clickable { color = c }
                    )
                }
                Box(modifier = Modifier.weight(1f))
                Text("Aa", color = if (textMode) VynoraNeonPink else Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { if (text.isBlank()) showTextDialog = true else textMode = !textMode }.padding(8.dp))
                Text("✏️", fontSize = 20.sp, modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { textMode = false }.padding(8.dp))
            }
            if (text.isNotBlank()) {
                Text(
                    "Text: tap Aa to move it, ✏️ to draw. Tap here to edit text.",
                    color = Color(0xFF94A3B8), fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth().clickable { showTextDialog = true }.padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                )
            }
        }
    }
}

private fun renderEdits(ctx: Context, src: Bitmap, strokes: List<DrawStroke>, text: String, textPos: Offset, textColor: Int): Uri? {
    return try {
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val c = AndroidCanvas(out)
        val w = out.width.toFloat()
        val h = out.height.toFloat()
        val sp = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
            style = AndroidPaint.Style.STROKE; strokeCap = AndroidPaint.Cap.ROUND; strokeJoin = AndroidPaint.Join.ROUND; strokeWidth = w * 0.012f
        }
        strokes.forEach { s ->
            if (s.pts.size > 1) {
                val path = AndroidPath()
                path.moveTo(s.pts[0].x * w, s.pts[0].y * h)
                for (i in 1 until s.pts.size) path.lineTo(s.pts[i].x * w, s.pts[i].y * h)
                sp.color = s.color
                c.drawPath(path, sp)
            }
        }
        if (text.isNotBlank()) {
            val tp = AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG).apply {
                color = textColor; textSize = w * 0.07f; typeface = Typeface.DEFAULT_BOLD
                textAlign = AndroidPaint.Align.CENTER; setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
            }
            c.drawText(text, textPos.x * w, textPos.y * h, tp)
        }
        val f = java.io.File(ctx.cacheDir, "vn_edit_${System.currentTimeMillis()}.jpg")
        f.outputStream().use { out.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        cacheFileUri(ctx, f)
    } catch (e: Exception) { null }
}
