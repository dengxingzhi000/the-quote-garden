package com.dailymind.feature.home

import com.dailymind.core.model.Quote

data class HomeUiState(
    val quote: Quote? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBrowsing: Boolean = false
)

sealed interface HomeEvent {
    data object Load : HomeEvent
    data object NextRandom : HomeEvent
    data class Favorite(val id: String) : HomeEvent
}
