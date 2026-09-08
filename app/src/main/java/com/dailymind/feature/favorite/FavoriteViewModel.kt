package com.dailymind.feature.favorite

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dailymind.core.data.QuoteRepository
import com.dailymind.core.model.Quote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoriteViewModel @Inject constructor(
    private val repo: QuoteRepository
) : ViewModel() {
    val favorites: StateFlow<List<Quote>> = repo.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onEvent(event: FavoriteEvent) {
        when (event) {
            is FavoriteEvent.Unfavorite -> viewModelScope.launch { repo.toggleFavorite(event.id) }
        }
    }
}

sealed interface FavoriteEvent {
    data class Unfavorite(val id: String) : FavoriteEvent
}
