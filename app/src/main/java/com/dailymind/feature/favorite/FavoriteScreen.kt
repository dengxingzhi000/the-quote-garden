package com.dailymind.feature.favorite

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailymind.core.designsystem.EditorialEmpty
import com.dailymind.core.designsystem.IndexRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteScreen(
    vm: FavoriteViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val list by vm.favorites.collectAsStateWithLifecycle()
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(64.dp))
                Text(
                    text = "Collection",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "A small archive of beautiful thoughts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "${list.size} saved",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (list.isEmpty()) {
                item {
                    EditorialEmpty(
                        title = "No saved quotes yet",
                        body = "Keep the lines that stay with you.",
                        actionLabel = "Explore →",
                        onAction = onNavigateBack
                    )
                }
            } else {
                itemsIndexed(list, key = { _, q -> q.id }) { index, q ->
                    IndexRow(
                        number = (index + 1).toString().padStart(2, '0'),
                        content = q.content,
                        author = q.author,
                        onClick = {},
                        onSwipeOut = { vm.onEvent(FavoriteEvent.Unfavorite(q.id)) },
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
