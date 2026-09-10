package com.quotegarden.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.quotegarden.core.database.entity.FavoriteEntity
import com.quotegarden.core.database.entity.QuoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Upsert
    suspend fun upsert(entry: FavoriteEntity)

    @Query("DELETE FROM favorite WHERE quoteId = :quoteId")
    suspend fun deleteById(quoteId: String)

    @Query("SELECT quote.* FROM quote INNER JOIN favorite ON quote.id = favorite.quoteId WHERE quote.deletedAt IS NULL ORDER BY favorite.createdAt DESC")
    fun observeFavorites(): Flow<List<QuoteEntity>>

    @Query("SELECT quoteId FROM favorite")
    suspend fun getIdsOnce(): List<String>
}
