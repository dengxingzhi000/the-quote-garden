package com.dailymind.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.datastore.CategoryPreferenceStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: QuoteRepository,
    private val categoryStore: CategoryPreferenceStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState

    private var currentFavIds: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            repo.observeFavoriteIds().collect { favIds ->
                currentFavIds = favIds
                _uiState.update { state ->
                    state.copy(isCurrentQuoteFavorite = state.quote?.id in favIds)
                }
            }
        }
        viewModelScope.launch {
            combine(
                categoryStore.selectedCategory,
                repo.observeCategoryCounts(),
            ) { selected, counts ->
                selected to counts.take(MAX_CHIPS).map { it.category }
            }.collect { (selected, topCats) ->
                _uiState.update { state ->
                    state.copy(
                        selectedCategory = selected,
                        availableCategories = topCats,
                        emptyMode = computeEmptyMode(
                            state.copy(isLoading = false),
                            selected,
                        ),
                    )
                }
            }
        }
        onEvent(HomeEvent.Load)
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Load -> viewModelScope.launch {
                val current = _uiState.value
                _uiState.update { it.copy(isLoading = true) }
                try {
                    val q = if (current.selectedCategory == null) {
                        // "All": pin → unviewed random → network fallback
                        repo.getDailyQuote(null) ?: repo.getRandomQuote()
                    } else {
                        // Per spec §4.4: strict filter, no fallback to other categories
                        repo.getDailyQuote(current.selectedCategory)
                    }
                    _uiState.update { state ->
                        val next = state.copy(
                            quote = q,
                            isLoading = false,
                            isBrowsing = false,
                            error = null,
                            isCurrentQuoteFavorite = q?.id in currentFavIds,
                        )
                        next.copy(
                            emptyMode = computeEmptyMode(
                                next,
                                current.selectedCategory,
                            ),
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { state ->
                        val next = state.copy(
                            error = e.message,
                            isLoading = false,
                            isCurrentQuoteFavorite = state.quote?.id in currentFavIds,
                        )
                        next.copy(
                            emptyMode = computeEmptyMode(
                                next,
                                current.selectedCategory,
                            ),
                        )
                    }
                }
            }
            is HomeEvent.NextRandom -> viewModelScope.launch {
                try {
                    val current = _uiState.value
                    val q = repo.getLocalRandomQuote(
                        excludeId = current.quote?.id,
                        category = current.selectedCategory,
                    ) ?: throw IllegalStateException("No other quotes yet")
                    _uiState.update { state ->
                        state.copy(
                            quote = q,
                            error = null,
                            isBrowsing = true,
                            isCurrentQuoteFavorite = q.id in currentFavIds,
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = e.message) }
                }
            }
            is HomeEvent.Favorite -> viewModelScope.launch {
                repo.toggleFavorite(event.id)
                _uiState.update { it.copy(favoriteTapKey = it.favoriteTapKey + 1) }
            }
            is HomeEvent.SelectCategory -> viewModelScope.launch {
                categoryStore.setSelectedCategory(event.category)
                _uiState.update { state ->
                    val next = state.copy(selectedCategory = event.category)
                    next.copy(
                        emptyMode = computeEmptyMode(
                            next.copy(isLoading = false),
                            event.category,
                        ),
                    )
                }
                val current = _uiState.value
                val shouldReload = current.quote == null ||
                    (event.category != null && current.quote?.category != event.category)
                if (shouldReload) onEvent(HomeEvent.Load)
            }
        }
    }

    private fun computeEmptyMode(state: HomeUiState, selected: String?): EmptyMode {
        if (state.quote != null || state.isLoading || state.error != null) return EmptyMode.Generic
        if (selected != null) return EmptyMode.NoCategoryLines
        return EmptyMode.Generic
    }

    private companion object {
        const val MAX_CHIPS = 6
    }
}