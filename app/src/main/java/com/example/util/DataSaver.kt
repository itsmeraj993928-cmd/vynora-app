package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** User setting: use less mobile data. */
object DataSaver {
    var enabled by mutableStateOf(false)
        private set

    fun init(ctx: Context) {
        enabled = try { ctx.getSharedPreferences("vynora_prefs", Context.MODE_PRIVATE).getBoolean("data_saver", false) } catch (e: Exception) { false }
    }

    fun set(ctx: Context, value: Boolean) {
        enabled = value
        try { ctx.getSharedPreferences("vynora_prefs", Context.MODE_PRIVATE).edit().putBoolean("data_saver", value).apply() } catch (e: Exception) { }
    }

    /** True on mobile data (or any metered network). */
    fun onMeteredNetwork(ctx: Context): Boolean = try {
        (ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).isActiveNetworkMetered
    } catch (e: Exception) { false }
}

/** Tracks whether the phone has internet, so the app can say so instead of failing silently. */
object NetworkMonitor {
    var online by mutableStateOf(true)
        private set
    private var started = false

    fun start(ctx: Context) {
        if (started) return
        started = true
        val cm = try { ctx.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager } catch (e: Exception) { return }
        online = try {
            val caps = cm.getNetworkCapabilities(cm.activeNetwork)
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (e: Exception) { true }
        val main = Handler(Looper.getMainLooper())
        try {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { main.post { online = true } }
                override fun onLost(network: Network) { main.post { online = false } }
            })
        } catch (e: Exception) { }
    }
}

/** Shrinks a photo before upload (less data, faster). Returns null if it should be uploaded as it is. */
object UploadCompress {
    fun image(ctx: Context, uri: android.net.Uri): android.net.Uri? {
        return try {
            val mime = ctx.contentResolver.getType(uri) ?: ""
            if (!mime.startsWith("image/") || mime.contains("gif")) return null
            val maxSide = if (DataSaver.enabled) 1080 else 1600
            val quality = if (DataSaver.enabled) 70 else 82
            val bmp = com.example.screens.decodeScaled(ctx, uri, maxSide) ?: return null
            val f = java.io.File(ctx.cacheDir, "vn_up_${System.currentTimeMillis()}.jpg")
            f.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, it) }
            com.example.screens.cacheFileUri(ctx, f)
        } catch (e: Exception) { null }
    }
}
