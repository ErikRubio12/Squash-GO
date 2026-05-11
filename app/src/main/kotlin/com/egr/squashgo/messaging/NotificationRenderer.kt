package com.egr.squashgo.messaging

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.egr.squashgo.MainActivity
import com.egr.squashgo.R
import com.egr.squashgo.core.model.NotificationType

object NotificationRenderer {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun render(context: Context, type: NotificationType, data: Map<String, String>) {
        val copy = resolveCopy(context, type, data)
        val notification = NotificationCompat.Builder(context, CHANNEL_MATCH_PLAY)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(copy.title)
            .setContentText(copy.body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent(context))
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(data), notification)
    }

    private fun resolveCopy(
        context: Context,
        type: NotificationType,
        data: Map<String, String>,
    ): NotificationCopy {
        val opponentName = data["challenger_name"]
            ?: data["submitter_name"]
            ?: data["opponent_name"]
            ?: context.getString(R.string.notif_fallback_opponent)
        return when (type) {
            NotificationType.CHALLENGE_CREATED -> NotificationCopy(
                title = context.getString(R.string.notif_challenge_created_title),
                body = context.getString(R.string.notif_challenge_created_body, opponentName),
            )
            NotificationType.CHALLENGE_ACCEPTED -> NotificationCopy(
                title = context.getString(R.string.notif_challenge_accepted_title),
                body = context.getString(R.string.notif_challenge_accepted_body, opponentName),
            )
            NotificationType.CHALLENGE_DECLINED -> NotificationCopy(
                title = context.getString(R.string.notif_challenge_declined_title),
                body = context.getString(R.string.notif_challenge_declined_body, opponentName),
            )
            NotificationType.CHALLENGE_CANCELLED -> NotificationCopy(
                title = context.getString(R.string.notif_challenge_cancelled_title),
                body = context.getString(R.string.notif_challenge_cancelled_body, opponentName),
            )
            NotificationType.MATCH_RESULT_SUBMITTED -> NotificationCopy(
                title = context.getString(R.string.notif_match_result_submitted_title),
                body = context.getString(R.string.notif_match_result_submitted_body, opponentName),
            )
            NotificationType.MATCH_CONFIRMED -> {
                val newElo = data["new_elo"]?.toIntOrNull() ?: 0
                NotificationCopy(
                    title = context.getString(R.string.notif_match_confirmed_title),
                    body = context.getString(R.string.notif_match_confirmed_body, newElo),
                )
            }
            NotificationType.MATCH_DISPUTED -> NotificationCopy(
                title = context.getString(R.string.notif_match_disputed_title),
                body = context.getString(R.string.notif_match_disputed_body, opponentName),
            )
            NotificationType.MATCH_AUTO_CONFIRMED -> NotificationCopy(
                title = context.getString(R.string.notif_match_auto_confirmed_title),
                body = context.getString(R.string.notif_match_auto_confirmed_body),
            )
        }
    }

    private fun contentIntent(context: Context): PendingIntent {
        // For now, tap → cold-start MainActivity. Deep-link routing per
        // notification type will be added in a later step.
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    // Unique-ish per match/challenge so concurrent events don't collapse into one.
    private fun notificationId(data: Map<String, String>): Int {
        val key = data["match_id"] ?: data["challenge_id"] ?: ""
        return key.hashCode().takeIf { it != 0 } ?: System.currentTimeMillis().toInt()
    }

    private data class NotificationCopy(val title: String, val body: String)
}
