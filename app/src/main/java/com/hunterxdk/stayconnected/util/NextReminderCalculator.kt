package com.hunterxdk.stayconnected.util

import com.hunterxdk.stayconnected.model.RecurringUnit
import com.hunterxdk.stayconnected.model.ScheduleType
import java.util.concurrent.TimeUnit

object NextReminderCalculator {

    /**
     * Calculates the next reminder timestamp in epoch milliseconds.
     *
     * For MANUAL schedule type: returns [manualDateTime]. After a call is logged against
     * a MANUAL reminder, the caller is responsible for setting [isActive] = false instead
     * of calling this function — MANUAL reminders do not auto-advance.
     *
     * @param scheduleType   The type of reminder schedule.
     * @param lastCalledAt   Epoch millis of the most recent call. Used as the base for
     *                       RECURRING and INTERVAL schedules. If null, uses current time.
     * @param manualDateTime Epoch millis for the user-selected date/time (MANUAL only).
     * @param intervalDays   Number of days between reminders (INTERVAL only).
     * @param recurringUnit  The recurring cadence (RECURRING only).
     */
    fun calculate(
        scheduleType: ScheduleType,
        lastCalledAt: Long?,
        manualDateTime: Long? = null,
        intervalDays: Int? = null,
        recurringUnit: RecurringUnit? = null
    ): Long {
        val base = lastCalledAt ?: System.currentTimeMillis()
        return when (scheduleType) {
            ScheduleType.MANUAL -> requireNotNull(manualDateTime) {
                "MANUAL schedule requires manualDateTime"
            }
            ScheduleType.RECURRING -> {
                val unit = requireNotNull(recurringUnit) {
                    "RECURRING schedule requires recurringUnit"
                }
                when (unit) {
                    RecurringUnit.WEEKLY    -> base + TimeUnit.DAYS.toMillis(7)
                    RecurringUnit.BIWEEKLY  -> base + TimeUnit.DAYS.toMillis(14)
                    RecurringUnit.MONTHLY   -> base + TimeUnit.DAYS.toMillis(30)
                }
            }
            ScheduleType.INTERVAL -> {
                val days = requireNotNull(intervalDays) {
                    "INTERVAL schedule requires intervalDays"
                }
                base + TimeUnit.DAYS.toMillis(days.toLong())
            }
        }
    }

    /**
     * Returns true if the given reminder timestamp is in the past (i.e. overdue).
     */
    fun isOverdue(nextReminderAt: Long): Boolean =
        System.currentTimeMillis() > nextReminderAt

    /**
     * Returns true if [nextReminderAt] falls within today (midnight to midnight).
     */
    fun isDueToday(nextReminderAt: Long): Boolean {
        val now = System.currentTimeMillis()
        val startOfDay = startOfDayMillis(now)
        val endOfDay = startOfDay + TimeUnit.DAYS.toMillis(1)
        return nextReminderAt in startOfDay until endOfDay
    }

    private fun startOfDayMillis(epochMillis: Long): Long {
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = epochMillis
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
