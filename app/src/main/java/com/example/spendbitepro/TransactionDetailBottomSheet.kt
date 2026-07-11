package com.example.spendbitepro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TransactionDetailBottomSheet : BottomSheetDialogFragment() {

    private lateinit var transaction: Transaction

    private lateinit var tvMerchant: TextView
    private lateinit var tvAmount: TextView
    private lateinit var tvTimestampPayment: TextView
    private lateinit var tvSource: TextView
    private lateinit var tvHistory: TextView
    private lateinit var tvTrend: TextView
    private lateinit var ivTrendIcon: ImageView
    private lateinit var spCategory: Spinner
    private lateinit var btnDelete: ImageView
    private lateinit var btnCancel: Button
    private lateinit var btnSave: Button

    private val categories = listOf("Meals", "Groceries", "Rent", "Utilities", "Discretionary")

    companion object {
        fun newInstance(transaction: Transaction): TransactionDetailBottomSheet {
            val fragment = TransactionDetailBottomSheet()
            val args = Bundle()
            args.putSerializable("transaction", transaction)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        transaction = arguments?.getSerializable("transaction") as? Transaction ?: Transaction()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_transaction_detail, container, false)

        tvMerchant = view.findViewById(R.id.tv_detail_merchant)
        tvAmount = view.findViewById(R.id.tv_detail_amount)
        tvTimestampPayment = view.findViewById(R.id.tv_detail_timestamp_payment)
        tvSource = view.findViewById(R.id.tv_detail_source)
        tvHistory = view.findViewById(R.id.tv_detail_history)
        tvTrend = view.findViewById(R.id.tv_detail_trend)
        ivTrendIcon = view.findViewById(R.id.iv_detail_trend_icon)
        spCategory = view.findViewById(R.id.sp_detail_category)
        btnDelete = view.findViewById(R.id.btn_delete_transaction)
        btnCancel = view.findViewById(R.id.btn_detail_close)
        btnSave = view.findViewById(R.id.btn_detail_save)

        // Bind data
        tvMerchant.text = transaction.merchant
        tvAmount.text = "₹${String.format("%.2f", transaction.amount)}"
        tvTimestampPayment.text = "${transaction.timestamp} • ${transaction.paymentMethod}"
        tvSource.text = transaction.source
        tvHistory.text = "${transaction.merchantHistory ?: 1} visits recorded"

        // Source Color Tint
        if (transaction.isParsed) {
            tvSource.setTextColor(ContextCompat.getColor(requireContext(), R.color.emerald_success))
        } else {
            tvSource.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_zinc_500))
        }

        // Trend Binding
        tvTrend.text = when (transaction.spendingTrend?.lowercase()) {
            "up" -> "Upward spike"
            "down" -> "Downward drop"
            else -> "Stable spending"
        }
        val trendColor = when (transaction.spendingTrend?.lowercase()) {
            "up" -> R.color.red_error
            "down" -> R.color.emerald_success
            else -> R.color.text_zinc_500
        }
        tvTrend.setTextColor(ContextCompat.getColor(requireContext(), trendColor))
        ivTrendIcon.setColorFilter(ContextCompat.getColor(requireContext(), trendColor))

        val trendIconRes = when (transaction.spendingTrend?.lowercase()) {
            "up" -> R.drawable.ic_trending_up
            "down" -> R.drawable.ic_trending_down
            else -> R.drawable.ic_trending_stable
        }
        ivTrendIcon.setImageResource(trendIconRes)

        // Setup Category Spinner
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = categoryAdapter

        // Select current category in spinner
        val selectionIndex = categories.indexOf(transaction.category)
        if (selectionIndex != -1) {
            spCategory.setSelection(selectionIndex)
        }

        // Button clicks
        btnCancel.setOnClickListener { dismiss() }
        btnSave.setOnClickListener { saveCategoryChange() }
        btnDelete.setOnClickListener { deleteTransactionItem() }

        return view
    }

    private fun saveCategoryChange() {
        val selectedCategory = spCategory.selectedItem.toString()
        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"

        val updatedTransaction = transaction.copy(category = selectedCategory)

        btnSave.isEnabled = false
        repository.updateTransaction(userId, updatedTransaction) { success ->
            btnSave.isEnabled = true
            if (success) {
                dismiss()
            } else {
                Toast.makeText(context, "Failed to update category", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteTransactionItem() {
        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"

        btnDelete.isEnabled = false
        repository.deleteTransaction(userId, transaction.id) { success ->
            btnDelete.isEnabled = true
            if (success) {
                dismiss()
            } else {
                Toast.makeText(context, "Failed to delete transaction", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
