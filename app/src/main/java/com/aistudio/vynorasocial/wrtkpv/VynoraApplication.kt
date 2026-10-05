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
    }
}
