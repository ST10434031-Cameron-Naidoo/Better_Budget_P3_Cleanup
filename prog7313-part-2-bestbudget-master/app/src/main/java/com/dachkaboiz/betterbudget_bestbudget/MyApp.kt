package com.dachkaboiz.betterbudget_bestbudget

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dachkaboiz.betterbudget_bestbudget.worker.AutomatedExpenseWorker
import java.util.concurrent.TimeUnit

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        val workRequest = PeriodicWorkRequestBuilder<AutomatedExpenseWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                "automated_expense_worker",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
    }
}
