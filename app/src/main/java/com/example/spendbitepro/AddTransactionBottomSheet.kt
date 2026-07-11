package com.example.spendbitepro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class AddTransactionBottomSheet : BottomSheetDialogFragment() {

    private lateinit var etMerchant: EditText
    private lateinit var etAmount: EditText
    private lateinit var spCategory: Spinner
    private lateinit var spPayment: Spinner
    private lateinit var etLocation: EditText
    private lateinit var switchParsed: SwitchCompat
    private lateinit var btnCancel: Button
    private lateinit var btnSave: Button

    private val categories = listOf("Meals", "Groceries", "Rent", "Utilities", "Discretionary")
    private val paymentMethods = listOf("UPI", "Credit Card", "Cash")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_add_transaction, container, false)

        etMerchant = view.findViewById(R.id.et_dialog_merchant)
        etAmount = view.findViewById(R.id.et_dialog_amount)
        spCategory = view.findViewById(R.id.sp_dialog_category)
        spPayment = view.findViewById(R.id.sp_dialog_payment)
        etLocation = view.findViewById(R.id.et_dialog_location)
        switchParsed = view.findViewById(R.id.switch_dialog_parsed)
        btnCancel = view.findViewById(R.id.btn_dialog_cancel)
        btnSave = view.findViewById(R.id.btn_dialog_save)

        // Setup Spinners
        val categoryAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spCategory.adapter = categoryAdapter

        val paymentAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, paymentMethods)
        paymentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPayment.adapter = paymentAdapter

        btnCancel.setOnClickListener { dismiss() }
        btnSave.setOnClickListener { saveTransaction() }

        return view
    }

    private fun saveTransaction() {
        val merchant = etMerchant.text.toString().trim()
        val amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0
        val category = spCategory.selectedItem.toString()
        val paymentMethod = spPayment.selectedItem.toString()
        val location = etLocation.text.toString().trim()
        val isParsed = switchParsed.isChecked

        if (merchant.isEmpty()) {
            Toast.makeText(context, "Please enter a merchant", Toast.LENGTH_SHORT).show()
            return
        }
        if (amount <= 0.0) {
            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"

        // Format date: e.g., "Today, 2:45 PM"
        val sdf = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
        val formattedDate = sdf.format(Date())

        // Simulated analytic details
        val randomVisits = Random.nextInt(1, 15)
        val trends = listOf("up", "down", "stable")
        val randomTrend = trends[Random.nextInt(trends.size)]

        val transaction = Transaction(
            merchant = merchant,
            amount = amount,
            category = category,
            paymentMethod = paymentMethod,
            timestamp = formattedDate,
            source = if (isParsed) "SMS Parse Check" else "Manual Entry",
            isParsed = isParsed,
            locationName = if (location.isNotEmpty()) location else null,
            merchantHistory = randomVisits,
            spendingTrend = randomTrend
        )

        btnSave.isEnabled = false
        repository.addTransaction(userId, transaction) { success ->
            btnSave.isEnabled = true
            if (success) {
                dismiss()
            } else {
                Toast.makeText(context, "Failed to save transaction", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
