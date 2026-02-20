// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import com.trackingplan.shared.adaptive.AdaptiveSamplingEvaluator
import com.trackingplan.shared.adaptive.AdaptiveSamplingPattern
import com.trackingplan.shared.adaptive.AdaptiveSamplingPatternParser
import com.trackingplan.shared.adaptive.Request
import com.trackingplan.shared.adaptive.SamplingResult
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents a Trackingplan session with sampling rate, tracking state, and sampling options.
 * Sessions expire after MAX_IDLE_DURATION of inactivity.
 *
 * The session embeds [SamplingOptions] directly to decouple session and config lifecycles.
 * This allows the session to be self-contained for all sampling decisions, even when
 * the ingest config is not available (e.g., after app restart with expired config cache).
 */
class TrackingplanSession private constructor(
    val sessionId: String,
    val samplingRate: Int,
    val trackingEnabled: Boolean,
    val createdAt: Long,
    var lastActivityTime: Long,
    val isNew: Boolean,
    val samplingOptions: SamplingOptions
) {

    private val timeProvider: TimeProvider
        get() = ServiceLocator.getTimeProvider()

    /**
     * Checks if the session has expired due to inactivity.
     * EMPTY sessions are always considered expired.
     */
    fun hasExpired(): Boolean {
        if (this == EMPTY) return true
        return getIdleDuration() >= MAX_IDLE_DURATION
    }

    /**
     * Updates the last activity time if more than 1 minute has passed.
     * Returns true if the activity time was updated.
     */
    fun updateLastActivity(): Boolean {
        val elapsedTimeSinceBoot = timeProvider.elapsedRealTime()

        if (lastActivityTime > elapsedTimeSinceBoot ||
            elapsedTimeSinceBoot > lastActivityTime + TimeProvider.MINUTE
        ) {
            lastActivityTime = elapsedTimeSinceBoot
            return true
        }

        return false
    }

    private fun getIdleDuration(): Long {
        val elapsedTimeSinceBoot = timeProvider.elapsedRealTime()

        // Session expires when device reboots since elapsedRealTime gets restarted
        if (lastActivityTime > elapsedTimeSinceBoot) {
            return Long.MAX_VALUE
        }

        return elapsedTimeSinceBoot - lastActivityTime
    }

    /**
     * Lazily parses adaptive sampling patterns from the embedded SamplingOptions.
     * Patterns are only parsed when adaptive sampling is enabled.
     */
    val parsedPatterns: List<AdaptiveSamplingPattern> by lazy {
        if (!samplingOptions.useAdaptiveSampling) {
            emptyList()
        } else {
            AdaptiveSamplingPatternParser.parsePatterns(samplingOptions.adaptiveSamplingPatterns)
        }
    }

    /**
     * Evaluates the sampling decision for a request using the two-tier sampling model.
     *
     * This method uses the embedded [samplingOptions] to make sampling decisions,
     * making the session self-contained and independent of the ingest config.
     *
     * @param request The request to evaluate
     * @return SamplingResult indicating whether to include or drop the request
     */
    fun evaluateSamplingDecision(request: Request): SamplingResult {
        return evaluateSamplingDecision(request, Random.Default)
    }

    /**
     * Evaluates the sampling decision for a request using the two-tier sampling model.
     *
     * This method uses the embedded [samplingOptions] to make sampling decisions,
     * making the session self-contained and independent of the ingest config.
     *
     * @param request The request to evaluate
     * @param random Random instance for probability calculations (injectable for testing)
     * @return SamplingResult indicating whether to include or drop the request
     */
    fun evaluateSamplingDecision(request: Request, random: Random): SamplingResult {
        return AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = samplingRate,
            sessionTrackingEnabled = trackingEnabled,
            adaptiveSamplingEnabled = samplingOptions.useAdaptiveSampling,
            patterns = parsedPatterns,
            random = random
        )
    }

    override fun toString(): String {
        return "TrackingplanSession(sessionId=$sessionId, samplingRate=$samplingRate, " +
                "trackingEnabled=$trackingEnabled, createdAt=$createdAt, lastActivityTime=$lastActivityTime)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TrackingplanSession

        return sessionId == other.sessionId &&
                createdAt == other.createdAt &&
                trackingEnabled == other.trackingEnabled &&
                samplingRate == other.samplingRate &&
                samplingOptions == other.samplingOptions
    }

    override fun hashCode(): Int {
        var result = sessionId.hashCode()
        result = 31 * result + samplingRate
        result = 31 * result + trackingEnabled.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + lastActivityTime.hashCode()
        result = 31 * result + samplingOptions.hashCode()
        return result
    }

    companion object {
        val MAX_IDLE_DURATION: Long = 30 * TimeProvider.MINUTE

        /** Empty/sentinel session for null-safety. hasExpired() always returns true. */
        val EMPTY = TrackingplanSession(
            sessionId = "",
            samplingRate = Int.MAX_VALUE,
            trackingEnabled = false,
            createdAt = 0,
            lastActivityTime = Long.MAX_VALUE,
            isNew = false,
            samplingOptions = SamplingOptions()
        )

        /**
         * Creates a new session with a generated UUID and current timestamps.
         *
         * @param samplingRate The session's sampling rate (from ingest config)
         * @param trackingEnabled Whether tracking is enabled (Tier 1 sampling result)
         * @param samplingOptions Sampling options including adaptive sampling config (from ingest config)
         */
        @OptIn(ExperimentalUuidApi::class)
        fun newSession(
            samplingRate: Int,
            trackingEnabled: Boolean,
            samplingOptions: SamplingOptions
        ): TrackingplanSession {
            val timeProvider = ServiceLocator.getTimeProvider()
            return TrackingplanSession(
                sessionId = Uuid.random().toString(),
                samplingRate = samplingRate,
                trackingEnabled = trackingEnabled,
                createdAt = timeProvider.currentTimeMillis(),
                lastActivityTime = timeProvider.elapsedRealTime(),
                isNew = true,
                samplingOptions = samplingOptions
            )
        }

        /**
         * Restores a session from storage. Used by Storage.loadSession().
         */
        fun fromStorage(
            sessionId: String,
            samplingRate: Int,
            trackingEnabled: Boolean,
            createdAt: Long,
            lastActivityTime: Long,
            samplingOptions: SamplingOptions
        ): TrackingplanSession {
            return TrackingplanSession(
                sessionId = sessionId,
                samplingRate = samplingRate,
                trackingEnabled = trackingEnabled,
                createdAt = createdAt,
                lastActivityTime = lastActivityTime,
                isNew = false,
                samplingOptions = samplingOptions
            )
        }
    }
}
