package com.quotegarden.core.database.di

import android.content.Context
import androidx.room.Room
import com.quotegarden.core.database.QuoteGardenDatabase
import com.quotegarden.core.database.TranslationBackfillMigration
import com.quotegarden.core.database.dao.FavoriteDao
import com.quotegarden.core.database.dao.HistoryDao
import com.quotegarden.core.database.dao.QuoteDao
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
    fun provideDatabase(@ApplicationContext context: Context): QuoteGardenDatabase {
        val translationsJson = try {
            context.assets.open(TranslationBackfillMigration.ASSET_PATH_PUBLIC)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        } catch (_: Exception) {
            ""
        }
        val translations = TranslationBackfillMigration.loadFromAssets(translationsJson)

        return Room.databaseBuilder(context, QuoteGardenDatabase::class.java, "quotegarden.db")
            .createFromAsset("quotegarden.db")
            .addMigrations(TranslationBackfillMigration(translations))
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideQuoteDao(db: QuoteGardenDatabase): QuoteDao = db.quoteDao()

    @Provides
    fun provideFavoriteDao(db: QuoteGardenDatabase): FavoriteDao = db.favoriteDao()

    @Provides
    fun provideHistoryDao(db: QuoteGardenDatabase): HistoryDao = db.historyDao()
}
