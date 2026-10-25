package com.example.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.io.File

/** Full-screen in-app camera (photo tap, or tap-to-record video for reels). */
@SuppressLint("MissingPermission")
@Composable
internal fun CameraCaptureScreen(
    initialMode: CreateMode,
    onCaptured: (Uri, Boolean, CreateMode, String) -> Unit,
    onClose: () -> Unit,
    onFail: (String) -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var camMode by remember { mutableStateOf(initialMode) }
    val videoMode = camMode == CreateMode.CLIP
    var filterName by remember { mutableStateOf("Original") }
    var useFront by remember { mutableStateOf(false) }
    var flashOn by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var seconds by remember { mutableStateOf(0) }
    var boomBusy by remember { mutableStateOf(false) }

    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    val recorder = remember {
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)))
            .build()
    }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    val previewView = remember { PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE; scaleType = PreviewView.ScaleType.FILL_CENTER } }

    // live filter preview: tint the whole preview layer with the chosen colour matrix
    LaunchedEffect(filterName, videoMode) {
        val m = if (videoMode) null else StudioFilters.matrix(filterName)
        if (m == null) previewView.setLayerType(android.view.View.LAYER_TYPE_NONE, null)
        else previewView.setLayerType(
            android.view.View.LAYER_TYPE_HARDWARE,
            android.graphics.Paint().apply { colorFilter = android.graphics.ColorMatrixColorFilter(android.graphics.ColorMatrix(m)) }
        )
    }

    DisposableEffect(useFront, videoMode) {
        val future = ProcessCameraProvider.getInstance(ctx)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                provider.unbindAll()
                val sel = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                if (videoMode) provider.bindToLifecycle(lifecycleOwner, sel, preview, videoCapture)
                else provider.bindToLifecycle(lifecycleOwner, sel, preview, imageCapture)
            } catch (e: Exception) {
                onFail("Camera couldn't start")
            }
        }, ContextCompat.getMainExecutor(ctx))
        onDispose {
            try { if (future.isDone) future.get().unbindAll() } catch (e: Exception) { }
        }
    }

    LaunchedEffect(flashOn) {
        imageCapture.flashMode = if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
    }

    // timer + 90s limit while recording
    LaunchedEffect(recording) {
        seconds = 0
        while (recording != null) {
            delay(1000)
            seconds += 1
            if (seconds >= 90) { recording?.stop(); break }
        }
    }

    fun takePhoto() {
        val f = File(ctx.cacheDir, "vn_cam_${System.currentTimeMillis()}.jpg")
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(f).build(),
            ContextCompat.getMainExecutor(ctx),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) { onCaptured(cacheFileUri(ctx, f), false, camMode, filterName) }
                override fun onError(exception: ImageCaptureException) { onFail("Couldn't take the photo") }
            }
        )
    }

    fun boomerang() {
        if (boomBusy || videoMode) return
        boomBusy = true
        camMode = CreateMode.MOMENT
        val frames = mutableListOf<android.graphics.Bitmap>()
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        fun next() {
            if (frames.size >= 10) {
                Thread {
                    val out = File(ctx.cacheDir, "vn_boom_${System.currentTimeMillis()}.mp4")
                    val ok = com.example.util.KenBurnsVideo.renderBoomerang(frames, out)
                    handler.post {
                        boomBusy = false
                        if (ok) onCaptured(cacheFileUri(ctx, out), true, CreateMode.MOMENT, "Original") else onFail("Boomerang failed")
                    }
                }.start()
                return
            }
            imageCapture.takePicture(ContextCompat.getMainExecutor(ctx), object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val raw = image.toBitmap()
                        val rot = image.imageInfo.rotationDegrees.toFloat()
                        val mx = android.graphics.Matrix().apply { postRotate(rot) }
                        val rotated = android.graphics.Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, mx, true)
                        val scale = 960f / maxOf(rotated.width, rotated.height)
                        frames.add(
                            if (scale < 1f) android.graphics.Bitmap.createScaledBitmap(rotated, (rotated.width * scale).toInt(), (rotated.height * scale).toInt(), true)
                            else rotated
                        )
                    } catch (e: Exception) {
                    } finally { image.close() }
                    next()
                }
                override fun onError(exception: ImageCaptureException) { boomBusy = false; onFail("Boomerang failed") }
            })
        }
        next()
    }

    fun toggleRecording() {
        val active = recording
        if (active != null) { active.stop(); return }
        val f = File(ctx.cacheDir, "vn_cam_${System.currentTimeMillis()}.mp4")
        var pending = recorder.prepareRecording(ctx, FileOutputOptions.Builder(f).build())
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            pending = pending.withAudioEnabled()
        }
        recording = pending.start(ContextCompat.getMainExecutor(ctx)) { ev ->
            if (ev is VideoRecordEvent.Finalize) {
                recording = null
                if (!ev.hasError()) onCaptured(cacheFileUri(ctx, f), true, camMode, "Original") else onFail("Recording failed")
            }
        }
    }

    BackHandler { recording?.stop(); onClose() }

    Dialog(
        onDismissRequest = { recording?.stop(); onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { recording?.stop(); onClose() }) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (recording != null) {
                        Text(
                            String.format("%d:%02d", seconds / 60, seconds % 60),
                            color = Color.White, fontSize = 15.sp,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFE11D48)).padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
                if (!videoMode) {
                    IconButton(onClick = { boomerang() }, enabled = !boomBusy) {
                        Text(if (boomBusy) "…" else "∞", color = Color.White, fontSize = 24.sp)
                    }
                    IconButton(onClick = { flashOn = !flashOn }) {
                        Icon(if (flashOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff, contentDescription = "Flash", tint = Color.White)
                    }
                } else Box(modifier = Modifier.size(48.dp))
            }

            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!videoMode) {
                    LazyRow(
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                    ) {
                        items(StudioFilters.names.size) { i ->
                            val n = StudioFilters.names[i]
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { filterName = n }) {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(CircleShape)
                                        .background(androidx.compose.ui.graphics.Brush.linearGradient(TextStoryThemes[i % TextStoryThemes.size].let { listOf(Color(it.first), Color(it.second)) }))
                                        .border(if (n == filterName) 2.dp else 0.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) { Text(n.take(1), color = Color.White, fontSize = 16.sp) }
                                Text(n, color = if (n == filterName) Color.White else Color(0xAAFFFFFF), fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(82.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(7.dp)
                            .clip(CircleShape)
                            .background(if (recording != null) Color(0xFFE11D48) else Color.White)
                            .clickable { if (videoMode) toggleRecording() else takePhoto() }
                    )
                    IconButton(
                        onClick = { useFront = !useFront },
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 36.dp).size(52.dp)
                    ) {
                        Icon(Icons.Filled.Cameraswitch, contentDescription = "Flip", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(28.dp)) {
                    listOf(CreateMode.POST to "POST", CreateMode.MOMENT to "STORY", CreateMode.CLIP to "REEL").forEach { (m, label) ->
                        Text(
                            label,
                            color = if (camMode == m) Color.White else Color(0x99FFFFFF),
                            fontSize = 15.sp,
                            fontWeight = if (camMode == m) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                            letterSpacing = 2.sp,
                            modifier = Modifier.clickable(enabled = recording == null) { camMode = m }
                        )
                    }
                }
            }
        }
    }
}
