package com.hunterxdk.stayconnected.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.hunterxdk.stayconnected.data.local.entities.CallLogEntity
import com.hunterxdk.stayconnected.data.repository.ContactRepository
import com.hunterxdk.stayconnected.model.ScheduleType
import com.hunterxdk.stayconnected.util.NextReminderCalculator
import com.hunterxdk.stayconnected.util.PhoneNormalizer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CallReceiver : BroadcastReceiver() {

    @Inject
    lateinit var contactRepository: ContactRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            @Suppress("DEPRECATION")
            val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            if (state == TelephonyManager.EXTRA_STATE_IDLE && phoneNumber != null) {
                handleCallEnded(phoneNumber)
            }
        } else if (action == Intent.ACTION_NEW_OUTGOING_CALL) {
            @Suppress("DEPRECATION")
            val phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
            if (phoneNumber != null) {
                handleCallEnded(phoneNumber)
            }
        }
    }

    private fun handleCallEnded(phoneNumber: String) {
        CoroutineScope(Dispatchers.IO).launch {
            // Normalize the incoming number and find a matching contact
            val normalizedIncoming = PhoneNormalizer.normalize(phoneNumber)
            val allContacts = contactRepository.getAllContactsList()
            val contact = allContacts.firstOrNull { contact ->
                PhoneNormalizer.isSameNumber(contact.phone, normalizedIncoming)
            }

            if (contact == null) {
                Log.d("CallReceiver", "No matching contact for number: $phoneNumber")
                return@launch
            }

            val calledAt = System.currentTimeMillis()
            contactRepository.insertCallLog(
                CallLogEntity(
                    contactId = contact.id,
                    calledAt = calledAt,
                    durationSeconds = 0, // duration fetched by CallLogSyncWorker from system log
                    markedManually = false
                )
            )
            Log.d("CallReceiver", "Logged call for contact: ${contact.name}")

            val reminder = contactRepository.getReminderByContactId(contact.id) ?: return@launch
            if (!reminder.isActive) return@launch

            if (reminder.scheduleType == ScheduleType.MANUAL) {
                // MANUAL reminders do not auto-advance — mark inactive after firing
                contactRepository.updateReminder(reminder.copy(isActive = false))
            } else {
                val nextReminderAt = NextReminderCalculator.calculate(
                    scheduleType  = reminder.scheduleType,
                    lastCalledAt  = calledAt,
                    intervalDays  = reminder.intervalDays,
                    recurringUnit = reminder.recurringUnit
                )
                contactRepository.updateReminder(
                    reminder.copy(
                        nextReminderAt  = nextReminderAt,
                        lastNotifiedAt  = null
                    )
                )
                Log.d("CallReceiver", "Next reminder for ${contact.name}: $nextReminderAt")
            }
        }
    }
}
