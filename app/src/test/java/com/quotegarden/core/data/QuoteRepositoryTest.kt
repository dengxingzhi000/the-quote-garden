package com.quotegarden.core.data

import com.quotegarden.core.database.dao.FavoriteDao
import com.quotegarden.core.database.dao.HistoryDao
import com.quotegarden.core.database.dao.QuoteDao
import com.quotegarden.core.database.entity.FavoriteEntity
import com.quotegarden.core.database.entity.HistoryEntity
import com.quotegarden.core.database.entity.QuoteEntity
import com.quotegarden.core.datastore.DailyQuoteStore
import com.quotegarden.core.datastore.PinnedDaily
import com.quotegarden.core.datastore.todayEpochDay
import com.quotegarden.core.network.ApiService
import com.quotegarden.core.network.dto.QuoteDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class QuoteRepositoryTest {
    private val dto = QuoteDto("9", "Real content", "真内容", "Real Author", "life", 2, null, null, 2000L, null)
    private val entity = QuoteEntity("9", "Real content", "真内容", "Real Author", "life", 2, null, null, 2000L, null)

    private fun repo(
        dao: QuoteDao = mockk(relaxed = true),
        api: ApiService = mockk(),
        historyDao: HistoryDao = mockk(relaxed = true),
        store: DailyQuoteStore = mockk(relaxed = true),
        favorites: FavoriteDao = mockk(relaxed = true)
    ) = QuoteRepositoryImpl(dao, api, historyDao, store, favorites)

    @Test fun `observeQuotes emits from dao`() = runTest {
        val dao = mockk<QuoteDao>()
        coEvery { dao.observeAll() } returns flowOf(listOf(QuoteEntity("1", "H", "t", "A", "c", 1, null, null, 1L, null)))
        val list = repo(dao = dao).observeQuotes().first()
        assertEquals(1, list.size)
    }

    @Test fun `getDailyQuote returns pinned quote when pinned today`() = runTest {
        val dao = mockk<QuoteDao>(relaxed = true)
        val api = mockk<ApiService>()
        val store = mockk<DailyQuoteStore>()
        coEvery { store.getPinned() } returns PinnedDaily(todayEpochDay(), "9")
        coEvery { dao.getById("9") } returns entity
        val quote = repo(dao = dao, api = api, store = store).getDailyQuote()
        assertEquals("Real content", quote?.content)
        coVerify(exactly = 0) { api.getDailyQuote() }
    }

    @Test fun `getDailyQuote picks unseen random and pins it`() = runTest {
        val dao = mockk<QuoteDao>(relaxed = true)
        val api = mockk<ApiService>()
        val historyDao = mockk<HistoryDao>(relaxed = true)
        val store = mockk<DailyQuoteStore>(relaxed = true)
        coEvery { store.getPinned() } returns null
        coEvery { historyDao.getAllQuoteIds() } returns listOf("old-1")
        coEvery { dao.getRandom() } returns entity
        val quote = repo(dao = dao, api = api, historyDao = historyDao, store = store).getDailyQuote()
        assertEquals("Real content", quote?.content)
        coVerify { historyDao.insert(match<HistoryEntity> { it.quoteId == "9" }) }
        coVerify { store.setPinned(todayEpochDay(), "9") }
    }

    @Test fun `getDailyQuote rerolls seen quotes`() = runTest {
        val seen = entity.copy(id = "seen-1", content = "Seen")
        val dao = mockk<QuoteDao>(relaxed = true)
        val historyDao = mockk<HistoryDao>(relaxed = true)
        val store = mockk<DailyQuoteStore>(relaxed = true)
        coEvery { store.getPinned() } returns null
        coEvery { historyDao.getAllQuoteIds() } returns listOf("seen-1")
        coEvery { dao.getRandom() } returnsMany listOf(seen, entity)
        val quote = repo(dao = dao, historyDao = historyDao, store = store).getDailyQuote()
        assertEquals("Real content", quote?.content)
    }

    @Test fun `getDailyQuote falls back to network when db empty`() = runTest {
        val dao = mockk<QuoteDao>(relaxed = true)
        val api = mockk<ApiService>()
        val store = mockk<DailyQuoteStore>(relaxed = true)
        coEvery { store.getPinned() } returns null
        coEvery { dao.getRandom() } returns null
        coEvery { dao.getAll() } returns emptyList()
        coEvery { api.getDailyQuote() } returns dto
        val quote = repo(dao = dao, api = api, store = store).getDailyQuote()
        assertEquals("Real content", quote?.content)
        coVerify { dao.upsertAll(listOf(entity)) }
    }

    @Test fun `getRandomQuote fetches from network and caches`() = runTest {
        val dao = mockk<QuoteDao>(relaxed = true)
        val api = mockk<ApiService>()
        coEvery { api.getRandomQuote() } returns dto
        val quote = repo(dao = dao, api = api).getRandomQuote()
        assertEquals("Real content", quote.content)
        coVerify { dao.upsertAll(listOf(entity)) }
    }

    @Test fun `getRandomQuote falls back to cache on network error`() = runTest {
        val dao = mockk<QuoteDao>(relaxed = true)
        val api = mockk<ApiService>()
        coEvery { api.getRandomQuote() } throws java.io.IOException("offline")
        coEvery { dao.getRandom() } returns entity
        assertEquals("Real content", repo(dao = dao, api = api).getRandomQuote().content)
    }

    @Test fun `toggleFavorite adds when absent`() = runTest {
        val favorites = mockk<FavoriteDao>(relaxed = true)
        coEvery { favorites.getIdsOnce() } returns emptyList()
        repo(favorites = favorites).toggleFavorite("9")
        coVerify { favorites.upsert(match<FavoriteEntity> { it.quoteId == "9" }) }
    }

    @Test fun `toggleFavorite removes when present`() = runTest {
        val favorites = mockk<FavoriteDao>(relaxed = true)
        coEvery { favorites.getIdsOnce() } returns listOf("9")
        repo(favorites = favorites).toggleFavorite("9")
        coVerify { favorites.deleteById("9") }
        coVerify(exactly = 0) { favorites.upsert(any()) }
    }

    @Test fun `observeFavorites emits joined quotes`() = runTest {
        val favorites = mockk<FavoriteDao>()
        coEvery { favorites.observeFavorites() } returns flowOf(listOf(entity))
        val list = repo(favorites = favorites).observeFavorites().first()
        assertEquals(listOf("9"), list.map { it.id })
    }

    @Test fun `getLocalRandomQuote excludes specified id`() = runTest {
        val dao = mockk<QuoteDao>()
        val q2 = QuoteEntity("2", "B", "译", "Y", "c", 1, null, null, 2L, null)
        coEvery { dao.getRandomExcludingFiltered("1", null) } returns q2
        val q = repo(dao = dao).getLocalRandomQuote(excludeId = "1")!!
        assertEquals("2", q.id)
        coVerify { dao.getRandomExcludingFiltered("1", null) }
    }

    @Test fun `getLocalRandomQuote returns null on empty db`() = runTest {
        val dao = mockk<QuoteDao>()
        coEvery { dao.getRandomExcludingFiltered("any", null) } returns null
        assertNull(repo(dao = dao).getLocalRandomQuote(excludeId = "any"))
    }

    @Test fun `getLocalRandomQuote calls getRandom when excludeId null`() = runTest {
        val dao = mockk<QuoteDao>()
        coEvery { dao.getRandomFiltered(null) } returns entity
        val q = repo(dao = dao).getLocalRandomQuote(excludeId = null)!!
        assertEquals("9", q.id)
        coVerify(exactly = 0) { dao.getRandomExcludingFiltered(any(), any()) }
    }

    @Test fun `observeFavoriteIds emits id set from favorites flow`() = runTest {
        val favorites = mockk<FavoriteDao>()
        coEvery { favorites.observeFavorites() } returns flowOf(
            listOf(entity, entity.copy(id = "8", content = "Other"))
        )
        val ids = repo(favorites = favorites).observeFavoriteIds().first()
        assertEquals(setOf("9", "8"), ids)
    }
}
