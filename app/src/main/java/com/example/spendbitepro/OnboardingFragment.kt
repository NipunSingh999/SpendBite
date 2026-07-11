package com.example.spendbitepro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment

class OnboardingFragment : Fragment() {

    private lateinit var etTotal: EditText
    private lateinit var etMeals: EditText
    private lateinit var etGroceries: EditText
    private lateinit var switchAlerts: SwitchCompat
    private lateinit var btnComplete: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_onboarding, container, false)

        etTotal = view.findViewById(R.id.et_onboard_total)
        etMeals = view.findViewById(R.id.et_onboard_meals)
        etGroceries = view.findViewById(R.id.et_onboard_groceries)
        switchAlerts = view.findViewById(R.id.switch_onboard_alerts)
        btnComplete = view.findViewById(R.id.btn_complete_onboarding)

        btnComplete.setOnClickListener {
            completeOnboarding()
        }

        return view
    }

    private fun completeOnboarding() {
        val totalLimit = etTotal.text.toString().toDoubleOrNull() ?: 15000.0
        val mealsLimit = etMeals.text.toString().toDoubleOrNull() ?: 5000.0
        val groceriesLimit = etGroceries.text.toString().toDoubleOrNull() ?: 4000.0
        val breachAlerts = switchAlerts.isChecked

        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"

        btnComplete.isEnabled = false

        // 1. Seed structural data
        repository.seedInitialData(userId) { success ->
            if (success) {
                // 2. Save Custom Onboarding Budgets
                val customSettings = BudgetSettings(totalLimit, mealsLimit, groceriesLimit, breachAlerts)
                repository.updateBudgetSettings(userId, customSettings) { updateSuccess ->
                    btnComplete.isEnabled = true
                    if (updateSuccess) {
                        val mainActivity = activity as? MainActivity
                        mainActivity?.navigateTo("dashboard")
                    } else {
                        Toast.makeText(context, "Failed to apply budgets", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                btnComplete.isEnabled = true
                Toast.makeText(context, "Failed to initialize database", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
