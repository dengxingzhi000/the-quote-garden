package com.quotegarden.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotegarden.core.datastore.OnboardingStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val store: OnboardingStore,
) : ViewModel() {
    val seen: StateFlow<Boolean> = store.seen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun markSeen() {
        viewModelScope.launch { store.setSeen() }
    }
}
