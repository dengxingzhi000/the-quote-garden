package com.dailymind.feature.home

import com.dailymind.MainDispatcherRule
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.model.Quote
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun `initial state loads daily quote`() = runTest {
        val repo = mockk<QuoteRepository>()
        coEvery { repo.getDailyQuote() } returns Quote("1", "Hello", "你好", "Anon", "life", 1, null, null, 1000L)
        val vm = HomeViewModel(repo)
        vm.onEvent(HomeEvent.Load)
        assertEquals("Hello", vm.uiState.value.quote?.content)
    }

    @Test fun `Load resets browsing mode`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        coEvery { repo.getDailyQuote() } returns Quote("1", "Hello", "你好", "Anon", "life", 1, null, null, 1000L)
        coEvery { repo.getRandomQuote() } returns Quote("2", "Next", "下一条", "Anon", "life", 1, null, null, 2000L)
        val vm = HomeViewModel(repo)
        vm.onEvent(HomeEvent.NextRandom)
        assertEquals(true, vm.uiState.value.isBrowsing)
        vm.onEvent(HomeEvent.Load)
        assertEquals(false, vm.uiState.value.isBrowsing)
    }

    @Test fun `NextRandom enters browsing mode`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        coEvery { repo.getRandomQuote() } returns Quote("2", "Next", "下一条", "Anon", "life", 1, null, null, 2000L)
        val vm = HomeViewModel(repo)
        vm.onEvent(HomeEvent.NextRandom)
        assertEquals(true, vm.uiState.value.isBrowsing)
    }
}
