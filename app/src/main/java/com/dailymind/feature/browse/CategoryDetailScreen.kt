package com.dailymind.feature.browse

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dailymind.core.designsystem.EditorialEmpty
import com.dailymind.core.designsystem.IndexRow

@Composable
fun CategoryDetailScreen(
    category: String,
    onBack: () -> Unit,
    viewModel: CategoryDetailViewModel = hiltViewModel(
        creationCallback = { factory: CategoryDetailViewModel.Factory -> factory.create(category) }
    ),
) {
    LaunchedEffect(category) {
        if (category.isBlank()) onBack()
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.isInvalid) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(48.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "← Back",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = state.category,
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${state.quotes.size} lines",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            if (state.isEmpty) {
                item {
                    EditorialEmpty(
                        title = "This category is empty",
                        body = "Pull to sync or pick another.",
                        actionLabel = "Back",
                        onAction = onBack
                    )
                }
            } else {
                itemsIndexed(state.quotes, key = { _, q -> q.id }) { index, q ->
                    IndexRow(
                        number = (index + 1).toString().padStart(2, '0'),
                        content = q.content,
                        author = q.author,
                        onClick = {},
                        onSwipeOut = null
                    )
                }
            }
        }
    }
}