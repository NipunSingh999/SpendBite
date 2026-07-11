package com.example.spendbitepro

import java.util.Locale

data class ParsedTransaction(
    val merchant: String,
    val amount: Double,
    val category: String,
    val paymentMethod: String
)

object SMSParser {

    private val amountRegex = Regex("""(?i)(?:rs\.?|inr|₹)\s*([0-9,]+(?:\.[0-9]+)?)""")

    private val merchantCategoryMap = mapOf(
        "zomato" to "Meals",
        "swiggy" to "Meals",
        "starbucks" to "Meals",
        "dominos" to "Meals",
        "domino's" to "Meals",
        "pizza hut" to "Meals",
        "pizzahut" to "Meals",
        "eatsure" to "Meals",
        "kfc" to "Meals",
        "burger king" to "Meals",
        "burgerking" to "Meals",
        "mcdonald" to "Meals",
        "mcdonald's" to "Meals",
        
        "blinkit" to "Groceries",
        "zepto" to "Groceries",
        "instamart" to "Groceries",
        "bigbasket" to "Groceries",
        "nature's basket" to "Groceries",
        "jiomart" to "Groceries",
        "amazon fresh" to "Groceries",
        "amazonfresh" to "Groceries",
        "flipkart minutes" to "Groceries",
        "fk minutes" to "Groceries"
    )

    fun parseMessage(body: String): ParsedTransaction? {
        val bodyLower = body.lowercase(Locale.getDefault())

        // 1. Identify if this is a transaction of one of our target food/grocery merchants
        var matchedMerchant: String? = null
        var matchedCategory: String? = null

        for ((merchant, category) in merchantCategoryMap) {
            if (bodyLower.contains(merchant)) {
                matchedMerchant = merchant.replaceFirstChar { it.uppercase() }
                matchedCategory = category
                break
            }
        }

        // Special case: Swiggy Instamart should map to Groceries, Swiggy to Meals
        if (bodyLower.contains("instamart")) {
            matchedMerchant = "Swiggy Instamart"
            matchedCategory = "Groceries"
        }

        if (matchedMerchant == null || matchedCategory == null) {
            return null // Not related to food/groceries
        }

        // 2. Extract Amount
        val amountMatch = amountRegex.find(body) ?: return null
        val amountStr = amountMatch.groupValues[1].replace(",", "")
        val amount = amountStr.toDoubleOrNull() ?: return null

        // 3. Extract Payment Mode (UPI vs Credit Card vs Debit Card)
        val paymentMethod = when {
            bodyLower.contains("credit card") || bodyLower.contains("cc spent") || bodyLower.contains("card ending") -> "Credit Card"
            bodyLower.contains("upi") || bodyLower.contains("vpa") || bodyLower.contains("gpay") || bodyLower.contains("phonepe") || bodyLower.contains("paytm") -> "UPI"
            else -> "Debit Card"
        }

        return ParsedTransaction(
            merchant = matchedMerchant,
            amount = amount,
            category = matchedCategory,
            paymentMethod = paymentMethod
        )
    }
}
