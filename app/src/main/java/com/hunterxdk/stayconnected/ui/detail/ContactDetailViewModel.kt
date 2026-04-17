package com.hunterxdk.stayconnected.ui.detail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.stayconnected.data.local.entities.CallLogEntity
import com.hunterxdk.stayconnected.data.local.entities.ContactEntity
import com.hunterxdk.stayconnected.data.local.entities.ReminderEntity
import com.hunterxdk.stayconnected.data.repository.ContactRepository
import com.hunterxdk.stayconnected.model.ScheduleType
import com.hunterxdk.stayconnected.util.NextReminderCalculator
import com.hunterxdk.stayconnected.util.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactDetailUiState(
    val contact: ContactEntity? = null,
    val reminder: ReminderEntity? = null,
    val callLogs: List<CallLogEntity> = emptyList(),
    val isOverdue: Boolean = false,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false
)

@HiltViewModel
class ContactDetailViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactDetailUiState())
    val uiState: StateFlow<ContactDetailUiState> = _uiState.asStateFlow()

    private val contactId: Long = savedStateHandle.get<Long>("contactId") ?: -1L

    init {
        if (contactId != -1L) {
            observeContact()
        }
    }

    private fun observeContact() {
        viewModelScope.launch {
            combine(
                contactRepository.getContactByIdFlow(contactId).filterNotNull(),
                contactRepository.getRemindersForContact(contactId),
                contactRepository.getCallLogsForContact(contactId)
            ) { contact, reminders, callLogs ->
                val reminder = reminders.firstOrNull { it.isActive }
                val isOverdue = reminder?.let {
                    NextReminderCalculator.isOverdue(it.nextReminderAt)
                } ?: false
                ContactDetailUiState(
                    contact   = contact,
                    reminder  = reminder,
                    callLogs  = callLogs,
                    isOverdue = isOverdue,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.update { state }
            }
        }
    }

    fun deleteContact() {
        viewModelScope.launch {
            val contact = _uiState.value.contact ?: return@launch
            contactRepository.deleteContact(contact)
            _uiState.update { it.copy(isDeleted = true) }
        }
    }

    fun logCallManually(calledAt: Long, durationSeconds: Long) {
        viewModelScope.launch {
            contactRepository.insertCallLog(
                CallLogEntity(
                    contactId       = contactId,
                    calledAt        = calledAt,
                    durationSeconds = durationSeconds,
                    markedManually  = true
                )
            )

            val reminder = _uiState.value.reminder ?: return@launch
            if (!reminder.isActive) return@launch

            if (reminder.scheduleType == ScheduleType.MANUAL) {
                contactRepository.updateReminder(reminder.copy(isActive = false))
                ReminderScheduler.cancelAlarm(context, contactId)
            } else {
                val nextReminderAt = NextReminderCalculator.calculate(
                    scheduleType  = reminder.scheduleType,
                    lastCalledAt  = calledAt,
                    intervalDays  = reminder.intervalDays,
                    recurringUnit = reminder.recurringUnit
                )
                val updated = reminder.copy(nextReminderAt = nextReminderAt, lastNotifiedAt = null)
                contactRepository.updateReminder(updated)
                ReminderScheduler.scheduleExactAlarm(context, updated.id, contactId, nextReminderAt)
            }
        }
    }

    fun updateCallLog(log: CallLogEntity, newCalledAt: Long, newDurationSeconds: Long) {
        viewModelScope.launch {
            contactRepository.updateCallLog(
                log.copy(calledAt = newCalledAt, durationSeconds = newDurationSeconds)
            )
        }
    }

    fun deleteCallLog(log: CallLogEntity) {
        viewModelScope.launch {
            contactRepository.deleteCallLog(log)
        }
    }

    fun snoozeReminder(newNextAt: Long) {
        viewModelScope.launch {
            val reminder = _uiState.value.reminder ?: return@launch
            val updated = reminder.copy(nextReminderAt = newNextAt, lastNotifiedAt = null)
            contactRepository.updateReminder(updated)
            ReminderScheduler.scheduleExactAlarm(context, updated.id, contactId, newNextAt)
        }
    }
}
