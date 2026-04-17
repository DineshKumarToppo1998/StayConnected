package com.hunterxdk.stayconnected.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

object RelativeTimeFormatter {

    /**
     * Returns the number of full calendar days between [epochMillis] and today.
     * Returns null if [epochMillis] is null.
     */
    fun daysSince(epochMillis: Long?): Long? {
        epochMillis ?: return null
        val now = System.currentTimeMillis()
        val diffMs = now - epochMillis
        return if (diffMs < 0) 0L else TimeUnit.MILLISECONDS.toDays(diffMs)
    }

    /**
     * Returns a human-readable relative string:
     *  - null → "Never"
     *  - same calendar day → "Today"
     *  - previous calendar day → "Yesterday"
     *  - 2–6 days ago → "X days ago"
     *  - older → "MMM d, yyyy"
     */
    fun format(epochMillis: Long?): String {
        epochMillis ?: return "Never"

        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = epochMillis }

        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfYesterday = (startOfToday.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }

        return when {
            then.after(startOfToday) || then.timeInMillis == startOfToday.timeInMillis ->
                "Today"
            then.after(startOfYesterday) ->
                "Yesterday"
            else -> {
                val days = TimeUnit.MILLISECONDS.toDays(
                    now.timeInMillis - epochMillis
                )
                if (days < 7) {
                    "$days days ago"
                } else {
                    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(then.time)
                }
            }
        }
    }

    /**
     * Returns "MMM d 'at' h:mm a" — e.g. "Aug 15 at 9:00 AM"
     */
    fun formatDateTime(epochMillis: Long): String {
        return SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault())
            .format(java.util.Date(epochMillis))
    }

    /**
     * Returns "MMM d, yyyy" — e.g. "Aug 15, 2024"
     */
    fun formatDate(epochMillis: Long): String {
        return SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            .format(java.util.Date(epochMillis))
    }

    /**
     * Returns "h:mm a" — e.g. "9:00 AM"
     */
    fun formatTime(epochMillis: Long): String {
        return SimpleDateFormat("h:mm a", Locale.getDefault())
            .format(java.util.Date(epochMillis))
    }

    /**
     * Formats call duration from seconds into "X min Y sec" or "X sec".
     */
    fun formatDuration(seconds: Long): String {
        val mins = seconds / 60
        val secs = seconds % 60
        return when {
            mins == 0L -> "$secs sec"
            secs == 0L -> "$mins min"
            else       -> "$mins min $secs sec"
        }
    }
}
