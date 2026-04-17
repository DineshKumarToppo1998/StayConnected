package com.hunterxdk.stayconnected.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hunterxdk.stayconnected.model.RecurringUnit
import com.hunterxdk.stayconnected.model.ScheduleType

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = ContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contactId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["contactId"])]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactId: Long,
    val scheduleType: ScheduleType,
    val intervalDays: Int?,
    val recurringUnit: RecurringUnit?,
    val nextReminderAt: Long,
    val lastNotifiedAt: Long?,
    val isActive: Boolean = true
)
