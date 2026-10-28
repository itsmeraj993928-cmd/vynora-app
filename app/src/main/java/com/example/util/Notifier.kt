package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.R

/** Small helper for system notifications: activity, daily reminder, and the admin report siren. */
object Notifier {
    const val CH_ACTIVITY = "activity_v1"
    const val CH_REMINDER = "reminder_v1"
    const val CH_SIREN = "reports_siren_v1"

    fun ensureChannels(ctx: Context) {
        if (android.os.Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CH_ACTIVITY, "Likes, comments and follows", NotificationManager.IMPORTANCE_DEFAULT))
        nm.createNotificationChannel(NotificationChannel(CH_REMINDER, "Daily reminder", NotificationManager.IMPORTANCE_LOW))
        val siren = NotificationChannel(CH_SIREN, "Report alerts (siren)", NotificationManager.IMPORTANCE_HIGH)
        siren.setSound(
            Uri.parse("android.resource://${ctx.packageName}/${R.raw.siren}"),
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
        )
        siren.enableVibration(true)
        siren.vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
        nm.createNotificationChannel(siren)
    }

    fun show(ctx: Context, id: Int, channel: String, title: String, text: String) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return
            ensureChannels(ctx)
            val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName) ?: return
            val pi = PendingIntent.getActivity(ctx, id, launch, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val b = NotificationCompat.Builder(ctx, channel)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .setContentIntent(pi)
            if (channel == CH_SIREN) {
                b.setPriority(NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_ALARM)
            }
            ctx.getSystemService(NotificationManager::class.java)?.notify(id, b.build())
        } catch (e: Exception) { }
    }

    /** Plays the siren once (used when the app is open and a report arrives). */
    fun playSiren(ctx: Context) {
        try {
            val mp = MediaPlayer.create(ctx, R.raw.siren) ?: return
            mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            mp.setOnCompletionListener { it.release() }
            mp.start()
        } catch (e: Exception) { }
    }
}
