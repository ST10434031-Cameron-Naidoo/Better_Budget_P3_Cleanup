package com.dachkaboiz.betterbudget_bestbudget.data.model

data class Expense(
    val firebaseId: String = "",
    val userEmail: String = "",
    val categoryFirebaseId: String = "",        // links to Category.firebaseId
    val subCategoryFirebaseId: String? = null,  // links to SubCategory.firebaseId
    val expenseAmount: Double = 0.0,
    val expenseDate: Long = 0L,
    val expenseDescription: String? = null,
    val imageUri: String? = null,
    val imageName: String? = null,
    val imageDescription: String? = null,
    val automationFrequency: String? = null,
    val automationMultiplier: Int? = null
)