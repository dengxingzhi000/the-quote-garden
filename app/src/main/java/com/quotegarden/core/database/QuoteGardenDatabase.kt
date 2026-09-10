package com.quotegarden.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.quotegarden.core.database.dao.FavoriteDao
import com.quotegarden.core.database.dao.HistoryDao
import com.quotegarden.core.database.dao.QuoteDao
import com.quotegarden.core.database.entity.FavoriteEntity
import com.quotegarden.core.database.entity.HistoryEntity
import com.quotegarden.core.database.entity.QuoteEntity

@Database(
    entities = [QuoteEntity::class, FavoriteEntity::class, HistoryEntity::class],
    version = 2,
    exportSchema = true
)
abstract class QuoteGardenDatabase : RoomDatabase() {
    abstract fun quoteDao(): QuoteDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
}
