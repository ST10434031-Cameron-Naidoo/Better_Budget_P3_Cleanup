package com.dachkaboiz.betterbudget_bestbudget.data.model

data class AutomatedExpense(
    val firebaseId: String = "",
    val categoryFirebaseId: String = "",
    val amount: Double = 0.0,
    val description: String? = null,
    val imageUri: String? = null,
    val frequencyUnit: String = "MONTH",    // DAY | WEEK | MONTH | YEAR
    val frequencyMultiplier: Int = 1,
    val nextRunDate: Long = 0L,
    val lastRunDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val active: Boolean = true,
    val userEmail: String = ""
)