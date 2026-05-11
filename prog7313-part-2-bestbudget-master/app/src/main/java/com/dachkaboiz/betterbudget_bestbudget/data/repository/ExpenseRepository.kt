package com.dachkaboiz.betterbudget_bestbudget.data.repository

import com.dachkaboiz.betterbudget_bestbudget.data.dao.ExpenseDao
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import kotlin.math.exp

class ExpenseRepository (private val expenseDao: ExpenseDao){

    suspend fun insertExpense(expense: Expense){
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: Expense){
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpense(expense: Expense){
        expenseDao.deleteExpense(expense)
    }

    suspend fun getExpenseByUser(email: String): List<Expense>{
        return expenseDao.getExpensesByUser(email)
    }

    suspend fun getExpensesByCategory(categoryID: Int): List<Expense> {
        return expenseDao.getExpensesByCategory(categoryID)
    }

    suspend fun getExpensesBySubCategory(subCategoryID: Int): List<Expense> {
        return expenseDao.getExpensesBySubCategory(subCategoryID)
    }

    suspend fun getExpenseById(expenseID: Int): Expense? {
        return expenseDao.getExpenseById(expenseID)
    }

    suspend fun getTotalSpentByCategory(categoryID: Int, startDate: Long, endDate: Long): Double? {
        return expenseDao.getTotalSpentByCategory(categoryID, startDate, endDate)
    }
}