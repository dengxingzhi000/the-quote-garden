package com.dailymind.core.data

import com.dailymind.core.database.dao.QuoteDao
import com.dailymind.core.database.entity.QuoteEntity
import com.dailymind.core.model.Quote
import com.dailymind.core.network.ApiService
import com.dailymind.core.network.dto.QuoteDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuoteRepositoryImpl @Inject constructor(
    private val dao: QuoteDao,
    private val api: ApiService
) : QuoteRepository {
    override fun observeQuotes(): Flow<List<Quote>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    override suspend fun getDailyQuote(): Quote? =
        dao.getAll().firstOrNull()?.toModel()

    override suspend fun getRandomQuote(): Quote =
        (dao.getRandom() ?: throw NoSuchElementException("no quotes offline")).toModel()

    override suspend fun sync(): Result<Unit> = runCatching {
        val max = dao.getMaxUpdatedAt() ?: 0L
        var cursor: String? = null
        do {
            val res = api.syncQuotes(updatedAfter = max, cursor = cursor)
            dao.upsertAll(res.items.map { it.toEntity() })
            cursor = res.nextCursor
        } while (cursor != null)
    }

    override suspend fun toggleFavorite(quoteId: String) {
        // V1 stub: actual favorite join implemented in Task 8 extension
    }

    private fun QuoteEntity.toModel() = Quote(id, content, translation, author, category, difficulty, audioUrl, imageUrl, updatedAt)
    private fun QuoteDto.toEntity() = QuoteEntity(id, content, translation, author, category, difficulty, audioUrl, imageUrl, updatedAt, deletedAt)
}
