package com.quotegarden.core.data

import com.quotegarden.core.database.CategoryCount
import com.quotegarden.core.database.dao.FavoriteDao
import com.quotegarden.core.database.dao.HistoryDao
import com.quotegarden.core.database.dao.QuoteDao
import com.quotegarden.core.database.entity.QuoteEntity
import com.quotegarden.core.datastore.DailyQuoteStore
import com.quotegarden.core.network.ApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class QuoteRepositoryCategoryTest {
    private val entity = QuoteEntity("9", "c", "t", "a", "love", 1, null, null, 1L, null)

    private fun repo(
        dao: QuoteDao = mockk(relaxed = true),
        api: ApiService = mockk(),
        historyDao: HistoryDao = mockk(relaxed = true),
        store: DailyQuoteStore = mockk(relaxed = true),
        favorites: FavoriteDao = mockk(relaxed = true)
    ) = QuoteRepositoryImpl(dao, api, historyDao, store, favorites)

    @Test fun `getDailyQuote with category ignores pin and calls getRandomFiltered`() = runTest {
        val dao = mockk<QuoteDao>(relaxed = true)
        val store = mockk<DailyQuoteStore>(relaxed = true)
        val api = mockk<ApiService>()
        coEvery { store.getPinned() } returns null
        coEvery { dao.getRandomFiltered("love") } returns entity
        val q = repo(dao = dao, api = api, store = store).getDailyQuote("love")
        assertNotNull(q)
        assertEquals("love", q!!.category)
        coVerify { dao.getRandomFiltered("love") }
        coVerify(exactly = 0) { store.setPinned(any(), any()) }
    }

    @Test fun `getDailyQuote with category returns null when category is empty`() = runTest {
        val dao = mockk<QuoteDao>(relaxed = true)
        val store = mockk<DailyQuoteStore>(relaxed = true)
        val api = mockk<ApiService>()
        coEvery { store.getPinned() } returns null
        coEvery { dao.getRandomFiltered("love") } returns null
        assertNull(repo(dao = dao, api = api, store = store).getDailyQuote("love"))
    }

    @Test fun `getDailyQuote with null preserves pin behavior`() = runTest {
        val dao = mockk<QuoteDao>(relaxed = true)
        val store = mockk<DailyQuoteStore>(relaxed = true)
        coEvery { store.getPinned() } returns null
        coEvery { dao.getRandom() } returns entity
        repo(dao = dao, store = store).getDailyQuote()
        coVerify { dao.getRandom() }
    }

    @Test fun `getLocalRandomQuote forwards excludeId and category to new DAO method`() = runTest {
        val dao = mockk<QuoteDao>()
        coEvery { dao.getRandomExcludingFiltered("1", "love") } returns entity
        val q = repo(dao = dao).getLocalRandomQuote(excludeId = "1", category = "love")
        assertEquals("9", q?.id)
    }

    @Test fun `observeQuotesByCategory maps entities to domain models`() = runTest {
        val dao = mockk<QuoteDao>()
        coEvery { dao.observeByCategory("love") } returns flowOf(listOf(entity))
        val list = repo(dao = dao).observeQuotesByCategory("love").first()
        assertEquals(listOf("9"), list.map { it.id })
        assertEquals("love", list[0].category)
    }

    @Test fun `observeCategories forwards DAO flow`() = runTest {
        val dao = mockk<QuoteDao>()
        coEvery { dao.observeCategories() } returns flowOf(listOf("love", "life"))
        assertEquals(listOf("love", "life"), repo(dao = dao).observeCategories().first())
    }

    @Test fun `observeCategoryCounts forwards DAO flow`() = runTest {
        val dao = mockk<QuoteDao>()
        coEvery { dao.observeCategoryCounts() } returns flowOf(listOf(CategoryCount("love", 1)))
        assertEquals(listOf(CategoryCount("love", 1)), repo(dao = dao).observeCategoryCounts().first())
    }
}
