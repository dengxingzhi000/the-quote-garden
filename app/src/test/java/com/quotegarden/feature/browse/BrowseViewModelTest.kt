package com.quotegarden.feature.browse

import com.quotegarden.MainDispatcherRule
import com.quotegarden.core.data.QuoteRepository
import com.quotegarden.core.database.CategoryCount
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun `empty counts produces empty state with loading off`() = runTest {
        val counts = MutableStateFlow<List<CategoryCount>>(emptyList())
        val repo = mockk<QuoteRepository>()
        every { repo.observeCategoryCounts() } returns counts
        val vm = BrowseViewModel(repo)
        val s = vm.state.first { !it.isLoading }
        assertEquals(emptyList<CategoryCount>(), s.tiles)
        assertEquals(0, s.totalQuotes)
        assertEquals(0, s.totalCategories)
        assertEquals(false, s.isLoading)
    }

    @Test fun `non-empty counts computes totals and tiles`() = runTest {
        val counts = MutableStateFlow(
            listOf(CategoryCount("love", 3), CategoryCount("life", 2), CategoryCount("\u5b64\u72ec", 1))
        )
        val repo = mockk<QuoteRepository>()
        every { repo.observeCategoryCounts() } returns counts
        val vm = BrowseViewModel(repo)
        val s = vm.state.first { !it.isLoading }
        assertEquals(6, s.totalQuotes)
        assertEquals(3, s.totalCategories)
        assertEquals(3, s.tiles.size)
    }
}
