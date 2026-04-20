package com.egr.squashgo.feature.discover.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.domain.repository.CourtRepository
import com.egr.squashgo.core.model.Court
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CourtListViewModel @Inject constructor(
    private val courtRepository: CourtRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CourtListUiState>(CourtListUiState.Loading)
    val uiState: StateFlow<CourtListUiState> = _uiState

    init {
        loadCourts()
    }

    fun loadCourts() {
        viewModelScope.launch {
            _uiState.value = CourtListUiState.Loading
            try {
                val courts = courtRepository.getCourts()
                _uiState.value = if (courts.isEmpty()) {
                    CourtListUiState.Empty
                } else {
                    CourtListUiState.Success(courts)
                }
            } catch (e: Exception) {
                _uiState.value = CourtListUiState.Error(e.message ?: "Failed to load courts")
            }
        }
    }
}

sealed interface CourtListUiState {
    data object Loading : CourtListUiState
    data object Empty : CourtListUiState
    data class Success(val courts: List<Court>) : CourtListUiState
    data class Error(val message: String) : CourtListUiState
}
