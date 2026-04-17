package com.hunterxdk.stayconnected.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hunterxdk.stayconnected.data.repository.ContactRepository
import com.hunterxdk.stayconnected.data.repository.SettingsRepository
import com.hunterxdk.stayconnected.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val contactRepository: ContactRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val currentTime = System.currentTimeMillis()
        val allContacts = contactRepository.getAllContactsList()
        val dueReminders = contactRepository.getAllActiveReminders().filter {
            it.nextReminderAt <= currentTime
        }

        if (dueReminders.isNotEmpty()) {
            val settings = settingsRepository.getSettingsOnce()

            if (settings.quietHoursEnabled &&
                isInQuietWindow(settings.quietWindowStart, settings.quietWindowEnd)) {
                Log.d("ReminderWorker", "Quiet window active. Skipping notifications.")
                return Result.success()
            }

            for (reminder in dueReminders) {
                val contact = allContacts.find { it.id == reminder.contactId }
                if (contact != null) {
                    val lastCall = contactRepository.getLastCallForContact(contact.id)
                    val daysSince = if (lastCall != null) {
                        (currentTime - lastCall.calledAt) / (1000L * 60 * 60 * 24)
                    } else null
                    Log.d("ReminderWorker", "Firing notification for contact ${contact.name}")
                    notificationHelper.showReminderNotification(
                        contactId = contact.id,
                        contactName = contact.name,
                        contactPhone = contact.phone,
                        daysSinceLastCall = daysSince,
                        photoUri = contact.photoUri
                    )
                }

                contactRepository.updateReminder(reminder.copy(lastNotifiedAt = currentTime))
            }
        }

        return Result.success()
    }

    private fun isInQuietWindow(start: String, end: String): Boolean {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)

        val startParts = start.split(":")
        val startHour = startParts[0].toInt()
        val startMinute = startParts[1].toInt()

        val endParts = end.split(":")
        val endHour = endParts[0].toInt()
        val endMinute = endParts[1].toInt()

        val currentMinutes = currentHour * 60 + currentMinute
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes < endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
}
