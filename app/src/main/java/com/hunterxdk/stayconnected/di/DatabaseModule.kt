package com.hunterxdk.stayconnected.di

import android.content.Context
import androidx.room.Room
import com.hunterxdk.stayconnected.data.local.dao.AppSettingsDao
import com.hunterxdk.stayconnected.data.local.dao.CallLogDao
import com.hunterxdk.stayconnected.data.local.dao.ContactDao
import com.hunterxdk.stayconnected.data.local.dao.ReminderDao
import com.hunterxdk.stayconnected.data.local.db.AppDatabase
import com.hunterxdk.stayconnected.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "stay_connected_db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5)
            .build()
    }

    @Provides
    fun provideContactDao(database: AppDatabase): ContactDao = database.contactDao()

    @Provides
    fun provideReminderDao(database: AppDatabase): ReminderDao = database.reminderDao()

    @Provides
    fun provideCallLogDao(database: AppDatabase): CallLogDao = database.callLogDao()

    @Provides
    fun provideAppSettingsDao(database: AppDatabase): AppSettingsDao = database.appSettingsDao()

    @Provides
    @Singleton
    fun provideSettingsRepository(dao: AppSettingsDao): SettingsRepository = SettingsRepository(dao)
}
