package com.example.spendbitepro

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UPIPaymentBottomSheet : BottomSheetDialogFragment() {

    interface OnPaymentCompletedListener {
        fun onPaymentSuccess()
    }

    var listener: OnPaymentCompletedListener? = null

    private lateinit var llMainContent: View
    private lateinit var llOverlay: View
    private lateinit var llLoadingState: View
    private lateinit var llSuccessState: View

    private lateinit var btnPayNow: Button
    private lateinit var btnCancelPayment: View
    private lateinit var btnClosePayment: View
    private lateinit var btnBackDashboard: Button

    private lateinit var tvSuccessAmountDetail: TextView
    private lateinit var tvReceiptTxnId: TextView
    private lateinit var tvReceiptDate: TextView

    private var payeeName = "Friend"
    private var amount = 0.0
    private var paymentCompleted = false

    private val UPI_PAYMENT_REQUEST_CODE = 999

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_upi_payment, container, false)

        payeeName = arguments?.getString("payee") ?: "Friend"
        amount = arguments?.getDouble("amount") ?: 0.0

        view.findViewById<TextView>(R.id.tv_upi_payee).text = payeeName
        view.findViewById<TextView>(R.id.tv_upi_amount).text = String.format("%,.2f", amount)

        llMainContent = view.findViewById(R.id.ll_upi_main_content)
        llOverlay = view.findViewById(R.id.ll_upi_overlay)
        llLoadingState = view.findViewById(R.id.ll_upi_loading_state)
        llSuccessState = view.findViewById(R.id.ll_upi_success_state)

        btnPayNow = view.findViewById(R.id.btn_pay_now)
        btnCancelPayment = view.findViewById(R.id.btn_cancel_payment)
        btnClosePayment = view.findViewById(R.id.btn_close_payment)
        btnBackDashboard = view.findViewById(R.id.btn_back_dashboard)

        tvSuccessAmountDetail = view.findViewById(R.id.tv_success_amount_detail)
        tvReceiptTxnId = view.findViewById(R.id.tv_receipt_txn_id)
        tvReceiptDate = view.findViewById(R.id.tv_receipt_date)

        setupListeners()

        return view
    }

    private fun setupListeners() {
        btnPayNow.setOnClickListener {
            launchRealUPI()
        }

        btnCancelPayment.setOnClickListener {
            dismiss()
        }

        btnClosePayment.setOnClickListener {
            dismiss()
        }

        btnBackDashboard.setOnClickListener {
            if (paymentCompleted) {
                listener?.onPaymentSuccess()
            }
            dismiss()
        }
    }

    private fun launchRealUPI() {
        // Construct the standard NPCI UPI payment URI
        val upiUri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", "alex.m@upi") // Payee UPI ID (VPA)
            .appendQueryParameter("pn", payeeName)     // Payee Name
            .appendQueryParameter("tn", "SpendBite Pro Settlement") // Transaction Note
            .appendQueryParameter("am", String.format(Locale.US, "%.2f", amount)) // Amount
            .appendQueryParameter("cu", "INR")         // Currency
            .build()

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = upiUri
        }

        try {
            // Check if there is any UPI app to handle the intent
            val chooser = Intent.createChooser(intent, "Pay via UPI App")
            startActivityForResult(chooser, UPI_PAYMENT_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(context, "No UPI apps found. Falling back to simulator.", Toast.LENGTH_LONG).show()
            triggerSimulatedProgress()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPI_PAYMENT_REQUEST_CODE) {
            var isSuccess = false
            var txnId = "#TXN${(1000000..9999999).random()}"

            if (data != null) {
                val response = data.getStringExtra("response") ?: ""
                val responseMap = parseUpiResponse(response)
                val status = responseMap["status"]?.lowercase() ?: ""
                if (status == "success" || response.lowercase().contains("success")) {
                    isSuccess = true
                    txnId = responseMap["txnid"] ?: responseMap["approvalrefno"] ?: txnId
                }
            } else {
                // Simulators or some apps don't return data, check if result code is OK
                if (resultCode == Activity.RESULT_OK) {
                    isSuccess = true
                }
            }

            if (isSuccess) {
                showSuccessOverlay(txnId)
            } else {
                Toast.makeText(context, "UPI Payment Failed or Cancelled", Toast.LENGTH_SHORT).show()
                // Offer simulation fallback so the user can test the UI flow even without banking apps configured
                showSimulationFallbackAlert()
            }
        }
    }

    private fun parseUpiResponse(response: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val parts = response.split("&")
        for (part in parts) {
            val kv = part.split("=")
            if (kv.size == 2) {
                map[kv[0].lowercase()] = kv[1]
            }
        }
        return map
    }

    private fun showSimulationFallbackAlert() {
        val context = context ?: return
        AlertDialog.Builder(context)
            .setTitle("Simulate Payment Success?")
            .setMessage("No real transaction was completed. Would you like to run the simulated successful transfer to update the groups ledger?")
            .setPositiveButton("Simulate") { _, _ ->
                triggerSimulatedProgress()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun triggerSimulatedProgress() {
        llMainContent.visibility = View.GONE
        llOverlay.visibility = View.VISIBLE
        llLoadingState.visibility = View.VISIBLE
        llSuccessState.visibility = View.GONE

        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) {
                val randomTxId = "#TXN${(1000000..9999999).random()}"
                showSuccessOverlay(randomTxId)
            }
        }, 1200)
    }

    private fun showSuccessOverlay(transactionId: String) {
        paymentCompleted = true
        llMainContent.visibility = View.GONE
        llOverlay.visibility = View.VISIBLE
        llLoadingState.visibility = View.GONE
        llSuccessState.visibility = View.VISIBLE

        tvSuccessAmountDetail.text = "Sent ₹${String.format("%,.2f", amount)} to $payeeName"
        tvReceiptTxnId.text = transactionId
        
        val sdf = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
        tvReceiptDate.text = sdf.format(Date())
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    companion object {
        fun newInstance(payeeName: String, amount: Double, listener: OnPaymentCompletedListener): UPIPaymentBottomSheet {
            val sheet = UPIPaymentBottomSheet()
            sheet.listener = listener
            sheet.arguments = Bundle().apply {
                putString("payee", payeeName)
                putDouble("amount", amount)
            }
            return sheet
        }
    }
}
