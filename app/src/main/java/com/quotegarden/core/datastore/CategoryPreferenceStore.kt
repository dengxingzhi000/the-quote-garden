package com.quotegarden.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class CategoryPreferenceStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val selectedCategory: Flow<String?> =
        dataStore.data.map { it[PreferencesKeys.CATEGORY] }

    suspend fun setSelectedCategory(value: String?) {
        dataStore.edit { prefs ->
            if (value == null) prefs.remove(PreferencesKeys.CATEGORY)
            else prefs[PreferencesKeys.CATEGORY] = value
        }
    }
}
