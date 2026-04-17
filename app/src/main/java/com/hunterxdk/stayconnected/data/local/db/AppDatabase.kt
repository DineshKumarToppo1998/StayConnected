package com.hunterxdk.stayconnected.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hunterxdk.stayconnected.data.local.dao.AppSettingsDao
import com.hunterxdk.stayconnected.data.local.dao.CallLogDao
import com.hunterxdk.stayconnected.data.local.dao.ContactDao
import com.hunterxdk.stayconnected.data.local.dao.ReminderDao
import com.hunterxdk.stayconnected.data.local.entities.AppSettingsEntity
import com.hunterxdk.stayconnected.data.local.entities.CallLogEntity
import com.hunterxdk.stayconnected.data.local.entities.ContactEntity
import com.hunterxdk.stayconnected.data.local.entities.ReminderEntity

@Database(
    entities = [
        ContactEntity::class,
        ReminderEntity::class,
        CallLogEntity::class,
        AppSettingsEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun reminderDao(): ReminderDao
    abstract fun callLogDao(): CallLogDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `app_settings` (
                        `id` INTEGER NOT NULL,
                        `quietWindowStart` TEXT NOT NULL DEFAULT '22:00',
                        `quietWindowEnd` TEXT NOT NULL DEFAULT '08:00',
                        `respectSystemDnd` INTEGER NOT NULL DEFAULT 1,
                        `defaultSnoozeMinutes` INTEGER NOT NULL DEFAULT 120,
                        `defaultGroup` TEXT NOT NULL DEFAULT 'FAMILY',
                        `autoDetectCalls` INTEGER NOT NULL DEFAULT 1,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE `app_settings` ADD COLUMN `quietHoursEnabled` INTEGER NOT NULL DEFAULT 1"
                )
            }
        }
    }
}
