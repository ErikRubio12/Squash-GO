package com.egr.squashgo.data.network.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
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
        loadSessionTokens: () -> Pair<String, String>? = { null },
        refreshSession: suspend (oldRefreshToken: String) -> Pair<String, String>? = { null },
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
            install(Auth) {
                bearer {
                    loadTokens {
                        loadSessionTokens()?.let { (access, refresh) ->
                            BearerTokens(access, refresh)
                        }
                    }
                    refreshTokens {
                        val oldRefresh = oldTokens?.refreshToken ?: return@refreshTokens null
                        refreshSession(oldRefresh)?.let { (access, refresh) ->
                            BearerTokens(access, refresh)
                        }
                    }
                    sendWithoutRequest { request ->
                        "auth" !in request.url.pathSegments
                    }
                }
            }
            defaultRequest {
                url(supabaseUrl)
                contentType(ContentType.Application.Json)
                header("apikey", supabaseAnonKey)
            }
        }
    }
}
