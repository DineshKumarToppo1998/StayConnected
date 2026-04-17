package com.hunterxdk.stayconnected.data.repository

import com.hunterxdk.stayconnected.data.local.dao.CallLogDao
import com.hunterxdk.stayconnected.data.local.dao.ContactDao
import com.hunterxdk.stayconnected.data.local.dao.ReminderDao
import com.hunterxdk.stayconnected.data.local.entities.CallLogEntity
import com.hunterxdk.stayconnected.data.local.entities.ContactEntity
import com.hunterxdk.stayconnected.data.local.entities.ReminderEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactRepository @Inject constructor(
    private val contactDao: ContactDao,
    private val reminderDao: ReminderDao,
    private val callLogDao: CallLogDao
) {
    fun getAllContacts(): Flow<List<ContactEntity>> = contactDao.getAllContacts()

    suspend fun getAllContactsList(): List<ContactEntity> = contactDao.getAllContactsList()

    suspend fun getContactById(id: Long): ContactEntity? = contactDao.getContactById(id)

    fun getContactByIdFlow(id: Long): Flow<ContactEntity?> = contactDao.getContactByIdFlow(id)

    suspend fun getContactByPhone(phone: String): ContactEntity? = contactDao.getContactByPhone(phone)

    fun getVipContacts(): Flow<List<ContactEntity>> = contactDao.getVipContacts()

    suspend fun insertContact(contact: ContactEntity): Long = contactDao.insertContact(contact)

    suspend fun updateContact(contact: ContactEntity) = contactDao.updateContact(contact)

    suspend fun deleteContact(contact: ContactEntity) = contactDao.deleteContact(contact)

    fun getRemindersForContact(contactId: Long): Flow<List<ReminderEntity>> = 
        reminderDao.getRemindersForContact(contactId)

    suspend fun getReminderByContactId(contactId: Long): ReminderEntity? = 
        reminderDao.getReminderByContactId(contactId)

    suspend fun getAllActiveReminders(): List<ReminderEntity> = reminderDao.getAllActiveReminders()

    fun getAllActiveRemindersFlow(): Flow<List<ReminderEntity>> = reminderDao.getAllActiveRemindersFlow()

    suspend fun insertReminder(reminder: ReminderEntity): Long = reminderDao.insertReminder(reminder)

    suspend fun updateReminder(reminder: ReminderEntity) = reminderDao.updateReminder(reminder)

    suspend fun deleteReminder(reminder: ReminderEntity) = reminderDao.deleteReminder(reminder)

    fun getCallLogsForContact(contactId: Long): Flow<List<CallLogEntity>> = 
        callLogDao.getCallLogsForContact(contactId)

    suspend fun insertCallLog(callLog: CallLogEntity): Long = callLogDao.insertCallLog(callLog)

    suspend fun updateCallLog(callLog: CallLogEntity) = callLogDao.updateCallLog(callLog)

    suspend fun deleteCallLog(callLog: CallLogEntity) = callLogDao.deleteCallLog(callLog)

    suspend fun getLastCallForContact(contactId: Long): CallLogEntity? =
        callLogDao.getLastCallForContact(contactId)

    fun getAllCallLogs(): Flow<List<CallLogEntity>> = callLogDao.getAllCallLogs()

    suspend fun getAllCallLogsList(): List<CallLogEntity> = callLogDao.getAllCallLogsList()
}
