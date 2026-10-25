package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

/** Turns one picture into a 5 second vertical MP4 with slow zoom/pan motion. Runs on the phone, no server. */
object KenBurnsVideo {
    private const val W = 720
    private const val H = 1280
    private const val FPS = 30

    /** style: 0 zoom in, 1 zoom out, 2 pan left, 3 pan right. Takes about as long as the video itself. */
    fun render(src: Bitmap, out: File, style: Int, seconds: Int = 5): Boolean {
        val total = seconds * FPS
        val bw = src.width.toFloat()
        val bh = src.height.toFloat()
        val cover = maxOf(W / bw, H / bh)
        val m = Matrix()
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        return encode(out, total) { canvas, f ->
            val t = f / (total - 1f)
            val zoom = when (style) { 0 -> 1f + 0.25f * t; 1 -> 1.25f - 0.25f * t; else -> 1.15f }
            val pan = when (style) { 2 -> (t - 0.5f) * 0.08f * W; 3 -> (0.5f - t) * 0.08f * W; else -> 0f }
            m.reset()
            m.postScale(cover, cover)
            m.postTranslate((W - bw * cover) / 2f, (H - bh * cover) / 2f)
            m.postScale(zoom, zoom, W / 2f, H / 2f)
            m.postTranslate(pan, 0f)
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(src, m, paint)
        }
    }

    /** Boomerang: plays the frames forward then backward, 3 times. */
    fun renderBoomerang(frames: List<Bitmap>, out: File): Boolean {
        if (frames.size < 3) return false
        val order = frames.indices.toList() + (frames.size - 2 downTo 1).toList()
        val seq = order + order + order
        val perFrame = 2 // each picture is shown for 2 video frames
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val m = Matrix()
        return encode(out, seq.size * perFrame) { canvas, f ->
            val b = frames[seq[f / perFrame]]
            val cover = maxOf(W / b.width.toFloat(), H / b.height.toFloat())
            m.reset()
            m.postScale(cover, cover)
            m.postTranslate((W - b.width * cover) / 2f, (H - b.height * cover) / 2f)
            canvas.drawColor(Color.BLACK)
            canvas.drawBitmap(b, m, paint)
        }
    }

    private fun encode(out: File, total: Int, drawFrame: (android.graphics.Canvas, Int) -> Unit): Boolean {
        var codec: MediaCodec? = null
        var muxer: MediaMuxer? = null
        var surface: android.view.Surface? = null
        var muxerStarted = false
        return try {
            val format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, W, H).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 4_000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, FPS)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            val enc = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            codec = enc
            enc.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val s = enc.createInputSurface()
            surface = s
            enc.start()
            val mux = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            muxer = mux
            var track = -1
            var firstTs = -1L
            val info = MediaCodec.BufferInfo()

            fun drain(endOfStream: Boolean) {
                var guard = 0
                while (guard++ < 2000) {
                    val idx = enc.dequeueOutputBuffer(info, if (endOfStream) 10_000L else 0L)
                    when {
                        idx == MediaCodec.INFO_TRY_AGAIN_LATER -> { if (!endOfStream) return }
                        idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            track = mux.addTrack(enc.outputFormat)
                            mux.start()
                            muxerStarted = true
                        }
                        idx >= 0 -> {
                            val buf = enc.getOutputBuffer(idx)
                            if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                            if (buf != null && info.size > 0 && muxerStarted) {
                                if (firstTs < 0) firstTs = info.presentationTimeUs
                                info.presentationTimeUs -= firstTs
                                buf.position(info.offset)
                                buf.limit(info.offset + info.size)
                                mux.writeSampleData(track, buf, info)
                            }
                            enc.releaseOutputBuffer(idx, false)
                            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) return
                        }
                    }
                }
            }

            for (f in 0 until total) {
                val canvas = try { s.lockHardwareCanvas() } catch (e: Exception) { s.lockCanvas(null) }
                drawFrame(canvas, f)
                s.unlockCanvasAndPost(canvas)
                drain(false)
                Thread.sleep(1000L / FPS)
            }
            enc.signalEndOfInputStream()
            drain(true)
            out.length() > 1000
        } catch (e: Exception) {
            false
        } finally {
            try { codec?.stop() } catch (e: Exception) { }
            try { codec?.release() } catch (e: Exception) { }
            try { surface?.release() } catch (e: Exception) { }
            try { if (muxerStarted) muxer?.stop() } catch (e: Exception) { }
            try { muxer?.release() } catch (e: Exception) { }
        }
    }
}
