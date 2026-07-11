package com.example.spendbitepro

import android.app.Application

class SpendBiteApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Firebase programmatically on app startup
        FirebaseManager.initialize(this)
    }
}
