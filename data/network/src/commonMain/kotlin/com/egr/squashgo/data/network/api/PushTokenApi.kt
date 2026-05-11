package com.egr.squashgo.data.network.api

import com.egr.squashgo.data.network.dto.UpsertPushTokenRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class PushTokenApi(private val client: HttpClient) {

    suspend fun upsert(request: UpsertPushTokenRequest) {
        client.post("/rest/v1/push_tokens") {
            parameter("on_conflict", "token")
            header("Prefer", "resolution=merge-duplicates,return=minimal")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deleteByToken(token: String) {
        client.delete("/rest/v1/push_tokens") {
            parameter("token", "eq.$token")
        }
    }
}
