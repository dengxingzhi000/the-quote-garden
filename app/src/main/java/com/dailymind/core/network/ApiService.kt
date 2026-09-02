package com.dailymind.core.network

import com.dailymind.core.network.dto.QuoteDto
import com.dailymind.core.network.dto.SyncResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("api/v1/quotes/daily")
    suspend fun getDailyQuote(): QuoteDto

    @GET("api/v1/quotes/random")
    suspend fun getRandomQuote(): QuoteDto

    @GET("api/v1/sync/quotes")
    suspend fun syncQuotes(
        @Query("updatedAfter") updatedAfter: Long,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 100
    ): SyncResponse
}
