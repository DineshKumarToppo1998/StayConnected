package com.hunterxdk.stayconnected.data.local.dao

import androidx.room.*
import com.hunterxdk.stayconnected.data.local.entities.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY name ASC")
    suspend fun getAllContactsList(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE id = :id")
    fun getContactByIdFlow(id: Long): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts WHERE phone = :phone")
    suspend fun getContactByPhone(phone: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity): Long

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)
}
