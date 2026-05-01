package com.egr.squashgo.data.network.api

import com.egr.squashgo.data.network.dto.ConfirmMatchRequest
import com.egr.squashgo.data.network.dto.ConfirmMatchResponse
import com.egr.squashgo.data.network.dto.DisputeMatchRequest
import com.egr.squashgo.data.network.dto.DisputeMatchResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class EdgeFunctionApi(private val client: HttpClient) {

    suspend fun confirmMatch(matchId: String): ConfirmMatchResponse {
        return client.post("/functions/v1/confirm-match") {
            setBody(ConfirmMatchRequest(matchId))
        }.body()
    }

    suspend fun disputeMatch(matchId: String, reason: String): DisputeMatchResponse {
        return client.post("/functions/v1/dispute-match") {
            setBody(DisputeMatchRequest(matchId, reason))
        }.body()
    }
}
