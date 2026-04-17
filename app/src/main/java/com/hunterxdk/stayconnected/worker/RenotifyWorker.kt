package com.hunterxdk.stayconnected.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hunterxdk.stayconnected.data.repository.ContactRepository
import com.hunterxdk.stayconnected.util.NextReminderCalculator
import com.hunterxdk.stayconnected.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class RenotifyWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val contactRepository: ContactRepository,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val contactId = inputData.getLong(KEY_CONTACT_ID, -1L)
        if (contactId == -1L) {
            Log.w(TAG, "RenotifyWorker called without contactId — aborting.")
            return Result.success()
        }

        val reminder = contactRepository.getReminderByContactId(contactId)
        if (reminder == null || !reminder.isActive) {
            Log.d(TAG, "No active reminder for contact $contactId — skipping re-notification.")
            return Result.success()
        }

        // Only re-fire if the reminder is still overdue (user didn't act on it)
        if (!NextReminderCalculator.isOverdue(reminder.nextReminderAt)) {
            Log.d(TAG, "Reminder for contact $contactId is no longer overdue — skipping.")
            return Result.success()
        }

        val contact = contactRepository.getContactById(contactId)
        if (contact == null) {
            Log.w(TAG, "Contact $contactId not found — skipping re-notification.")
            return Result.success()
        }

        val lastCall = contactRepository.getLastCallForContact(contactId)
        val daysSince = if (lastCall != null) {
            (System.currentTimeMillis() - lastCall.calledAt) / (1000L * 60 * 60 * 24)
        } else null

        Log.d(TAG, "Re-notifying for contact ${contact.name}")
        notificationHelper.showReminderNotification(
            contactId = contact.id,
            contactName = contact.name,
            contactPhone = contact.phone,
            daysSinceLastCall = daysSince,
            photoUri = contact.photoUri
        )

        return Result.success()
    }

    companion object {
        private const val TAG = "RenotifyWorker"
        const val KEY_CONTACT_ID = "contact_id"
    }
}
