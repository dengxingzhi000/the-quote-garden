package com.quotegarden.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.quotegarden.core.database.CategoryCount
import com.quotegarden.core.database.entity.QuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {
    @Query("SELECT * FROM quote WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<QuoteEntity>>

    @Query("SELECT * FROM quote WHERE deletedAt IS NULL")
    suspend fun getAll(): List<QuoteEntity>

    @Query("SELECT * FROM quote WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): QuoteEntity?

    @Query("SELECT * FROM quote WHERE deletedAt IS NULL ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandom(): QuoteEntity?

    @Query("SELECT * FROM quote WHERE deletedAt IS NULL AND id != :excludeId ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomExcluding(excludeId: String): QuoteEntity?

    @Upsert
    suspend fun upsertAll(entities: List<QuoteEntity>)

    @Transaction
    suspend fun upsertAllPreserveTranslation(entities: List<QuoteEntity>) {
        for (e in entities) {
            if (e.translation.isNullOrBlank()) {
                val existing = getById(e.id)
                if (existing != null && !existing.translation.isNullOrBlank()) {
                    upsertAll(listOf(e.copy(translation = existing.translation)))
                    continue
                }
            }
            upsertAll(listOf(e))
        }
    }

    @Query("UPDATE quote SET translation = :translation WHERE id = :id AND (translation IS NULL OR translation = '')")
    suspend fun updateTranslationIfEmpty(id: String, translation: String): Int

    @Transaction
    suspend fun applyTranslations(translations: Map<String, String>) {
        for ((id, t) in translations) {
            updateTranslationIfEmpty(id, t)
        }
    }

    @Query("SELECT MAX(updatedAt) FROM quote")
    suspend fun getMaxUpdatedAt(): Long?

    @Query("""
        SELECT DISTINCT category FROM quote
        WHERE category IS NOT NULL AND deletedAt IS NULL
        ORDER BY category
    """)
    fun observeCategories(): Flow<List<String>>

    @Query("""
        SELECT category AS category, COUNT(*) AS count FROM quote
        WHERE category IS NOT NULL AND deletedAt IS NULL
        GROUP BY category
        ORDER BY count DESC, category ASC
    """)
    fun observeCategoryCounts(): Flow<List<CategoryCount>>

    @Query("""
        SELECT * FROM quote
        WHERE category = :category AND deletedAt IS NULL
        ORDER BY id
    """)
    fun observeByCategory(category: String): Flow<List<QuoteEntity>>

    @Query("""
        SELECT * FROM quote
        WHERE deletedAt IS NULL
          AND (:category IS NULL OR category = :category)
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun getRandomFiltered(category: String?): QuoteEntity?

    @Query("""
        SELECT * FROM quote
        WHERE id != :excludeId AND deletedAt IS NULL
          AND (:category IS NULL OR category = :category)
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun getRandomExcludingFiltered(excludeId: String, category: String?): QuoteEntity?
}

