package com.dailymind.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

object PreferencesKeys {
    val LAST_SYNC = longPreferencesKey("last_sync")
    val THEME = stringPreferencesKey("theme")
    val DAILY_QUOTE_DATE = longPreferencesKey("daily_quote_date")
    val DAILY_QUOTE_ID = stringPreferencesKey("daily_quote_id")
}

data class PinnedDaily(val day: Long, val quoteId: String)

/** 当天标识（本地时区 epoch day），跨天即换一句。 */
fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

/**
 * 首页"每日一句"钉选：同一天重复打开返回同一条。
 * 纯本地状态（Room + DataStore），离线可用。
 */
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
