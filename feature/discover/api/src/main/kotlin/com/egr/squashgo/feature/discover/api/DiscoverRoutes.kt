package com.egr.squashgo.feature.discover.api

import kotlinx.serialization.Serializable

@Serializable
object CourtListRoute

@Serializable
data class CourtDetailRoute(val courtId: String)
