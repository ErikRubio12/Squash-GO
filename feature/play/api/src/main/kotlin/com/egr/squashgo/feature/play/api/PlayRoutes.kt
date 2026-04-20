package com.egr.squashgo.feature.play.api

import kotlinx.serialization.Serializable

@Serializable
data class ChallengeCreateRoute(val challengedId: String, val courtId: String? = null)

@Serializable
object ChallengesRoute

@Serializable
data class MatchResultRoute(val matchId: String)

@Serializable
data class MatchConfirmRoute(val matchId: String)
