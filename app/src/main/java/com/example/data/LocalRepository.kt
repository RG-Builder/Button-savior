package com.example.data

import kotlinx.coroutines.flow.Flow

class LocalRepository(private val appDao: AppDao) {
    val settingsFlow: Flow<SettingsEntity?> = appDao.getSettingsFlow()

    suspend fun getSettings(): SettingsEntity {
        return appDao.getSettings() ?: SettingsEntity().also {
            appDao.saveSettings(it)
        }
    }

    suspend fun saveSettings(settings: SettingsEntity) {
        appDao.saveSettings(settings)
    }

    val statsFlow: Flow<List<StatsEntity>> = appDao.getAllStatsFlow()

    suspend fun incrementStat(actionName: String) {
        val existing = appDao.getStatByName(actionName)
        val newCount = (existing?.count ?: 0) + 1
        appDao.saveStat(StatsEntity(actionName, newCount))
    }
}
