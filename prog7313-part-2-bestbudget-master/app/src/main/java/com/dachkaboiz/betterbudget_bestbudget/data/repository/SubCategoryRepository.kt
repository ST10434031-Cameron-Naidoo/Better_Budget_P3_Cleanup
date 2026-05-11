package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.dao.SubCategoryDao
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategory

class SubCategoryRepository(private val subCategoryDao: SubCategoryDao) {

    suspend fun insertSubCategory(subCategory: SubCategory) {
        subCategoryDao.insertSubCategory(subCategory)
    }

    suspend fun updateSubCategory(subCategory: SubCategory) {
        subCategoryDao.updateSubCategory(subCategory)
    }

    suspend fun deleteSubCategory(subCategory: SubCategory) {
        subCategoryDao.deleteSubCategory(subCategory)
    }

    suspend fun getSubCategoriesByCategory(categoryID: Int): List<SubCategory> {
        return subCategoryDao.getSubCategoriesByCategory(categoryID)
    }

    suspend fun getSubCategoryById(subCategoryID: Int): SubCategory? {
        return subCategoryDao.getSubCategoryById(subCategoryID)
    }
}