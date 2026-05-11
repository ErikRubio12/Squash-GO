package com.egr.squashgo.messaging

import android.util.Log
import com.egr.squashgo.core.auth.FcmTokenProvider
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class FirebaseFcmTokenProvider @Inject constructor() : FcmTokenProvider {

    override suspend fun currentToken(): String? = suspendCancellableCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> cont.resume(token) }
            .addOnFailureListener { error ->
                Log.w(TAG, "fcm token fetch failed", error)
                cont.resume(null)
            }
    }

    private companion object {
        const val TAG = "FcmTokenProvider"
    }
}
