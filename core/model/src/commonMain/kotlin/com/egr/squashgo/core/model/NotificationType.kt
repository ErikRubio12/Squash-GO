package com.egr.squashgo.core.model

/**
 * Closed enum of push-notification kinds. Mirror of the server-side
 * NotificationType union in `supabase/functions/send-notification/index.ts`.
 * Any new variant must be added on both sides simultaneously.
 */
enum class NotificationType {
    CHALLENGE_CREATED,
    CHALLENGE_ACCEPTED,
    CHALLENGE_DECLINED,
    CHALLENGE_CANCELLED,
    MATCH_RESULT_SUBMITTED,
    MATCH_CONFIRMED,
    MATCH_DISPUTED,
    MATCH_AUTO_CONFIRMED;

    companion object {
        fun fromKey(key: String?): NotificationType? =
            entries.firstOrNull { it.name == key }
    }
}
