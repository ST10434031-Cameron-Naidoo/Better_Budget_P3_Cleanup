package com.dachkaboiz.betterbudget_bestbudget.data.model
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subcategory_goals",
    indices = [
        Index(value = ["subCategoryID"]),
        Index(value = ["categoryID"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = SubCategory::class,
            parentColumns = ["subCategoryID"],
            childColumns = ["subCategoryID"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["categoryID"],
            childColumns = ["categoryID"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SubCategoryGoal(
    @PrimaryKey(autoGenerate = true)
    val subCategoryGoalID: Int = 0,
    val subCategoryID: Int,
    val categoryID: Int,
    val minGoal: Double? = null,
    val maxGoal: Double? = null,
    val month: Int,
    val year: Int
)
