package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.dao.SubCategoryGoalDao
import com.dachkaboiz.betterbudget_bestbudget.data.model.SubCategoryGoal

class SubCategoryGoalRepository(private val subCategoryGoalDao: SubCategoryGoalDao) {

    suspend fun insertSubCategoryGoal(subCategoryGoal: SubCategoryGoal) {
        subCategoryGoalDao.insertSubCategoryGoal(subCategoryGoal)
    }

    suspend fun updateSubCategoryGoal(subCategoryGoal: SubCategoryGoal) {
        subCategoryGoalDao.updateSubCategoryGoal(subCategoryGoal)
    }

    suspend fun deleteSubCategoryGoal(subCategoryGoal: SubCategoryGoal) {
        subCategoryGoalDao.deleteSubCategoryGoal(subCategoryGoal)
    }

    suspend fun getGoalsBySubCategory(subCategoryID: Int): SubCategoryGoal? {
        return subCategoryGoalDao.getGoalsBySubCategory(subCategoryID)
    }

    suspend fun getGoalBySubCategoryAndMonth(subCategoryID: Int, month: Int, year: Int): SubCategoryGoal? {
        return subCategoryGoalDao.getGoalBySubCategoryAndMonth(subCategoryID, month, year)
    }

    suspend fun getGoalsByCategoryAndMonth(categoryID: Int, month: Int, year: Int): List<SubCategoryGoal> {
        return subCategoryGoalDao.getGoalsByCategoryAndMonth(categoryID, month, year)
    }
}