package com.example.spendbitepro

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class SettlementAdapter(
    private var settlements: List<QuickSettlement>,
    private val onSettleClick: (QuickSettlement) -> Unit
) : RecyclerView.Adapter<SettlementAdapter.SettlementViewHolder>() {

    fun updateList(newList: List<QuickSettlement>) {
        settlements = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettlementViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_settlement, parent, false)
        return SettlementViewHolder(view)
    }

    override fun onBindViewHolder(holder: SettlementViewHolder, position: Int) {
        holder.bind(settlements[position], onSettleClick)
    }

    override fun getItemCount(): Int = settlements.size

    class SettlementViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val flAvatar: View = itemView.findViewById(R.id.fl_settle_avatar)
        private val tvInitials: TextView = itemView.findViewById(R.id.tv_settle_initials)
        private val tvName: TextView = itemView.findViewById(R.id.tv_settle_name)
        private val tvAmountText: TextView = itemView.findViewById(R.id.tv_settle_amount_text)
        private val btnSettle: Button = itemView.findViewById(R.id.btn_settle_action)

        fun bind(settlement: QuickSettlement, onSettleClick: (QuickSettlement) -> Unit) {
            tvName.text = settlement.name
            tvInitials.text = settlement.initials

            val context = itemView.context

            // Formulate debt summary and button styling
            if (settlement.type == "owe") {
                tvAmountText.text = "you owe ₹${String.format("%.2f", settlement.amount)}"
                tvAmountText.setTextColor(ContextCompat.getColor(context, R.color.red_error))
                
                btnSettle.text = "Pay Now"
                btnSettle.setBackgroundResource(R.drawable.button_accent_bg)
                btnSettle.setTextColor(Color.WHITE)
            } else {
                tvAmountText.text = "owes you ₹${String.format("%.2f", settlement.amount)}"
                tvAmountText.setTextColor(ContextCompat.getColor(context, R.color.brand_secondary))
                
                btnSettle.text = "Remind"
                btnSettle.setBackgroundColor(Color.TRANSPARENT)
                btnSettle.setTextColor(ContextCompat.getColor(context, R.color.brand_primary))
            }

            // Set Avatar Circle background color dynamically
            try {
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.OVAL
                drawable.setColor(Color.parseColor(settlement.bgClass))
                flAvatar.background = drawable
            } catch (e: Exception) {
                // Fallback if parsing fails
                flAvatar.setBackgroundResource(R.drawable.button_accent_bg)
            }

            btnSettle.setOnClickListener { onSettleClick(settlement) }
        }
    }
}
