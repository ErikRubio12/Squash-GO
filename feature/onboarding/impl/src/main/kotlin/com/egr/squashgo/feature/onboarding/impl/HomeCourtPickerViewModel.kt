package com.egr.squashgo.feature.onboarding.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.core.domain.repository.CourtRepository
import com.egr.squashgo.core.domain.repository.PlayerRepository
import com.egr.squashgo.core.model.Court
import com.egr.squashgo.feature.onboarding.impl.model.HomeCourtPickerError
import com.egr.squashgo.feature.onboarding.impl.model.HomeCourtPickerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeCourtPickerViewModel @Inject constructor(
    private val courtRepository: CourtRepository,
    private val playerRepository: PlayerRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeCourtPickerUiState>(HomeCourtPickerUiState.Loading)
    val uiState: StateFlow<HomeCourtPickerUiState> = _uiState

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private var allCourts: List<Court> = emptyList()

    init {
        load()
    }

    fun onQueryChange(value: String) {
        _query.value = value
        refreshFiltered()
    }

    fun toggleCourt(courtId: String) {
        val current = _uiState.value as? HomeCourtPickerUiState.Ready ?: return
        val selected = current.selectedCourtIds.toMutableList()
        if (courtId in selected) {
            selected.remove(courtId)
        } else if (selected.size < MAX_HOME_COURTS) {
            selected.add(courtId)
        } else {
            return
        }
        val newPrimary = when {
            selected.isEmpty() -> null
            current.primaryCourtId !in selected -> selected.first()
            else -> current.primaryCourtId
        }
        _uiState.update {
            current.copy(
                selectedCourtIds = selected,
                primaryCourtId = newPrimary,
                validationError = null,
            )
        }
    }

    fun setPrimary(courtId: String) {
        val current = _uiState.value as? HomeCourtPickerUiState.Ready ?: return
        if (courtId !in current.selectedCourtIds) return
        _uiState.update { current.copy(primaryCourtId = courtId) }
    }

    fun retryLoad() {
        load()
    }

    fun save() {
        val current = _uiState.value as? HomeCourtPickerUiState.Ready ?: return
        val userId = sessionManager.currentUserId
        if (userId.isNullOrBlank()) {
            _uiState.value = HomeCourtPickerUiState.Error(HomeCourtPickerError.NotAuthenticated)
            return
        }
        if (current.selectedCourtIds.isEmpty()) {
            _uiState.update { current.copy(validationError = HomeCourtPickerError.MinNotMet) }
            return
        }
        val primary = current.primaryCourtId ?: current.selectedCourtIds.first()
        // Backend impl marks the first courtId as primary. Order the list accordingly.
        val ordered = buildList {
            add(primary)
            addAll(current.selectedCourtIds.filterNot { it == primary })
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true, validationError = null)
            try {
                playerRepository.setPlayerCourts(userId, ordered)
                sessionManager.setOnboarded()
                _uiState.value = HomeCourtPickerUiState.Saved
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "setPlayerCourts failed", e)
                _uiState.value = current.copy(
                    isSaving = false,
                    validationError = HomeCourtPickerError.Network,
                )
            }
        }
    }

    private fun load() {
        val userId = sessionManager.currentUserId
        if (userId.isNullOrBlank()) {
            _uiState.value = HomeCourtPickerUiState.Error(HomeCourtPickerError.NotAuthenticated)
            return
        }
        viewModelScope.launch {
            _uiState.value = HomeCourtPickerUiState.Loading
            try {
                val existing = playerRepository.getPlayerCourts(userId)
                if (existing.isNotEmpty()) {
                    sessionManager.setOnboarded()
                    _uiState.value = HomeCourtPickerUiState.AlreadyComplete
                    return@launch
                }
                allCourts = courtRepository.getCourts()
                    .filter { it.isVerified }
                    .sortedBy { it.name.lowercase() }
                _uiState.value = HomeCourtPickerUiState.Ready(
                    filteredCourts = allCourts,
                    selectedCourtIds = emptyList(),
                    primaryCourtId = null,
                    isSaving = false,
                    validationError = null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "load courts failed", e)
                _uiState.value = HomeCourtPickerUiState.Error(HomeCourtPickerError.Network)
            }
        }
    }

    private fun refreshFiltered() {
        val current = _uiState.value as? HomeCourtPickerUiState.Ready ?: return
        val q = _query.value.trim()
        val filtered = if (q.isEmpty()) {
            allCourts
        } else {
            allCourts.filter { it.name.contains(q, ignoreCase = true) }
        }
        _uiState.update { current.copy(filteredCourts = filtered) }
    }

    companion object {
        const val MAX_HOME_COURTS = 3
        private const val TAG = "HomeCourtPickerVM"
    }
}

