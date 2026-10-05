package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageCompressUtil {

    fun uriToCompressedBase64(context: Context, uri: Uri, maxSize: Int = 400): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            inputStream?.close()
            bitmapToCompressedBase64(originalBitmap, maxSize)
        } catch (e: Exception) {
            null
        }
    }

    fun bitmapToCompressedBase64(bitmap: Bitmap, maxSize: Int = 400): String {
        val width = bitmap.width
        val height = bitmap.height
        val maxDim = max(width, height)
        val scaledBitmap = if (maxDim > maxSize) {
            val scale = maxSize.toFloat() / maxDim.toFloat()
            val newWidth = (width * scale).toInt()
            val newHeight = (height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        // Compress as JPEG at 80% quality for optimal size under 40KB
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val clean = if (base64Str.startsWith("data:image")) {
                base64Str.substringAfter(",")
            } else base64Str
            val decodedBytes = Base64.decode(clean, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
