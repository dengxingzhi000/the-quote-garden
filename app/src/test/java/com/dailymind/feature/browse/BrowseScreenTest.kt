package com.dailymind.feature.browse

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.database.CategoryCount
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
            listOf(CategoryCount("励志", 3), CategoryCount("爱情", 2))
        )
        val repo = mockk<QuoteRepository>()
        every { repo.observeCategoryCounts() } returns counts
        val vm = BrowseViewModel(repo)
        var clicked: String? = null

        composeTestRule.setContent {
            BrowseScreen(viewModel = vm, onSelectCategory = { clicked = it })
        }
        composeTestRule.onNodeWithText("励志").assertExists()
        composeTestRule.onNodeWithText("爱情").assertExists()
        composeTestRule.onNodeWithText("By mood").assertExists()
        composeTestRule.onNodeWithText("2 categories · 5 lines").assertExists()
        composeTestRule.onNodeWithText("励志").performClick()
        assertEquals("励志", clicked)
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