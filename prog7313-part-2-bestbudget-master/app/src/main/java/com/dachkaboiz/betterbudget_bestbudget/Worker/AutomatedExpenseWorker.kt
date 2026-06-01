package com.dachkaboiz.betterbudget_bestbudget.worker

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dachkaboiz.betterbudget_bestbudget.data.model.AutomatedExpense
import com.dachkaboiz.betterbudget_bestbudget.data.model.Expense
import com.dachkaboiz.betterbudget_bestbudget.data.repository.AutomatedExpenseRepository
import com.dachkaboiz.betterbudget_bestbudget.data.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class AutomatedExpenseWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val uid = getUserUid() ?: return@withContext Result.success()

            val automatedRepo = AutomatedExpenseRepository(uid)
            val expenseRepo = ExpenseRepository(uid)

            val automatedList = automatedRepo.getAll()
            val now = System.currentTimeMillis()

            automatedList
                .filter { it.active }
                .forEach { auto ->
                    if (now >= auto.nextRunDate) {
                        // 1. Create real expense
                        val newExpenseId = expenseRepo.generateExpenseId()

                        val expense = Expense(
                            expenseID = newExpenseId,
                            userEmail = auto.userEmail,
                            categoryId = auto.categoryFirebaseId,
                            subCategoryId = auto.categoryFirebaseId,
                            expenseAmount = auto.amount,
                            expenseDate = now,
                            expenseDescription = auto.description,
                            imageUri = auto.imageUri,
                            imageName = null,
                            imageDescription = null,
                            automationFrequency = null,
                            automationMultiplier = null
                        )

                        expenseRepo.insertExpense(expense)

                        // 2. Update nextRunDate
                        val updated = auto.copy(
                            lastRunDate = now,
                            nextRunDate = calculateNextRun(auto, now)
                        )

                        automatedRepo.update(updated)
                    }
                }
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(applicationContext, "Automation Worker Ran", Toast.LENGTH_SHORT).show()
            }

            scheduleNextDebugRun()
             Result.success()



        } catch (e: Exception) {
            Result.failure()
        }
    }
    private fun scheduleNextDebugRun() {
        val request = OneTimeWorkRequestBuilder<AutomatedExpenseWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(applicationContext)
            .enqueue(request)
    }

    private fun calculateNextRun(auto: AutomatedExpense, from: Long): Long {
        val multiplier = auto.frequencyMultiplier
        val day = 24L * 60 * 60 * 1000

        return when (auto.frequencyUnit.uppercase()) {
            "DAY" -> from + (multiplier * day)
            "WEEK" -> from + (multiplier * 7 * day)
            "MONTH" -> from + (multiplier * 30 * day)
            "YEAR" -> from + (multiplier * 365 * day)
            else -> from + day   // fallback so WHEN is exhaustive
        }
    }


    private fun getUserUid(): String? {
        val prefs = applicationContext.getSharedPreferences("auth", Context.MODE_PRIVATE)
        return prefs.getString("uid", null)
    }
}
