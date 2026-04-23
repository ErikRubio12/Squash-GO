package com.egr.squashgo.core.domain.repository

import com.egr.squashgo.core.model.Player
import com.egr.squashgo.core.model.PlayerCourt
import com.egr.squashgo.core.model.Rating

interface PlayerRepository {
    suspend fun getPlayer(playerId: String): Player
    suspend fun getPlayerWithRating(playerId: String): PlayerWithRating
    suspend fun getPlayersWithRatings(playerIds: List<String>): List<PlayerWithRating>
    suspend fun updatePlayer(player: Player): Player
    suspend fun getPlayerCourts(playerId: String): List<PlayerCourt>
    suspend fun setPlayerCourts(playerId: String, courtIds: List<String>)
}

data class PlayerWithRating(
    val player: Player,
    val rating: Rating,
)
