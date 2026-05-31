package com.dachkaboiz.betterbudget_bestbudget.data.model

data class SubCategoryGoal(
    val subCategoryID: String = "",
    val categoryID: String = "",
//    val firebaseSubCategoryId: String = "",      // links to SubCategory.firebaseId
//    val firebaseParentCategoryId: String = "",   // links to Category.firebaseId
    val minGoal: Double? = null,
    val maxGoal: Double? = null,
    val month: Int = 0,
    val year: Int = 0
)