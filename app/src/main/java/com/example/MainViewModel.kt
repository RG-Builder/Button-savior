package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.LocalRepository
import com.example.data.SettingsEntity
import com.example.data.StatsEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LocalRepository

    val settingsState: StateFlow<SettingsEntity>
    val statsState: StateFlow<List<StatsEntity>>

    init {
        val db = AppDatabase.getInstance(application)
        repository = LocalRepository(db.appDao())

        // Expose settings reactively
        settingsState = repository.settingsFlow
            .map { it ?: SettingsEntity() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = SettingsEntity()
            )

        // Expose stats reactively
        statsState = repository.statsFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun updateButtonSize(size: Int) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(buttonSizeDp = size))
        }
    }

    fun updateButtonOpacity(opacity: Float) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(buttonOpacity = opacity))
        }
    }

    fun updateTheme(themeName: String) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(themeName = themeName))
        }
    }

    fun toggleHaptic(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(hapticEnabled = enabled))
        }
    }

    fun toggleShortcut(shortcutId: String) {
        viewModelScope.launch {
            val current = repository.getSettings()
            val list = current.enabledShortcuts.split(",").toMutableList()
            if (list.contains(shortcutId)) {
                list.remove(shortcutId)
            } else {
                list.add(shortcutId)
            }
            repository.saveSettings(current.copy(enabledShortcuts = list.joinToString(",")))
        }
    }

    fun simulateActionClick(actionId: String) {
        viewModelScope.launch {
            repository.incrementStat(actionId)
        }
    }

    fun clearStats() {
        viewModelScope.launch {
            val db = AppDatabase.getInstance(getApplication())
            // Insert empty/0 stats for all actions
            val actions = listOf("BACK", "HOME", "RECENTS", "NOTIFICATIONS", "QUICK_SETTINGS", "LOCK_SCREEN", "VOLUME_UP", "VOLUME_DOWN", "POWER_DIALOG")
            for (action in actions) {
                db.appDao().saveStat(StatsEntity(action, 0))
            }
        }
    }
}
