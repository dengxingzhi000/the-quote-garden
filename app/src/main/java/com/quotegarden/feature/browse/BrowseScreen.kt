package com.quotegarden.feature.browse

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotegarden.R
import com.quotegarden.core.database.CategoryCount
import com.quotegarden.core.designsystem.categoryDrawable

private val OnDark = Color.White
private val OnDarkMuted = Color.White.copy(alpha = 0.72f)
private val OnDarkFaint = Color.White.copy(alpha = 0.32f)
private val HeroScrim = Color.Black.copy(alpha = 0.5f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel = hiltViewModel(),
    onSelectCategory: (String) -> Unit = {},
    onGoHome: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(id = R.drawable.ic_daily_quote),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    Brush.verticalGradient(
                        0f to HeroScrim,
                        0.45f to HeroScrim,
                        1f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )
        Scaffold(containerColor = Color.Transparent) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(modifier = Modifier.height(64.dp))
                Text(
                    text = "By mood",
                    style = MaterialTheme.typography.displayLarge,
                    color = OnDark
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${state.totalCategories} categories - ${state.totalQuotes} lines",
                    style = MaterialTheme.typography.labelSmall,
                    color = OnDarkMuted
                )
                Spacer(modifier = Modifier.height(24.dp))
                if (state.tiles.isEmpty() && !state.isLoading) {
                    HeroEmptyBlock(
                        title = "Nothing to browse yet",
                        body = "Pull to sync on Home.",
                        actionLabel = "Go to Home",
                        onAction = onGoHome,
                        illustration = R.drawable.empty_browse,
                        illustrationSize = 160.dp
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(count = state.tiles.size, key = { state.tiles[it].category }) {
                            HeroCategoryTile(state.tiles[it], onSelectCategory)
                        }
                    }
                }
                Spacer(modifier = Modifier.navigationBarsPadding().height(8.dp))
            }
        }
    }
}

@Composable
private fun HeroCategoryTile(
    item: CategoryCount,
    onClick: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick(item.category) }
    ) {
        Image(
            painter = painterResource(id = categoryDrawable(item.category)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.85f)
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = item.category,
                style = MaterialTheme.typography.headlineSmall,
                color = OnDark
            )
            Text(
                text = "${item.count} lines",
                style = MaterialTheme.typography.labelSmall,
                color = OnDarkMuted
            )
        }
    }
}

@Composable
private fun HeroEmptyBlock(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    illustration: Int? = null,
    illustrationSize: androidx.compose.ui.unit.Dp = 120.dp,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (illustration != null) {
            Image(
                painter = painterResource(id = illustration),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(illustrationSize)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = OnDark,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = OnDarkMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = actionLabel,
            style = MaterialTheme.typography.labelLarge,
            color = OnDark,
            modifier = Modifier.clickable(onClick = onAction).padding(vertical = 8.dp)
        )
    }
}
