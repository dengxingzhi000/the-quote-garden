package com.dailymind.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    fun resolveDark(isSystemDark: Boolean): Boolean = when (this) {
        LIGHT -> false
        DARK -> true
        SYSTEM -> isSystemDark
    }
}

@Singleton
class ThemeStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val mode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[PreferencesKeys.THEME] ?: "SYSTEM") }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[PreferencesKeys.THEME] = mode.name }
    }
}
