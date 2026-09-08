package com.dailymind.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.model.Quote
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CategoryDetailUiState(
    val category: String = "",
    val quotes: List<Quote> = emptyList(),
    val isEmpty: Boolean = false,
    val isInvalid: Boolean = false,
)

@HiltViewModel
class CategoryDetailViewModel @Inject constructor(
    private val repo: QuoteRepository,
    initialCategory: String,
) : ViewModel() {
    private val _state = MutableStateFlow(CategoryDetailUiState(category = initialCategory))
    val state: StateFlow<CategoryDetailUiState> = _state.asStateFlow()

    init {
        if (initialCategory.isBlank()) {
            _state.value = _state.value.copy(isInvalid = true)
        } else {
            viewModelScope.launch {
                repo.observeQuotesByCategory(initialCategory).collect { list ->
                    _state.value = _state.value.copy(
                        quotes = list,
                        isEmpty = list.isEmpty(),
                    )
                }
            }
        }
    }
}
