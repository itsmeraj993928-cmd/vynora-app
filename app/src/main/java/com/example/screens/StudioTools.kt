package com.example.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import java.io.ByteArrayOutputStream
import java.io.File

/** Photo filters shared by the live preview (Compose) and the saved JPEG (Canvas). */
internal object StudioFilters {
    val names = listOf("Original", "Neon Glow", "Cyberpunk", "Sunset Warm", "Noir", "Vibrant", "Vintage", "Cool", "Fade", "Sepia")

    private fun fm(sat: Float, r: Float, g: Float, b: Float, off: Float): FloatArray {
        val i = 1f - sat
        return floatArrayOf(
            (0.213f * i + sat) * r, 0.715f * i * r, 0.072f * i * r, 0f, off,
            0.213f * i * g, (0.715f * i + sat) * g, 0.072f * i * g, 0f, off,
            0.213f * i * b, 0.715f * i * b, (0.072f * i + sat) * b, 0f, off,
            0f, 0f, 0f, 1f, 0f
        )
    }

    fun matrix(name: String): FloatArray? = when (name) {
        "Neon Glow" -> fm(1.4f, 0.95f, 1.05f, 1.2f, 6f)
        "Cyberpunk" -> fm(1.5f, 1.15f, 0.9f, 1.2f, 0f)
        "Sunset Warm" -> fm(1.2f, 1.15f, 1.0f, 0.8f, 8f)
        "Noir" -> fm(0f, 1.1f, 1.1f, 1.1f, -12f)
        "Vibrant" -> fm(1.8f, 1f, 1f, 1f, 0f)
        "Vintage" -> fm(0.7f, 1.1f, 1.0f, 0.8f, 10f)
        "Cool" -> fm(1.1f, 0.9f, 1.0f, 1.15f, 0f)
        "Fade" -> fm(0.8f, 1f, 1f, 1f, 25f)
        "Sepia" -> floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
        else -> null
    }
}

private fun newCacheFile(ctx: Context, prefix: String, ext: String): File =
    File(ctx.cacheDir, "${prefix}_${System.currentTimeMillis()}.$ext")

internal fun cacheFileUri(ctx: Context, f: File): Uri =
    FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)

/** A fresh empty file + shareable Uri for the camera to write into. */
internal fun newCameraTarget(ctx: Context, video: Boolean): Uri =
    cacheFileUri(ctx, newCacheFile(ctx, "vn_cam", if (video) "mp4" else "jpg").also { try { it.createNewFile() } catch (e: Exception) { } })

private fun saveJpeg(ctx: Context, bmp: Bitmap, prefix: String): Uri? = try {
    val f = newCacheFile(ctx, prefix, "jpg")
    f.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    cacheFileUri(ctx, f)
} catch (e: Exception) { null }

/** Decodes an image, scaled so the longest side is about maxSide, and fixes phone rotation. */
internal fun decodeScaled(ctx: Context, uri: Uri, maxSide: Int): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val raw = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    if (raw == null) null else {
        val rot = try {
            ctx.contentResolver.openInputStream(uri)?.use {
                when (android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
                    android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) { 0f }
        if (rot == 0f) raw else Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rot) }, true)
    }
} catch (e: Exception) { null }

/** Saves a filtered copy of the photo and returns its Uri (null on failure). */
internal fun bakeFilter(ctx: Context, uri: Uri, filterName: String): Uri? {
    val m = StudioFilters.matrix(filterName) ?: return null
    val src = decodeScaled(ctx, uri, 1600) ?: return null
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { colorFilter = ColorMatrixColorFilter(ColorMatrix(m)) }
    Canvas(out).drawBitmap(src, 0f, 0f, paint)
    return saveJpeg(ctx, out, "vn_filt")
}

