package com.example.spendbitepro

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationBar: View
    private lateinit var viewPager: ViewPager2
    private lateinit var pagerAdapter: MainPagerAdapter
    
    // Bottom Tab items
    private lateinit var tabAnalytics: View
    private lateinit var tabSplit: View
    private lateinit var tabSubscriptions: View
    private lateinit var tabProfile: View

    // Bottom Tab Icons & Texts
    private lateinit var ivAnalytics: ImageView
    private lateinit var ivSplit: ImageView
    private lateinit var ivSubscriptions: ImageView
    private lateinit var ivProfile: ImageView

    private lateinit var tvAnalytics: TextView
    private lateinit var tvSplit: TextView
    private lateinit var tvSubscriptions: TextView
    private lateinit var tvProfile: TextView

    private var currentView: String = "splash"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Adjust for Edge to Edge system bars padding
        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Views
        bottomNavigationBar = findViewById(R.id.bottom_navigation_bar)
        viewPager = findViewById(R.id.view_pager)
        
        tabAnalytics = findViewById(R.id.tab_analytics)
        tabSplit = findViewById(R.id.tab_split)
        tabSubscriptions = findViewById(R.id.tab_subscriptions)
        tabProfile = findViewById(R.id.tab_profile)

        ivAnalytics = findViewById(R.id.iv_tab_analytics)
        ivSplit = findViewById(R.id.iv_tab_split)
        ivSubscriptions = findViewById(R.id.iv_tab_subscriptions)
        ivProfile = findViewById(R.id.iv_tab_profile)

        tvAnalytics = findViewById(R.id.tv_tab_analytics)
        tvSplit = findViewById(R.id.tv_tab_split)
        tvSubscriptions = findViewById(R.id.tv_tab_subscriptions)
        tvProfile = findViewById(R.id.tv_tab_profile)

        // Setup ViewPager2
        pagerAdapter = MainPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 3
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val tabViewName = when (position) {
                    0 -> "dashboard"
                    1 -> "split_bite"
                    2 -> "subscriptions"
                    3 -> "profile"
                    else -> "dashboard"
                }
                currentView = tabViewName
                updateBottomBarSelection(tabViewName)
            }
        })

        // Setup bottom navigation tab clicks
        tabAnalytics.setOnClickListener { navigateTo("dashboard") }
        tabSplit.setOnClickListener { navigateTo("split_bite") }
        tabSubscriptions.setOnClickListener { navigateTo("subscriptions") }
        tabProfile.setOnClickListener { navigateTo("profile") }

        // Initial view load: Splash
        if (savedInstanceState == null) {
            navigateTo("splash")
        }
    }

    fun navigateTo(viewName: String, arguments: Bundle? = null) {
        currentView = viewName
        val isTabScreen = viewName == "dashboard" || viewName == "split_bite" || viewName == "subscriptions" || viewName == "profile"

        if (isTabScreen) {
            // Hide FrameLayout container, show ViewPager2
            findViewById<View>(R.id.fragment_container).visibility = View.GONE
            viewPager.visibility = View.VISIBLE
            bottomNavigationBar.visibility = View.VISIBLE

            // Trigger adapter update to show tabs and notify change
            pagerAdapter.isLoggedOut = false
            pagerAdapter.notifyDataSetChanged()

            syncUserProfile()

            val tabIndex = when (viewName) {
                "dashboard" -> 0
                "split_bite" -> 1
                "subscriptions" -> 2
                "profile" -> 3
                else -> 0
            }
            if (viewPager.currentItem != tabIndex) {
                viewPager.setCurrentItem(tabIndex, true)
            } else {
                updateBottomBarSelection(viewName)
            }
        } else {
            // Trigger adapter update to destroy cached tab fragments when logging out
            pagerAdapter.isLoggedOut = true
            pagerAdapter.notifyDataSetChanged()

            // Show FrameLayout container, hide ViewPager2
            findViewById<View>(R.id.fragment_container).visibility = View.VISIBLE
            viewPager.visibility = View.GONE
            bottomNavigationBar.visibility = View.GONE

            val fragment: Fragment = when (viewName) {
                "splash" -> SplashFragment()
                "login" -> LoginFragment()
                "onboarding" -> OnboardingFragment()
                else -> SplashFragment()
            }

            if (arguments != null) {
                fragment.arguments = arguments
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commitAllowingStateLoss()
        }
    }

    private fun updateBottomBarSelection(activeView: String) {
        val activeColor = ContextCompat.getColor(this, R.color.purple_active)
        val inactiveColor = ContextCompat.getColor(this, R.color.text_zinc_500)

        // Reset all tabs
        ivAnalytics.setColorFilter(inactiveColor)
        tvAnalytics.setTextColor(inactiveColor)
        ivSplit.setColorFilter(inactiveColor)
        tvSplit.setTextColor(inactiveColor)
        ivSubscriptions.setColorFilter(inactiveColor)
        tvSubscriptions.setTextColor(inactiveColor)
        ivProfile.setColorFilter(inactiveColor)
        tvProfile.setTextColor(inactiveColor)

        // Reset scales with a smooth transition
        animateIconScale(ivAnalytics, 1.0f)
        animateIconScale(ivSplit, 1.0f)
        animateIconScale(ivSubscriptions, 1.0f)
        animateIconScale(ivProfile, 1.0f)

        // Highlight selected tab
        when (activeView) {
            "dashboard" -> {
                ivAnalytics.setColorFilter(activeColor)
                tvAnalytics.setTextColor(activeColor)
                animateIconScale(ivAnalytics, 1.25f)
            }
            "split_bite" -> {
                ivSplit.setColorFilter(activeColor)
                tvSplit.setTextColor(activeColor)
                animateIconScale(ivSplit, 1.25f)
            }
            "subscriptions" -> {
                ivSubscriptions.setColorFilter(activeColor)
                tvSubscriptions.setTextColor(activeColor)
                animateIconScale(ivSubscriptions, 1.25f)
            }
            "profile" -> {
                ivProfile.setColorFilter(activeColor)
                tvProfile.setTextColor(activeColor)
                animateIconScale(ivProfile, 1.25f)
            }
        }
    }

    private fun animateIconScale(view: View, targetScale: Float) {
        view.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(200)
            .setInterpolator(android.view.animation.OvershootInterpolator(1.5f))
            .start()
    }

    inner class MainPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        var isLoggedOut = false

        override fun getItemCount(): Int = if (isLoggedOut) 0 else 4
        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> DashboardFragment()
                1 -> SplitBiteFragment()
                2 -> SubscriptionsFragment()
                3 -> ProfileFragment()
                else -> DashboardFragment()
            }
        }
    }

    private fun syncUserProfile() {
        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: return
        if (userId == "demo_user") return

        repository.getUserProfile(userId) { profile ->
            if (profile != null) {
                val sharedPref = getSharedPreferences("SpendBiteProPrefs", android.content.Context.MODE_PRIVATE)
                val editor = sharedPref.edit()

                if (!profile.nickname.isEmpty()) {
                    editor.putString("user_nickname", profile.nickname)
                }

                if (!profile.profilePhotoBase64.isEmpty()) {
                    try {
                        val file = java.io.File(filesDir, "profile_photo.jpg")
                        val bytes = android.util.Base64.decode(profile.profilePhotoBase64, android.util.Base64.DEFAULT)
                        file.writeBytes(bytes)
                        editor.putString("user_profile_photo", file.absolutePath)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                }
                editor.apply()
                notifyProfileChanged()
            }
        }
    }

    fun notifyProfileChanged() {
        runOnUiThread {
            supportFragmentManager.fragments.forEach { fragment ->
                // Check direct fragments
                (fragment as? DashboardFragment)?.refreshAvatar()
                (fragment as? SplitBiteFragment)?.refreshAvatar()
                (fragment as? SubscriptionsFragment)?.refreshAvatar()
                (fragment as? ProfileFragment)?.refreshAvatar()

                // Check child fragments (for ViewPager2 fragments)
                fragment.childFragmentManager.fragments.forEach { child ->
                    (child as? DashboardFragment)?.refreshAvatar()
                    (child as? SplitBiteFragment)?.refreshAvatar()
                    (child as? SubscriptionsFragment)?.refreshAvatar()
                    (child as? ProfileFragment)?.refreshAvatar()
                }
            }
        }
    }
}