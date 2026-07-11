package com.example.spendbitepro

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment

class SMSOnboardingDialog : DialogFragment() {

    interface OnSMSOnboardingListener {
        fun onPermissionGranted()
    }

    var listener: OnSMSOnboardingListener? = null
    private val SMS_PERMISSION_CODE = 102

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_sms_onboarding, container, false)

        val btnGrant = view.findViewById<Button>(R.id.btn_grant_access)
        val btnMaybe = view.findViewById<Button>(R.id.btn_maybe_later)

        btnGrant.setOnClickListener {
            requestPermissions(
                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS),
                SMS_PERMISSION_CODE
            )
        }

        btnMaybe.setOnClickListener {
            val context = context
            if (context != null) {
                val prefs = context.getSharedPreferences("spendbite_prefs", Context.MODE_PRIVATE)
                prefs.edit().putBoolean("sms_prompt_shown", true).apply()
            }
            dismiss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_PERMISSION_CODE) {
            val context = context ?: return
            val prefs = context.getSharedPreferences("spendbite_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("sms_prompt_shown", true).apply()

            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                listener?.onPermissionGranted()
                dismiss()
            } else {
                Toast.makeText(context, "SMS tracking blocked. To enable, go to Settings -> Apps -> SpendBite Pro. Tap the three dots (top-right), select 'Allow restricted settings', then turn on SMS under Permissions.", Toast.LENGTH_LONG).show()
                dismiss()
            }
        }
    }

    companion object {
        fun newInstance(listener: OnSMSOnboardingListener): SMSOnboardingDialog {
            val dialog = SMSOnboardingDialog()
            dialog.listener = listener
            return dialog
        }
    }
}
