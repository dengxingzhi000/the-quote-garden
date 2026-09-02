package com.dailymind.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dailymind.core.database.entity.QuoteEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuoteDaoTest {
    @Test fun insertAndRead() = runTest {
        val db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DailyMindDatabase::class.java
        ).allowMainThreadQueries().build()
        val dao = db.quoteDao()
        dao.upsertAll(listOf(QuoteEntity("1", "Hello", "你好", "Anon", "life", 1, null, null, 1000L, null)))
        val all = dao.getAll()
        assertEquals(1, all.size)
        assertEquals("Hello", all[0].content)
        db.close()
    }
}
