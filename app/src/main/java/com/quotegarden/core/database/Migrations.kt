package com.quotegarden.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONObject

/**
 * Migration 1 -> 2: backfill `quote.translation` for existing installs.
 *
 * The Room prepopulated DB (`assets/quotegarden.db`) is copied to the
 * device only on first install — subsequent APK updates do NOT re-copy
 * it, so any translations added to the asset AFTER first install would
 * otherwise never reach users.
 *
 * This migration reads `assets/translations_v2.json` (pre-loaded by
 * [com.quotegarden.core.database.di.DatabaseModule] before the DB is
 * built) and writes `translation` for each known quote id. Rows whose
 * `translation` is already non-null are preserved as-is so server-side
 * updates win once they ship.
 */
class TranslationBackfillMigration(
    private val translations: Map<String, String>,
) : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        if (translations.isEmpty()) return
        db.beginTransaction()
        try {
            for ((id, translation) in translations) {
                db.execSQL(
                    "UPDATE quote SET translation = ? " +
                        "WHERE id = ? AND (translation IS NULL OR translation = '')",
                    arrayOf(translation, id),
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    companion object {
        private const val ASSET_PATH = "translations_v2.json"

        fun loadFromAssets(json: String): Map<String, String> {
            val root = JSONObject(json)
            val entries = root.optJSONArray("entries") ?: return emptyMap()
            val out = HashMap<String, String>(entries.length())
            for (i in 0 until entries.length()) {
                val e = entries.optJSONObject(i) ?: continue
                val id = e.optString("id").takeIf { it.isNotEmpty() } ?: continue
                val t = e.optString("translation").takeIf { it.isNotEmpty() } ?: continue
                out[id] = t
            }
            return out
        }

        const val ASSET_PATH_PUBLIC = ASSET_PATH
    }
}
