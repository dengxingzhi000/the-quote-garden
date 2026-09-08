package com.dailymind.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.dailymind.core.database.CategoryCount
import com.dailymind.core.database.entity.QuoteEntity
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
