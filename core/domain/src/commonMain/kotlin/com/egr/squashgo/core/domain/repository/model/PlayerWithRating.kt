package com.egr.squashgo.core.domain.repository.model

import com.egr.squashgo.core.model.Player
import com.egr.squashgo.core.model.Rating

data class PlayerWithRating(
    val player: Player,
    val rating: Rating,
)
