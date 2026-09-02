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
        (dao.getRandom() ?: QuoteEntity(
            id = "fallback-1",
            content = "The only way to do great work is to love what you do.",
            translation = "成就伟大事业的唯一方法是热爱你的工作。",
            author = "Steve Jobs",
            category = "motivation",
            difficulty = 1,
            audioUrl = null,
            imageUrl = null,
            updatedAt = System.currentTimeMillis(),
            deletedAt = null
        )).toModel()

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
