package com.dailymind.feature.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w800dp", manifest = Config.NONE)
class HomeChipsTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Composable
    private fun Wrap(content: @Composable () -> Unit) {
        MaterialTheme(colorScheme = darkColorScheme()) { content() }
    }

    @Test fun `All chip is selected by default and tapping a category emits SelectCategory`() {
        var emitted: String? = null
        composeTestRule.setContent {
            Wrap {
                CategoryChips(
                    selected = null,
                    available = listOf("love", "life"),
                    onPick = { emitted = it }
                )
            }
        }
        composeTestRule.onNodeWithText("All").assertExists()
        composeTestRule.onNodeWithText("love").performClick()
        assert(emitted == "love") { "expected 'love' got $emitted" }
    }

    @Test fun `More chip appears when more than 6 categories available`() {
        val cats = (1..8).map { "c$it" }
        composeTestRule.setContent {
            Wrap {
                CategoryChips(
                    selected = null,
                    available = cats,
                    onPick = {},
                    onMore = {}
                )
            }
        }
        composeTestRule.onNodeWithText("More\u2026").assertExists()
    }
}