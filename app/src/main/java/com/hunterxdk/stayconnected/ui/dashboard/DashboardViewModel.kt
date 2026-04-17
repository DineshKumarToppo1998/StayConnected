package com.hunterxdk.stayconnected.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.stayconnected.data.local.entities.CallLogEntity
import com.hunterxdk.stayconnected.data.local.entities.ContactEntity
import com.hunterxdk.stayconnected.data.local.entities.ReminderEntity
import com.hunterxdk.stayconnected.data.repository.ContactRepository
import com.hunterxdk.stayconnected.util.NextReminderCalculator
import com.hunterxdk.stayconnected.util.StreakCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class DashboardUiState(
    val allContacts: List<ContactEntity> = emptyList(),
    val filteredContacts: List<ContactEntity> = emptyList(),
    val reminders: Map<Long, ReminderEntity> = emptyMap(),
    val lastCalls: Map<Long, CallLogEntity?> = emptyMap(),
    val streak: Int = 0,
    val overdueCount: Int = 0,
    val dueTodayCount: Int = 0,
    val doneThisWeekCount: Int = 0,
    val activeFilter: String = "ALL",
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val contactRepository: ContactRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeDashboard()
    }

    private fun observeDashboard() {
        viewModelScope.launch {
            combine(
                contactRepository.getAllContacts(),
                contactRepository.getAllActiveRemindersFlow(),
                contactRepository.getAllCallLogs()
            ) { contacts, reminders, callLogs ->
                Triple(contacts, reminders, callLogs)
            }.collect { (contacts, reminders, callLogs) ->
                val remindersMap = reminders.associateBy { it.contactId }

                // Last call per contact
                val lastCallsMap = contacts.associate { contact ->
                    contact.id to callLogs.firstOrNull { it.contactId == contact.id }
                }

                // Stats
                val now = System.currentTimeMillis()
                val weekAgo = now - TimeUnit.DAYS.toMillis(7)
                val overdueCount = remindersMap.values.count {
                    NextReminderCalculator.isOverdue(it.nextReminderAt)
                }
                val dueTodayCount = remindersMap.values.count {
                    !NextReminderCalculator.isOverdue(it.nextReminderAt) &&
                            NextReminderCalculator.isDueToday(it.nextReminderAt)
                }
                val doneThisWeekCount = callLogs.count { it.calledAt >= weekAgo }

                val streak = StreakCalculator.calculateStreak(callLogs)

                // Sort by nextReminderAt ascending (overdue/soonest first)
                val sorted = contacts.sortedBy { contact ->
                    remindersMap[contact.id]?.nextReminderAt ?: Long.MAX_VALUE
                }

                val currentFilter = _uiState.value.activeFilter
                val filtered = applyFilter(sorted, remindersMap, currentFilter)

                _uiState.update { current ->
                    current.copy(
                        allContacts = sorted,
                        filteredContacts = filtered,
                        reminders = remindersMap,
                        lastCalls = lastCallsMap,
                        streak = streak,
                        overdueCount = overdueCount,
                        dueTodayCount = dueTodayCount,
                        doneThisWeekCount = doneThisWeekCount,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun setFilter(filter: String) {
        _uiState.update { current ->
            current.copy(
                activeFilter = filter,
                filteredContacts = applyFilter(current.allContacts, current.reminders, filter)
            )
        }
    }

    private fun applyFilter(
        contacts: List<ContactEntity>,
        reminders: Map<Long, ReminderEntity>,
        filter: String
    ): List<ContactEntity> = when (filter) {
        "FAMILY"  -> contacts.filter { it.primaryGroup.name == "FAMILY" }
        "FRIENDS" -> contacts.filter { it.primaryGroup.name == "FRIENDS" }
        "WORK"    -> contacts.filter { it.primaryGroup.name == "WORK" }
        "OVERDUE" -> contacts.filter { contact ->
            reminders[contact.id]?.let { NextReminderCalculator.isOverdue(it.nextReminderAt) } == true
        }
        else      -> contacts // "ALL"
    }
}
