package com.dailymind.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object PreferencesKeys {
    val LAST_SYNC = longPreferencesKey("last_sync")
    val THEME = stringPreferencesKey("theme")
    val DAILY_QUOTE_DATE = longPreferencesKey("daily_quote_date")
    val DAILY_QUOTE_ID = stringPreferencesKey("daily_quote_id")
}

data class PinnedDaily(val day: Long, val quoteId: String)

/** Local-day identifier (epoch day in the device timezone). */
fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

/** Same-day quote pinning. Stored in DataStore, no network required. */
@Singleton
class DailyQuoteStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val pinnedFlow: Flow<PinnedDaily?> = dataStore.data.map { prefs ->
        val id = prefs[PreferencesKeys.DAILY_QUOTE_ID] ?: return@map null
        PinnedDaily(prefs[PreferencesKeys.DAILY_QUOTE_DATE] ?: -1L, id)
    }

    suspend fun getPinned(): PinnedDaily? = pinnedFlow.first()

    suspend fun setPinned(day: Long, quoteId: String) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.DAILY_QUOTE_DATE] = day
            prefs[PreferencesKeys.DAILY_QUOTE_ID] = quoteId
        }
    }
}
