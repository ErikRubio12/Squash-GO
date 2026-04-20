package com.egr.squashgo.data.network.api

import com.egr.squashgo.data.network.dto.PlayerDto
import com.egr.squashgo.data.network.dto.RatingDto
import com.egr.squashgo.data.network.dto.UpdatePlayerRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody

class PlayerApi(private val client: HttpClient) {

    suspend fun getPlayer(playerId: String): PlayerDto {
        val players: List<PlayerDto> = client.get("/rest/v1/players") {
            parameter("select", "*")
            parameter("id", "eq.$playerId")
        }.body()
        return players.first()
    }

    suspend fun updatePlayer(playerId: String, request: UpdatePlayerRequest): PlayerDto {
        val players: List<PlayerDto> = client.patch("/rest/v1/players") {
            parameter("id", "eq.$playerId")
            header("Prefer", "return=representation")
            setBody(request)
        }.body()
        return players.first()
    }

    suspend fun getRating(playerId: String): RatingDto {
        val ratings: List<RatingDto> = client.get("/rest/v1/ratings") {
            parameter("select", "*")
            parameter("player_id", "eq.$playerId")
        }.body()
        return ratings.first()
    }

    suspend fun getPlayersAtCourt(courtId: String): List<PlayerDto> {
        return client.get("/rest/v1/players") {
            parameter("select", "*,player_courts!inner(court_id)")
            parameter("player_courts.court_id", "eq.$courtId")
        }.body()
    }
}
