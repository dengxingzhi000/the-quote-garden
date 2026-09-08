package com.dailymind.feature.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.model.Quote
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
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

@HiltViewModel(assistedFactory = CategoryDetailViewModel.Factory::class)
class CategoryDetailViewModel @AssistedInject constructor(
    private val repo: QuoteRepository,
    @Assisted initialCategory: String,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(category: String): CategoryDetailViewModel
    }

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