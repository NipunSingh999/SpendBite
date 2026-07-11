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

class AddSubscriptionBottomSheet : BottomSheetDialogFragment() {

    private lateinit var etName: EditText
    private lateinit var etFee: EditText
    private lateinit var spBrand: Spinner
    private lateinit var switchActive: SwitchCompat
    private lateinit var btnCancel: Button
    private lateinit var btnAdd: Button

    private val brandsList = listOf(
        "Zomato Red",
        "Swiggy Orange",
        "Zepto Yellow",
        "Blinkit Green",
        "Brand Deep Blue",
        "Custom Purple"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_add_subscription, container, false)

        etName = view.findViewById(R.id.et_sub_dialog_name)
        etFee = view.findViewById(R.id.et_sub_dialog_fee)
        spBrand = view.findViewById(R.id.sp_sub_dialog_brand)
        switchActive = view.findViewById(R.id.switch_sub_dialog_active)
        btnCancel = view.findViewById(R.id.btn_sub_dialog_cancel)
        btnAdd = view.findViewById(R.id.btn_sub_dialog_add)

        // Setup Brand Spinner
        val brandAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, brandsList)
        brandAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spBrand.adapter = brandAdapter

        btnCancel.setOnClickListener { dismiss() }
        btnAdd.setOnClickListener { saveSubscription() }

        return view
    }

    private fun saveSubscription() {
        val name = etName.text.toString().trim()
        val feeStr = etFee.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(context, "Please enter a membership name", Toast.LENGTH_SHORT).show()
            return
        }

        val fee = feeStr.toDoubleOrNull()
        if (fee == null || fee <= 0.0) {
            Toast.makeText(context, "Please enter a valid monthly fee", Toast.LENGTH_SHORT).show()
            return
        }

        // Map selected brand style to colors and icon strings
        val selectedBrand = spBrand.selectedItem.toString()
        val (bgHex, iconType) = when (selectedBrand) {
            "Zomato Red" -> Pair("#E23744", "utensils")
            "Swiggy Orange" -> Pair("#FC8019", "truck")
            "Zepto Yellow" -> Pair("#F59E0B", "shopping")
            "Blinkit Green" -> Pair("#1b6d24", "coffee")
            "Brand Deep Blue" -> Pair("#000666", "analytics")
            else -> Pair("#6366F1", "split") // Custom Purple
        }

        val newSub = SubscriptionItem(
            id = "sub_${System.currentTimeMillis()}",
            name = name,
            monthlyFee = fee,
            isActive = switchActive.isChecked,
            color = bgHex,
            icon = iconType
        )

        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"

        btnAdd.isEnabled = false
        repository.addSubscription(userId, newSub) { success ->
            btnAdd.isEnabled = true
            if (success) {
                Toast.makeText(context, " Culinary Membership added successfully", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                Toast.makeText(context, "Failed to add membership", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
