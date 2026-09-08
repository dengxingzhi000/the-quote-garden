package com.dailymind.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.database.CategoryCount
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BrowseUiState(
    val tiles: List<CategoryCount> = emptyList(),
    val totalQuotes: Int = 0,
    val totalCategories: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repo: QuoteRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BrowseUiState())
    val state: StateFlow<BrowseUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeCategoryCounts().collect { counts ->
                _state.value = BrowseUiState(
                    tiles = counts,
                    totalQuotes = counts.sumOf { it.count },
                    totalCategories = counts.size,
                    isLoading = false,
                )
            }
        }
    }
}
