package com.dachkaboiz.betterbudget_bestbudget.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subcategories",
    indices = [Index(value = ["parentCategoryID"])],
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["categoryID"],
        childColumns = ["parentCategoryID"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class SubCategory(
    @PrimaryKey(autoGenerate = true)
    val subCategoryID: Int = 0,
    val parentCategoryID: Int,
    val subCategoryName: String,
    val subCategoryIcon: String,
    val subCategoryDescription: String? = null
)