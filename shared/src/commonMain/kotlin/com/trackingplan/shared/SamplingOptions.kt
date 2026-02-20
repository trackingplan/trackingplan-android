// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import kotlinx.serialization.Serializable

/**
 * Additional options for sampling configuration.
 *
 * This class is used both by TrackingplanIngestConfig (when downloaded from server)
 * and TrackingplanSession (persisted with session for lifecycle independence).
 *
 * @property useAdaptiveSampling Enable adaptive sampling based on patterns
 * @property adaptiveSamplingPatterns Patterns for adaptive sampling rules (raw JSON strings)
 */
@Serializable
data class SamplingOptions(
    val useAdaptiveSampling: Boolean = false,
    val adaptiveSamplingPatterns: List<String> = emptyList()
) {
    companion object {
        /** Empty/default sentinel with adaptive sampling disabled. */
        val EMPTY = SamplingOptions()
    }
}
