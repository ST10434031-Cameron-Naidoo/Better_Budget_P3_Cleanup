package com.dachkaboiz.betterbudget_bestbudget.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [Index(value = ["userEmail"])],
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["email"],
        childColumns = ["userEmail"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val categoryID: Int = 0,
    val userEmail: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryDescription: String
)