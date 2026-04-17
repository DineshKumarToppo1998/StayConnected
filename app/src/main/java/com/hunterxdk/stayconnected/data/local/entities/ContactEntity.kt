package com.hunterxdk.stayconnected.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hunterxdk.stayconnected.model.ContactGroup

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val photoUri: String?,
    val primaryGroup: ContactGroup,
    val tags: List<String>,
    val notes: String?,
    val createdAt: Long = System.currentTimeMillis()
)
