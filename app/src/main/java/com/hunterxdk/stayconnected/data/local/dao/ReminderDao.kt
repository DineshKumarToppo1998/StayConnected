package com.hunterxdk.stayconnected.data.local.dao

import androidx.room.*
import com.hunterxdk.stayconnected.data.local.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE contactId = :contactId")
    fun getRemindersForContact(contactId: Long): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE isActive = 1 AND nextReminderAt <= :timestamp")
    suspend fun getDueReminders(timestamp: Long): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isActive = 1")
    suspend fun getAllActiveReminders(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE isActive = 1")
    fun getAllActiveRemindersFlow(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE contactId = :contactId LIMIT 1")
    suspend fun getReminderByContactId(contactId: Long): ReminderEntity?
}
