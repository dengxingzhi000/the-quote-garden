package com.dailymind.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.dailymind.core.designsystem.EditorialError
import com.dailymind.core.designsystem.EditorialTopBar
import com.dailymind.core.designsystem.OutlineTextAction
import com.dailymind.core.designsystem.QuoteBlock
import com.dailymind.core.designsystem.TextAction
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToFavorite: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val date = LocalDate.now().format(DateTimeFormatter.ofPattern("MM / dd"))
    val greeting = when (LocalTime.now().hour) {
        in 5..11 -> "Good Morning"
        in 12..17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(64.dp))
                EditorialTopBar(date = date, greeting = greeting)
                Spacer(modifier = Modifier.height(48.dp))
            }
            item {
                AnimatedVisibility(visible = state.isLoading) {
                    Text(
                        text = "Loading…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                }
            }
            item {
                AnimatedContent(
                    targetState = state.quote?.id ?: state.error ?: "empty",
                    transitionSpec = {
                        (fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 12 }) togetherWith
                            (fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 12 })
                    },
                    label = "quoteContent"
                ) { _ ->
                    when {
                        state.quote != null -> {
                            val q = state.quote!!
                            Text(
                                text = "TODAY'S QUOTE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            QuoteBlock(quote = q)
                            Spacer(modifier = Modifier.height(32.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                TextAction(
                                    label = "Next →",
                                    onClick = { viewModel.onEvent(HomeEvent.NextRandom) },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlineTextAction(
                                    label = "+ Favorite",
                                    onClick = { viewModel.onEvent(HomeEvent.Favorite(q.id)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            TextAction(label = "Saved →", onClick = onNavigateToFavorite)
                            if (q.id.startsWith("fallback")) {
                                Text(
                                    text = "Local demo content · syncs when online",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                        }
                        state.error != null -> {
                            EditorialError(
                                message = state.error!!,
                                onRetry = { viewModel.onEvent(HomeEvent.Load) }
                            )
                        }
                        !state.isLoading -> {
                            EditorialEmpty(
                                title = "Nothing here yet",
                                body = "Check your connection and try again.",
                                actionLabel = "Retry →",
                                onAction = { viewModel.onEvent(HomeEvent.Load) }
                            )
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(48.dp))
                Text(
                    text = "Offline-first · cached first, syncs quietly",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
