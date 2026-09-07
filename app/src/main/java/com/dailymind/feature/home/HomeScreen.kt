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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToFavorite: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val date = "%02d / %02d".format(now.monthNumber, now.dayOfMonth)
    val greeting = when (now.hour) {
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
                AnimatedContent(
                    targetState = state.isBrowsing,
                    transitionSpec = {
                        (fadeIn(tween(200)) togetherWith fadeOut(tween(200)))
                    },
                    label = "topBar"
                ) { browsing ->
                    if (browsing) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            TextAction(
                                label = "Saved →",
                                onClick = onNavigateToFavorite,
                                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd)
                            )
                        }
                    } else {
                        EditorialTopBar(
                            date = date,
                            greeting = greeting,
                            action = {
                                TextAction(label = "Saved →", onClick = onNavigateToFavorite)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
            item {
                AnimatedVisibility(visible = state.quote != null && !state.isBrowsing) {
                    Column {
                        Text(
                            text = "TODAY'S QUOTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
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
                        (fadeIn(tween(200)) +
                                slideInVertically(tween(200)) { it / 12 }) togetherWith
                            (fadeOut(tween(200)) +
                                    slideOutVertically(tween(200)) { -it / 12 })
                    },
                    label = "quoteContent"
                ) { key ->
                    val quote = state.quote
                    val error = state.error
                    when {
                        quote != null && key == quote.id -> {
                            QuoteBlock(quote = quote)
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        error != null && key == error -> {
                            EditorialError(
                                message = error,
                                onRetry = { viewModel.onEvent(HomeEvent.Load) }
                            )
                        }
                        key == "empty" && !state.isLoading -> {
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
                if (state.quote != null) {
                    val q = state.quote!!
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlineTextAction(
                            label = "+ Favorite",
                            onClick = { viewModel.onEvent(HomeEvent.Favorite(q.id)) },
                            modifier = Modifier.weight(1f)
                        )
                        TextAction(
                            label = "Next →",
                            onClick = { viewModel.onEvent(HomeEvent.NextRandom) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            item {
                AnimatedVisibility(visible = !state.isBrowsing) {
                    Column {
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
    }
}
