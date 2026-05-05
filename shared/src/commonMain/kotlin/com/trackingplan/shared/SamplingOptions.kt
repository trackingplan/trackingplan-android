// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Additional options for sampling configuration.
 *
 * This class is used both by TrackingplanIngestConfig (when downloaded from server)
 * and TrackingplanSession (persisted with session for lifecycle independence).
 *
 * @property useAdaptiveSampling Enable adaptive sampling based on patterns
 * @property adaptiveSamplingPatterns Adaptive sampling rules as raw JsonElement values. Each
 * element is interpreted lazily by AdaptiveSamplingPatternParser; unrecognised shapes are
 * skipped at evaluation time, so the wire format can evolve without crashing older SDKs.
 */
@Serializable
data class SamplingOptions(
    val useAdaptiveSampling: Boolean = false,
    val adaptiveSamplingPatterns: List<JsonElement> = emptyList()
) {
    companion object {
        /** Empty/default sentinel with adaptive sampling disabled. */
        val EMPTY = SamplingOptions()
    }
}
