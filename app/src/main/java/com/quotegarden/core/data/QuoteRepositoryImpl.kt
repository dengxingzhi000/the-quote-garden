package com.quotegarden.core.data

import com.quotegarden.core.database.CategoryCount
import com.quotegarden.core.database.dao.FavoriteDao
import com.quotegarden.core.database.dao.HistoryDao
import com.quotegarden.core.database.dao.QuoteDao
import com.quotegarden.core.database.entity.FavoriteEntity
import com.quotegarden.core.database.entity.HistoryEntity
import com.quotegarden.core.database.entity.QuoteEntity
import com.quotegarden.core.datastore.DailyQuoteStore
import com.quotegarden.core.datastore.todayEpochDay
import com.quotegarden.core.model.Quote
import com.quotegarden.core.network.ApiService
import com.quotegarden.core.network.dto.QuoteDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuoteRepositoryImpl @Inject constructor(
    private val dao: QuoteDao,
    private val api: ApiService,
    private val historyDao: HistoryDao,
    private val dailyStore: DailyQuoteStore,
    private val favorites: FavoriteDao
) : QuoteRepository {
    override fun observeQuotes(): Flow<List<Quote>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override fun observeFavorites(): Flow<List<Quote>> =
        favorites.observeFavorites().map { list -> list.map { it.toModel() } }

    override suspend fun getDailyQuote(category: String?): Quote? {
        if (category != null) {
            return dao.getRandomFiltered(category)?.toModel()
        }
        val today = todayEpochDay()
        dailyStore.getPinned()?.takeIf { it.day == today }?.let { pinned ->
            dao.getById(pinned.quoteId)
                ?.takeIf { it.deletedAt == null }
                ?.toModel()
                ?.let { return it }
        }
        val seen = historyDao.getAllQuoteIds().toSet()
        var pick: QuoteEntity? = null
        for (i in 0 until MAX_REROLL) {
            val candidate = dao.getRandom() ?: break
            pick = candidate
            if (candidate.id !in seen) break
        }
        val entity = pick?.takeIf { it.deletedAt == null }
            ?: dao.getAll().firstOrNull { it.deletedAt == null }
            ?: return fetchNetworkDaily()
        recordToday(entity.id)
        return entity.toModel()
    }

    override suspend fun getLocalRandomQuote(excludeId: String?, category: String?): Quote? =
        if (excludeId == null) dao.getRandomFiltered(category)?.toModel()
        else dao.getRandomExcludingFiltered(excludeId, category)?.toModel()

    override fun observeFavoriteIds(): Flow<Set<String>> =
        favorites.observeFavorites().map { list -> list.map { it.id }.toSet() }

    override suspend fun getRandomQuote(): Quote =
        try {
            val dto = api.getRandomQuote()
            dao.upsertAllPreserveTranslation(listOf(dto.toEntity()))
            dto.toModel()
        } catch (e: Exception) {
            dao.getRandom()?.toModel()
                ?: throw IllegalStateException("No cached quote and network unavailable")
        }

    override suspend fun sync(): Result<Unit> = runCatching {
        val max = dao.getMaxUpdatedAt() ?: 0L
        var cursor: String? = null
        do {
            val res = api.syncQuotes(updatedAfter = max, cursor = cursor)
            dao.upsertAllPreserveTranslation(res.items.map { it.toEntity() })
            cursor = res.nextCursor
        } while (cursor != null)
    }

    override suspend fun toggleFavorite(quoteId: String) {
        if (favorites.getIdsOnce().contains(quoteId)) {
            favorites.deleteById(quoteId)
        } else {
            favorites.upsert(FavoriteEntity(quoteId, System.currentTimeMillis()))
        }
    }

    override fun observeCategories(): Flow<List<String>> = dao.observeCategories()

    override fun observeCategoryCounts(): Flow<List<CategoryCount>> = dao.observeCategoryCounts()

    override fun observeQuotesByCategory(category: String): Flow<List<Quote>> =
        dao.observeByCategory(category).map { list -> list.map { it.toModel() } }

    private suspend fun fetchNetworkDaily(): Quote? = try {
        val dto = api.getDailyQuote()
        dao.upsertAllPreserveTranslation(listOf(dto.toEntity()))
        recordToday(dto.id)
        dto.toModel()
    } catch (e: Exception) {
        null
    }

    private suspend fun recordToday(quoteId: String) {
        historyDao.insert(HistoryEntity(quoteId = quoteId, viewedAt = System.currentTimeMillis()))
        dailyStore.setPinned(todayEpochDay(), quoteId)
    }

    private fun QuoteEntity.toModel() = Quote(id, content, translation, author, category, difficulty, audioUrl, imageUrl, updatedAt)
    private fun QuoteDto.toModel() = Quote(id, content, translation, author, category, difficulty, audioUrl, imageUrl, updatedAt)
    private fun QuoteDto.toEntity() = QuoteEntity(id, content, translation, author, category, difficulty, audioUrl, imageUrl, updatedAt, deletedAt)

    companion object {
        private const val MAX_REROLL = 20
    }
}
