package com.egr.squashgo.messaging

import android.util.Log
import com.egr.squashgo.core.model.NotificationType
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class SquashGoMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d(TAG, "onNewToken: $token")
        // Re-registration with Supabase happens on next cold start via
        // AppBootstrapViewModel — the new token is what FirebaseMessaging
        // returns from then on. A foreground refresh is a follow-up.
    }

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "onMessageReceived: data=${message.data}")
        val type = NotificationType.fromKey(message.data["type"])
        if (type == null) {
            Log.w(TAG, "unknown notification type: ${message.data["type"]}")
            return
        }
        NotificationRenderer.render(this, type, message.data)
    }

    private companion object {
        const val TAG = "SquashGoFCM"
    }
}
