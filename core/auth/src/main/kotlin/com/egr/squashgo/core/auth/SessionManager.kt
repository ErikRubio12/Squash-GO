package com.egr.squashgo.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val prefs: SharedPreferences = openPrefs(context)

    @Volatile private var _accessToken: String? = prefs.getString(KEY_ACCESS_TOKEN, null)
    @Volatile private var _refreshToken: String? = prefs.getString(KEY_REFRESH_TOKEN, null)
    @Volatile private var _currentUserId: String? = prefs.getString(KEY_USER_ID, null)
    @Volatile private var _expiresAtEpochSeconds: Long = prefs.getLong(KEY_EXPIRES_AT, 0L)
    @Volatile private var _isOnboarded: Boolean = prefs.getBoolean(KEY_IS_ONBOARDED, false)

    val accessToken: String? get() = _accessToken
    val refreshToken: String? get() = _refreshToken
    val currentUserId: String? get() = _currentUserId
    val isLoggedIn: Boolean get() = _accessToken != null
    val isOnboarded: Boolean get() = _isOnboarded

    fun hasRefreshToken(): Boolean = !_refreshToken.isNullOrBlank()

    fun setOnboarded() {
        if (_isOnboarded) return
        _isOnboarded = true
        prefs.edit().putBoolean(KEY_IS_ONBOARDED, true).apply()
    }

    fun isAccessTokenExpired(nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        val expiresAt = _expiresAtEpochSeconds
        if (expiresAt == 0L) return true
        return nowEpochSeconds >= expiresAt - EXPIRATION_SKEW_SECONDS
    }

    fun updateSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        expiresInSeconds: Long,
    ) {
        val expiresAt = System.currentTimeMillis() / 1000 + expiresInSeconds
        _accessToken = accessToken
        _refreshToken = refreshToken
        _currentUserId = userId
        _expiresAtEpochSeconds = expiresAt
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    fun clearSession() {
        _accessToken = null
        _refreshToken = null
        _currentUserId = null
        _expiresAtEpochSeconds = 0L
        _isOnboarded = false
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_FILE = "squashgo_secure_session"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_EXPIRES_AT = "expires_at"
        const val KEY_IS_ONBOARDED = "is_onboarded"
        const val EXPIRATION_SKEW_SECONDS = 60L
        const val TAG = "SessionManager"

        fun openPrefs(context: Context): SharedPreferences = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences init failed; resetting keystore-backed store", e)
            context.deleteSharedPreferences(PREFS_FILE)
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }
    }
}
