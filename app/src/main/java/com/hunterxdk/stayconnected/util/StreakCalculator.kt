package com.hunterxdk.stayconnected.util

import com.hunterxdk.stayconnected.data.local.entities.CallLogEntity
import java.util.*

object StreakCalculator {

    fun calculateStreak(callLogs: List<CallLogEntity>): Int {
        if (callLogs.isEmpty()) return 0

        val sortedLogs = callLogs.sortedByDescending { it.calledAt }
        val callDates = sortedLogs.map { 
            Calendar.getInstance().apply { 
                timeInMillis = it.calledAt
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.distinct()

        var streak = 0
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        var currentDate = calendar.timeInMillis

        // If no call today, check if there was a call yesterday to continue the streak
        if (callDates.isEmpty() || callDates[0] < currentDate) {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterday = calendar.timeInMillis
            if (callDates.isEmpty() || callDates[0] < yesterday) {
                return 0
            }
            currentDate = yesterday
        }

        for (date in callDates) {
            if (date == currentDate) {
                streak++
                calendar.timeInMillis = currentDate
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                currentDate = calendar.timeInMillis
            } else if (date < currentDate) {
                break
            }
        }

        return streak
    }
}
