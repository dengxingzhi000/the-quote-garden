package com.dailymind.core.datastore

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferencesKeys {
    val LAST_SYNC = longPreferencesKey("last_sync")
    val THEME = stringPreferencesKey("theme")
}
