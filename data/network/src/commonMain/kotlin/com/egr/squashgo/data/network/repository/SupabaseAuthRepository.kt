package com.egr.squashgo.data.network.repository

import com.egr.squashgo.core.domain.exception.AuthException
import com.egr.squashgo.core.domain.repository.AuthRepository
import com.egr.squashgo.core.model.Session
import com.egr.squashgo.data.network.api.AuthApi
import com.egr.squashgo.data.network.mapper.AuthMapper
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException

class SupabaseAuthRepository(
    private val authApi: AuthApi,
) : AuthRepository {

    override suspend fun sendMagicLink(email: String, redirectUrl: String) {
        try {
            authApi.sendMagicLink(email, redirectUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ClientRequestException) {
            throw when (e.response.status) {
                HttpStatusCode.TooManyRequests -> AuthException.RateLimited(e)
                HttpStatusCode.UnprocessableEntity,
                HttpStatusCode.BadRequest -> AuthException.InvalidEmail(e)
                else -> AuthException.Unknown(e)
            }
        } catch (e: ServerResponseException) {
            throw AuthException.Server(e)
        } catch (e: Throwable) {
            throw AuthException.Network(e)
        }
    }

    override suspend fun verifyOtp(email: String, token: String): Session {
        return AuthMapper.toDomain(authApi.verifyOtp(email, token))
    }

    override suspend fun refreshToken(refreshToken: String): Session {
        return AuthMapper.toDomain(authApi.refreshToken(refreshToken))
    }
}
