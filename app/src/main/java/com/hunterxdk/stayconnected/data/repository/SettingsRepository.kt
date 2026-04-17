package com.hunterxdk.stayconnected.data.repository

import com.hunterxdk.stayconnected.data.local.dao.AppSettingsDao
import com.hunterxdk.stayconnected.data.local.entities.AppSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(private val dao: AppSettingsDao) {

    fun getSettings(): Flow<AppSettingsEntity> = dao.getSettings().map {
        it ?: AppSettingsEntity()
    }

    suspend fun getSettingsOnce(): AppSettingsEntity =
        dao.getSettingsOnce() ?: AppSettingsEntity()

    suspend fun updateSettings(settings: AppSettingsEntity) = dao.upsertSettings(settings)

    suspend fun getQuietWindowStart(): String = getSettingsOnce().quietWindowStart

    suspend fun getQuietWindowEnd(): String = getSettingsOnce().quietWindowEnd

    suspend fun isAutoDetectCallsEnabled(): Boolean = getSettingsOnce().autoDetectCalls
}
