package com.dailymind.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dailymind.core.database.dao.FavoriteDao
import com.dailymind.core.database.dao.HistoryDao
import com.dailymind.core.database.dao.QuoteDao
import com.dailymind.core.database.entity.FavoriteEntity
import com.dailymind.core.database.entity.HistoryEntity
import com.dailymind.core.database.entity.QuoteEntity

@Database(
    entities = [QuoteEntity::class, FavoriteEntity::class, HistoryEntity::class],
    version = 1,
    exportSchema = true
)
abstract class DailyMindDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
}
