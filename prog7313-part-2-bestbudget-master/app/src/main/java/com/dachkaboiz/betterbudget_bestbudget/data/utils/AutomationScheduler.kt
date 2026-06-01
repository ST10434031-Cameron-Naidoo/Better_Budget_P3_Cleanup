package com.dachkaboiz.betterbudget_bestbudget.data.utils

import java.util.Calendar

object AutomationScheduler {

    fun calculateNextRunDate(fromDate: Long, unit: String, multiplier: Int): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = fromDate }

        when (unit.uppercase()) {
            "DAY"   -> cal.add(Calendar.DAY_OF_YEAR, multiplier)
            "WEEK"  -> cal.add(Calendar.WEEK_OF_YEAR, multiplier)
            "MONTH" -> cal.add(Calendar.MONTH, multiplier)
            "YEAR"  -> cal.add(Calendar.YEAR, multiplier)
        }

        return cal.timeInMillis
    }
}
