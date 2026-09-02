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
        when {
            state.quote != null -> {
                val q = state.quote!!
                Text(q.content, style = MaterialTheme.typography.headlineSmall)
                q.translation?.let { Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp)) }
                q.author?.let { Text("- $it", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp)) }
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    Button(onClick = { viewModel.onEvent(HomeEvent.NextRandom) }) { Text("下一句") }
                    OutlinedButton(onClick = { viewModel.onEvent(HomeEvent.Favorite(q.id)) }, modifier = Modifier.padding(start = 8.dp)) { Text("收藏") }
                }
                if (q.id.startsWith("fallback")) {
                    Text("当前为本地演示数据，联网后将同步最新内容", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
                }
            }
            state.error != null -> {
                Text("加载失败: ${state.error}", color = MaterialTheme.colorScheme.error)
                Button(onClick = { viewModel.onEvent(HomeEvent.Load) }, modifier = Modifier.padding(top = 8.dp)) { Text("重试") }
            }
            !state.isLoading -> {
                Text("暂无数据", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { viewModel.onEvent(HomeEvent.Load) }, modifier = Modifier.padding(top = 8.dp)) { Text("重试") }
            }
        }
    }
}
