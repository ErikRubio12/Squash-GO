package com.egr.squashgo.core.domain.repository.model

import com.egr.squashgo.core.model.Court

data class CourtWithPlayers(
    val court: Court,
    val players: List<PlayerWithRating>,
)
