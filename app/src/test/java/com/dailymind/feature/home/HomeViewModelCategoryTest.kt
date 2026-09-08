package com.dailymind.feature.home

import com.dailymind.MainDispatcherRule
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.database.CategoryCount
import com.dailymind.core.datastore.CategoryPreferenceStore
import com.dailymind.core.model.Quote
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class HomeViewModelCategoryTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private fun quote(id: String, cat: String? = null) =
        Quote(id, "Q-$id", null, null, cat, 1, null, null, 1L)

    private fun storeWithSelected(value: String?): CategoryPreferenceStore {
        val s = mockk<CategoryPreferenceStore>(relaxed = true)
        every { s.selectedCategory } returns MutableStateFlow<String?>(value)
        return s
    }

    @Test fun `SelectCategory writes store and updates state`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        every { repo.observeCategoryCounts() } returns flowOf(emptyList())
        val vm = HomeViewModel(repo, storeWithSelected(null))
        vm.onEvent(HomeEvent.SelectCategory("love"))
        coVerify { repo.getDailyQuote("love") }
        assertEquals("love", vm.uiState.value.selectedCategory)
    }

    @Test fun `SelectCategory to null does not reload when current quote has category`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        every { repo.observeCategoryCounts() } returns flowOf(emptyList())
        val vm = HomeViewModel(repo, storeWithSelected("love"))
        (vm.uiState as MutableStateFlow<HomeUiState>).value =
            vm.uiState.value.copy(quote = quote("1", cat = "love"))
        vm.onEvent(HomeEvent.SelectCategory(null))
        assertNull(vm.uiState.value.selectedCategory)
        coVerify(exactly = 0) { repo.getDailyQuote(null) }
    }

    @Test fun `SelectCategory to a non-matching category reloads`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        every { repo.observeCategoryCounts() } returns flowOf(emptyList())
        coEvery { repo.getDailyQuote("life") } returns quote("9", cat = "life")
        val vm = HomeViewModel(repo, storeWithSelected("love"))
        (vm.uiState as MutableStateFlow<HomeUiState>).value =
            vm.uiState.value.copy(quote = quote("1", cat = "love"))
        vm.onEvent(HomeEvent.SelectCategory("life"))
        assertEquals("9", vm.uiState.value.quote?.id)
    }

    @Test fun `availableCategories reflects top 6 by count`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        every { repo.observeCategoryCounts() } returns flowOf(
            listOf(
                CategoryCount("a", 1), CategoryCount("b", 1), CategoryCount("c", 1),
                CategoryCount("d", 1), CategoryCount("e", 1), CategoryCount("f", 1),
                CategoryCount("g", 1),
            )
        )
        val vm = HomeViewModel(repo, storeWithSelected(null))
        assertEquals(listOf("a", "b", "c", "d", "e", "f"), vm.uiState.value.availableCategories)
    }

    @Test fun `emptyMode is NoCategoryLines when category is selected and getDailyQuote returns null`() = runTest {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavoriteIds() } returns flowOf(emptySet())
        every { repo.observeCategoryCounts() } returns flowOf(emptyList())
        coEvery { repo.getDailyQuote("love") } returns null
        // Per spec §4.4: getRandomQuote is NOT called as a fallback when a category is selected
        val vm = HomeViewModel(repo, storeWithSelected("love"))
        vm.onEvent(HomeEvent.SelectCategory("love"))
        assertNull(vm.uiState.value.quote)
        assertEquals(EmptyMode.NoCategoryLines, vm.uiState.value.emptyMode)
        coVerify(exactly = 0) { repo.getRandomQuote() }
    }
}