/** Small JPEG of the picked media, for the AI to look at. */
internal fun jpegForAi(ctx: Context, uri: Uri, isVideo: Boolean): ByteArray? {
    val bmp: Bitmap? = if (isVideo) {
        try {
            val r = android.media.MediaMetadataRetriever()
            r.setDataSource(ctx, uri)
            val f = r.getFrameAtTime(1_000_000L) ?: r.getFrameAtTime(0L)
            r.release()
            f
        } catch (e: Exception) { null }
    } else decodeScaled(ctx, uri, 768)
    if (bmp == null) return null
    val bos = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, 80, bos)
    return bos.toByteArray()
}

internal val TextStoryThemes: List<Pair<Int, Int>> = listOf(
    0xFFFF2E93.toInt() to 0xFF7C3AED.toInt(),
    0xFF06B6D4.toInt() to 0xFF1D4ED8.toInt(),
    0xFFF59E0B.toInt() to 0xFFEF4444.toInt(),
    0xFF10B981.toInt() to 0xFF0F766E.toInt(),
    0xFF111827.toInt() to 0xFF4B5563.toInt(),
    0xFFEC4899.toInt() to 0xFFF59E0B.toInt()
)

/** "Aa Create": a gradient card with big centred text, saved as a 1080x1920 image. */
internal fun renderTextStory(ctx: Context, text: String, themeIndex: Int): Uri? {
    val w = 1080
    val h = 1920
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    val (c1, c2) = TextStoryThemes[themeIndex.coerceIn(0, TextStoryThemes.lastIndex)]
    val bg = Paint().apply { shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), c1, c2, Shader.TileMode.CLAMP) }
    c.drawRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), bg)
    val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textSize = if (text.length > 120) 56f else if (text.length > 50) 76f else 100f
    }
    val sl = StaticLayout.Builder.obtain(text, 0, text.length, tp, w - 200)
        .setAlignment(Layout.Alignment.ALIGN_CENTER).setLineSpacing(0f, 1.1f).build()
    c.save()
    c.translate(100f, (h - sl.height) / 2f)
    sl.draw(c)
    c.restore()
    return saveJpeg(ctx, bmp, "vn_text")
}

/** Layout: 2 photos side by side, 3 = one big + two small, 4 = grid. */
internal fun buildCollage(ctx: Context, uris: List<Uri>): Uri? {
    val list = uris.take(4)
    if (list.size < 2) return null
    val w = 1080
    val h = 1080
    val gap = 6f
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.drawColor(android.graphics.Color.WHITE)
    val cells: List<RectF> = when (list.size) {
        2 -> listOf(RectF(0f, 0f, w / 2f - gap / 2, h.toFloat()), RectF(w / 2f + gap / 2, 0f, w.toFloat(), h.toFloat()))
        3 -> listOf(
            RectF(0f, 0f, w / 2f - gap / 2, h.toFloat()),
            RectF(w / 2f + gap / 2, 0f, w.toFloat(), h / 2f - gap / 2),
            RectF(w / 2f + gap / 2, h / 2f + gap / 2, w.toFloat(), h.toFloat())
        )
        else -> listOf(
            RectF(0f, 0f, w / 2f - gap / 2, h / 2f - gap / 2), RectF(w / 2f + gap / 2, 0f, w.toFloat(), h / 2f - gap / 2),
            RectF(0f, h / 2f + gap / 2, w / 2f - gap / 2, h.toFloat()), RectF(w / 2f + gap / 2, h / 2f + gap / 2, w.toFloat(), h.toFloat())
        )
    }
    val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    list.forEachIndexed { i, u ->
        val b = decodeScaled(ctx, u, 1100) ?: return@forEachIndexed
        val cell = cells[i]
        // centre-crop the photo into its cell
        val scale = maxOf(cell.width() / b.width, cell.height() / b.height)
        val sw = cell.width() / scale
        val sh = cell.height() / scale
        val sx = (b.width - sw) / 2f
        val sy = (b.height - sh) / 2f
        c.save()
        c.clipRect(cell)
        c.drawBitmap(b, android.graphics.Rect(sx.toInt(), sy.toInt(), (sx + sw).toInt(), (sy + sh).toInt()), cell, paint)
        c.restore()
    }
    return saveJpeg(ctx, bmp, "vn_collage")
}
