package com.egr.squashgo.core.domain.exception

sealed class AuthException(cause: Throwable? = null) : Exception(cause) {
    class InvalidEmail(cause: Throwable? = null) : AuthException(cause)
    class RateLimited(cause: Throwable? = null) : AuthException(cause)
    class Network(cause: Throwable? = null) : AuthException(cause)
    class Server(cause: Throwable? = null) : AuthException(cause)
    class Unknown(cause: Throwable? = null) : AuthException(cause)
}
