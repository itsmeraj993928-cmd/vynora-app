package com.example.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume

/** Shrinks big videos (max 720p, about 2.5 Mbps) before upload. Returns the original uri if anything goes wrong. */
object VideoCompress {
    private fun even(v: Float): Int = (v.toInt() / 2) * 2

    suspend fun compress(ctx: Context, src: Uri): Uri = withContext(Dispatchers.Main) {
        try {
            var w = 0; var h = 0; var rot = 0
            val r = MediaMetadataRetriever()
            try {
                r.setDataSource(ctx, src)
                w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            } finally { try { r.release() } catch (_: Throwable) { } }
            if (w <= 0 || h <= 0) return@withContext src
            val dispW = if (rot == 90 || rot == 270) h else w
            val dispH = if (rot == 90 || rot == 270) w else h
            val shortSide = minOf(dispW, dispH)
            val bytes = try { ctx.contentResolver.openAssetFileDescriptor(src, "r")?.use { it.length } ?: 0L } catch (_: Exception) { 0L }
            // already small enough: leave it alone
            if (shortSide <= 720 && bytes in 1..(15L * 1024 * 1024)) return@withContext src
            if (shortSide <= 720 && bytes == 0L) return@withContext src

            val scale = if (shortSide > 720) 720f / shortSide else 1f
            val outW = maxOf(2, even(dispW * scale))
            val outH = maxOf(2, even(dispH * scale))
            val bitrate = if (DataSaver.enabled) 1_500_000 else 2_500_000
            val out = File(ctx.cacheDir, "vynora_compressed_${System.currentTimeMillis()}.mp4")

            val done = withTimeoutOrNull(4 * 60 * 1000L) {
                suspendCancellableCoroutine<Boolean> { cont ->
                    val transformer = Transformer.Builder(ctx)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .setAudioMimeType(MimeTypes.AUDIO_AAC)
                        .setEncoderFactory(
                            DefaultEncoderFactory.Builder(ctx)
                                .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(bitrate).build())
                                .build()
                        )
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) { if (cont.isActive) cont.resume(true) }
                            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) { if (cont.isActive) cont.resume(false) }
                        })
                        .build()
                    val item = EditedMediaItem.Builder(MediaItem.fromUri(src))
                        .setEffects(Effects(listOf(), listOf(Presentation.createForWidthAndHeight(outW, outH, Presentation.LAYOUT_SCALE_TO_FIT))))
                        .build()
                    cont.invokeOnCancellation { try { transformer.cancel() } catch (_: Throwable) { } }
                    transformer.start(item, out.absolutePath)
                }
            }
            // keep the compressed file only if it really is smaller
            if (done == true && out.exists() && out.length() > 0 && (bytes == 0L || out.length() < bytes)) Uri.fromFile(out) else { out.delete(); src }
        } catch (e: Throwable) {
            src
        }
    }
}
