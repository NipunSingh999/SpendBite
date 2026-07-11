package com.example.spendbitepro

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ForecastBottomSheet : BottomSheetDialogFragment() {

    private lateinit var tvDailyAvg: TextView
    private lateinit var tvProjected: TextView
    private lateinit var tvLimit: TextView
    private lateinit var llStatusBadge: View
    private lateinit var ivStatusIcon: ImageView
    private lateinit var tvStatusTitle: TextView
    private lateinit var tvExplanation: TextView
    private lateinit var btnClose: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_forecast, container, false)

        tvDailyAvg = view.findViewById(R.id.tv_forecast_daily_avg)
        tvProjected = view.findViewById(R.id.tv_forecast_projected)
        tvLimit = view.findViewById(R.id.tv_forecast_limit)
        llStatusBadge = view.findViewById(R.id.ll_forecast_status_badge)
        ivStatusIcon = view.findViewById(R.id.iv_forecast_status_icon)
        tvStatusTitle = view.findViewById(R.id.tv_forecast_status_title)
        tvExplanation = view.findViewById(R.id.tv_forecast_explanation)
        btnClose = view.findViewById(R.id.btn_forecast_close)

        val dailyAvg = arguments?.getDouble("daily_avg") ?: 0.0
        val projected = arguments?.getDouble("projected") ?: 0.0
        val limit = arguments?.getDouble("limit") ?: 0.0

        tvDailyAvg.text = "₹${String.format("%,.0f", dailyAvg)}"
        tvProjected.text = "₹${String.format("%,.0f", projected)}"
        tvLimit.text = "₹${String.format("%,.0f", limit)}"

        val context = requireContext()
        if (projected > limit) {
            // Breach Risk Warning
            llStatusBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.icon_tint_restaurant_bg))
            ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert)
            ivStatusIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.red_error))
            tvStatusTitle.text = "BREACH RISK: Budget over-limit forecasted."
            tvStatusTitle.setTextColor(ContextCompat.getColor(context, R.color.red_error))
            
            val breachAmount = projected - limit
            tvExplanation.text = "Based on your daily average spending of ₹${String.format("%,.0f", dailyAvg)}, you are projected to breach your monthly limit by ₹${String.format("%,.0f", breachAmount)} this month. We recommend cutting down on dine-out or grocery deliveries."
        } else {
            // Safe / On Track
            llStatusBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.icon_tint_grocery_bg))
            ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_info)
            ivStatusIcon.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.emerald_success))
            tvStatusTitle.text = "ON TRACK: Safe spending forecasted."
            tvStatusTitle.setTextColor(ContextCompat.getColor(context, R.color.emerald_success))
            tvExplanation.text = "Your spending habits are looking healthy! If you maintain this average daily pace of ₹${String.format("%,.0f", dailyAvg)}, you are projected to spend ₹${String.format("%,.0f", projected)} total, remaining safely within your budget limit of ₹${String.format("%,.0f", limit)}."
        }

        btnClose.setOnClickListener { dismiss() }

        return view
    }

    companion object {
        fun newInstance(dailyAvg: Double, projected: Double, limit: Double): ForecastBottomSheet {
            val fragment = ForecastBottomSheet()
            val args = Bundle().apply {
                putDouble("daily_avg", dailyAvg)
                putDouble("projected", projected)
                putDouble("limit", limit)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
