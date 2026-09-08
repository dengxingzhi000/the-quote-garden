package com.dailymind.feature.home

import com.dailymind.MainDispatcherRule
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.datastore.CategoryPreferenceStore
import com.dailymind.core.model.Quote
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private fun quote(id: String, content: String = "Q-$id") = Quote(id, content, null, null, null, 1, null, null, 1L)

    private fun categoryStore(): CategoryPreferenceStore {
        val s = mockk<CategoryPreferenceStore>(relaxed = true)
        every { s.selectedCategory } returns MutableStateFlow<String?>(null)
        return s
    }

    @Test fun `initial state loads daily quote`() = runTest {
        val repo = mockk<QuoteRepository>()
        coEvery { repo.getDailyQuote(null) } returns quote("1")
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        every { repo.observeCategoryCounts() } returns flowOf(emptyList())
        val vm = HomeViewModel(repo, categoryStore())
        vm.onEvent(HomeEvent.Load)
        assertEquals("Q-1", vm.uiState.value.quote?.content)
    }

    @Test fun `NextRandom picks local random excluding current quote`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        coEvery { repo.getLocalRandomQuote(excludeId = "1", category = null) } returns quote("2", "Q-2")
        val vm = HomeViewModel(repo, categoryStore())
        @Suppress("UNCHECKED_CAST")
        (vm.uiState as MutableStateFlow<HomeUiState>).value = vm.uiState.value.copy(quote = quote("1"))
        vm.onEvent(HomeEvent.NextRandom)
        assertEquals("Q-2", vm.uiState.value.quote?.content)
        assertEquals(true, vm.uiState.value.isBrowsing)
        coVerify { repo.getLocalRandomQuote(excludeId = "1", category = null) }
    }

    @Test fun `NextRandom surfaces error when local random returns null`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        coEvery { repo.getLocalRandomQuote(any(), any()) } returns null
        val vm = HomeViewModel(repo, categoryStore())
        vm.onEvent(HomeEvent.NextRandom)
        assertNotNull(vm.uiState.value.error)
    }

    @Test fun `Favorite increments favoriteTapKey`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        val vm = HomeViewModel(repo, categoryStore())
        val before = vm.uiState.value.favoriteTapKey
        vm.onEvent(HomeEvent.Favorite("1"))
        assertEquals(before + 1, vm.uiState.value.favoriteTapKey)
    }

    @Test fun `isCurrentQuoteFavorite reflects observeFavoriteIds`() = runTest {
        val repo = mockk<QuoteRepository>()
        coEvery { repo.getDailyQuote(null) } returns quote("1")
        val favIds = MutableStateFlow(setOf("1"))
        every { repo.observeFavoriteIds() } returns favIds
        every { repo.observeCategoryCounts() } returns flowOf(emptyList())
        val vm = HomeViewModel(repo, categoryStore())
        vm.onEvent(HomeEvent.Load)
        assertEquals(true, vm.uiState.value.isCurrentQuoteFavorite)
    }
}