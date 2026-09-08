package com.dailymind.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    viewModel: HomeViewModel = hiltViewModel()
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
                        Spacer(modifier = Modifier.height(48.dp))
                    } else {
                        EditorialTopBar(
                            date = date,
                            greeting = greeting
                        )
                    }
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
            item {
                CategoryChips(
                    selected = state.selectedCategory,
                    available = state.availableCategories,
                    onPick = { viewModel.onEvent(HomeEvent.SelectCategory(it)) },
                    onMore = { /* future: show full sheet */ }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            item {
                AnimatedVisibility(visible = state.quote != null && !state.isBrowsing) {
                    Column {
                        Text(
                            text = "TODAY'S QUOTE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
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
                            when (state.emptyMode) {
                                EmptyMode.NoCategoryLines -> EditorialEmpty(
                                    title = "No lines in this category yet",
                                    body = "This category hasn't been synced to your device.",
                                    actionLabel = "Switch to All",
                                    onAction = { viewModel.onEvent(HomeEvent.SelectCategory(null)) }
                                )
                                EmptyMode.Generic -> EditorialEmpty(
                                    title = "Nothing here yet",
                                    body = "Check your connection and try again.",
                                    actionLabel = "Retry →",
                                    onAction = { viewModel.onEvent(HomeEvent.Load) }
                                )
                            }
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
                            label = if (state.isCurrentQuoteFavorite) "Saved ✓" else "+ Save",
                            onClick = { viewModel.onEvent(HomeEvent.Favorite(q.id)) },
                            isOn = state.isCurrentQuoteFavorite,
                            pulseKey = state.favoriteTapKey,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryChips(
    selected: String?,
    available: List<String>,
    onPick: (String?) -> Unit,
    onMore: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    var sheetVisible by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            ChipLabel(text = "All", isSelected = selected == null, onClick = { onPick(null) })
        }
        items(available) { cat ->
            ChipLabel(text = cat, isSelected = selected == cat, onClick = { onPick(cat) })
        }
        if (available.size > 6 && onMore != null) {
            item {
                ChipLabel(text = "More\u2026", isSelected = false, onClick = { sheetVisible = true })
            }
        }
    }
    if (sheetVisible) {
        ModalBottomSheet(onDismissRequest = { sheetVisible = false }, sheetState = sheetState) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                available.forEach { cat ->
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clickable {
                                onPick(cat)
                                sheetVisible = false
                            }
                            .padding(vertical = 12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ChipLabel(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .heightIn(min = 40.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
