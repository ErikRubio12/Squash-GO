package com.egr.squashgo.feature.discover.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.core.domain.repository.CourtRepository
import com.egr.squashgo.feature.discover.impl.model.CourtDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CourtDetailViewModel @Inject constructor(
    private val courtRepository: CourtRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CourtDetailUiState>(CourtDetailUiState.Loading)
    val uiState: StateFlow<CourtDetailUiState> = _uiState

    private var loadedCourtId: String? = null

    fun load(courtId: String) {
        if (loadedCourtId == courtId && _uiState.value is CourtDetailUiState.Ready) return
        loadedCourtId = courtId
        viewModelScope.launch {
            _uiState.value = CourtDetailUiState.Loading
            try {
                val result = courtRepository.getCourtWithPlayers(courtId)
                val currentUserId = sessionManager.currentUserId
                val others = result.players
                    .filter { it.player.id != currentUserId && !it.player.isInactive }
                    .sortedByDescending { it.rating.eloScore }
                _uiState.value = CourtDetailUiState.Ready(
                    court = result.court,
                    players = others,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "getCourtWithPlayers failed", e)
                _uiState.value = CourtDetailUiState.Error
            }
        }
    }

    fun retry() {
        loadedCourtId?.let {
            loadedCourtId = null
            load(it)
        }
    }

    private companion object {
        const val TAG = "CourtDetailVM"
    }
}

