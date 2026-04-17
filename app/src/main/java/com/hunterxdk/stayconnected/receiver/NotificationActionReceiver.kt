package com.hunterxdk.stayconnected.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.work.WorkManager
import com.hunterxdk.stayconnected.data.repository.ContactRepository
import com.hunterxdk.stayconnected.util.ReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var contactRepository: ContactRepository

    override fun onReceive(context: Context, intent: Intent) {
        val contactId = intent.getLongExtra(EXTRA_CONTACT_ID, -1L)
        val notifId   = intent.getIntExtra(EXTRA_NOTIF_ID, contactId.toInt())

        if (contactId == -1L) {
            Log.w(TAG, "Received notification action with no contactId — ignoring.")
            return
        }

        // Always cancel the visible notification first
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(notifId)

        when (intent.action) {
            ACTION_CALL_NOW -> {
                val phone = intent.getStringExtra(EXTRA_CONTACT_PHONE)
                if (phone.isNullOrBlank()) {
                    Log.w(TAG, "ACTION_CALL_NOW received but no phone number.")
                    return
                }
                try {
                    val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phone")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(callIntent)
                } catch (e: SecurityException) {
                    // CALL_PHONE permission not granted — fall back to dialler (no permission needed)
                    Log.w(TAG, "CALL_PHONE permission denied, falling back to ACTION_DIAL", e)
                    val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(dialIntent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initiate call for contact $contactId", e)
                }
                // User acted — cancel the pending re-notification
                WorkManager.getInstance(context).cancelUniqueWork("Renotify_$contactId")
                Log.d(TAG, "Call initiated for contact $contactId")
            }

            ACTION_SNOOZE_1H, ACTION_SNOOZE_2H, ACTION_SNOOZE_4H -> {
                val delayMs = when (intent.action) {
                    ACTION_SNOOZE_1H -> 60L * 60_000L
                    ACTION_SNOOZE_2H -> 120L * 60_000L
                    else             -> 240L * 60_000L
                }
                val delayMinutes = delayMs / 60_000L
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        val reminder = contactRepository.getReminderByContactId(contactId)
                        if (reminder != null && reminder.isActive) {
                            val triggerAt = System.currentTimeMillis() + delayMs
                            contactRepository.updateReminder(
                                reminder.copy(nextReminderAt = triggerAt)
                            )
                            // Try exact alarm (requires permission on API 31+)
                            ReminderScheduler.scheduleExactAlarm(
                                context, reminder.id, contactId, triggerAt
                            )
                            // Replace the old 2h RenotifyWorker with one timed to the
                            // snooze window. This fires at the right time even when the
                            // exact alarm permission is denied.
                            ReminderScheduler.scheduleRenotify(
                                context, contactId, delayMinutes = delayMinutes
                            )
                            Log.d(TAG, "Snoozed for contact $contactId, delay ${delayMinutes}m")
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            ACTION_DISMISS -> {
                // User explicitly dismissed — cancel the pending re-notification
                WorkManager.getInstance(context).cancelUniqueWork("Renotify_$contactId")
                Log.d(TAG, "Dismissed notification for contact $contactId")
            }

            else -> Log.w(TAG, "Unknown action: ${intent.action}")
        }
    }

    companion object {
        private const val TAG = "NotifActionReceiver"

        const val ACTION_CALL_NOW  = "com.hunterxdk.stayconnected.ACTION_CALL_NOW"
        const val ACTION_SNOOZE_1H = "com.hunterxdk.stayconnected.ACTION_SNOOZE_1H"
        const val ACTION_SNOOZE_2H = "com.hunterxdk.stayconnected.ACTION_SNOOZE_2H"
        const val ACTION_SNOOZE_4H = "com.hunterxdk.stayconnected.ACTION_SNOOZE_4H"
        const val ACTION_DISMISS   = "com.hunterxdk.stayconnected.ACTION_DISMISS"

        const val EXTRA_CONTACT_ID    = "extra_contact_id"
        const val EXTRA_NOTIF_ID      = "extra_notif_id"
        const val EXTRA_CONTACT_PHONE = "extra_contact_phone"
    }
}
