package com.egr.squashgo.data.network.api

import com.egr.squashgo.data.network.dto.MagicLinkRequest
import com.egr.squashgo.data.network.dto.RefreshTokenRequest
import com.egr.squashgo.data.network.dto.SessionDto
import com.egr.squashgo.data.network.dto.VerifyOtpRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody

class AuthApi(private val client: HttpClient) {

    suspend fun sendMagicLink(email: String, redirectUrl: String) {
        client.post("/auth/v1/magiclink") {
            setBody(MagicLinkRequest(email))
            url {
                parameters.append("redirect_to", redirectUrl)
            }
        }
    }

    suspend fun verifyOtp(email: String, token: String): SessionDto {
        return client.post("/auth/v1/verify") {
            setBody(VerifyOtpRequest(email, token))
        }.body()
    }

    suspend fun refreshToken(refreshToken: String): SessionDto {
        return client.post("/auth/v1/token?grant_type=refresh_token") {
            setBody(RefreshTokenRequest(refreshToken))
        }.body()
    }
}
