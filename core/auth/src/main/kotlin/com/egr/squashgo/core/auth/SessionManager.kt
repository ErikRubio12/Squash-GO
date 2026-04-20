package com.egr.squashgo.core.auth

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {

    private var _accessToken: String? = null

    private var _currentUserId: String? = null

    val accessToken: String? get() = _accessToken
    val currentUserId: String? get() = _currentUserId
    val isLoggedIn: Boolean get() = _accessToken != null

    fun updateSession(accessToken: String, userId: String) {
        _accessToken = accessToken
        _currentUserId = userId
    }

    fun clearSession() {
        _accessToken = null
        _currentUserId = null
    }
}
