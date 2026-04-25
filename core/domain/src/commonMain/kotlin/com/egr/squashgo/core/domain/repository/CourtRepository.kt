package com.egr.squashgo.core.domain.repository

import com.egr.squashgo.core.domain.repository.model.CourtWithPlayers
import com.egr.squashgo.core.model.Court

interface CourtRepository {
    suspend fun getCourts(): List<Court>
    suspend fun getCourtById(courtId: String): Court
    suspend fun getCourtWithPlayers(courtId: String): CourtWithPlayers
}
