package com.dachkaboiz.betterbudget_bestbudget.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
data class SubCategoryGoal(
    val firebaseId: String = "",
    val subCategoryFirebaseId: String = "",
    val categoryFirebaseId: String = "",
    val minGoal: Double? = null,
    val maxGoal: Double? = null,
    val month: Int = 0,
    val year: Int = 0
)