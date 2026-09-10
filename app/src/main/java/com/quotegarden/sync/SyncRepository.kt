package com.quotegarden.sync

import com.quotegarden.core.data.QuoteRepository
import javax.inject.Inject

class SyncRepository @Inject constructor(
    private val repo: QuoteRepository
) {
    suspend fun syncAll() { repo.sync() }
}
