package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.dao.CategoryGoalDao
import com.dachkaboiz.betterbudget_bestbudget.data.model.CategoryGoal

class CategoryGoalRepository(private val categoryGoalDao: CategoryGoalDao) {

    suspend fun insertCategoryGoal(categoryGoal: CategoryGoal) {
        categoryGoalDao.insertCategoryGoal(categoryGoal)
    }

    suspend fun updateCategoryGoal(categoryGoal: CategoryGoal) {
        categoryGoalDao.updateCategoryGoal(categoryGoal)
    }

    suspend fun deleteCategoryGoal(categoryGoal: CategoryGoal) {
        categoryGoalDao.deleteCategoryGoal(categoryGoal)
    }

    suspend fun getGoalByCategory(categoryID: Int): CategoryGoal? {
        return categoryGoalDao.getGoalsByCategory(categoryID)
    }

    suspend fun getGoalByCategoryAndMonth(categoryID: Int, month: Int, year: Int): CategoryGoal? {
        return categoryGoalDao.getGoalByCategoryAndMonth(categoryID, month, year)
    }

    suspend fun getAllGoalsByMonth(month: Int, year: Int): List<CategoryGoal> {
        return categoryGoalDao.getAllGoalsByMonth(month, year)
    }
}