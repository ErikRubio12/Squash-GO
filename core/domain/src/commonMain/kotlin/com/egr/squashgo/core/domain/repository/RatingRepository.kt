package com.egr.squashgo.core.domain.repository

import com.egr.squashgo.core.model.Rating
import com.egr.squashgo.core.model.RatingHistory

interface RatingRepository {
    suspend fun getRating(playerId: String): Rating
    suspend fun getRatingHistory(playerId: String): List<RatingHistory>
}
