package com.quotegarden.feature.browse

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.quotegarden.core.data.QuoteRepository
import com.quotegarden.core.database.CategoryCount
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class BrowseScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test fun `renders category tiles and invokes onSelectCategory on tap`() {
        val counts = MutableStateFlow(
            listOf(CategoryCount("\u52b1\u5fd7", 3), CategoryCount("\u7231\u60c5", 2))
        )
        val repo = mockk<QuoteRepository>()
        every { repo.observeCategoryCounts() } returns counts
        val vm = BrowseViewModel(repo)
        var clicked: String? = null

        composeTestRule.setContent {
            BrowseScreen(viewModel = vm, onSelectCategory = { clicked = it })
        }
        composeTestRule.onNodeWithText("\u52b1\u5fd7").assertExists()
        composeTestRule.onNodeWithText("\u7231\u60c5").assertExists()
        composeTestRule.onNodeWithText("By mood").assertExists()
        composeTestRule.onNodeWithText("2 categories - 5 lines").assertExists()
        composeTestRule.onNodeWithText("\u52b1\u5fd7").performClick()
        assertEquals("\u52b1\u5fd7", clicked)
    }

    @Test fun `empty counts renders empty state`() {
        val counts = MutableStateFlow<List<CategoryCount>>(emptyList())
        val repo = mockk<QuoteRepository>()
        every { repo.observeCategoryCounts() } returns counts
        val vm = BrowseViewModel(repo)
        composeTestRule.setContent {
            BrowseScreen(viewModel = vm, onSelectCategory = {})
        }
        composeTestRule.onNodeWithText("Nothing to browse yet").assertExists()
    }
}
