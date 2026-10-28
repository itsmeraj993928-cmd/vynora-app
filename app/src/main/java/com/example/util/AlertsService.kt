package com.example.util

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.viewmodel.AppForeground
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Keeps a light connection to Firestore even when the app is closed, so likes, comments, follows
 * and (for admins) new reports show up instantly, with the siren for reports. No server needed.
 */
class AlertsService : Service() {
    private val regs = ArrayList<ListenerRegistration>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Notifier.ensureChannels(this)
        val note: Notification = NotificationCompat.Builder(this, Notifier.CH_SERVICE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("VYNORA")
            .setContentText("Instant alerts are on")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
        try {
            if (android.os.Build.VERSION.SDK_INT >= 34) startForeground(9001, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(9001, note)
        } catch (e: Exception) { stopSelf(); return START_NOT_STICKY }

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) { stopSelf(); return START_NOT_STICKY }
        if (running) return START_STICKY
        running = true
        startListening(uid)
        return START_STICKY
    }

    private fun startListening(uid: String) {
        val db = FirebaseFirestore.getInstance()
        val startedAt = System.currentTimeMillis()

        // likes, comments, follows: only documents that arrive after the first load
        var firstNotifs = true
        regs += db.collection("notifications").whereEqualTo("recipientId", uid).addSnapshotListener { snap, err ->
            if (err != null || snap == null) return@addSnapshotListener
            if (firstNotifs) { firstNotifs = false; return@addSnapshotListener }
            if (AppForeground.inForeground) return@addSnapshotListener
            for (ch in snap.documentChanges) {
                if (ch.type != DocumentChange.Type.ADDED) continue
                val d = ch.document
                val created = d.getLong("createdAt") ?: 0L
                if (created < startedAt - 60_000L || d.getBoolean("isRead") == true) continue
                val who = (d.get("user") as? Map<*, *>)?.get("username") as? String ?: "Someone"
                val preview = d.getString("contentPreview").orEmpty()
                val text = when (d.getString("type")) {
                    "LIKE" -> "@$who liked your post"
                    "COMMENT" -> "@$who commented" + if (preview.isNotBlank()) ": $preview" else ""
                    "FOLLOW" -> "@$who started following you"
                    "MENTION" -> "@$who mentioned you"
                    else -> preview.ifBlank { "You have a new notification" }
                }
                Notifier.show(this, 7100 + (d.id.hashCode() and 0xFFF), Notifier.CH_ACTIVITY, "VYNORA", text)
            }
        }

        // admin only: siren for every new report
        db.collection("users").document(uid).get().addOnSuccessListener { me ->
            if (me.getString("role") != "ADMIN") return@addOnSuccessListener
            var firstReports = true
            regs += db.collection("reports").whereEqualTo("status", "PENDING").addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                if (firstReports) { firstReports = false; return@addSnapshotListener }
                if (AppForeground.inForeground) return@addSnapshotListener
                val added = snap.documentChanges.count { it.type == DocumentChange.Type.ADDED }
                if (added > 0) Notifier.show(this, 7002, Notifier.CH_SIREN, "🚨 New report", "$added new report" + (if (added > 1) "s" else "") + " waiting for review.")
            }
        }
    }

    override fun onDestroy() {
        regs.forEach { try { it.remove() } catch (_: Exception) { } }
        regs.clear()
        running = false
        super.onDestroy()
    }

    companion object {
        @Volatile var running: Boolean = false

        fun enabled(ctx: Context): Boolean =
            ctx.getSharedPreferences("vynora_prefs", Context.MODE_PRIVATE).getBoolean("instant_alerts", true)

        fun setEnabled(ctx: Context, on: Boolean) {
            ctx.getSharedPreferences("vynora_prefs", Context.MODE_PRIVATE).edit().putBoolean("instant_alerts", on).apply()
            if (on) start(ctx) else stop(ctx)
        }

        fun start(ctx: Context) {
            try {
                if (!enabled(ctx) || FirebaseAuth.getInstance().currentUser == null) return
                ContextCompat.startForegroundService(ctx, Intent(ctx, AlertsService::class.java))
            } catch (_: Exception) { }
        }

        fun stop(ctx: Context) {
            try { ctx.stopService(Intent(ctx, AlertsService::class.java)) } catch (_: Exception) { }
        }
    }
}

/** Restarts the alerts after the phone reboots. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) AlertsService.start(context)
    }
}
