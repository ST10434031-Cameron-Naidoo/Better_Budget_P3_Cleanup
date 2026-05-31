package com.dachkaboiz.betterbudget_bestbudget.ui

import com.dachkaboiz.betterbudget_bestbudget.data.model.AutomatedExpense
import java.util.Calendar

class AutomationScheduler {
    fun calculateNextRunDate(fromDate: Long, unit: String, multiplier: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = fromDate }
        when (unit) {
            "DAY"   -> cal.add(Calendar.DAY_OF_YEAR, multiplier)
            "WEEK"  -> cal.add(Calendar.WEEK_OF_YEAR, multiplier)
            "MONTH" -> cal.add(Calendar.MONTH, multiplier)
            "YEAR"  -> cal.add(Calendar.YEAR, multiplier)
        }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getDueExpenses(all: List<AutomatedExpense>): List<AutomatedExpense> =
        all.filter { it.active && it.nextRunDate <= System.currentTimeMillis() }
}