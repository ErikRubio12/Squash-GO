package com.egr.squashgo.data.network.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object SupabaseApi {

    fun createClient(
        supabaseUrl: String,
        supabaseAnonKey: String,
        tokenProvider: () -> String? = { null },
    ): HttpClient {
        return HttpClient {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                })
            }
            install(Logging) {
                level = LogLevel.BODY
            }
            defaultRequest {
                url(supabaseUrl)
                contentType(ContentType.Application.Json)
                header("apikey", supabaseAnonKey)
                val token = tokenProvider()
                if (token != null) {
                    header("Authorization", "Bearer $token")
                }
            }
        }
    }
}
