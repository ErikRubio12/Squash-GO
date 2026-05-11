package com.egr.squashgo.data

import com.egr.squashgo.core.designsystem.sync.RemoteThemeSource
import com.egr.squashgo.core.designsystem.tokens.BrandPalette
import com.egr.squashgo.core.designsystem.tokens.SamplePalettes
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-process simulation of a remote palette source.
 *
 * Cycles through [SamplePalettes.all] on each fetch and pauses for [LATENCY_MS] to mimic
 * network latency. Useful for demoing the async/loading-state path of the resolver chain
 * without a live backend.
 *
 * Replace this binding in `DesignSystemModule` with a real `SupabaseThemeSource` to ship
 * server-driven theming. The contract surface stays identical, so nothing else in the
 * codebase changes.
 */
@Singleton
class SimulatedRemoteThemeSource @Inject constructor() : RemoteThemeSource {

    private val rotation: List<BrandPalette> = SamplePalettes.all
    private val cursor = AtomicInteger(0)

    override suspend fun fetchActivePalette(): BrandPalette {
        delay(LATENCY_MS)
        val next = cursor.getAndIncrement()
        return rotation[next % rotation.size]
    }

    private companion object {
        const val LATENCY_MS = 1_500L
    }
}
