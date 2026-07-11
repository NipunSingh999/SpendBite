package com.example.spendbitepro

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File

class NavigationDrawerBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_navigation_drawer, container, false)

        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"

        // Set email
        val tvEmail = view.findViewById<TextView>(R.id.tv_menu_email)
        val email = if (userId == "demo_user") "gastronome@spendbite.pro" else (FirebaseManager.auth?.currentUser?.email ?: "user@spendbite.pro")
        tvEmail.text = email

        // Setup custom profile icon
        val ivMenuAvatar = view.findViewById<ImageView>(R.id.iv_menu_avatar)
        if (ivMenuAvatar != null) {
            val sharedPref = requireContext().getSharedPreferences("SpendBiteProPrefs", Context.MODE_PRIVATE)
            val photoPath = sharedPref.getString("user_profile_photo", null)
            if (!photoPath.isNullOrEmpty()) {
                val file = File(photoPath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        ivMenuAvatar.setImageBitmap(bitmap)
                        ivMenuAvatar.imageTintList = null
                    }
                }
            } else {
                ivMenuAvatar.setImageResource(R.drawable.ic_profile)
                ivMenuAvatar.imageTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.brand_primary)
                )
            }
        }

        // Setup click listeners
        view.findViewById<View>(R.id.btn_menu_analytics).setOnClickListener {
            (activity as? MainActivity)?.navigateTo("dashboard")
            dismiss()
        }

        view.findViewById<View>(R.id.btn_menu_split).setOnClickListener {
            (activity as? MainActivity)?.navigateTo("split_bite")
            dismiss()
        }

        view.findViewById<View>(R.id.btn_menu_subscriptions).setOnClickListener {
            (activity as? MainActivity)?.navigateTo("subscriptions")
            dismiss()
        }

        view.findViewById<View>(R.id.btn_menu_profile).setOnClickListener {
            (activity as? MainActivity)?.navigateTo("profile")
            dismiss()
        }

        view.findViewById<View>(R.id.btn_menu_logout).setOnClickListener {
            // Sign out from Firebase
            FirebaseManager.auth?.signOut()

            // Also sign out from Google client so Account Chooser always displays next time
            try {
                val context = requireContext()
                val webClientIdResId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                if (webClientIdResId != 0) {
                    val webClientId = context.getString(webClientIdResId)
                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build()
                    val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                    googleSignInClient.signOut()
                    googleSignInClient.revokeAccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Clear local cached profile info
            try {
                val context = requireContext()
                val sharedPref = context.getSharedPreferences("SpendBiteProPrefs", Context.MODE_PRIVATE)
                sharedPref.edit()
                    .remove("user_nickname")
                    .remove("user_profile_photo")
                    .apply()
                
                val file = File(context.filesDir, "profile_photo.jpg")
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            (activity as? MainActivity)?.navigateTo("login")
            dismiss()
        }

        return view
    }
}
