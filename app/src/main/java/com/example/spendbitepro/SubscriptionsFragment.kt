package com.example.spendbitepro

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubscriptionsFragment : Fragment() {

    // Summary widgets
    private lateinit var tvRoiSaved: TextView
    private lateinit var pbRoiProgress: ProgressBar
    private lateinit var tvRoiCost: TextView
    private lateinit var tvRoiBenefit: TextView
    private lateinit var tvRoiNetGain: TextView
    private lateinit var tvRoiStatusText: TextView
    private lateinit var llRoiStatusBadge: View
    private lateinit var btnRoiInsights: Button

    // List Control
    private lateinit var rvSubscriptionsList: RecyclerView
    private lateinit var btnAddSubscriptionTrigger: View
    
    private lateinit var llInsightChip: View
    private lateinit var tvInsightText: TextView

    // Breakdown widgets
    private lateinit var tvBreakdownDelivery: TextView
    private lateinit var tvBreakdownDeliveryDesc: TextView
    private lateinit var tvBreakdownDiscounts: TextView
    private lateinit var tvBreakdownPlatform: TextView
    private lateinit var tvBreakdownPlatformDesc: TextView
    private lateinit var tvBreakdownBreakeven: TextView
    private lateinit var tvBreakdownBreakevenDesc: TextView

    private lateinit var btnSave: Button
    private lateinit var adapter: SubscriptionAdapter
    private lateinit var llSubscriptionWarnings: LinearLayout

    // Observers reference
    private var subListener: Any? = null
    private var txListener: Any? = null
    private var activeTransactions: List<Transaction> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_subscriptions, container, false)

        // ROI Summary bindings
        tvRoiSaved = view.findViewById(R.id.tv_roi_saved)
        pbRoiProgress = view.findViewById(R.id.pb_roi_progress)
        tvRoiCost = view.findViewById(R.id.tv_roi_cost)
        tvRoiBenefit = view.findViewById(R.id.tv_roi_benefit)
        tvRoiNetGain = view.findViewById(R.id.tv_roi_net_gain)
        tvRoiStatusText = view.findViewById(R.id.tv_roi_status_text)
        llRoiStatusBadge = view.findViewById(R.id.ll_roi_status_badge)
        btnRoiInsights = view.findViewById(R.id.btn_roi_insights)

        // List bindings
        rvSubscriptionsList = view.findViewById(R.id.rv_subscriptions_list)
        btnAddSubscriptionTrigger = view.findViewById(R.id.btn_add_subscription_trigger)
        
        llInsightChip = view.findViewById(R.id.ll_insight_chip)
        tvInsightText = view.findViewById(R.id.tv_insight_text)

        // Breakdown bindings
        tvBreakdownDelivery = view.findViewById(R.id.tv_breakdown_delivery)
        tvBreakdownDeliveryDesc = view.findViewById(R.id.tv_breakdown_delivery_desc)
        tvBreakdownDiscounts = view.findViewById(R.id.tv_breakdown_discounts)
        tvBreakdownPlatform = view.findViewById(R.id.tv_breakdown_platform)
        tvBreakdownPlatformDesc = view.findViewById(R.id.tv_breakdown_platform_desc)
        tvBreakdownBreakeven = view.findViewById(R.id.tv_breakdown_breakeven)
        tvBreakdownBreakevenDesc = view.findViewById(R.id.tv_breakdown_breakeven_desc)

        btnSave = view.findViewById(R.id.btn_save_subs)
        llSubscriptionWarnings = view.findViewById(R.id.ll_subscription_warnings)

        // Setup RecyclerView
        rvSubscriptionsList.layoutManager = LinearLayoutManager(context)
        adapter = SubscriptionAdapter(emptyList()) {
            recalculateLiability()
        }
        rvSubscriptionsList.adapter = adapter

        // Add trigger
        btnAddSubscriptionTrigger.setOnClickListener {
            val addSheet = AddSubscriptionBottomSheet()
            addSheet.show(childFragmentManager, "AddSubscriptionBottomSheet")
        }

        // Save action
        btnSave.setOnClickListener {
            saveSubscriptions()
        }

        // Insights click
        llInsightChip.setOnClickListener {
            showInsightsDialog()
        }
        btnRoiInsights.setOnClickListener {
            showInsightsDialog()
        }

        // Hamburger Menu click
        val btnMenu = view.findViewById<View>(R.id.btn_menu)
        btnMenu?.setOnClickListener {
            val menuSheet = NavigationDrawerBottomSheet()
            menuSheet.show(childFragmentManager, "NavigationDrawerBottomSheet")
        }

        // Circular Avatar Click
        val cvAvatar = view.findViewById<View>(R.id.cv_avatar)
        cvAvatar?.setOnClickListener {
            (activity as? MainActivity)?.navigateTo("profile")
        }

        loadSubscriptions()

        return view
    }

    private fun loadSubscriptions() {
        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"

        txListener = repository.observeTransactions(userId) { list ->
            activeTransactions = list
            recalculateLiability()
        }

        subListener = repository.observeSubscriptions(userId) { list ->
            if (isAdded) {
                adapter.updateList(list)
                recalculateLiability()
            }
        }
    }

    private fun recalculateLiability() {
        if (!isAdded) return

        var totalCost = 0.0
        var totalBenefit = 0.0
        var deliveryFees = 0.0
        var discounts = 0.0
        var platformFees = 0.0
        var activeCount = 0
        var lastActiveSub: SubscriptionItem? = null
        
        var totalDeliveryOrdersAvoided = 0
        var totalPlatformWaivers = 0

        val currentList = adapter.getItems()
        currentList.forEach { sub ->
            if (sub.isActive) {
                activeCount++
                lastActiveSub = sub
                
                // Cost over 3 billing cycles
                val cycles = 3.0
                val cost = sub.monthlyFee * cycles
                totalCost += cost

                var subBenefit = 0.0
                var subDelivery = 0.0
                var subDiscounts = 0.0
                var subPlatform = 0.0

                if (sub.id == "sub_zomato") {
                    val zomatoTxs = activeTransactions.filter { 
                        it.merchant.contains("Zomato", ignoreCase = true) 
                    }
                    zomatoTxs.forEach { tx ->
                        val amt = tx.amount
                        var orderDelivery = 0.0
                        var orderDiscount = 0.0
                        var orderPlatform = 0.0

                        if (amt >= 199.0) {
                            orderDelivery = 40.0
                            totalDeliveryOrdersAvoided++
                        }
                        orderDiscount = Math.min(amt * 0.10, 100.0)
                        orderPlatform = 5.0
                        totalPlatformWaivers++

                        subDelivery += orderDelivery
                        subDiscounts += orderDiscount
                        subPlatform += orderPlatform
                    }
                } else if (sub.id == "sub_swiggy") {
                    val swiggyTxs = activeTransactions.filter { 
                        it.merchant.contains("Swiggy", ignoreCase = true) || 
                        it.merchant.contains("Instamart", ignoreCase = true)
                    }
                    swiggyTxs.forEach { tx ->
                        val amt = tx.amount
                        var orderDelivery = 0.0
                        var orderDiscount = 0.0
                        var orderPlatform = 0.0

                        if (tx.category.contains("Groceries", ignoreCase = true)) {
                            // Instamart
                            if (amt >= 199.0) {
                                orderDelivery = 30.0
                                totalDeliveryOrdersAvoided++
                            }
                            orderDiscount = Math.min(amt * 0.05, 50.0)
                        } else {
                            // Swiggy Food
                            if (amt >= 149.0) {
                                orderDelivery = 35.0
                                totalDeliveryOrdersAvoided++
                            }
                            orderDiscount = Math.min(amt * 0.10, 80.0)
                            orderPlatform = 5.0
                            totalPlatformWaivers++
                        }

                        subDelivery += orderDelivery
                        subDiscounts += orderDiscount
                        subPlatform += orderPlatform
                    }
                } else {
                    // Custom subscription: match merchant containing subscription name
                    val customTxs = activeTransactions.filter { 
                        it.merchant.contains(sub.name, ignoreCase = true) ||
                        sub.name.contains(it.merchant, ignoreCase = true)
                    }
                    if (customTxs.isNotEmpty()) {
                        customTxs.forEach { tx ->
                            val amt = tx.amount
                            val orderDelivery = 40.0
                            val orderDiscount = amt * 0.05
                            totalDeliveryOrdersAvoided++

                            subDelivery += orderDelivery
                            subDiscounts += orderDiscount
                        }
                    } else {
                        // Fallback baseline multiplier
                        val baselineBenefit = cost * 1.5
                        subDelivery = baselineBenefit * 0.60
                        subDiscounts = baselineBenefit * 0.40
                    }
                }

                subBenefit = subDelivery + subDiscounts + subPlatform
                totalBenefit += subBenefit
                
                deliveryFees += subDelivery
                discounts += subDiscounts
                platformFees += subPlatform
            }
        }

        val netSaved = totalBenefit - totalCost

        // Display results
        tvRoiSaved.text = "₹${String.format("%,.0f", if (netSaved > 0) netSaved else 0.0)}"
        tvRoiCost.text = "Total Cost: ₹${String.format("%,.0f", totalCost)}"
        tvRoiBenefit.text = "Total Benefit: ₹${String.format("%,.0f", totalBenefit)}"

        // Ratio progress
        val progress = if (totalBenefit > 0) {
            ((totalCost / totalBenefit) * 100).toInt()
        } else 0
        pbRoiProgress.progress = Math.min(100, Math.max(0, 100 - progress))

        val context = requireContext()
        // Status badge: PROFITABLE or LOSS-MAKING
        if (netSaved > 0) {
            llRoiStatusBadge.visibility = View.VISIBLE
            tvRoiStatusText.text = "PROFITABLE"
            llRoiStatusBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.icon_tint_grocery_bg))
            tvRoiStatusText.setTextColor(ContextCompat.getColor(context, R.color.brand_secondary))
            
            val gain = netSaved / 3.0 // Monthly net gain
            tvRoiNetGain.text = "₹${String.format("%,.0f", gain)}"
        } else if (netSaved < 0) {
            llRoiStatusBadge.visibility = View.VISIBLE
            tvRoiStatusText.text = "LOSS-MAKING"
            llRoiStatusBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.icon_tint_restaurant_bg))
            tvRoiStatusText.setTextColor(ContextCompat.getColor(context, R.color.red_error))
            
            tvRoiNetGain.text = "₹0"
        } else {
            llRoiStatusBadge.visibility = View.INVISIBLE
            tvRoiNetGain.text = "₹0"
        }

        // Savings Breakdown text views
        tvBreakdownDelivery.text = "₹${String.format("%,.0f", deliveryFees)}"
        tvBreakdownDiscounts.text = "₹${String.format("%,.0f", discounts)}"
        tvBreakdownPlatform.text = "₹${String.format("%,.0f", platformFees)}"

        if (activeCount > 0) {
            tvBreakdownDeliveryDesc.text = if (totalDeliveryOrdersAvoided > 0) "$totalDeliveryOrdersAvoided orders avoided fees" else "0 orders avoided fees"
            tvBreakdownPlatformDesc.text = if (totalPlatformWaivers > 0) "Waived on $totalPlatformWaivers orders" else "No platform fee waivers"
            
            if (netSaved > 0) {
                tvBreakdownBreakevenDesc.text = "Achieved this billing cycle"
            } else {
                tvBreakdownBreakevenDesc.text = "Not yet achieved"
            }
        } else {
            tvBreakdownDeliveryDesc.text = "No orders tracked yet"
            tvBreakdownPlatformDesc.text = "No platform fee waivers"
            tvBreakdownBreakevenDesc.text = "Not yet achieved"
        }

        if (activeCount > 1) {
            tvBreakdownBreakeven.text = "Order #3"
            llInsightChip.visibility = View.VISIBLE
            val ratio = if (totalCost > 0) (totalBenefit / totalCost) else 1.0
            tvInsightText.text = "You're saving ${String.format("%.1fx", ratio)} the cost of your memberships."
            llInsightChip.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.tertiary_fixed))
        } else if (activeCount == 1 && lastActiveSub != null) {
            val ratio = if (totalCost > 0) (totalBenefit / totalCost) else 1.0
            llInsightChip.visibility = View.VISIBLE
            tvInsightText.text = "You're saving ${String.format("%.1fx", ratio)} the cost of your ${lastActiveSub!!.name} membership."
            llInsightChip.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.tertiary_fixed))
            
            var cumulativeBenefit = 0.0
            var breakevenIndex = -1
            val matchedTxs = activeTransactions.filter { tx: Transaction ->
                if (lastActiveSub!!.id == "sub_swiggy") {
                    tx.merchant.contains("Swiggy", ignoreCase = true) || tx.merchant.contains("Instamart", ignoreCase = true)
                } else {
                    tx.merchant.contains(lastActiveSub!!.name, ignoreCase = true)
                }
            }.sortedBy { parseTransactionTimestamp(it.timestamp) }

            matchedTxs.forEachIndexed { idx, tx ->
                val amt = tx.amount
                val orderBenefit = if (lastActiveSub!!.id == "sub_swiggy") {
                    if (tx.category.contains("Groceries", ignoreCase = true)) {
                        (if (amt >= 199.0) 30.0 else 0.0) + Math.min(amt * 0.05, 50.0)
                    } else {
                        (if (amt >= 149.0) 35.0 else 0.0) + Math.min(amt * 0.10, 80.0) + 5.0
                    }
                } else if (lastActiveSub!!.id == "sub_zomato") {
                    (if (amt >= 199.0) 40.0 else 0.0) + Math.min(amt * 0.10, 100.0) + 5.0
                } else {
                    40.0 + (amt * 0.05)
                }
                
                cumulativeBenefit += orderBenefit
                if (cumulativeBenefit >= totalCost && breakevenIndex == -1) {
                    breakevenIndex = idx + 1
                }
            }

            tvBreakdownBreakeven.text = if (breakevenIndex != -1) "Order #$breakevenIndex" else "N/A"
        } else {
            tvBreakdownBreakeven.text = "N/A"
            llInsightChip.visibility = View.GONE
        }

        // Dynamic warnings compiler
        llSubscriptionWarnings.removeAllViews()
        val warnings = mutableListOf<View>()

        currentList.forEach { sub ->
            if (sub.isActive) {
                val monthlyFee = sub.monthlyFee
                val cycles = 3.0
                val cost = monthlyFee * cycles
                
                val subTxs = activeTransactions.filter {
                    if (sub.id == "sub_zomato") it.merchant.contains("Zomato", ignoreCase = true)
                    else if (sub.id == "sub_swiggy") it.merchant.contains("Swiggy", ignoreCase = true) || it.merchant.contains("Instamart", ignoreCase = true)
                    else it.merchant.contains(sub.name, ignoreCase = true)
                }
                
                var monthlyBenefit = 0.0
                subTxs.forEach { tx ->
                    val amt = tx.amount
                    monthlyBenefit += if (sub.id == "sub_zomato") {
                        (if (amt >= 199.0) 40.0 else 0.0) + Math.min(amt * 0.10, 100.0) + 5.0
                    } else if (sub.id == "sub_swiggy") {
                        if (tx.category.contains("Groceries", ignoreCase = true)) {
                            (if (amt >= 199.0) 30.0 else 0.0) + Math.min(amt * 0.05, 50.0)
                        } else {
                            (if (amt >= 149.0) 35.0 else 0.0) + Math.min(amt * 0.10, 80.0) + 5.0
                        }
                    } else {
                        40.0 + (amt * 0.05)
                    }
                }
                
                val avgMonthlyBenefit = monthlyBenefit / cycles
                
                if (avgMonthlyBenefit < monthlyFee) {
                    val alertView = createAlertCard(
                        title = "${sub.name} is Unprofitable",
                        message = "Your average monthly savings (₹${avgMonthlyBenefit.toInt()}) are less than your monthly fee of ₹${monthlyFee.toInt()}. Try ordering more on ${sub.name} to maximize your subscription value.",
                        isWarning = true
                    )
                    warnings.add(alertView)
                } else {
                    val alertView = createAlertCard(
                        title = "${sub.name} Renewal",
                        message = "Renewal of ₹${monthlyFee.toInt()} is due soon. Net accumulated savings: ₹${(monthlyBenefit - cost).toInt()}.",
                        isWarning = false
                    )
                    warnings.add(alertView)
                }
            }
        }

        if (warnings.isNotEmpty()) {
            llSubscriptionWarnings.visibility = View.VISIBLE
            warnings.forEach { llSubscriptionWarnings.addView(it) }
        } else {
            llSubscriptionWarnings.visibility = View.GONE
        }
    }

    private fun saveSubscriptions() {
        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"

        btnSave.isEnabled = false
        val currentList = adapter.getItems()
        
        if (currentList.isEmpty()) {
            btnSave.isEnabled = true
            Toast.makeText(context, "No memberships to update", Toast.LENGTH_SHORT).show()
            return
        }

        var pendingUpdates = currentList.size
        var anyFailed = false

        currentList.forEach { item ->
            repository.updateSubscription(userId, item) { success ->
                if (!success) anyFailed = true
                pendingUpdates--
                if (pendingUpdates == 0) {
                    btnSave.isEnabled = true
                    if (!anyFailed) {
                        Toast.makeText(context, "Membership modifications applied", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to apply some membership changes", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun createAlertCard(title: String, message: String, isWarning: Boolean): View {
        val context = requireContext()
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dpToPx(12))
            }
            val pad = dpToPx(12)
            setPadding(pad, pad, pad, pad)
            
            val shape = GradientDrawable().apply {
                cornerRadius = dpToPx(12).toFloat()
                if (isWarning) {
                    setColor(Color.parseColor("#FFFBEB")) // light yellow bg
                    setStroke(dpToPx(1), Color.parseColor("#F59E0B")) // amber border
                } else {
                    setColor(Color.parseColor("#EFF6FF")) // light blue bg
                    setStroke(dpToPx(1), Color.parseColor("#3B82F6")) // blue border
                }
            }
            background = shape
        }

        val iconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(20), dpToPx(20)).apply {
                gravity = android.view.Gravity.TOP
                topMargin = dpToPx(2)
            }
            val resId = if (isWarning) {
                android.R.drawable.ic_dialog_alert
            } else {
                android.R.drawable.ic_dialog_info
            }
            setImageResource(resId)
            val color = if (isWarning) "#D97706" else "#2563EB"
            imageTintList = ColorStateList.valueOf(Color.parseColor(color))
        }
        card.addView(iconView)

        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dpToPx(10)
            }
        }

        val titleView = TextView(context).apply {
            text = title
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            val color = if (isWarning) "#92400E" else "#1E40AF"
            setTextColor(Color.parseColor(color))
        }
        textContainer.addView(titleView)

        val descView = TextView(context).apply {
            text = message
            textSize = 12f
            val color = if (isWarning) "#B45309" else "#1D4ED8"
            setTextColor(Color.parseColor(color))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dpToPx(2)
            }
        }
        textContainer.addView(descView)

        card.addView(textContainer)
        return card
    }

    private fun showInsightsDialog() {
        val context = context ?: return
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_custom_confirm, null)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tv_dialog_title)
        val tvMsg = dialogView.findViewById<TextView>(R.id.tv_dialog_message)
        val btnClose = dialogView.findViewById<Button>(R.id.btn_dialog_positive)
        val btnNeg = dialogView.findViewById<Button>(R.id.btn_dialog_negative)

        val ivIcon = dialogView.findViewById<ImageView>(R.id.iv_dialog_icon)
        ivIcon?.setImageResource(android.R.drawable.ic_dialog_info)
        ivIcon?.imageTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(context, R.color.brand_primary)
        )

        tvTitle.text = "Membership ROI Insights"
        
        val message = StringBuilder()
        var activeCount = 0
        var totalCost = 0.0
        var totalBenefit = 0.0
        
        adapter.getItems().forEach { sub ->
            if (sub.isActive) {
                activeCount++
                val cost = sub.monthlyFee * 3.0
                totalCost += cost

                var benefit = 0.0
                if (sub.id == "sub_zomato") {
                    val zomatoTxs = activeTransactions.filter { it.merchant.contains("Zomato", ignoreCase = true) }
                    zomatoTxs.forEach { tx ->
                        val amt = tx.amount
                        val delivery = if (amt >= 199.0) 40.0 else 0.0
                        val discount = Math.min(amt * 0.10, 100.0)
                        val platform = 5.0
                        benefit += (delivery + discount + platform)
                    }
                } else if (sub.id == "sub_swiggy") {
                    val swiggyTxs = activeTransactions.filter { 
                        it.merchant.contains("Swiggy", ignoreCase = true) || it.merchant.contains("Instamart", ignoreCase = true)
                    }
                    swiggyTxs.forEach { tx ->
                        val amt = tx.amount
                        if (tx.category.contains("Groceries", ignoreCase = true)) {
                            val delivery = if (amt >= 199.0) 30.0 else 0.0
                            val discount = Math.min(amt * 0.05, 50.0)
                            benefit += (delivery + discount)
                        } else {
                            val delivery = if (amt >= 149.0) 35.0 else 0.0
                            val discount = Math.min(amt * 0.10, 80.0)
                            val platform = 5.0
                            benefit += (delivery + discount + platform)
                        }
                    }
                } else {
                    val customTxs = activeTransactions.filter { 
                        it.merchant.contains(sub.name, ignoreCase = true) || sub.name.contains(it.merchant, ignoreCase = true)
                    }
                    if (customTxs.isNotEmpty()) {
                        customTxs.forEach { tx ->
                            val amt = tx.amount
                            benefit += 40.0 + (amt * 0.05)
                        }
                    } else {
                        benefit = cost * 1.5
                    }
                }
                
                totalBenefit += benefit
                val netGain = benefit - cost
                message.append("• ${sub.name}:\n")
                message.append("  - Cost (3 cycles): ₹${cost.toInt()}\n")
                message.append("  - Benefit: ₹${benefit.toInt()}\n")
                if (netGain >= 0) {
                    message.append("  - Status: Net Gain of ₹${netGain.toInt()} (PROFITABLE)\n\n")
                } else {
                    message.append("  - Status: Net Loss of ₹${Math.abs(netGain).toInt()} (LOSS-MAKING)\n\n")
                }
            }
        }
        
        if (activeCount == 0) {
            message.append("No active memberships detected. Add or enable a membership below to start tracking your Return on Investment (ROI) insights!")
        } else {
            val netSaved = totalBenefit - totalCost
            message.append("SUMMARY:\n")
            message.append("You are tracking $activeCount active membership(s).\n\n")
            if (netSaved >= 0) {
                message.append("Overall, you are saving ₹${netSaved.toInt()} across all subscriptions. Keep using your memberships to maximize savings!")
            } else {
                message.append("Overall, you are in a net loss of ₹${Math.abs(netSaved).toInt()}. We recommend placing more orders to reach breakeven, or disabling under-utilized subscriptions.")
            }
        }
        
        tvMsg.text = message.toString()
        btnClose.text = "Got It"
        btnNeg.visibility = View.GONE

        val dialog = androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .create()
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (subListener as? com.google.firebase.firestore.ListenerRegistration)?.remove()
        (txListener as? com.google.firebase.firestore.ListenerRegistration)?.remove()
    }

    private fun parseTransactionTimestamp(timestamp: String): Date {
        val now = Date()
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
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
            sdfFull.parse(datePartStr) ?: now
        } catch (e: Exception) {
            now
        }
    }

    override fun onResume() {
        super.onResume()
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
}
