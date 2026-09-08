package com.dailymind.core.database.di

import android.content.Context
import androidx.room.Room
import com.dailymind.core.database.DailyMindDatabase
import com.dailymind.core.database.dao.FavoriteDao
import com.dailymind.core.database.dao.HistoryDao
import com.dailymind.core.database.dao.QuoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DailyMindDatabase =
        Room.databaseBuilder(context, DailyMindDatabase::class.java, "dailymind.db")
            .createFromAsset("dailymind.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideQuoteDao(db: DailyMindDatabase): QuoteDao = db.quoteDao()

    @Provides
    fun provideFavoriteDao(db: DailyMindDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideHistoryDao(db: DailyMindDatabase): HistoryDao = db.historyDao()
}
