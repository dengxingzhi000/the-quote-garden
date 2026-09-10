package com.quotegarden.core.data

import com.quotegarden.core.database.CategoryCount
import com.quotegarden.core.model.Quote
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {
    fun observeQuotes(): Flow<List<Quote>>
    fun observeFavorites(): Flow<List<Quote>>

    suspend fun getDailyQuote(category: String? = null): Quote?

    suspend fun getRandomQuote(): Quote

    suspend fun getLocalRandomQuote(
        excludeId: String? = null,
        category: String? = null,
    ): Quote?

    fun observeFavoriteIds(): Flow<Set<String>>
    suspend fun sync(): Result<Unit>
    suspend fun toggleFavorite(quoteId: String)

    fun observeCategories(): Flow<List<String>>
    fun observeCategoryCounts(): Flow<List<CategoryCount>>
    fun observeQuotesByCategory(category: String): Flow<List<Quote>>
}
