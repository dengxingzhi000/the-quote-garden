package com.quotegarden.core.datastore

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.quotegarden.core.database.dao.QuoteDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Self-healing translation patcher.
 *
 * Detects "asset out of sync with local DB" on every app launch and
 * applies the diff in the background. Logic:
 *
 * 1. Read `assets/translations.json` (or `translations_v2.json`), extract
 *    the embedded `version` field.
 * 2. Compare to the last-applied version stored in SharedPreferences
 *    (key [KEY_APPLIED_VERSION]).
 * 3. If different (or missing), iterate entries and `UPDATE quote SET
 *    translation = ? WHERE id = ? AND translation IS NULL OR ''`.
 *    The WHERE clause makes the patch idempotent and protects server-
 *    supplied translations from being clobbered.
 * 4. Persist the new version stamp.
 *
 * Future flows:
 * - Improve translation quality → bump the JSON's `version` field, ship
 *   the new asset. The patcher detects the change and re-applies on
 *   next launch. No DB version bump required.
 * - Add a second translation source → drop `translations_v3.json` next
 *   to it, bump the field, patcher picks it up.
 */
@Singleton
class TranslationPatcher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val quoteDao: QuoteDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Fire-and-forget; safe to call from [android.app.Application.onCreate]. */
    fun ensureApplied() {
        scope.launch { runOnce() }
    }

    /** Synchronous variant for tests / debug screens. */
    suspend fun runOnce(): Result<Int> = runCatching {
        val payload = readPayload() ?: return@runCatching 0
        val assetVersion = payload.optString(FIELD_VERSION)
        if (assetVersion.isEmpty()) {
            Log.w(TAG, "asset $ASSET_PATH missing $FIELD_VERSION; skipping")
            return@runCatching 0
        }
        val appliedVersion = prefs.getString(KEY_APPLIED_VERSION, null)
        if (appliedVersion == assetVersion) {
            Log.d(TAG, "translation patch $assetVersion already applied; noop")
            return@runCatching 0
        }
        val translations = parseEntries(payload)
        if (translations.isEmpty()) {
            Log.w(TAG, "asset $ASSET_PATH has no entries; skipping")
            return@runCatching 0
        }
        quoteDao.applyTranslations(translations)
        prefs.edit().putString(KEY_APPLIED_VERSION, assetVersion).apply()
        Log.i(TAG, "applied translation patch $assetVersion (${translations.size} entries)")
        translations.size
    }.onFailure { Log.e(TAG, "patch run failed", it) }

    private fun readPayload(): JSONObject? = runCatching {
        context.assets.open(ASSET_PATH).use { input ->
            val raw = input.bufferedReader(Charsets.UTF_8).use { it.readText() }
            JSONObject(raw)
        }
    }.getOrNull()

    private fun parseEntries(payload: JSONObject): Map<String, String> {
        val entries = payload.optJSONArray(FIELD_ENTRIES) ?: return emptyMap()
        val out = HashMap<String, String>(entries.length())
        for (i in 0 until entries.length()) {
            val e = entries.optJSONObject(i) ?: continue
            val id = e.optString(FIELD_ID)
            val t = e.optString(FIELD_TRANSLATION)
            if (id.isNotEmpty() && t.isNotEmpty()) out[id] = t
        }
        return out
    }

    companion object {
        private const val TAG = "TranslationPatcher"
        const val ASSET_PATH = "translations_v2.json"
        const val PREFS = "translation_patch"
        const val KEY_APPLIED_VERSION = "applied_version"
        private const val FIELD_VERSION = "version"
        private const val FIELD_ENTRIES = "entries"
        private const val FIELD_ID = "id"
        private const val FIELD_TRANSLATION = "translation"
    }
}
