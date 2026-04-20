package com.egr.squashgo.data.network.api

import com.egr.squashgo.data.network.dto.CreateMatchRequest
import com.egr.squashgo.data.network.dto.MatchDto
import com.egr.squashgo.data.network.dto.RatingHistoryDto
import com.egr.squashgo.data.network.dto.SubmitResultRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class MatchApi(private val client: HttpClient) {

    suspend fun createMatch(request: CreateMatchRequest): MatchDto {
        val matches: List<MatchDto> = client.post("/rest/v1/matches") {
            header("Prefer", "return=representation")
            setBody(request)
        }.body()
        return matches.first()
    }

    suspend fun getMatch(matchId: String): MatchDto {
        val matches: List<MatchDto> = client.get("/rest/v1/matches") {
            parameter("select", "*")
            parameter("id", "eq.$matchId")
        }.body()
        return matches.first()
    }

    suspend fun getMatchesForPlayer(playerId: String): List<MatchDto> {
        return client.get("/rest/v1/matches") {
            parameter("select", "*")
            parameter("or", "(player_a_id.eq.$playerId,player_b_id.eq.$playerId)")
            parameter("order", "created_at.desc")
        }.body()
    }

    suspend fun submitResult(matchId: String, request: SubmitResultRequest): MatchDto {
        val matches: List<MatchDto> = client.patch("/rest/v1/matches") {
            parameter("id", "eq.$matchId")
            header("Prefer", "return=representation")
            setBody(request)
        }.body()
        return matches.first()
    }

    suspend fun getRatingHistory(playerId: String): List<RatingHistoryDto> {
        return client.get("/rest/v1/rating_history") {
            parameter("select", "*")
            parameter("player_id", "eq.$playerId")
            parameter("order", "created_at.desc")
        }.body()
    }
}
