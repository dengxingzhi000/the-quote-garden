package com.quotegarden.feature.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
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

    @Test @Config(qualifiers = "w200dp")
    fun `scrollable chips keep container row-high so quote zone is not crushed`() {
        // Regression: the trailing edge-fade overlay must not stretch the container
        // to full height (fillMaxHeight in a wrapping Box does that), which collapsed
        // the weight(1f) quote zone to 0dp and left Home with a blank quote area.
        val cats = (1..12).map { "category-$it" }
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
        composeTestRule.onNodeWithTag("chipsEdgeFade").assertExists()
        val chipsBounds = composeTestRule.onNodeWithTag("categoryChips").getBoundsInRoot()
        val chipsHeightPx = with(composeTestRule.density) {
            (chipsBounds.bottom - chipsBounds.top).toPx()
        }
        val maxPx = with(composeTestRule.density) { 160.dp.toPx() }
        assert(chipsHeightPx <= maxPx) {
            "chips container too tall: ${chipsHeightPx}px (max ${maxPx}px)"
        }
    }
}
