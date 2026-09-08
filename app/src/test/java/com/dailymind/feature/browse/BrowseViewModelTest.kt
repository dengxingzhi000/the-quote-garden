package com.dailymind.feature.browse

import com.dailymind.MainDispatcherRule
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.database.CategoryCount
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BrowseViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun `empty counts produces empty state with loading off`() {
        val counts = MutableStateFlow<List<CategoryCount>>(emptyList())
        val repo = mockk<QuoteRepository>()
        every { repo.observeCategoryCounts() } returns counts
        val vm = BrowseViewModel(repo)
        assertEquals(emptyList<CategoryCount>(), vm.state.value.tiles)
        assertEquals(0, vm.state.value.totalQuotes)
        assertEquals(0, vm.state.value.totalCategories)
        assertEquals(false, vm.state.value.isLoading)
    }

    @Test fun `non-empty counts computes totals and tiles`() {
        val counts = MutableStateFlow(
            listOf(CategoryCount("love", 3), CategoryCount("life", 2), CategoryCount("孤独", 1))
        )
        val repo = mockk<QuoteRepository>()
        every { repo.observeCategoryCounts() } returns counts
        val vm = BrowseViewModel(repo)
        assertEquals(6, vm.state.value.totalQuotes)
        assertEquals(3, vm.state.value.totalCategories)
        assertEquals(3, vm.state.value.tiles.size)
    }
}
