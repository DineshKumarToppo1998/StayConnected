package com.hunterxdk.stayconnected.data.local.dao

import androidx.room.*
import com.hunterxdk.stayconnected.data.local.entities.CallLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs WHERE contactId = :contactId ORDER BY calledAt DESC")
    fun getCallLogsForContact(contactId: Long): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLogEntity): Long

    @Query("SELECT * FROM call_logs WHERE contactId = :contactId ORDER BY calledAt DESC LIMIT 1")
    suspend fun getLastCallForContact(contactId: Long): CallLogEntity?

    @Query("SELECT * FROM call_logs ORDER BY calledAt DESC")
    fun getAllCallLogs(): Flow<List<CallLogEntity>>

    @Query("SELECT * FROM call_logs ORDER BY calledAt DESC")
    suspend fun getAllCallLogsList(): List<CallLogEntity>

    @Update
    suspend fun updateCallLog(callLog: CallLogEntity)

    @Delete
    suspend fun deleteCallLog(callLog: CallLogEntity)
}
