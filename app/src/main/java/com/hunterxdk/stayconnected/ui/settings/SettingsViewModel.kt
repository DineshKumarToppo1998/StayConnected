package com.hunterxdk.stayconnected.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hunterxdk.stayconnected.data.local.entities.AppSettingsEntity
import com.hunterxdk.stayconnected.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettingsEntity> = settingsRepository
        .getSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettingsEntity()
        )

    fun updateQuietHoursEnabled(value: Boolean) = update { it.copy(quietHoursEnabled = value) }

    fun updateQuietWindowStart(value: String) = update { it.copy(quietWindowStart = value) }

    fun updateQuietWindowEnd(value: String) = update { it.copy(quietWindowEnd = value) }

    fun updateRespectDnd(value: Boolean) = update { it.copy(respectSystemDnd = value) }

    fun updateDefaultSnooze(minutes: Int) = update { it.copy(defaultSnoozeMinutes = minutes) }

    fun updateDefaultGroup(group: String) = update { it.copy(defaultGroup = group) }

    fun updateAutoDetectCalls(value: Boolean) = update { it.copy(autoDetectCalls = value) }

    fun updateAppTheme(value: String) = update { it.copy(appTheme = value) }

    private fun update(transform: (AppSettingsEntity) -> AppSettingsEntity) {
        viewModelScope.launch {
            settingsRepository.updateSettings(transform(settings.value))
        }
    }
}
