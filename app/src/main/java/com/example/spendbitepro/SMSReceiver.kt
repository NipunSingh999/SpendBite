package com.example.spendbitepro

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale

class SMSReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val body = message.messageBody ?: continue
                val parsed = SMSParser.parseMessage(body) ?: continue

                // Add to database
                val repository = RepositoryProvider.getRepository()
                val userId = repository.getCurrentUserId() ?: "demo_user"
                
                val sdf = SimpleDateFormat("Today, h:mm a", Locale.getDefault())
                val formattedDate = sdf.format(Date())

                val transaction = Transaction(
                    id = "tx_sms_${System.currentTimeMillis()}_${(0..999).random()}",
                    merchant = parsed.merchant,
                    category = parsed.category,
                    amount = parsed.amount,
                    timestamp = formattedDate,
                    paymentMethod = parsed.paymentMethod,
                    source = "SMS Parse Check",
                    isParsed = true,
                    locationName = null,
                    merchantHistory = null,
                    spendingTrend = "up"
                )

                repository.addTransaction(userId, transaction) { success ->
                    if (success) {
                        Toast.makeText(
                            context.applicationContext,
                            "SpendBite Pro: Auto-logged ₹${parsed.amount} spent at ${parsed.merchant}",
                            Toast.LENGTH_LONG
                        ).show()

                        // Check budget limits and trigger notifications in background
                        checkBudgetAlerts(context, userId)
                    }
                }
            }
        }
    }

    private fun checkBudgetAlerts(context: Context, userId: String) {
        val pendingResult = goAsync()
        Thread {
            try {
                val repository = RepositoryProvider.getRepository()
                val isDemo = (FirebaseManager.auth?.currentUser == null)
                
                val settings: BudgetSettings
                val transactions: List<Transaction>
                
                if (isDemo) {
                    var localSettings = BudgetSettings()
                    repository.observeBudgetSettings(userId) { localSettings = it }
                    var localTxs = emptyList<Transaction>()
                    repository.observeTransactions(userId) { localTxs = it }
                    settings = localSettings
                    transactions = localTxs
                } else {
                    val db = FirebaseManager.firestore
                    if (db != null) {
                        val settingsTask = db.collection("users").document(userId).collection("budget").document("settings").get()
                        val settingsSnap = com.google.android.gms.tasks.Tasks.await(settingsTask)
                        settings = settingsSnap.toObject(BudgetSettings::class.java) ?: BudgetSettings()

                        val txTask = db.collection("users").document(userId).collection("transactions").get()
                        val txSnap = com.google.android.gms.tasks.Tasks.await(txTask)
                        transactions = txSnap.documents.mapNotNull { it.toObject(Transaction::class.java) }
                    } else {
                        settings = BudgetSettings()
                        transactions = emptyList()
                    }
                }
                
                val totalLimit = settings.totalMonthlyLimit
                if (totalLimit > 0 && settings.breachAlerts) {
                    var totalSpent = 0.0
                    for (tx in transactions) {
                        if (isCurrentCalendarMonth(tx.timestamp)) {
                            totalSpent += tx.amount
                        }
                    }
                    val remaining = totalLimit - totalSpent
                    val progress = ((totalSpent / totalLimit) * 100).toInt()

                    val prefs = context.getSharedPreferences("SpendBiteProPrefs", Context.MODE_PRIVATE)
                    val nickname = prefs.getString("user_nickname", "User") ?: "User"

                    val key80 = "alert_80_$userId"
                    val key100 = "alert_100_$userId"

                    val sent80 = prefs.getBoolean(key80, false)
                    val sent100 = prefs.getBoolean(key100, false)

                    if (progress >= 100) {
                        if (!sent100) {
                            val title = "Budget Limit Reached! 🚨"
                            val msg = "Hey $nickname, you have spent 100% of your monthly budget (₹${String.format("%,.2f", totalSpent)}). Please watch your expenses!"
                            NotificationHelper.sendNotification(context, title, msg)
                            prefs.edit().putBoolean(key100, true).putBoolean(key80, true).apply()
                        }
                    } else if (progress >= 80) {
                        if (!sent80) {
                            val title = "Budget Warning! ⚠️"
                            val msg = "Hey $nickname, you have reached 80% of your monthly budget limit. Remaining balance is ₹${String.format("%,.2f", remaining)}."
                            NotificationHelper.sendNotification(context, title, msg)
                            prefs.edit().putBoolean(key80, true).apply()
                        }
                        if (sent100) {
                            prefs.edit().putBoolean(key100, false).apply()
                        }
                    } else {
                        if (sent80 || sent100) {
                            prefs.edit().putBoolean(key80, false).putBoolean(key100, false).apply()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    private fun isCurrentCalendarMonth(timestamp: String): Boolean {
        val now = Date()
        val currentMonthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(now)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val sdfFull = SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault())
        
        var datePartStr = when {
            timestamp.startsWith("Today,", ignoreCase = true) -> {
                val currentDayPrefix = SimpleDateFormat("MMM dd,", Locale.getDefault()).format(now)
                timestamp.replace("Today,", currentDayPrefix)
            }
            timestamp.startsWith("Yesterday,", ignoreCase = true) -> {
                val yesterday = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000)
                val yesterdayPrefix = SimpleDateFormat("MMM dd,", Locale.getDefault()).format(yesterday)
                timestamp.replace("Yesterday,", yesterdayPrefix)
            }
            else -> timestamp
        }
        
        if (!datePartStr.contains(currentYear.toString())) {
            val commaIndex = datePartStr.indexOf(",")
            if (commaIndex != -1) {
                datePartStr = datePartStr.substring(0, commaIndex) + ", " + currentYear + datePartStr.substring(commaIndex)
            } else {
                datePartStr = "$datePartStr, $currentYear"
            }
        }
        
        return try {
            val date = sdfFull.parse(datePartStr) ?: now
            val txMonthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
            txMonthYear == currentMonthYear
        } catch (e: Exception) {
            true
        }
    }
}
