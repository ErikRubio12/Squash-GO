package com.egr.squashgo.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import com.egr.squashgo.R

internal const val CHANNEL_MATCH_PLAY = "match_play"

object NotificationChannels {

    fun registerAll(context: Context) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val channel = NotificationChannel(
            CHANNEL_MATCH_PLAY,
            context.getString(R.string.notif_channel_match_play_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_match_play_desc)
        }
        manager.createNotificationChannel(channel)
    }
}
