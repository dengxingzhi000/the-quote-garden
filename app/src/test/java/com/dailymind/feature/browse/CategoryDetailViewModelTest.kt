package com.dailymind.feature.browse

import com.dailymind.MainDispatcherRule
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.model.Quote
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CategoryDetailViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun `valid category exposes list and not empty`() {
        val q = Quote("1", "Hello", null, null, "love", 1, null, null, 1L)
        val flow = MutableStateFlow(listOf(q))
        val repo = mockk<QuoteRepository>()
        every { repo.observeQuotesByCategory("love") } returns flow
        val vm = CategoryDetailViewModel(repo, "love")
        assertEquals(listOf("1"), vm.state.value.quotes.map { it.id })
        assertEquals(false, vm.state.value.isEmpty)
    }

    @Test fun `empty flow yields empty state`() {
        val repo = mockk<QuoteRepository>()
        every { repo.observeQuotesByCategory("life") } returns MutableStateFlow(emptyList())
        val vm = CategoryDetailViewModel(repo, "life")
        assertEquals(true, vm.state.value.isEmpty)
    }

    @Test fun `blank category immediately reports invalid`() {
        val repo = mockk<QuoteRepository>()
        val vm = CategoryDetailViewModel(repo, "   ")
        assertEquals(true, vm.state.value.isInvalid)
    }
}
