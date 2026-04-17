package com.hunterxdk.stayconnected.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hunterxdk.stayconnected.data.repository.ContactRepository
import com.hunterxdk.stayconnected.data.repository.SettingsRepository
import com.hunterxdk.stayconnected.util.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class ReminderAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var contactRepository: ContactRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var notificationHelper: NotificationHelper

    companion object {
        const val EXTRA_CONTACT_ID  = "extra_contact_id"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        private const val TAG = "ReminderAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val contactId  = intent.getLongExtra(EXTRA_CONTACT_ID, -1L)
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (contactId == -1L || reminderId == -1L) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val settings = settingsRepository.getSettingsOnce()
                if (settings.quietHoursEnabled &&
                    isInQuietWindow(settings.quietWindowStart, settings.quietWindowEnd)) {
                    Log.d(TAG, "Quiet window active — skipping notification for contact $contactId")
                    return@launch
                }

                val contact = contactRepository.getContactById(contactId) ?: return@launch
                val reminder = contactRepository.getReminderByContactId(contactId)

                // Stale alarm guard: if reminder was updated since this alarm was scheduled, skip
                if (reminder == null || !reminder.isActive || reminder.id != reminderId) {
                    Log.d(TAG, "Stale or inactive reminder $reminderId — skipping")
                    return@launch
                }

                val lastCall = contactRepository.getLastCallForContact(contactId)
                val daysSince = lastCall?.let {
                    (System.currentTimeMillis() - it.calledAt) / (1000L * 60 * 60 * 24)
                }

                notificationHelper.showReminderNotification(
                    contactId         = contact.id,
                    contactName       = contact.name,
                    contactPhone      = contact.phone,
                    daysSinceLastCall = daysSince,
                    photoUri          = contact.photoUri
                )

                contactRepository.updateReminder(
                    reminder.copy(lastNotifiedAt = System.currentTimeMillis())
                )
                Log.d(TAG, "Alarm notification fired for ${contact.name}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun isInQuietWindow(start: String, end: String): Boolean {
        val now = Calendar.getInstance()
        val cur = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val (sh, sm) = start.split(":").map { it.toInt() }
        val (eh, em) = end.split(":").map { it.toInt() }
        val s = sh * 60 + sm
        val e = eh * 60 + em
        return if (s < e) cur in s..e else cur >= s || cur <= e
    }
}
