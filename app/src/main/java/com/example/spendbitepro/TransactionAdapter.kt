package com.example.spendbitepro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class TransactionAdapter(
    private var transactions: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    fun updateList(newList: List<Transaction>) {
        transactions = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        holder.bind(transaction, onItemClick)
    }

    override fun getItemCount(): Int = transactions.size

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.iv_category_icon)
        private val tvMerchant: TextView = itemView.findViewById(R.id.tv_merchant)
        private val ivParsedBadge: ImageView = itemView.findViewById(R.id.iv_parsed_badge)
        private val tvCategoryTimestamp: TextView = itemView.findViewById(R.id.tv_category_timestamp)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_amount)
        private val tvPaymentMethod: TextView = itemView.findViewById(R.id.tv_payment_method)
        private val ivTrend: ImageView = itemView.findViewById(R.id.iv_trend)

        fun bind(transaction: Transaction, onItemClick: (Transaction) -> Unit) {
            tvMerchant.text = transaction.merchant
            tvCategoryTimestamp.text = "${transaction.category} • ${transaction.timestamp}"
            tvAmount.text = "-₹${String.format("%.2f", transaction.amount)}"
            tvPaymentMethod.text = transaction.paymentMethod

            // Parsed Badge
            ivParsedBadge.visibility = if (transaction.isParsed) View.VISIBLE else View.GONE

            // Category Icon mapping
            val context = itemView.context
            val iconRes = when (transaction.category.lowercase()) {
                "meals", "dining" -> R.drawable.ic_coffee
                "groceries" -> R.drawable.ic_shopping
                "rent" -> R.drawable.ic_home
                "utilities" -> R.drawable.ic_plane // using plane or custom
                else -> R.drawable.ic_utensils
            }
            ivCategoryIcon.setImageResource(iconRes)

            // Trend Icon mapping
            val trendRes = when (transaction.spendingTrend?.lowercase()) {
                "up" -> R.drawable.ic_trending_up
                "down" -> R.drawable.ic_trending_down
                else -> R.drawable.ic_trending_stable
            }
            ivTrend.setImageResource(trendRes)

            // Trend color coding
            val trendColor = when (transaction.spendingTrend?.lowercase()) {
                "up" -> R.color.red_error
                "down" -> R.color.emerald_success
                else -> R.color.text_zinc_500
            }
            ivTrend.setColorFilter(ContextCompat.getColor(context, trendColor))

            itemView.setOnClickListener { onItemClick(transaction) }
        }
    }
}
