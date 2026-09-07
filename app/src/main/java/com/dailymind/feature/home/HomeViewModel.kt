package com.dailymind.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailymind.core.data.QuoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: QuoteRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState

    init { onEvent(HomeEvent.Load) }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.Load -> viewModelScope.launch {
                _uiState.value = _uiState.value.copy(isLoading = true)
                try {
                    val q = repo.getDailyQuote() ?: repo.getRandomQuote()
                    _uiState.value = HomeUiState(quote = q, isLoading = false)
                } catch (e: Exception) {
                    _uiState.value = HomeUiState(error = e.message, isLoading = false)
                }
            }
            is HomeEvent.NextRandom -> viewModelScope.launch {
                try {
                    val q = repo.getRandomQuote()
                    _uiState.value = _uiState.value.copy(quote = q, error = null, isBrowsing = true)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(error = e.message)
                }
            }
            is HomeEvent.Favorite -> viewModelScope.launch { repo.toggleFavorite(event.id) }
        }
    }
}
