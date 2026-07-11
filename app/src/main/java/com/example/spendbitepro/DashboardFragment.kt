package com.example.spendbitepro

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.google.android.material.floatingactionbutton.FloatingActionButton

class DashboardFragment : Fragment() {

    private lateinit var tvTotalSpent: TextView
    private lateinit var tvBudgetLimit: TextView
    private lateinit var tvBudgetRemaining: TextView
    private lateinit var pbTotalBudget: ProgressBar
    private lateinit var tvBadgeStatus: TextView

    // Projection
    private lateinit var tvProjectionAmount: TextView
    private lateinit var tvProjectionDescription: TextView
    private lateinit var btnForecast: Button
    private var computedDailyAvg = 0.0
    private var computedProjected = 0.0

    // Donut Split & Legends
    private lateinit var pbDonutChart: DonutChartView
    private lateinit var tvDonutLabel: TextView
    private lateinit var tvDonutPercent: TextView
    private var selectedCategory: String = "Food"
    private lateinit var llCategoryLegends: LinearLayout

    // SMS Sync UI
    private lateinit var btnSmsSync: Button
    private lateinit var viewSmsStatusDot: View
    private lateinit var tvSmsStatusBadge: TextView

    private lateinit var vTrendLine: TrendLineView
    private lateinit var btnSeeAll: View

    private lateinit var rvTransactions: RecyclerView

    // Merchant Spending Views
    private lateinit var tvMerchantZomatoVal: TextView
    private lateinit var pbMerchantZomato: ProgressBar
    private lateinit var tvMerchantSwiggyVal: TextView
    private lateinit var pbMerchantSwiggy: ProgressBar
    private lateinit var tvMerchantBlinkitVal: TextView
    private lateinit var pbMerchantBlinkit: ProgressBar
    private lateinit var tvMerchantZeptoVal: TextView
    private lateinit var pbMerchantZepto: ProgressBar
    private lateinit var fabAddTransaction: FloatingActionButton

    private lateinit var adapter: TransactionAdapter
    private var activeTransactions: List<Transaction> = emptyList()
    private var budgetSettings = BudgetSettings()

    // Listeners references to clean up if needed
    private var txListener: Any? = null
    private var budgetListener: Any? = null

    private val SMS_PERMISSION_CODE = 101

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        tvTotalSpent = view.findViewById(R.id.tv_total_spent)
        tvBudgetLimit = view.findViewById(R.id.tv_budget_limit)
        tvBudgetRemaining = view.findViewById(R.id.tv_budget_remaining)
        pbTotalBudget = view.findViewById(R.id.pb_total_budget)
        tvBadgeStatus = view.findViewById(R.id.tv_badge_status)

        tvProjectionAmount = view.findViewById(R.id.tv_projection_amount)
        tvProjectionDescription = view.findViewById(R.id.tv_projection_description)
        btnForecast = view.findViewById(R.id.btn_forecast)

        pbDonutChart = view.findViewById(R.id.pb_donut_chart)
        tvDonutLabel = view.findViewById(R.id.tv_donut_label)
        tvDonutPercent = view.findViewById(R.id.tv_donut_percent)
        llCategoryLegends = view.findViewById(R.id.ll_category_legends)

        btnSmsSync = view.findViewById(R.id.btn_sms_sync)
        viewSmsStatusDot = view.findViewById(R.id.view_sms_status_dot)
        tvSmsStatusBadge = view.findViewById(R.id.tv_sms_status_badge)

        vTrendLine = view.findViewById(R.id.v_trend_line)
        btnSeeAll = view.findViewById(R.id.btn_see_all)

        rvTransactions = view.findViewById(R.id.rv_transactions)

