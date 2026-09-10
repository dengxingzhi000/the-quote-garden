package com.quotegarden.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quotegarden.core.database.entity.FavoriteEntity
import com.quotegarden.core.database.entity.QuoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FavoriteDaoTest {
    private fun db() = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        QuoteGardenDatabase::class.java
    ).allowMainThreadQueries().build()

    @Test fun toggleRoundTrip() = runTest {
        val db = db()
        db.quoteDao().upsertAll(listOf(QuoteEntity("1", "Hello", "你好", "Anon", "life", 1, null, null, 1000L, null)))
        db.favoriteDao().upsert(FavoriteEntity("1", 2000L))
        assertEquals(listOf("1"), db.favoriteDao().getIdsOnce())
        assertEquals("Hello", db.favoriteDao().observeFavorites().first()[0].content)
        db.favoriteDao().deleteById("1")
        assertTrue(db.favoriteDao().getIdsOnce().isEmpty())
        db.close()
    }
}
