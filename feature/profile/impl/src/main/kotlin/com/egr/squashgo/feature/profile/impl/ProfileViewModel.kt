package com.egr.squashgo.feature.profile.impl

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.egr.squashgo.core.auth.SessionManager
import com.egr.squashgo.core.domain.rating.TierMapper
import com.egr.squashgo.core.domain.repository.CourtRepository
import com.egr.squashgo.core.domain.repository.PlayerRepository
import com.egr.squashgo.feature.profile.impl.model.CourtRow
import com.egr.squashgo.feature.profile.impl.model.ProfileData
import com.egr.squashgo.feature.profile.impl.model.ProfileError
import com.egr.squashgo.feature.profile.impl.model.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val playerRepository: PlayerRepository,
    private val courtRepository: CourtRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun refresh() {
        load()
    }

    private fun load() {
        val userId = sessionManager.currentUserId
        if (userId.isNullOrBlank()) {
            _uiState.value = ProfileUiState.Error(ProfileError.NotAuthenticated)
            return
        }

        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val (playerWithRating, playerCourts) = coroutineScope {
                    val playerDeferred = async { playerRepository.getPlayerWithRating(userId) }
                    val courtsDeferred = async { playerRepository.getPlayerCourts(userId) }
                    playerDeferred.await() to courtsDeferred.await()
                }

                val courtRows = if (playerCourts.isEmpty()) {
                    emptyList()
                } else {
                    val courtIds = playerCourts.map { it.courtId }.toSet()
                    val courtsById = courtRepository.getCourts()
                        .filter { it.id in courtIds }
                        .associateBy { it.id }
                    playerCourts
                        .mapNotNull { pc ->
                            courtsById[pc.courtId]?.let { court ->
                                CourtRow(
                                    courtId = court.id,
                                    name = court.name,
                                    isPrimary = pc.isPrimary,
                                )
                            }
                        }
                        .sortedByDescending { it.isPrimary }
                }

                val rating = playerWithRating.rating
                _uiState.value = ProfileUiState.Ready(
                    data = ProfileData(
                        displayName = playerWithRating.player.displayName,
                        email = playerWithRating.player.email,
                        tierLabel = TierMapper.displayStringForRating(
                            elo = rating.eloScore,
                            isProvisional = rating.isProvisional,
                        ),
                        eloScore = rating.eloScore,
                        rankedMatchesPlayed = rating.rankedMatchesPlayed,
                        isProvisional = rating.isProvisional,
                        courts = courtRows,
                    ),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "load profile failed", e)
                _uiState.value = ProfileUiState.Error(ProfileError.Network)
            }
        }
    }

    private companion object {
        const val TAG = "ProfileVM"
    }
}