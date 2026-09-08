package com.dailymind.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dailymind.core.database.entity.HistoryEntity

@Dao
interface HistoryDao {
    @Insert
    suspend fun insert(entry: HistoryEntity)

    @Query("SELECT quoteId FROM history")
    suspend fun getAllQuoteIds(): List<String>
}