        // Bind Merchant Views
        tvMerchantZomatoVal = view.findViewById(R.id.tv_merchant_zomato_val)
        pbMerchantZomato = view.findViewById(R.id.pb_merchant_zomato)
        tvMerchantSwiggyVal = view.findViewById(R.id.tv_merchant_swiggy_val)
        pbMerchantSwiggy = view.findViewById(R.id.pb_merchant_swiggy)
        tvMerchantBlinkitVal = view.findViewById(R.id.tv_merchant_blinkit_val)
        pbMerchantBlinkit = view.findViewById(R.id.pb_merchant_blinkit)
        tvMerchantZeptoVal = view.findViewById(R.id.tv_merchant_zepto_val)
        pbMerchantZepto = view.findViewById(R.id.pb_merchant_zepto)
        fabAddTransaction = view.findViewById(R.id.fab_add_transaction)

        // Setup RecyclerView
        rvTransactions.layoutManager = LinearLayoutManager(context)
        adapter = TransactionAdapter(emptyList()) { transaction ->
            val bottomSheet = TransactionDetailBottomSheet.newInstance(transaction)
            bottomSheet.show(childFragmentManager, "TransactionDetailBottomSheet")
        }
        rvTransactions.adapter = adapter

        // Forecast Click
        btnForecast.setOnClickListener {
            val limit = budgetSettings.totalMonthlyLimit
            val bottomSheet = ForecastBottomSheet.newInstance(computedDailyAvg, computedProjected, limit)
            bottomSheet.show(childFragmentManager, "ForecastBottomSheet")
        }

        // FAB Click
        fabAddTransaction.setOnClickListener {
            val addSheet = AddTransactionBottomSheet()
            addSheet.show(childFragmentManager, "AddTransactionBottomSheet")
        }

        // Navigate to Profile on Avatar Click
        val cvAvatar = view.findViewById<View>(R.id.cv_avatar)
        cvAvatar.setOnClickListener {
            (activity as? MainActivity)?.navigateTo("profile")
        }

        // Open Navigation Menu on Three-Line Click
        val btnMenu = view.findViewById<View>(R.id.btn_menu)
        btnMenu.setOnClickListener {
            val menuSheet = NavigationDrawerBottomSheet()
            menuSheet.show(childFragmentManager, "NavigationDrawerBottomSheet")
        }

        // SMS Sync Trigger Click
        btnSmsSync.setOnClickListener {
            if (hasSMSPermission()) {
                triggerSMSSync()
            } else {
                showSMSExplainerDialog()
            }
        }

        // See All Transactions click listener
        btnSeeAll.setOnClickListener {
            val bottomSheet = AllTransactionsBottomSheet()
            bottomSheet.show(childFragmentManager, "AllTransactionsBottomSheet")
        }

        // Start observers
        startObservers()

        // Sync & Permission checks
        updateSMSCardUI()
        checkSMSPromptOnLogin()
        checkNicknamePromptOnLogin()
        checkNotificationPermission()

