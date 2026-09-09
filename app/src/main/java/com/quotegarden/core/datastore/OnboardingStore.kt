package com.quotegarden.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OnboardingStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val seen: Flow<Boolean> =
        dataStore.data.map { it[PreferencesKeys.ONBOARDING_SEEN] ?: false }

    suspend fun setSeen() {
        dataStore.edit { prefs -> prefs[PreferencesKeys.ONBOARDING_SEEN] = true }
    }
}
