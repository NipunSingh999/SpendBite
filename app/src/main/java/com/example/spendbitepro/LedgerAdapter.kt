package com.example.spendbitepro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class LedgerAdapter(
    private var ledgerItems: List<SharedLedgerItem>,
    private val onItemLongClicked: (SharedLedgerItem) -> Unit
) : RecyclerView.Adapter<LedgerAdapter.LedgerViewHolder>() {

    fun updateList(newList: List<SharedLedgerItem>) {
        ledgerItems = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LedgerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ledger, parent, false)
        return LedgerViewHolder(view)
    }

    override fun onBindViewHolder(holder: LedgerViewHolder, position: Int) {
        val item = ledgerItems[position]
        holder.bind(item)
        holder.itemView.setOnLongClickListener {
            onItemLongClicked(item)
            true
        }
    }

    override fun getItemCount(): Int = ledgerItems.size

    class LedgerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_ledger_title)
        private val tvPaidBy: TextView = itemView.findViewById(R.id.tv_ledger_paid_by)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_ledger_amount)
        private val tvOweText: TextView = itemView.findViewById(R.id.tv_ledger_owe_text)
        private val ivReceiptIcon: ImageView = itemView.findViewById(R.id.iv_receipt_icon)

        fun bind(item: SharedLedgerItem) {
            tvTitle.text = item.title
            tvPaidBy.text = "Paid by ${item.paidBy}"
            tvAmount.text = "₹${String.format("%.2f", item.amount)}"
            tvOweText.text = item.oweText

            val context = itemView.context

            // Icon showing receipt mock attachment
            if (item.image != null) {
                ivReceiptIcon.setImageResource(android.R.drawable.ic_menu_gallery)
            } else {
                ivReceiptIcon.setImageResource(R.drawable.ic_shopping)
            }

            // Owe type styling
            val colorRes = when (item.oweType?.lowercase()) {
                "success" -> R.color.emerald_success
                "error" -> R.color.red_error
                else -> R.color.text_zinc_500
            }
            tvOweText.setTextColor(ContextCompat.getColor(context, colorRes))
        }
    }
}
