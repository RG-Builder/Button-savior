package com.example.data

import androidx.room.*

@Entity(tableName = "app_settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 0,
    val buttonSizeDp: Int = 60,
    val buttonOpacity: Float = 0.75f,
    val themeName: String = "GAMIFIED", // "MINIMAL", "GAMIFIED", "ELITE"
    val hapticEnabled: Boolean = true,
    val enabledShortcuts: String = "BACK,HOME,RECENTS,NOTIFICATIONS,QUICK_SETTINGS,LOCK_SCREEN,VOLUME_UP,VOLUME_DOWN"
)
