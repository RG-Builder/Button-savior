package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM app_settings WHERE id = 0")
    fun getSettingsFlow(): Flow<SettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 0")
    suspend fun getSettings(): SettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: SettingsEntity)

    @Query("SELECT * FROM usage_stats")
    fun getAllStatsFlow(): Flow<List<StatsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveStat(stat: StatsEntity)

    @Query("SELECT * FROM usage_stats WHERE actionName = :actionName")
    suspend fun getStatByName(actionName: String): StatsEntity?
}
