package com.hunterxdk.stayconnected.data.local.db

import androidx.room.TypeConverter
import com.hunterxdk.stayconnected.model.ContactGroup
import com.hunterxdk.stayconnected.model.RecurringUnit
import com.hunterxdk.stayconnected.model.ScheduleType

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun toStringList(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(",")

    @TypeConverter
    fun fromContactGroup(value: ContactGroup): String = value.name

    @TypeConverter
    fun toContactGroup(value: String): ContactGroup = ContactGroup.valueOf(value)

    @TypeConverter
    fun fromScheduleType(value: ScheduleType): String = value.name

    @TypeConverter
    fun toScheduleType(value: String): ScheduleType = ScheduleType.valueOf(value)

    @TypeConverter
    fun fromRecurringUnit(value: RecurringUnit?): String? = value?.name

    @TypeConverter
    fun toRecurringUnit(value: String?): RecurringUnit? = value?.let { RecurringUnit.valueOf(it) }
}
