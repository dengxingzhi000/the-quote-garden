package com.dailymind.feature.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailymind.core.database.CategoryCount
import com.dailymind.core.designsystem.EditorialEmpty
import com.dailymind.core.designsystem.EditorialTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel = hiltViewModel(),
    onSelectCategory: (String) -> Unit = {},
    onGoHome: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(64.dp))
            EditorialTopBar(
                date = "",
                greeting = "By mood"
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${state.totalCategories} categories · ${state.totalQuotes} lines",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (state.tiles.isEmpty() && !state.isLoading) {
                EditorialEmpty(
                    title = "Nothing to browse yet",
                    body = "Pull to sync on Home.",
                    actionLabel = "Go to Home",
                    onAction = onGoHome
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(count = state.tiles.size, key = { state.tiles[it].category }) { idx ->
                        CategoryTile(state.tiles[idx], idx, onSelectCategory)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTile(
    item: CategoryCount,
    index: Int,
    onClick: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant)
            .background(MaterialTheme.colorScheme.background)
            .clickable { onClick(item.category) }
            .padding(16.dp)
    ) {
        Text(
            text = "#%02d".format(index + 1),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.TopStart)
        )
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(
                text = item.category,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${item.count} lines",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}