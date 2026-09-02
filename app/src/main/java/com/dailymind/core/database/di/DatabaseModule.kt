package com.dailymind.core.database.di

import android.content.Context
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dailymind.core.database.DailyMindDatabase
import com.dailymind.core.database.dao.QuoteDao
import com.dailymind.core.database.entity.QuoteEntity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DailyMindDatabase =
        Room.databaseBuilder(context, DailyMindDatabase::class.java, "dailymind.db")
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Pre-populate handled via fallback in repository; seed async to avoid blocking
                    CoroutineScope(Dispatchers.IO).launch {
                        // Seed will be done lazily via repository fallback; keep callback lightweight
                    }
                }
            })
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideQuoteDao(db: DailyMindDatabase): QuoteDao = db.quoteDao()
}
