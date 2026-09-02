package com.dailymind.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToFavorite: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.quote?.let { q ->
            Text(q.content, style = MaterialTheme.typography.headlineSmall)
            q.translation?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            q.author?.let { Text("- $it", style = MaterialTheme.typography.labelMedium) }
            Row {
                Button(onClick = { viewModel.onEvent(HomeEvent.NextRandom) }) { Text("下一句") }
                OutlinedButton(onClick = { viewModel.onEvent(HomeEvent.Favorite(q.id)) }) { Text("收藏") }
            }
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
