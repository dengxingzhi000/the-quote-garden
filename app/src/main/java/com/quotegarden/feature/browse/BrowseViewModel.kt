package com.quotegarden.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotegarden.core.data.QuoteRepository
import com.quotegarden.core.database.CategoryCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BrowseUiState(
    val tiles: List<CategoryCount> = emptyList(),
    val totalQuotes: Int = 0,
    val totalCategories: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repo: QuoteRepository
) : ViewModel() {
    val state: StateFlow<BrowseUiState> = repo.observeCategoryCounts()
        .map { counts ->
            BrowseUiState(
                tiles = counts,
                totalQuotes = counts.sumOf { it.count },
                totalCategories = counts.size,
                isLoading = false,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrowseUiState())
}
