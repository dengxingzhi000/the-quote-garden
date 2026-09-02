package com.dailymind.core.data

import com.dailymind.core.database.dao.QuoteDao
import com.dailymind.core.database.entity.QuoteEntity
import com.dailymind.core.network.ApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class QuoteRepositoryTest {
    @Test fun `observeQuotes emits from dao`() = runTest {
        val dao = mockk<QuoteDao>()
        val api = mockk<ApiService>()
        coEvery { dao.observeAll() } returns flowOf(listOf(QuoteEntity("1", "H", "t", "A", "c", 1, null, null, 1L, null)))
        val repo = QuoteRepositoryImpl(dao, api)
        val list = repo.observeQuotes().first()
        assertEquals(1, list.size)
    }
}
