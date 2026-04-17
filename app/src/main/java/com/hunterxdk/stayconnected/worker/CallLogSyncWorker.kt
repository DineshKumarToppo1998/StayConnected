package com.hunterxdk.stayconnected.worker

import android.content.Context
import android.provider.CallLog
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hunterxdk.stayconnected.data.local.entities.CallLogEntity
import com.hunterxdk.stayconnected.data.repository.ContactRepository
import com.hunterxdk.stayconnected.data.repository.SettingsRepository
import com.hunterxdk.stayconnected.model.ScheduleType
import com.hunterxdk.stayconnected.util.NextReminderCalculator
import com.hunterxdk.stayconnected.util.PhoneNormalizer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class CallLogSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val contactRepository: ContactRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val settings = settingsRepository.getSettingsOnce()
        if (!settings.autoDetectCalls) {
            Log.d(TAG, "Auto-detect calls disabled — skipping sync.")
            return Result.success()
        }

        val windowMs = TimeUnit.MINUTES.toMillis(LOOKBACK_MINUTES)
        val since = System.currentTimeMillis() - windowMs

        val systemCalls = querySystemCallLog(since)
        if (systemCalls.isEmpty()) return Result.success()

        val allContacts = contactRepository.getAllContactsList()

        for (call in systemCalls) {
            val normalizedIncoming = PhoneNormalizer.normalize(call.number)
            val contact = allContacts.find {
                PhoneNormalizer.isSameNumber(it.phone, normalizedIncoming)
            } ?: continue

            // Skip if a log already exists within a 60-second dedup window
            val existing = contactRepository.getLastCallForContact(contact.id)
            if (existing != null && Math.abs(existing.calledAt - call.calledAt) < TimeUnit.SECONDS.toMillis(60)) {
                Log.d(TAG, "Duplicate call detected for ${contact.name} — skipping.")
                continue
            }

            val callLog = CallLogEntity(
                contactId = contact.id,
                calledAt = call.calledAt,
                durationSeconds = call.durationSeconds,
                markedManually = false
            )
            contactRepository.insertCallLog(callLog)
            Log.d(TAG, "Logged auto-detected call for ${contact.name}")

            // Recalculate next reminder (skip MANUAL — those are deactivated, not advanced)
            val reminder = contactRepository.getReminderByContactId(contact.id)
            if (reminder != null && reminder.isActive && reminder.scheduleType != ScheduleType.MANUAL) {
                val newNextAt = NextReminderCalculator.calculate(
                    scheduleType = reminder.scheduleType,
                    lastCalledAt = call.calledAt,
                    intervalDays = reminder.intervalDays,
                    recurringUnit = reminder.recurringUnit
                )
                contactRepository.updateReminder(reminder.copy(nextReminderAt = newNextAt))
                Log.d(TAG, "Updated next reminder for ${contact.name} → $newNextAt")
            }
        }

        return Result.success()
    }

    private fun querySystemCallLog(since: Long): List<RawCall> {
        val calls = mutableListOf<RawCall>()
        try {
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.TYPE
            )
            val selection = "${CallLog.Calls.DATE} >= ?"
            val selectionArgs = arrayOf(since.toString())

            applicationContext.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CallLog.Calls.DATE} DESC"
            )?.use { cursor ->
                val numberIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
                val dateIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DATE)
                val durationIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.DURATION)
                val typeIdx = cursor.getColumnIndexOrThrow(CallLog.Calls.TYPE)

                while (cursor.moveToNext()) {
                    val type = cursor.getInt(typeIdx)
                    // Only process incoming and outgoing calls (not missed)
                    if (type == CallLog.Calls.INCOMING_TYPE || type == CallLog.Calls.OUTGOING_TYPE) {
                        calls.add(
                            RawCall(
                                number = cursor.getString(numberIdx) ?: "",
                                calledAt = cursor.getLong(dateIdx),
                                durationSeconds = cursor.getLong(durationIdx)
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "No READ_CALL_LOG permission — skipping sync.", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying call log.", e)
        }
        return calls
    }

    private data class RawCall(val number: String, val calledAt: Long, val durationSeconds: Long)

    companion object {
        private const val TAG = "CallLogSyncWorker"
        private const val LOOKBACK_MINUTES = 16L
    }
}