        return view
    }

    private fun startObservers() {
        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"

        // 1. Observe Budget Settings
        budgetListener = repository.observeBudgetSettings(userId) { settings ->
            budgetSettings = settings
            updateCalculations()
        }

        // 2. Observe Transactions
        txListener = repository.observeTransactions(userId) { list ->
            activeTransactions = list
            val currentMonthTxs = list.filter { isCurrentCalendarMonth(it.timestamp) }
            adapter.updateList(currentMonthTxs)
            updateCalculations()
        }
    }

    private fun updateCalculations() {
        if (!isAdded) return

        val currentMonthTransactions = activeTransactions.filter { isCurrentCalendarMonth(it.timestamp) }

        var totalSpent = 0.0
        var mealsSpent = 0.0
        var groceriesSpent = 0.0
        var rentSpent = 0.0
        var utilitiesSpent = 0.0
        var discretionarySpent = 0.0
        var othersSpent = 0.0

        val categoryColors = mapOf(
            "meals" to "#000666",
            "dining" to "#000666",
            "dining out" to "#000666",
            "restaurant" to "#000666",
            "food" to "#000666",
            "groceries" to "#1B6D24",
            "grocery" to "#1B6D24",
            "rent" to "#6366F1",
            "utilities" to "#F59E0B",
            "discretionary" to "#EC4899",
            "others" to "#9CA3AF"
        )
        val fallbackColors = listOf("#6366F1", "#10B981", "#F59E0B", "#EF4444", "#EC4899", "#8B5CF6")

        currentMonthTransactions.forEach {
            totalSpent += it.amount
            when (it.category.lowercase().trim()) {
                "meals", "dining", "food", "dining out", "restaurant" -> mealsSpent += it.amount
                "groceries", "grocery" -> groceriesSpent += it.amount
                "rent" -> rentSpent += it.amount
                "utilities" -> utilitiesSpent += it.amount
                "discretionary" -> discretionarySpent += it.amount
                else -> othersSpent += it.amount
            }
        }

        // --- Total Calculations ---
        tvTotalSpent.text = "₹${String.format("%,.2f", totalSpent)}"
        tvBudgetLimit.text = "₹${String.format("%,.2f", budgetSettings.totalMonthlyLimit)}"
        
        val remaining = budgetSettings.totalMonthlyLimit - totalSpent
        val progress = if (budgetSettings.totalMonthlyLimit > 0) {
            ((totalSpent / budgetSettings.totalMonthlyLimit) * 100).toInt()
        } else 0
        pbTotalBudget.progress = progress

        // Alert styling for total & badge
        val context = requireContext()
        if (budgetSettings.breachAlerts && totalSpent > budgetSettings.totalMonthlyLimit) {
            // Breached State
            pbTotalBudget.setProgressDrawableTiled(ContextCompat.getDrawable(context, R.drawable.progress_bar_red))
            tvBadgeStatus.text = "Limit Breached ($progress%)"
            tvBadgeStatus.setTextColor(ContextCompat.getColor(context, R.color.white))
            tvBadgeStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.red_error))
            
            tvBudgetRemaining.text = "Breached: -₹${String.format("%,.2f", -remaining)}"
            tvBudgetRemaining.setTextColor(ContextCompat.getColor(context, R.color.red_error))
        } else if (progress >= 75) {
            // Approaching State
            pbTotalBudget.setProgressDrawableTiled(ContextCompat.getDrawable(context, R.drawable.progress_bar_amber))
            tvBadgeStatus.text = "Approaching Limit ($progress%)"
            tvBadgeStatus.setTextColor(ContextCompat.getColor(context, R.color.brand_on_tertiary_container))
            tvBadgeStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.icon_tint_delivery_bg))
            
            tvBudgetRemaining.text = "₹${String.format("%,.2f", remaining)} remaining"
            tvBudgetRemaining.setTextColor(ContextCompat.getColor(context, R.color.text_zinc_500))
        } else {
            // Healthy State
            pbTotalBudget.setProgressDrawableTiled(ContextCompat.getDrawable(context, R.drawable.progress_bar_emerald))
            tvBadgeStatus.text = "Under Control ($progress%)"
            tvBadgeStatus.setTextColor(ContextCompat.getColor(context, R.color.emerald_success))
            tvBadgeStatus.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.icon_tint_grocery_bg))
            
            tvBudgetRemaining.text = "₹${String.format("%,.2f", remaining)} remaining"
            tvBudgetRemaining.setTextColor(ContextCompat.getColor(context, R.color.text_zinc_500))
        }

        // Trigger notifications if milestone reached
        triggerBudgetNotificationAlerts(progress, totalSpent, remaining)

        // --- Update custom DonutChartView ---
        val categoryData = mapOf(
            "Meals" to mealsSpent,
            "Groceries" to groceriesSpent,
            "Rent" to rentSpent,
            "Utilities" to utilitiesSpent,
            "Discretionary" to discretionarySpent,
            "Others" to othersSpent
        )
        pbDonutChart.setData(categoryData)

        // Dynamic center text selection based on state
        if (selectedCategory.equals("Food", ignoreCase = true)) {
            val foodSpent = mealsSpent + groceriesSpent
            val foodSharePercent = if (totalSpent > 0) {
                ((foodSpent / totalSpent) * 100).toInt()
            } else 0
            
            tvDonutLabel.text = "Food"
            tvDonutPercent.text = "$foodSharePercent%"
            tvDonutPercent.setTextColor(ContextCompat.getColor(context, R.color.brand_primary))
        } else {
            val amount = categoryData[selectedCategory] ?: 0.0
            val pct = if (totalSpent > 0) ((amount / totalSpent) * 100).toInt() else 0
            val colorStr = categoryColors[selectedCategory.lowercase()] ?: "#6366F1"
            
            tvDonutLabel.text = selectedCategory
            tvDonutPercent.text = "$pct%"
            tvDonutPercent.setTextColor(Color.parseColor(colorStr))
        }

        // Reset center to Food on chart clicked
        pbDonutChart.setOnClickListener {
            selectedCategory = "Food"
            updateCalculations()
        }

        // --- Render Dynamic Legends ---
        llCategoryLegends.removeAllViews()

        val sortedActiveCategories = categoryData.filter { it.value > 0.0 }
        
        sortedActiveCategories.entries.forEachIndexed { index, entry ->
            val category = entry.key
            val amount = entry.value
            val colorStr = categoryColors[category.lowercase()] ?: fallbackColors[index % fallbackColors.size]
            val pct = if (totalSpent > 0) ((amount / totalSpent) * 100).toInt() else 0

            val row = RelativeLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 0, 0, dpToPx(8))
                }
                isClickable = true
                focusable = View.FOCUSABLE
                
                val attrs = intArrayOf(android.R.attr.selectableItemBackground)
                val typedArray = context.obtainStyledAttributes(attrs)
                background = typedArray.getDrawable(0)
                typedArray.recycle()

                setOnClickListener {
                    selectedCategory = category
                    tvDonutLabel.text = category
                    tvDonutPercent.text = "$pct%"
                    tvDonutPercent.setTextColor(Color.parseColor(colorStr))
                }
            }

            val dot = View(context).apply {
                id = View.generateViewId()
                val size = dpToPx(10)
                layoutParams = RelativeLayout.LayoutParams(size, size).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_START)
                    addRule(RelativeLayout.CENTER_VERTICAL)
                }
                val shape = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(colorStr))
                }
                background = shape
            }
            row.addView(dot)

            val label = TextView(context).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.END_OF, dot.id)
                    addRule(RelativeLayout.CENTER_VERTICAL)
                    marginStart = dpToPx(8)
                }
                text = category
                setTextColor(ContextCompat.getColor(context, R.color.text_zinc_500))
                textSize = 13f
            }
            row.addView(label)

            val valueText = TextView(context).apply {
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                    addRule(RelativeLayout.CENTER_VERTICAL)
                }
                text = "₹${String.format("%,.0f", amount)} ($pct%)"
                setTextColor(ContextCompat.getColor(context, R.color.text_zinc_900))
                textSize = 13f
                typeface = android.graphics.Typeface.MONOSPACE
                paintFlags = paintFlags or android.graphics.Paint.SUBPIXEL_TEXT_FLAG
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            }
            row.addView(valueText)

            llCategoryLegends.addView(row)
        }

        // --- Merchant Spending Calculations ---
        var zomatoSpent = 0.0
        var swiggySpent = 0.0
        var blinkitSpent = 0.0
        var zeptoSpent = 0.0

        currentMonthTransactions.forEach { tx ->
            val merchantLower = tx.merchant.lowercase()
            when {
                merchantLower.contains("zomato") -> zomatoSpent += tx.amount
                merchantLower.contains("swiggy") -> swiggySpent += tx.amount
                merchantLower.contains("blinkit") -> blinkitSpent += tx.amount
                merchantLower.contains("zepto") -> zeptoSpent += tx.amount
            }
        }

        tvMerchantZomatoVal.text = "₹${String.format("%,.0f", zomatoSpent)}"
        tvMerchantSwiggyVal.text = "₹${String.format("%,.0f", swiggySpent)}"
        tvMerchantBlinkitVal.text = "₹${String.format("%,.0f", blinkitSpent)}"
        tvMerchantZeptoVal.text = "₹${String.format("%,.0f", zeptoSpent)}"

        val maxMerchantSpent = maxOf(zomatoSpent, swiggySpent, blinkitSpent, zeptoSpent)
        pbMerchantZomato.progress = if (maxMerchantSpent > 0) ((zomatoSpent / maxMerchantSpent) * 100).toInt() else 0
        pbMerchantSwiggy.progress = if (maxMerchantSpent > 0) ((swiggySpent / maxMerchantSpent) * 100).toInt() else 0
        pbMerchantBlinkit.progress = if (maxMerchantSpent > 0) ((blinkitSpent / maxMerchantSpent) * 100).toInt() else 0
        pbMerchantZepto.progress = if (maxMerchantSpent > 0) ((zeptoSpent / maxMerchantSpent) * 100).toInt() else 0

        // --- Projections ---
        val calendar = java.util.Calendar.getInstance()
        val dayOfMonth = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val daysInMonth = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

        val dailyAverage = if (totalSpent > 0.0) {
            totalSpent / dayOfMonth.toDouble()
        } else {
            0.0
        }
        val projectedMonthly = dailyAverage * daysInMonth.toDouble()
        computedDailyAvg = dailyAverage
        computedProjected = projectedMonthly
        tvProjectionAmount.text = "₹${String.format("%,.0f", projectedMonthly)}"

        val limit = budgetSettings.totalMonthlyLimit
        if (totalSpent <= 0.0) {
            tvProjectionDescription.text = "Add your first transaction to start tracking daily averages and monthly projections."
        } else if (limit <= 0.0) {
            tvProjectionDescription.text = "Based on your current daily average of ₹${String.format("%,.0f", dailyAverage)}. Set a monthly budget limit in Profile to analyze thresholds."
        } else {
            if (projectedMonthly > limit) {
                val overdraftPercent = (((projectedMonthly - limit) / limit) * 100).toInt()
                tvProjectionDescription.text = "Based on your current daily average of ₹${String.format("%,.0f", dailyAverage)}, you are projected to exceed your monthly limit by $overdraftPercent%."
            } else {
                val utilizationPercent = ((projectedMonthly / limit) * 100).toInt()
                tvProjectionDescription.text = "Based on your current daily average of ₹${String.format("%,.0f", dailyAverage)}, you will remain within your monthly limit (using $utilizationPercent% of your budget)."
            }
        }

        // --- Weekly Trend line data feed ---
        val daysData = DoubleArray(7) { 0.0 }
        currentMonthTransactions.forEach { tx ->
            val timeLower = tx.timestamp.lowercase()
            when {
                timeLower.contains("today") || timeLower.contains("just now") -> daysData[6] += tx.amount
                timeLower.contains("yesterday") -> daysData[5] += tx.amount
                timeLower.contains("2 days ago") -> daysData[4] += tx.amount
                timeLower.contains("3 days ago") -> daysData[3] += tx.amount
                timeLower.contains("4 days ago") -> daysData[2] += tx.amount
                timeLower.contains("5 days ago") -> daysData[1] += tx.amount
                timeLower.contains("6 days ago") -> daysData[0] += tx.amount
                else -> {
                    val idx = (tx.amount.toInt() % 6)
                    daysData[idx] += tx.amount
                }
            }
        }
        vTrendLine.setSpendingData(daysData.toList())
    }

    private fun hasSMSPermission(): Boolean {
        val readSms = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS)
        val receiveSms = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECEIVE_SMS)
        return readSms == PackageManager.PERMISSION_GRANTED && receiveSms == PackageManager.PERMISSION_GRANTED
    }

    private fun checkSMSPromptOnLogin() {
        if (!hasSMSPermission()) {
            val prefs = requireContext().getSharedPreferences("spendbite_prefs", Context.MODE_PRIVATE)
            val hasPrompted = prefs.getBoolean("sms_prompt_shown", false)
            if (!hasPrompted) {
                // Post-login explainer popup
                showSMSExplainerDialog()
            }
        }
    }

    private fun checkNicknamePromptOnLogin() {
        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"
        val prefs = requireContext().getSharedPreferences("SpendBiteProPrefs", Context.MODE_PRIVATE)
        val hasPrompted = prefs.getBoolean("nickname_prompt_shown_$userId", false)
        val currentName = prefs.getString("user_nickname", null)

        if (currentName.isNullOrEmpty() && !hasPrompted) {
            val nicknameSheet = NicknameBottomSheet()
            nicknameSheet.setOnNicknameSavedListener(object : NicknameBottomSheet.OnNicknameSavedListener {
                override fun onNicknameSaved(nickname: String) {
                    prefs.edit().putBoolean("nickname_prompt_shown_$userId", true).apply()
                }
            })
            nicknameSheet.show(childFragmentManager, "NicknameBottomSheet")
            prefs.edit().putBoolean("nickname_prompt_shown_$userId", true).apply()
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 102)
            }
        }
    }

    private fun triggerBudgetNotificationAlerts(progress: Int, totalSpent: Double, remaining: Double) {
        if (!budgetSettings.breachAlerts) return
        val context = context ?: return
        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"
        val prefs = context.getSharedPreferences("SpendBiteProPrefs", Context.MODE_PRIVATE)
        val nickname = prefs.getString("user_nickname", "User") ?: "User"

        val key80 = "alert_80_$userId"
        val key100 = "alert_100_$userId"

        val sent80 = prefs.getBoolean(key80, false)
        val sent100 = prefs.getBoolean(key100, false)

        if (progress >= 100) {
            if (!sent100) {
                val title = "Budget Limit Reached! 🚨"
                val msg = "Hey $nickname, you have spent 100% of your monthly budget (₹${String.format("%,.2f", totalSpent)}). Please watch your expenses!"
                NotificationHelper.sendNotification(context, title, msg)
                prefs.edit().putBoolean(key100, true).putBoolean(key80, true).apply()
            }
        } else if (progress >= 80) {
            if (!sent80) {
                val title = "Budget Warning! ⚠️"
                val msg = "Hey $nickname, you have reached 80% of your monthly budget limit. Remaining balance is ₹${String.format("%,.2f", remaining)}."
                NotificationHelper.sendNotification(context, title, msg)
                prefs.edit().putBoolean(key80, true).apply()
            }
            if (sent100) {
                prefs.edit().putBoolean(key100, false).apply()
            }
        } else {
            if (sent80 || sent100) {
                prefs.edit().putBoolean(key80, false).putBoolean(key100, false).apply()
            }
        }
    }

    private fun showSMSExplainerDialog() {
        val dialog = SMSOnboardingDialog.newInstance(object : SMSOnboardingDialog.OnSMSOnboardingListener {
            override fun onPermissionGranted() {
                updateSMSCardUI()
                triggerSMSSync()
            }
        })
        dialog.show(childFragmentManager, "SMSOnboardingDialog")
    }

    private fun triggerSMSSync() {
        val context = context ?: return
        Toast.makeText(context, "Scanning transaction history...", Toast.LENGTH_SHORT).show()
        SMSSyncManager.syncHistoricalSMS(context) { count ->
            if (isAdded) {
                Toast.makeText(context, "Successfully synced $count transactions from the last 30 days!", Toast.LENGTH_LONG).show()
                updateCalculations()
            }
        }
    }

    private fun updateSMSCardUI() {
        if (!isAdded) return
        if (hasSMSPermission()) {
            viewSmsStatusDot.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.brand_secondary))
            tvSmsStatusBadge.text = "Active"
            tvSmsStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.brand_secondary))
            btnSmsSync.text = "Force Sync Now"
        } else {
            viewSmsStatusDot.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.text_zinc_400))
            tvSmsStatusBadge.text = "Disabled"
            tvSmsStatusBadge.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_zinc_500))
            btnSmsSync.text = "Link SMS Ledger"
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            val context = context ?: return
            val prefs = context.getSharedPreferences("spendbite_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("sms_prompt_shown", true).apply()

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateSMSCardUI()
                triggerSMSSync()
            } else {
                Toast.makeText(context, "SMS auto-tracking disabled. You can link it later from this card.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val smsPollHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val smsPollRunnable = object : Runnable {
        override fun run() {
            triggerSilentSMSSync()
            smsPollHandler.postDelayed(this, 10000)
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasSMSPermission()) {
            smsPollHandler.postDelayed(smsPollRunnable, 10000)
        }
        displayTopAvatar()
    }

    fun refreshAvatar() {
        if (isAdded) {
            displayTopAvatar()
        }
    }

    private fun displayTopAvatar() {
        val view = view ?: return
        val ivTopAvatar = view.findViewById<ImageView>(R.id.iv_top_avatar) ?: return
        val context = context ?: return
        val sharedPref = context.getSharedPreferences("SpendBiteProPrefs", Context.MODE_PRIVATE)
        val photoPath = sharedPref.getString("user_profile_photo", null)
        if (!photoPath.isNullOrEmpty()) {
            val file = java.io.File(photoPath)
            if (file.exists()) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    ivTopAvatar.setImageBitmap(bitmap)
                    ivTopAvatar.imageTintList = null
                }
            }
        } else {
            ivTopAvatar.setImageResource(R.drawable.ic_profile)
            ivTopAvatar.imageTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(context, R.color.brand_primary)
            )
        }
    }

    override fun onPause() {
        super.onPause()
        smsPollHandler.removeCallbacks(smsPollRunnable)
    }

    private fun triggerSilentSMSSync() {
        val context = context ?: return
        if (!hasSMSPermission()) return
        SMSSyncManager.syncHistoricalSMS(context) { count ->
            if (isAdded && count > 0) {
                updateCalculations()
            }
        }
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    private fun isCurrentCalendarMonth(timestamp: String): Boolean {
        val now = Date()
        val currentMonthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val sdfFull = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault())
        
        var datePartStr = when {
            timestamp.startsWith("Today,", ignoreCase = true) -> {
                val currentDayPrefix = SimpleDateFormat("MMM dd,", Locale.getDefault()).format(now)
                timestamp.replace("Today,", currentDayPrefix)
            }
            timestamp.startsWith("Yesterday,", ignoreCase = true) -> {
                val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
                val yesterdayPrefix = SimpleDateFormat("MMM dd,", Locale.getDefault()).format(yesterday)
                timestamp.replace("Yesterday,", yesterdayPrefix)
            }
            else -> timestamp
        }
        
        if (!datePartStr.contains(currentYear.toString())) {
            val commaIndex = datePartStr.indexOf(",")
            if (commaIndex != -1) {
                datePartStr = datePartStr.substring(0, commaIndex) + ", " + currentYear + datePartStr.substring(commaIndex)
            } else {
                datePartStr = "$datePartStr, $currentYear"
            }
        }
        
        return try {
            val date = sdfFull.parse(datePartStr) ?: now
            val txMonthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
            txMonthYear == currentMonthYear
        } catch (e: Exception) {
            true
        }
    }
}
