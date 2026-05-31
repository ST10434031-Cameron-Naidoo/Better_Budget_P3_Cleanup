package com.dachkaboiz.betterbudget_bestbudget.data.model

data class CategoryGoal(
    val firebaseId: String = "",
    val categoryFirebaseId: String = "",   // links to Category.firebaseId
    val minGoal: Double? = null,
    val maxGoal: Double? = null,
    val month: Int = 0,
    val year: Int = 0
)