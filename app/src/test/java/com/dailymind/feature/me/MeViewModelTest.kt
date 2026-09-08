package com.dailymind.feature.me

import com.dailymind.MainDispatcherRule
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.datastore.ThemeMode
import com.dailymind.core.datastore.ThemeStore
import com.dailymind.core.model.Quote
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private fun repoWithEmptyFavorites(): QuoteRepository {
        val repo = mockk<QuoteRepository>(relaxed = true)
        every { repo.observeFavorites() } returns flowOf(emptyList())
        return repo
    }

    private fun themeStore(): ThemeStore {
        val store = mockk<ThemeStore>(relaxed = true)
        every { store.mode } returns flowOf(ThemeMode.SYSTEM)
        return store
    }

    @Test fun `emits favorites from repository`() = runTest {
        val repo = mockk<QuoteRepository>()
        every { repo.observeFavorites() } returns flowOf(
            listOf(Quote("1", "Hello", "你好", "Anon", "life", 1, null, null, 1000L))
        )
        val vm = MeViewModel(repo, themeStore())
        assertEquals("Hello", vm.favorites.first { it.isNotEmpty() }[0].content)
    }

    @Test fun `Unfavorite calls repo toggleFavorite`() = runTest {
        val repo = repoWithEmptyFavorites()
        val vm = MeViewModel(repo, themeStore())
        vm.onEvent(MeEvent.Unfavorite("42"))
        coVerify { repo.toggleFavorite("42") }
    }

    @Test fun `setThemeMode delegates to store`() = runTest {
        val store = themeStore()
        val vm = MeViewModel(repoWithEmptyFavorites(), store)
        vm.setThemeMode(ThemeMode.DARK)
        coVerify { store.setMode(ThemeMode.DARK) }
    }
}
