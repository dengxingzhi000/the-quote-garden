package com.dailymind.sync

import com.dailymind.core.data.QuoteRepository
import javax.inject.Inject

class SyncRepository @Inject constructor(
    private val repo: QuoteRepository
) {
    suspend fun syncAll() { repo.sync() }
}
