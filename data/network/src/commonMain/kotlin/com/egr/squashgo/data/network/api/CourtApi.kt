package com.egr.squashgo.data.network.api

import com.egr.squashgo.data.network.dto.CourtDto
import com.egr.squashgo.data.network.dto.CourtWithPlayersDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class CourtApi(private val client: HttpClient) {

    suspend fun getCourts(): List<CourtDto> {
        return client.get("/rest/v1/courts") {
            parameter("select", "*")
            parameter("is_verified", "eq.true")
            parameter("order", "name.asc")
        }.body()
    }

    suspend fun getCourtById(courtId: String): CourtDto {
        val courts: List<CourtDto> = client.get("/rest/v1/courts") {
            parameter("select", "*")
            parameter("id", "eq.$courtId")
        }.body()
        return courts.first()
    }

    suspend fun getCourtWithPlayers(courtId: String): CourtWithPlayersDto {
        val courts: List<CourtWithPlayersDto> = client.get("/rest/v1/courts") {
            parameter("select", "*,player_courts(player_id,players(*))")
            parameter("id", "eq.$courtId")
        }.body()
        return courts.first()
    }
}
