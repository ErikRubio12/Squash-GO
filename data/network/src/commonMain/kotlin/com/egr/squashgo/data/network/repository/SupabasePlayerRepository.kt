package com.egr.squashgo.data.network.repository

import com.egr.squashgo.core.domain.repository.PlayerRepository
import com.egr.squashgo.core.model.Player
import com.egr.squashgo.core.model.PlayerCourt
import com.egr.squashgo.data.network.api.PlayerApi
import com.egr.squashgo.data.network.dto.UpdatePlayerRequest
import com.egr.squashgo.data.network.mapper.PlayerMapper
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class SupabasePlayerRepository(
    private val playerApi: PlayerApi,
    private val client: HttpClient,
) : PlayerRepository {

    override suspend fun getPlayer(playerId: String): Player {
        return PlayerMapper.toDomain(playerApi.getPlayer(playerId))
    }

    override suspend fun updatePlayer(player: Player): Player {
        val request = UpdatePlayerRequest(
            displayName = player.displayName,
            availabilityNote = player.availabilityNote,
        )
        return PlayerMapper.toDomain(playerApi.updatePlayer(player.id, request))
    }

    override suspend fun getPlayerCourts(playerId: String): List<PlayerCourt> {
        val dtos: List<PlayerCourtDto> = client.get("/rest/v1/player_courts") {
            parameter("select", "*")
            parameter("player_id", "eq.$playerId")
        }.body()
        return dtos.map { PlayerCourt(it.playerId, it.courtId, it.isPrimary) }
    }

    override suspend fun setPlayerCourts(playerId: String, courtIds: List<String>) {
        // Delete existing associations
        client.delete("/rest/v1/player_courts") {
            parameter("player_id", "eq.$playerId")
        }
        // Insert new associations
        if (courtIds.isNotEmpty()) {
            val requests = courtIds.mapIndexed { index, courtId ->
                PlayerCourtDto(playerId, courtId, isPrimary = index == 0)
            }
            client.post("/rest/v1/player_courts") {
                header("Prefer", "return=minimal")
                setBody(requests)
            }
        }
    }
}

@Serializable
private data class PlayerCourtDto(
    @SerialName("player_id") val playerId: String,
    @SerialName("court_id") val courtId: String,
    @SerialName("is_primary") val isPrimary: Boolean = false,
)
