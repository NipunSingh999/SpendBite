package com.example.spendbitepro

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    var isInitialized = false
        private set

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            // Check if Firebase is already initialized by standard google-services.json
            if (FirebaseApp.getApps(context).isNotEmpty()) {
                isInitialized = true
                Log.d(TAG, "Firebase initialized automatically via google-services.json")
                return
            }

            // Fallback: Programmatic initialization if google-services.json is missing
            val options = FirebaseOptions.Builder()
                .setApplicationId("1:123456789012:android:123456789012") // Dummy App ID
                .setProjectId("spendbitepro-mock") // Dummy Project ID
                .setApiKey("AIzaSyMockKeyForDevelopmentOnly1234") // Dummy API Key
                .build()

            FirebaseApp.initializeApp(context, options)
            isInitialized = true
            Log.d(TAG, "Firebase initialized programmatically with custom options")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase: ${e.message}", e)
            isInitialized = false
        }
    }

    val auth: FirebaseAuth?
        get() = if (isInitialized) FirebaseAuth.getInstance() else null

    val firestore: FirebaseFirestore?
        get() = if (isInitialized) FirebaseFirestore.getInstance() else null
}
