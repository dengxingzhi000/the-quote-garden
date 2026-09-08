package com.dailymind.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
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
}
