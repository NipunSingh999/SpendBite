package com.example.spendbitepro

import android.content.Context
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SMSSyncManager {

    fun syncHistoricalSMS(context: Context, callback: (Int) -> Unit) {
        val repository = RepositoryProvider.getRepository()
        val userId = repository.getCurrentUserId() ?: "demo_user"
        
        val inboxUri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("body", "date")
        
        // Scanning messages from the start of the current calendar month
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfCurrentMonth = calendar.timeInMillis
        
        val selection = "date > ?"
        val selectionArgs = arrayOf(startOfCurrentMonth.toString())
        
        var importedCount = 0

        try {
            val contentResolver = context.contentResolver
            val cursor = contentResolver.query(inboxUri, projection, selection, selectionArgs, "date DESC")
            
            if (cursor != null) {
                val bodyIndex = cursor.getColumnIndex("body")
                val dateIndex = cursor.getColumnIndex("date")
                
                val format = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
                
                while (cursor.moveToNext()) {
                    val body = cursor.getString(bodyIndex) ?: continue
                    val dateMs = cursor.getLong(dateIndex)
                    val parsed = SMSParser.parseMessage(body) ?: continue
                    
                    // Create a deterministic ID to prevent duplicates
                    val deterministicId = "tx_sms_hist_${dateMs}_${(parsed.amount * 100).toLong()}"
                    val formattedDate = format.format(Date(dateMs))
                    
                    val transaction = Transaction(
                        id = deterministicId,
                        merchant = parsed.merchant,
                        category = parsed.category,
                        amount = parsed.amount,
                        timestamp = formattedDate,
                        paymentMethod = parsed.paymentMethod,
                        source = "SMS Parse Check",
                        isParsed = true,
                        locationName = null,
                        merchantHistory = null,
                        spendingTrend = "stable"
                    )
                    
                    repository.addTransaction(userId, transaction) {}
                    importedCount++
                }
                cursor.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        callback(importedCount)
    }
}
