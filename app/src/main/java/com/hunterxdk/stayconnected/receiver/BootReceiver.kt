package com.hunterxdk.stayconnected.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hunterxdk.stayconnected.data.repository.ContactRepository
import com.hunterxdk.stayconnected.util.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var contactRepository: ContactRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON") return

        Log.d("BootReceiver", "Boot completed — rescheduling WorkManager jobs and exact alarms.")
        ReminderScheduler.schedulePeriodicReminders(context)
        ReminderScheduler.scheduleCallLogSync(context)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val now = System.currentTimeMillis()
                val activeReminders = contactRepository.getAllActiveReminders()
                for (reminder in activeReminders) {
                    // Already overdue → fire 1 minute after boot so device fully initializes
                    val triggerAt = if (reminder.nextReminderAt < now) now + 60_000L
                                    else reminder.nextReminderAt
                    ReminderScheduler.scheduleExactAlarm(
                        context, reminder.id, reminder.contactId, triggerAt
                    )
                }
                Log.d("BootReceiver", "Re-registered ${activeReminders.size} exact alarms.")
            } finally {
                pendingResult.finish()
            }
        }
    }
}
