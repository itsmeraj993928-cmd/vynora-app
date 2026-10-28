package com.example.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.viewmodel.AppForeground
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * Runs about every 15 minutes even when the app is closed (no server needed):
 * new likes/comments/follows, the daily "post today" reminder, and the siren for admins when a new report arrives.
 */
class NotifyWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        try {
            val user = FirebaseAuth.getInstance().currentUser ?: return Result.success()
            if (AppForeground.inForeground) return Result.success()
            val prefs = ctx.getSharedPreferences("vynora_prefs", Context.MODE_PRIVATE)
            val db = FirebaseFirestore.getInstance()
            val now = System.currentTimeMillis()
            val last = prefs.getLong("worker_last_check", now - 15 * 60 * 1000L)

            // 1) activity
            val notifs = db.collection("notifications").whereEqualTo("recipientId", user.uid).get().await()
                .documents.mapNotNull { d -> d.getLong("createdAt")?.let { t -> Triple(t, d.getString("type") ?: "", d.getBoolean("isRead") ?: false) } }
                .filter { it.first > last && !it.third }
            if (notifs.isNotEmpty()) {
                val likes = notifs.count { it.second == "LIKE" }
                val comments = notifs.count { it.second == "COMMENT" }
                val follows = notifs.count { it.second == "FOLLOW" }
                val parts = ArrayList<String>()
                if (likes > 0) parts.add("$likes new like" + if (likes > 1) "s" else "")
                if (comments > 0) parts.add("$comments new comment" + if (comments > 1) "s" else "")
                if (follows > 0) parts.add("$follows new follower" + if (follows > 1) "s" else "")
                if (parts.isEmpty()) parts.add("${notifs.size} new notification" + if (notifs.size > 1) "s" else "")
                Notifier.show(ctx, 7001, Notifier.CH_ACTIVITY, "VYNORA", parts.joinToString(", "))
            }

            // 2) admin: siren for new reports
            val me = db.collection("users").document(user.uid).get().await()
            if (me.getString("role") == "ADMIN") {
                val reps = db.collection("reports").whereEqualTo("status", "PENDING").get().await()
                    .documents.mapNotNull { it.getLong("createdAt") }.filter { it > last }
                if (reps.isNotEmpty()) {
                    Notifier.show(ctx, 7002, Notifier.CH_SIREN, "🚨 New report", "${reps.size} new report" + (if (reps.size > 1) "s" else "") + " waiting for review.")
                }
            }

            // 3) daily reminder, once a day between 6pm and 9pm
            if (prefs.getBoolean("reminders_on", true)) {
                val cal = Calendar.getInstance()
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val today = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
                if (hour in 18..20 && prefs.getInt("reminder_day", 0) != today) {
                    prefs.edit().putInt("reminder_day", today).apply()
                    Notifier.show(ctx, 7003, Notifier.CH_REMINDER, "Today's challenge is waiting ✨", "Post a reel or photo today and keep your streak alive 🔥")
                }
            }
            prefs.edit().putLong("worker_last_check", now).apply()
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
}
