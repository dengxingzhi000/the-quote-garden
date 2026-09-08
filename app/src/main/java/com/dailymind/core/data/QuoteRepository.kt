package com.dailymind.core.data

import com.dailymind.core.model.Quote
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {
    fun observeQuotes(): Flow<List<Quote>>
    fun observeFavorites(): Flow<List<Quote>>
    suspend fun getDailyQuote(): Quote?
    suspend fun getRandomQuote(): Quote
    suspend fun getLocalRandomQuote(excludeId: String?): Quote?
    fun observeFavoriteIds(): Flow<Set<String>>
    suspend fun sync(): Result<Unit>
    suspend fun toggleFavorite(quoteId: String)
}
