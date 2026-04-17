package com.hunterxdk.stayconnected.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Long = 1,
    val quietHoursEnabled: Boolean = true,
    val quietWindowStart: String = "22:00",
    val quietWindowEnd: String = "08:00",
    val respectSystemDnd: Boolean = true,
    val defaultSnoozeMinutes: Int = 120,
    val defaultGroup: String = "FAMILY",
    val autoDetectCalls: Boolean = true,
    val appTheme: String = "DARK"
)
