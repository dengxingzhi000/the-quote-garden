package com.quotegarden.feature.home

import com.quotegarden.core.model.Quote

enum class EmptyMode {
    Generic,
    NoCategoryLines,
}

data class HomeUiState(
    val quote: Quote? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isBrowsing: Boolean = false,
    val isCurrentQuoteFavorite: Boolean = false,
    val favoriteTapKey: Int = 0,
    val selectedCategory: String? = null,
    val availableCategories: List<String> = emptyList(),
    val emptyMode: EmptyMode = EmptyMode.Generic,
    val history: List<Quote> = emptyList(),
    val historyIndex: Int = -1,
)

sealed interface HomeEvent {
    data object Load : HomeEvent
    data object NextRandom : HomeEvent
    data object Prev : HomeEvent
    data class Favorite(val id: String) : HomeEvent
    data class SelectCategory(val category: String?) : HomeEvent
}