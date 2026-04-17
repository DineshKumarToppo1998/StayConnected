package com.hunterxdk.stayconnected.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hunterxdk.stayconnected.receiver.ReminderAlarmReceiver
import com.hunterxdk.stayconnected.worker.CallLogSyncWorker
import com.hunterxdk.stayconnected.worker.RenotifyWorker
import com.hunterxdk.stayconnected.worker.ReminderWorker
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    fun schedulePeriodicReminders(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            60, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "ReminderWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun scheduleCallLogSync(context: Context) {
        val workRequest = PeriodicWorkRequestBuilder<CallLogSyncWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "CallLogSync",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun scheduleRenotify(context: Context, contactId: Long, delayMinutes: Long = 120L) {
        val inputData = Data.Builder()
            .putLong(RenotifyWorker.KEY_CONTACT_ID, contactId)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<RenotifyWorker>()
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "Renotify_$contactId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun scheduleExactAlarm(
        context: Context,
        reminderId: Long,
        contactId: Long,
        triggerAtMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // API 31-32: fall back to WorkManager polling if user denied SCHEDULE_EXACT_ALARM
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w("ReminderScheduler", "No exact alarm permission — WorkManager fallback active for contact $contactId")
            return
        }

        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_CONTACT_ID, contactId)
            putExtra(ReminderAlarmReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        // One PendingIntent per contact (keyed by contactId). FLAG_UPDATE_CURRENT replaces
        // stale reminderId extras when a reminder is rescheduled.
        val pi = PendingIntent.getBroadcast(
            context,
            contactId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        Log.d("ReminderScheduler", "Exact alarm set for contact $contactId at $triggerAtMillis")
    }

    fun cancelAlarm(context: Context, contactId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            contactId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
            Log.d("ReminderScheduler", "Alarm cancelled for contact $contactId")
        }
    }
}
