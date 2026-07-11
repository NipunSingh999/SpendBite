package com.example.spendbitepro

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class SplashFragment : Fragment() {

    private val delayHandler = Handler(Looper.getMainLooper())
    private val navigateRunnable = Runnable {
        if (isAdded) {
            checkAuthAndNavigate()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val llBrandGroup = view.findViewById<LinearLayout>(R.id.ll_brand_group)
        val tvSplashTagline = view.findViewById<TextView>(R.id.tv_splash_tagline)

        // Load premium animations
        val context = context
        if (context != null) {
            val fadeInScale = AnimationUtils.loadAnimation(context, R.anim.fade_in_scale)
            val fadeInUp = AnimationUtils.loadAnimation(context, R.anim.fade_in_up)

            llBrandGroup?.startAnimation(fadeInScale)
            tvSplashTagline?.startAnimation(fadeInUp)
        }

        // Auto-navigate after 2.5 seconds (2500ms) to simulate assets loading
        delayHandler.postDelayed(navigateRunnable, 2500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel the delay check if view is destroyed early
        delayHandler.removeCallbacks(navigateRunnable)
    }

    private fun checkAuthAndNavigate() {
        val mainActivity = activity as? MainActivity
        val repository = RepositoryProvider.getRepository()
        
        // Auto-login check: If there is an active user, go directly to Dashboard
        val currentUserId = repository.getCurrentUserId()
        if (currentUserId != null && currentUserId != "demo_user") {
            mainActivity?.navigateTo("dashboard")
        } else {
            mainActivity?.navigateTo("login")
        }
    }
}
