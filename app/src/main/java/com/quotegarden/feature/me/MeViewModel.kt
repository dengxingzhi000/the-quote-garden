package com.quotegarden.feature.me

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotegarden.core.data.QuoteRepository
import com.quotegarden.core.datastore.ThemeMode
import com.quotegarden.core.datastore.ThemeStore
import com.quotegarden.core.model.Quote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MeViewModel @Inject constructor(
    private val repo: QuoteRepository,
    private val themeStore: ThemeStore
) : ViewModel() {
    val favorites: StateFlow<List<Quote>> = repo.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themeMode: StateFlow<ThemeMode> = themeStore.mode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun onEvent(event: MeEvent) {
        when (event) {
            is MeEvent.Unfavorite -> viewModelScope.launch { repo.toggleFavorite(event.id) }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeStore.setMode(mode) }
    }
}

sealed interface MeEvent {
    data class Unfavorite(val id: String) : MeEvent
}
