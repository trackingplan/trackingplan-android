// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Configuration downloaded from the Trackingplan config endpoint.
 * This class encapsulates all configuration options for the ingest pipeline,
 * including sampling rates and adaptive sampling options.
 *
 * @property sampleRate The default sampling rate (1 = 100%, 2 = 50%, etc.)
 * @property environmentRates Environment-specific sampling rates that override the default
 * @property options Additional sampling configuration options
 */
@Serializable
data class TrackingplanIngestConfig(
    @SerialName("sample_rate")
    val sampleRate: Int = 1,
    @SerialName("environment_rates")
    val environmentRates: Map<String, Int> = emptyMap(),
    val options: SamplingOptions = SamplingOptions(),
    @kotlinx.serialization.Transient
    private val random: Random = Random.Default
) {

    /**
     * Gets the sampling rate for a specific environment.
     * First checks environment-specific rates, then falls back to the default sample rate.
     *
     * @param environment The environment name (e.g., "PRODUCTION", "STAGING")
     * @return The sampling rate for the environment
     */
    fun getSamplingRate(environment: String): Int {
        return environmentRates[environment] ?: sampleRate
    }

    /**
     * Checks if adaptive sampling is enabled.
     */
    fun isAdaptiveSamplingEnabled(): Boolean {
        return options.useAdaptiveSampling
    }

    /**
     * Determines if tracking should be enabled for a session based on the sampling rate.
     * Uses random sampling: generates a random number and compares to 1/samplingRate.
     *
     * @param environment The environment name to get the appropriate sampling rate
     * @return true if tracking should be enabled, false otherwise
     */
    fun shouldEnableTracking(environment: String): Boolean {
        val rate = getSamplingRate(environment)
        return shouldEnableTracking(rate)
    }

    /**
     * Determines if tracking should be enabled based on a specific sampling rate value.
     *
     * @param samplingRate The sampling rate (1 = 100%, 2 = 50%, etc.)
     * @return true if tracking should be enabled, false otherwise
     */
    fun shouldEnableTracking(samplingRate: Int): Boolean {
        if (samplingRate <= 0) return false
        if (samplingRate == 1) return true

        val randomValue = random.nextFloat()
        val threshold = 1.0f / samplingRate.toFloat()
        return randomValue <= threshold
    }
}
