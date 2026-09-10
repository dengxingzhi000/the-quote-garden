package com.quotegarden.feature.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.quotegarden.R
import kotlinx.coroutines.launch

private data class OnboardingPage(
    @param:DrawableRes val image: Int,
    val title: String,
    val subtitle: String,
)

private val PAGES = listOf(
    OnboardingPage(
        image = R.drawable.ic_welcome,
        title = "Quote Garden",
        subtitle = "A quiet line, every day."
    ),
    OnboardingPage(
        image = R.drawable.ic_daily_quote,
        title = "Today's Quote",
        subtitle = "Start each morning with a line worth keeping."
    ),
    OnboardingPage(
        image = R.drawable.ic_favorites,
        title = "Keep What You Love",
        subtitle = "Save the words that stay with you."
    ),
    OnboardingPage(
        image = R.drawable.onboard_4,
        title = "Sync Quietly",
        subtitle = "Your library refreshes in the background."
    )
)

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { PAGES.size })
    val scope = rememberCoroutineScope()
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPageContent(
                page = PAGES[page],
                isLast = page == PAGES.lastIndex
            ) {
                if (page == PAGES.lastIndex) onDone()
                else scope.launch { pagerState.animateScrollToPage(page + 1) }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    isLast: Boolean,
    onPrimary: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = page.image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 48.dp)
        ) {
            Text(
                text = page.title,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = page.subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = if (isLast) "Begin ->" else "Next")
            }
        }
    }
}
