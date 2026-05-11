package com.dachkaboiz.betterbudget_bestbudget.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["userEmail"]),
        Index(value = ["categoryID"]),
        Index(value = ["subCategoryID"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["email"],
            childColumns = ["userEmail"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["categoryID"],
            childColumns = ["categoryID"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = SubCategory::class,
            parentColumns = ["subCategoryID"],
            childColumns = ["subCategoryID"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val expenseID: Int = 0,
    val userEmail: String,
    val categoryID: Int,
    val subCategoryID: Int? = null,
    val expenseAmount: Double,
    val expenseDate: Long,
    val expenseDescription: String? = null,
    val imageUri: String?,
    val imageName: String?,
    val imageDescription: String?,
    val automationFrequency: String? = null
)