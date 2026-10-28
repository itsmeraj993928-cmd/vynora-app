package com.aistudio.vynorasocial.wrtkpv

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class VynoraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (e: Exception) {
            Log.e("VynoraApplication", "FirebaseApp initialization failed: ${e.message}", e)
        }
        try {
            com.example.util.Notifier.ensureChannels(this)
            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "vynora_notify",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                androidx.work.PeriodicWorkRequestBuilder<com.example.util.NotifyWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
                    .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build())
                    .build()
            )
        } catch (e: Exception) {
            Log.e("VynoraApplication", "Notification worker failed: ${e.message}", e)
        }
    }
}
