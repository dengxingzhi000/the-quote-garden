package com.dailymind.feature.home

import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.model.Quote
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class HomeViewModelTest {
    @Test fun `initial state loads daily quote`() = runTest {
        val repo = mockk<QuoteRepository>()
        coEvery { repo.getDailyQuote() } returns Quote("1", "Hello", "你好", "Anon", "life", 1, null, null, 1000L)
        val vm = HomeViewModel(repo)
        vm.onEvent(HomeEvent.Load)
        assertEquals("Hello", vm.uiState.value.quote?.content)
    }
}
