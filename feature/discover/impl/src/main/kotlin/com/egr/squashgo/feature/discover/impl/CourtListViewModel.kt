package com.egr.squashgo.feature.discover.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.domain.repository.CourtRepository
import com.egr.squashgo.feature.discover.impl.model.CourtListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "loadCourts failed", e)
                _uiState.value = CourtListUiState.Error
            }
        }
    }

    private companion object {
        const val TAG = "CourtListViewModel"
    }
}

