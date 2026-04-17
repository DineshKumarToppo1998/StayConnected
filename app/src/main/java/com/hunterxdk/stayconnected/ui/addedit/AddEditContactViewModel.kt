package com.hunterxdk.stayconnected.ui.addedit

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.stayconnected.data.local.entities.ContactEntity
import com.hunterxdk.stayconnected.data.local.entities.ReminderEntity
import com.hunterxdk.stayconnected.data.repository.ContactRepository
import com.hunterxdk.stayconnected.model.ContactGroup
import com.hunterxdk.stayconnected.model.RecurringUnit
import com.hunterxdk.stayconnected.model.ScheduleType
import com.hunterxdk.stayconnected.util.NextReminderCalculator
import com.hunterxdk.stayconnected.util.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AddEditUiState(
    val contactId: Long? = null,
    val reminderId: Long? = null,
    val name: String = "",
    val phone: String = "",
    val photoUri: String? = null,
    val group: ContactGroup = ContactGroup.FAMILY,
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val scheduleType: ScheduleType = ScheduleType.MANUAL,
    val manualDateTime: Long? = null,
    val intervalDays: Int = 7,
    val recurringUnit: RecurringUnit = RecurringUnit.WEEKLY,
    val isSaving: Boolean = false,
    val savedSuccessfully: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddEditContactViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contactRepository: ContactRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    init {
        val contactId = savedStateHandle.get<Long>("contactId")?.takeIf { it != -1L }
        contactId?.let { loadContact(it) }
    }

    private fun loadContact(contactId: Long) {
        viewModelScope.launch {
            val contact = contactRepository.getContactById(contactId) ?: return@launch
            val reminder = contactRepository.getReminderByContactId(contactId)
            _uiState.update {
                it.copy(
                    contactId     = contact.id,
                    reminderId    = reminder?.id,
                    name          = contact.name,
                    phone         = contact.phone,
                    photoUri      = contact.photoUri,
                    group         = contact.primaryGroup,
                    tags          = contact.tags,
                    notes         = contact.notes ?: "",
                    scheduleType  = reminder?.scheduleType ?: ScheduleType.MANUAL,
                    manualDateTime = reminder?.nextReminderAt,
                    intervalDays  = reminder?.intervalDays ?: 7,
                    recurringUnit = reminder?.recurringUnit ?: RecurringUnit.WEEKLY
                )
            }
        }
    }

    fun onPhonebookContactSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val contentResolver = context.contentResolver

            // 1. Get contact _ID, display name, photo URI
            var phonebookId: Long? = null
            var name = ""
            var photoUri: String? = null

            contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.PHOTO_URI
                ),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    phonebookId = cursor.getLong(
                        cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                    )
                    name = cursor.getString(
                        cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                    ) ?: ""
                    photoUri = cursor.getString(
                        cursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)
                    )
                }
            }

            // 2. Get phone number for this contact
            var phone = ""
            phonebookId?.let { id ->
                contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(id.toString()),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        phone = cursor.getString(
                            cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        ) ?: ""
                    }
                }
            }

            _uiState.update { it.copy(name = name, phone = phone, photoUri = photoUri) }
        }
    }

    fun updateName(value: String)     = _uiState.update { it.copy(name = value) }
    fun updatePhone(value: String)    = _uiState.update { it.copy(phone = value) }
    fun updateGroup(value: ContactGroup) = _uiState.update { it.copy(group = value) }
    fun updateNotes(value: String)    = _uiState.update { it.copy(notes = value) }
    fun updateScheduleType(value: ScheduleType) = _uiState.update { it.copy(scheduleType = value) }
    fun updateManualDateTime(value: Long)       = _uiState.update { it.copy(manualDateTime = value) }
    fun updateIntervalDays(value: Int)          = _uiState.update { it.copy(intervalDays = value) }
    fun updateRecurringUnit(value: RecurringUnit) = _uiState.update { it.copy(recurringUnit = value) }

    fun addTag(tag: String) {
        val trimmed = tag.trim()
        if (trimmed.isNotEmpty() && trimmed !in _uiState.value.tags) {
            _uiState.update { it.copy(tags = it.tags + trimmed) }
        }
    }

    fun removeTag(tag: String) {
        _uiState.update { it.copy(tags = it.tags - tag) }
    }

    fun saveContact() {
        val state = _uiState.value
        if (state.name.isBlank() || state.phone.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Name and phone number are required.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val savedContactId = if (state.contactId != null) {
                    // Update existing contact
                    contactRepository.updateContact(
                        ContactEntity(
                            id           = state.contactId,
                            name         = state.name,
                            phone        = state.phone,
                            photoUri     = state.photoUri,
                            primaryGroup = state.group,
                            tags         = state.tags,
                            notes        = state.notes
                        )
                    )
                    state.contactId
                } else {
                    // Insert new contact
                    contactRepository.insertContact(
                        ContactEntity(
                            name         = state.name,
                            phone        = state.phone,
                            photoUri     = state.photoUri,
                            primaryGroup = state.group,
                            tags         = state.tags,
                            notes        = state.notes
                        )
                    )
                }

                // Calculate nextReminderAt
                val nextReminderAt = NextReminderCalculator.calculate(
                    scheduleType   = state.scheduleType,
                    lastCalledAt   = null,
                    manualDateTime = state.manualDateTime,
                    intervalDays   = state.intervalDays,
                    recurringUnit  = state.recurringUnit
                )

                val reminder = ReminderEntity(
                    id             = state.reminderId ?: 0L,
                    contactId      = savedContactId,
                    scheduleType   = state.scheduleType,
                    intervalDays   = if (state.scheduleType == ScheduleType.INTERVAL) state.intervalDays else null,
                    recurringUnit  = if (state.scheduleType == ScheduleType.RECURRING) state.recurringUnit else null,
                    nextReminderAt = nextReminderAt,
                    lastNotifiedAt = null,
                    isActive       = true
                )

                val savedReminderId: Long = if (state.reminderId != null) {
                    contactRepository.updateReminder(reminder)
                    state.reminderId
                } else {
                    contactRepository.insertReminder(reminder)
                }

                ReminderScheduler.scheduleExactAlarm(
                    context         = context,
                    reminderId      = savedReminderId,
                    contactId       = savedContactId,
                    triggerAtMillis = nextReminderAt
                )

                _uiState.update { it.copy(isSaving = false, savedSuccessfully = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Failed to save: ${e.message}")
                }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
