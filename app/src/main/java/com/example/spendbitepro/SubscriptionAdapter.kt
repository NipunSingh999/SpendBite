package com.example.spendbitepro

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class SubscriptionAdapter(
    private var items: List<SubscriptionItem>,
    private val onItemChanged: () -> Unit
) : RecyclerView.Adapter<SubscriptionAdapter.SubscriptionViewHolder>() {

    fun updateList(newList: List<SubscriptionItem>) {
        items = newList
        notifyDataSetChanged()
    }

    fun getItems(): List<SubscriptionItem> = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubscriptionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subscription_control, parent, false)
        return SubscriptionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        holder.bind(items[position], onItemChanged)
    }

    override fun getItemCount(): Int = items.size

    class SubscriptionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val flIconBg: View = itemView.findViewById(R.id.fl_sub_icon_bg)
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_sub_icon)
        private val tvName: TextView = itemView.findViewById(R.id.tv_sub_name)
        private val switchActive: SwitchCompat = itemView.findViewById(R.id.switch_sub_active)
        private val etFee: EditText = itemView.findViewById(R.id.et_sub_fee)
        private val llInputContainer: View = itemView.findViewById(R.id.ll_sub_input_container)

        private var activeTextWatcher: TextWatcher? = null

        fun bind(item: SubscriptionItem, onItemChanged: () -> Unit) {
            tvName.text = item.name

            // 1. Remove old TextWatcher to prevent recycle collisions
            activeTextWatcher?.let { etFee.removeTextChangedListener(it) }

            // 2. Set input properties
            etFee.setText(item.monthlyFee.toInt().toString())

            // 3. Clear and set Checked Listener safely
            switchActive.setOnCheckedChangeListener(null)
            switchActive.isChecked = item.isActive

            // Enable/disable input container opacity
            if (item.isActive) {
                llInputContainer.alpha = 1.0f
                etFee.isEnabled = true
            } else {
                llInputContainer.alpha = 0.4f
                etFee.isEnabled = false
            }

            // 4. Set background circle color
            try {
                val bgDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor(item.color))
                }
                flIconBg.background = bgDrawable
            } catch (e: Exception) {
                flIconBg.setBackgroundColor(Color.GRAY)
            }

            // 5. Select category icon
            val context = itemView.context
            val iconRes = when (item.icon.lowercase()) {
                "truck" -> R.drawable.ic_truck
                "utensils" -> R.drawable.ic_utensils
                "shopping" -> R.drawable.ic_shopping
                "coffee" -> R.drawable.ic_coffee
                "analytics" -> R.drawable.ic_analytics
                else -> R.drawable.ic_split
            }
            ivIcon.setImageDrawable(ContextCompat.getDrawable(context, iconRes))

            // 6. Setup new Switch Listener
            switchActive.setOnCheckedChangeListener { _, isChecked ->
                item.isActive = isChecked
                if (isChecked) {
                    llInputContainer.alpha = 1.0f
                    etFee.isEnabled = true
                } else {
                    llInputContainer.alpha = 0.4f
                    etFee.isEnabled = false
                }
                onItemChanged()
            }

            // 7. Setup new TextWatcher
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val newFee = s.toString().toDoubleOrNull() ?: 0.0
                    item.monthlyFee = newFee
                    onItemChanged()
                }
                override fun afterTextChanged(s: Editable?) {}
            }
            etFee.addTextChangedListener(watcher)
            activeTextWatcher = watcher
        }
    }
}
