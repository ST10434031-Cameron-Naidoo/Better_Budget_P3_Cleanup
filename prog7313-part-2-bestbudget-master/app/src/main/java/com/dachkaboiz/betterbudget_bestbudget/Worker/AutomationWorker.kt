package com.dachkaboiz.betterbudget_bestbudget.Worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.repository.AutomatedExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.utils.AutomationScheduler
import com.google.firebase.auth.FirebaseAuth

class AutomationWorker (
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params){
    override suspend fun doWork(): Result {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return Result.success() // not logged in, nothing to do

        val userEmail = applicationContext
            .getSharedPreferences("auth", Context.MODE_PRIVATE)
            .getString("email", "") ?: ""

        val automatedRepo = AutomatedExpenseRepository(uid)
        val expenseRepo   = ExpenseRepository(uid)

        return try {
            // 1. Fetch all automation schedules directly (No suspendCoroutine needed)
            val allAutomated = automatedRepo.getAll()

            // 2. Find ones that are due (nextRunDate is in the past)
            val due = AutomationScheduler.getDueExpenses(allAutomated)

            for (auto in due) {
                // 3. Insert the new expense
                val newExpenseId = expenseRepo.generateExpenseId()
                val expense = Expense(
                    expenseID = newExpenseId,
                    userEmail = userEmail,
                    categoryId = auto.categoryFirebaseId,
                    subCategoryId = null,
                    expenseAmount = auto.amount,
                    expenseDate = auto.nextRunDate,
                    expenseDescription = auto.description,
                    imageUri = auto.imageUri,
                    imageName = null,
                    imageDescription = null,
                    automationFrequency = null,
                    automationMultiplier = null
                )
                expenseRepo.insertExpense(expense)

                // 4. Advance nextRunDate forward by one interval
                val nextDate = AutomationScheduler.calculateNextRunDate(
                    fromDate   = auto.nextRunDate,
                    unit       = auto.frequencyUnit,
                    multiplier = auto.frequencyMultiplier
                )
                val updated = auto.copy(
                    lastRunDate = auto.nextRunDate,
                    nextRunDate = nextDate
                )

                // Update directly via suspend function
                automatedRepo.update(updated)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}