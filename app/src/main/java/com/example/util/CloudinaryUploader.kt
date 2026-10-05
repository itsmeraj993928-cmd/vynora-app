package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

data class CloudinaryUploadResult(
    val secureUrl: String,
    val publicId: String = "",
    val format: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val bytes: Long = 0L,
    val resourceType: String = "image"
)

/**
 * Production-ready Cloudinary media uploader with streaming progress tracking
 * and automatic retry for network errors.
 */
object CloudinaryUploader {
    private const val TAG = "CloudinaryUploader"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    /**
     * Uploads raw media bytes to Cloudinary with real-time progress callbacks.
     */
    suspend fun uploadBytes(
        bytes: ByteArray,
        mimeType: String = "image/jpeg",
        filename: String = "upload_${System.currentTimeMillis()}.jpg",
        maxRetries: Int = 2,
        onProgress: (Int) -> Unit = {}
    ): Result<CloudinaryUploadResult> = withContext(Dispatchers.IO) {
        var attempt = 0
        var lastException: Exception? = null

        while (attempt <= maxRetries) {
            attempt++
            try {
                val mediaType = mimeType.toMediaTypeOrNull()

                // Custom RequestBody that reports chunk write progress
                val countingBody = object : RequestBody() {
                    override fun contentType(): MediaType? = mediaType
                    override fun contentLength(): Long = bytes.size.toLong()
                    override fun writeTo(sink: BufferedSink) {
                        val total = bytes.size.toLong()
                        var written = 0L
                        val buffer = ByteArray(8192)
                        val input = bytes.inputStream()
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            sink.write(buffer, 0, read)
                            sink.flush()
                            written += read
                            val progress = if (total > 0) ((written * 100) / total).toInt() else 0
                            onProgress(progress.coerceIn(0, 99))
                        }
                    }
                }

                val multipartBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
                    .addFormDataPart("file", filename, countingBody)
                    .build()

                val request = Request.Builder()
                    .url(CloudinaryConfig.UPLOAD_URL)
                    .post(multipartBody)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.e(TAG, "Cloudinary upload attempt $attempt failed HTTP ${response.code}: $responseBody")
                    if (attempt <= maxRetries) continue
                    return@withContext Result.failure(
                        IOException("Upload failed (${response.code}): $responseBody")
                    )
                }

                val json = JSONObject(responseBody)
                val secureUrl = json.getString("secure_url")
                val publicId = json.optString("public_id", "")
                val format = json.optString("format", "")
                val width = json.optInt("width", 0)
                val height = json.optInt("height", 0)
                val byteSize = json.optLong("bytes", bytes.size.toLong())
                val resourceType = json.optString("resource_type", if (mimeType.contains("video")) "video" else "image")

                onProgress(100)
                return@withContext Result.success(
                    CloudinaryUploadResult(
                        secureUrl = secureUrl,
                        publicId = publicId,
                        format = format,
                        width = width,
                        height = height,
                        bytes = byteSize,
                        resourceType = resourceType
                    )
                )
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "Cloudinary upload attempt $attempt error: ${e.message}")
                if (attempt > maxRetries) {
                    return@withContext Result.failure(e)
                }
            }
        }

        Result.failure(lastException ?: IOException("Upload failed after $maxRetries retries"))
    }

    /**
     * Uploads media directly from an Android content Uri (e.g. from photo picker).
     */
    suspend fun uploadUri(
        context: Context,
        uri: Uri,
        onProgress: (Int) -> Unit = {}
    ): Result<CloudinaryUploadResult> = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IOException("Cannot open input stream for $uri"))

            val bytes = inputStream.use { it.readBytes() }
            val extension = if (mimeType.contains("video")) "mp4" else "jpg"
            val filename = "upload_${System.currentTimeMillis()}.$extension"

            uploadBytes(
                bytes = bytes,
                mimeType = mimeType,
                filename = filename,
                onProgress = onProgress
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading URI for Cloudinary upload: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Uploads a Bitmap (e.g. from camera capture).
     */
    suspend fun uploadBitmap(
        bitmap: Bitmap,
        quality: Int = 85,
        onProgress: (Int) -> Unit = {}
    ): Result<CloudinaryUploadResult> = withContext(Dispatchers.IO) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val bytes = stream.toByteArray()
        uploadBytes(
            bytes = bytes,
            mimeType = "image/jpeg",
            filename = "photo_${System.currentTimeMillis()}.jpg",
            onProgress = onProgress
        )
    }
}
