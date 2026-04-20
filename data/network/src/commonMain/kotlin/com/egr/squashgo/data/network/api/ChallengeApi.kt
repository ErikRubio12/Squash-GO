package com.egr.squashgo.data.network.api

import com.egr.squashgo.data.network.dto.ChallengeDto
import com.egr.squashgo.data.network.dto.CreateChallengeRequest
import com.egr.squashgo.data.network.dto.UpdateChallengeRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class ChallengeApi(private val client: HttpClient) {

    suspend fun createChallenge(request: CreateChallengeRequest): ChallengeDto {
        val challenges: List<ChallengeDto> = client.post("/rest/v1/challenges") {
            header("Prefer", "return=representation")
            setBody(request)
        }.body()
        return challenges.first()
    }

    suspend fun getIncomingChallenges(playerId: String): List<ChallengeDto> {
        return client.get("/rest/v1/challenges") {
            parameter("select", "*")
            parameter("challenged_id", "eq.$playerId")
            parameter("status", "eq.pending")
            parameter("order", "created_at.desc")
        }.body()
    }

    suspend fun getOutgoingChallenges(playerId: String): List<ChallengeDto> {
        return client.get("/rest/v1/challenges") {
            parameter("select", "*")
            parameter("challenger_id", "eq.$playerId")
            parameter("status", "in.(pending,accepted)")
            parameter("order", "created_at.desc")
        }.body()
    }

    suspend fun updateChallengeStatus(challengeId: String, status: String): ChallengeDto {
        val challenges: List<ChallengeDto> = client.patch("/rest/v1/challenges") {
            parameter("id", "eq.$challengeId")
            header("Prefer", "return=representation")
            setBody(UpdateChallengeRequest(status))
        }.body()
        return challenges.first()
    }
}
