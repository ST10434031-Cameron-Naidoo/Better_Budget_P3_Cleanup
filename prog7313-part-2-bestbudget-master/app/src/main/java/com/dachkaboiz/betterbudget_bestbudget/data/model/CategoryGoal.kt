package com.dachkaboiz.betterbudget_bestbudget.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "category_goals",
    indices = [Index(value = ["categoryID"])]
    // Category foreign key removed — Category is now in Firebase
)
data class CategoryGoal(
    @PrimaryKey(autoGenerate = true)
    val categoryGoalID: Int = 0,
    val categoryID: Int,
    val minGoal: Double? = null,
    val maxGoal: Double? = null,
    val month: Int,
    val year: Int
)