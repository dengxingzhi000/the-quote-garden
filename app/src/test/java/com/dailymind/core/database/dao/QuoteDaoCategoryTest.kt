package com.dailymind.core.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dailymind.core.database.CategoryCount
import com.dailymind.core.database.DailyMindDatabase
import com.dailymind.core.database.entity.QuoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class QuoteDaoCategoryTest {
    private lateinit var db: DailyMindDatabase
    private lateinit var dao: QuoteDao

    @Before fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DailyMindDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.quoteDao()
    }

    @After fun tearDown() { db.close() }

    private fun entity(id: String, cat: String? = null, deletedAt: Long? = null, updatedAt: Long = 1L) =
        QuoteEntity(id, "Q-$id", null, "A-$id", cat, 1, null, null, updatedAt, deletedAt)

    @Test fun `observeCategories returns distinct non-null categories sorted asc`() = runTest {
        dao.upsertAll(listOf(
            entity("1", "励志"),
            entity("2", "爱情"),
            entity("3", "励志"),
            entity("4", null),
        ))
        assertEquals(listOf("励志", "爱情"), dao.observeCategories().first())
    }

    @Test fun `observeCategories excludes deleted rows`() = runTest {
        dao.upsertAll(listOf(
            entity("1", "励志"),
            entity("2", "爱情", deletedAt = 1L),
        ))
        assertEquals(listOf("励志"), dao.observeCategories().first())
    }

    @Test fun `observeCategoryCounts returns counts ordered desc then asc`() = runTest {
        dao.upsertAll(listOf(
            entity("1", "励志"),
            entity("2", "励志"),
            entity("3", "励志"),
            entity("4", "爱情"),
            entity("5", "爱情"),
            entity("6", "孤独"),
        ))
        assertEquals(
            listOf(
                CategoryCount("励志", 3),
                CategoryCount("爱情", 2),
                CategoryCount("孤独", 1),
            ),
            dao.observeCategoryCounts().first()
        )
    }

    @Test fun `observeByCategory returns only matching rows ordered by id`() = runTest {
        dao.upsertAll(listOf(
            entity("a", "励志"),
            entity("b", "爱情"),
            entity("c", "励志"),
        ))
        val rows = dao.observeByCategory("励志").first()
        assertEquals(listOf("a", "c"), rows.map { it.id })
    }

    @Test fun `getRandomFiltered with null returns any row`() = runTest {
        dao.upsertAll(listOf(entity("1", "励志"), entity("2", "爱情")))
        val picked = dao.getRandomFiltered(null)
        assertNotNull(picked)
        assertTrue(picked!!.id in listOf("1", "2"))
    }

    @Test fun `getRandomFiltered with category returns only that category`() = runTest {
        dao.upsertAll(listOf(entity("1", "励志"), entity("2", "爱情"), entity("3", "励志")))
        repeat(20) {
            assertEquals("励志", dao.getRandomFiltered("励志")?.category)
        }
    }

    @Test fun `getRandomExcludingFiltered excludes id and filters category`() = runTest {
        dao.upsertAll(listOf(
            entity("1", "励志"),
            entity("2", "励志"),
            entity("3", "爱情"),
        ))
        val picked = dao.getRandomExcludingFiltered("1", "励志")
        assertEquals("2", picked?.id)
    }

    @Test fun `getRandomFiltered returns null when category is empty`() = runTest {
        dao.upsertAll(listOf(entity("1", "爱情")))
        assertNull(dao.getRandomFiltered("励志"))
    }
}
