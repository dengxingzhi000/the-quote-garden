package com.dailymind.feature.favorite

import com.dailymind.MainDispatcherRule
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.model.Quote
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class FavoriteViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test fun `emits favorites from repository`() = runTest {
        val repo = mockk<QuoteRepository>()
        every { repo.observeFavorites() } returns flowOf(
            listOf(Quote("1", "Hello", "你好", "Anon", "life", 1, null, null, 1000L))
        )
        val vm = FavoriteViewModel(repo)
        assertEquals("Hello", vm.favorites.first { it.isNotEmpty() }[0].content)
    }
}
