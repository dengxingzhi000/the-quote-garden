package com.dailymind.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailymind.core.data.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: QuoteRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch {
            repo.observeFavoriteIds()
                .combine(_uiState) { favIds, state ->
                    state.copy(isCurrentQuoteFavorite = state.quote?.id in favIds)
                }
                .collect { _uiState.value = it }
        }
        onEvent(HomeEvent.Load)
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Load -> viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                try {
                    val q = repo.getDailyQuote() ?: repo.getRandomQuote()
                    _uiState.value = _uiState.value.copy(quote = q, isLoading = false, isBrowsing = false)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message, isLoading = false)
                }
            }
            is HomeEvent.NextRandom -> viewModelScope.launch {
                try {
                    val current = _uiState.value.quote?.id
                    val q = repo.getLocalRandomQuote(excludeId = current)
                        ?: throw IllegalStateException("No other quotes yet")
                    _uiState.value = _uiState.value.copy(quote = q, error = null, isBrowsing = true)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            }
            is HomeEvent.Favorite -> viewModelScope.launch {
                repo.toggleFavorite(event.id)
                _uiState.value = _uiState.value.copy(favoriteTapKey = _uiState.value.favoriteTapKey + 1)
            }
        }
    }
}
