package com.example.spendbitepro

import java.io.Serializable

data class Transaction(
    val id: String = "",
    val merchant: String = "",
    val category: String = "Meals", // "Meals" | "Groceries" | "Rent" | "Utilities" | "Discretionary" | etc.
    val amount: Double = 0.0,
    val timestamp: String = "", // e.g., ISO-8601 or "Today, 2:45 PM"
    val paymentMethod: String = "UPI", // "UPI" | "Credit Card" | "Cash"
    val source: String = "Manual Entry", // "SMS Parse Check" | "Manual Entry"
    val isParsed: Boolean = false,
    val locationName: String? = null,
    val merchantHistory: Int? = null,
    val spendingTrend: String? = null // "up" | "down" | "stable"
) : Serializable

data class Group(
    val id: String = "",
    val name: String = "",
    val categoryIcon: String = "coffee", // "home" | "plane" | "coffee" | "shopping" | etc.
    val statusText: String = "All settled",
    val statusType: String = "settled", // "settled" | "owe" | "owed"
    val membersCount: Int = 1,
    val description: String = "",
    val members: List<String> = emptyList() // Initials or names
) : Serializable

data class SharedLedgerItem(
    val id: String = "",
    val title: String = "",
    val paidBy: String = "", // E.g., "You" or "Alex M."
    val amount: Double = 0.0,
    val splitStatus: String? = "none", // "split_now" | "split_shared" | "none"
    val groupName: String = "",
    val image: String? = null, // Receipt preview image placeholder
    val oweText: String? = null, // Calculated balance string
    val oweType: String? = "none" // "error" | "success" | "none"
) : Serializable

data class QuickSettlement(
    val id: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val type: String = "owe", // "owe" | "owed"
    val initials: String = "",
    val bgClass: String = "" // Visual styling color class
) : Serializable

data class BudgetSettings(
    val totalMonthlyLimit: Double = 15000.0,
    val mealsLimit: Double = 5000.0,
    val groceriesLimit: Double = 4000.0,
    val breachAlerts: Boolean = true
) : Serializable

data class SubscriptionItem(
    var id: String = "",
    var name: String = "",
    var monthlyFee: Double = 0.0,
    var isActive: Boolean = false,
    var color: String = "#CCCCCC", // Hex color
    var icon: String = "utensils" // "utensils" | "truck" | etc.
) : Serializable

data class UserProfile(
    val nickname: String = "",
    val profilePhotoBase64: String = ""
) : Serializable
