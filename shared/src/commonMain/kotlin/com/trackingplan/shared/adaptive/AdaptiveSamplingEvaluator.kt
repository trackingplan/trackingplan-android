// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared.adaptive

import kotlin.random.Random

/**
 * Evaluates sampling decisions for requests using the two-tier sampling model.
 *
 * Tier 1: Session-level sampling (determined at session creation)
 * Tier 2: Event-level adaptive sampling (evaluated per-request when Tier 1 fails)
 *
 * This enables important events to be sampled at higher rates than the default
 * session-level rate, without double-counting events that would have been
 * included anyway.
 */
object AdaptiveSamplingEvaluator {

    /**
     * Evaluates whether a request should be sampled and at what rate.
     *
     * @param request The request to evaluate
     * @param sessionSampleRate The session's default sample rate (1 = 100%, 10 = 10%, etc.)
     * @param sessionTrackingEnabled Whether the session was selected for sampling (Tier 1 result)
     * @param adaptiveSamplingEnabled Whether adaptive sampling is enabled in config
     * @param patterns List of adaptive sampling patterns to match against
     * @param random Random instance for probability calculations (injectable for deterministic testing)
     * @return SamplingResult indicating whether to include or drop the request
     */
    fun evaluate(
        request: Request,
        sessionSampleRate: Int,
        sessionTrackingEnabled: Boolean,
        adaptiveSamplingEnabled: Boolean,
        patterns: List<AdaptiveSamplingPattern>,
        random: Random = Random.Default
    ): SamplingResult {

        // Early exit: if session rate is 0 or negative, tracking is completely disabled
        if (sessionSampleRate <= 0) {
            return SamplingResult.Drop(DropReason.TRACKING_DISABLED)
        }

        if (sessionTrackingEnabled) {
            // Session was selected for sampling - request is always included
            return evaluateForSampledSession(
                request = request,
                sessionSampleRate = sessionSampleRate,
                adaptiveSamplingEnabled = adaptiveSamplingEnabled,
                patterns = patterns
            )
        } else {
            // Session was NOT selected - only rescue if pattern matches and probability allows
            return evaluateForUnsampledSession(
                request = request,
                sessionSampleRate = sessionSampleRate,
                adaptiveSamplingEnabled = adaptiveSamplingEnabled,
                patterns = patterns,
                random = random
            )
        }
    }

    /**
     * Evaluates sampling for a session that was already selected for sampling.
     *
     * When the session is sampled, the request is always included. We just need
     * to determine the effective sample rate and whether a pattern matched.
     */
    private fun evaluateForSampledSession(
        request: Request,
        sessionSampleRate: Int,
        adaptiveSamplingEnabled: Boolean,
        patterns: List<AdaptiveSamplingPattern>
    ): SamplingResult.Include {
        if (!adaptiveSamplingEnabled) {
            return SamplingResult.Include(
                effectiveSampleRate = sessionSampleRate,
                matchedPattern = null,
                samplingMode = SamplingMode.DEFAULT
            )
        }

        val matchResult = AdaptiveSamplingMatcher.matchRequest(request, patterns)

        return if (matchResult.matched && matchResult.sampleRate != null && matchResult.sampleRate > 0) {
            SamplingResult.Include(
                effectiveSampleRate = minOf(matchResult.sampleRate, sessionSampleRate),
                matchedPattern = matchResult.matchedPattern,
                samplingMode = SamplingMode.SESSION_SAMPLED_PATTERN_MATCHED
            )
        } else {
            SamplingResult.Include(
                effectiveSampleRate = sessionSampleRate,
                matchedPattern = null,
                samplingMode = SamplingMode.SESSION_SAMPLED_NO_PATTERN
            )
        }
    }

    /**
     * Evaluates sampling for a session that was NOT selected for sampling.
     *
     * The request can only be included if:
     * 1. Adaptive sampling is enabled
     * 2. A pattern matches the request
     * 3. The rescue probability check passes
     */
    private fun evaluateForUnsampledSession(
        request: Request,
        sessionSampleRate: Int,
        adaptiveSamplingEnabled: Boolean,
        patterns: List<AdaptiveSamplingPattern>,
        random: Random
    ): SamplingResult {
        if (!adaptiveSamplingEnabled) {
            return SamplingResult.Drop(DropReason.ADAPTIVE_SAMPLING_DISABLED)
        }

        val matchResult = AdaptiveSamplingMatcher.matchRequest(request, patterns)

        if (!matchResult.matched || matchResult.sampleRate == null || matchResult.sampleRate <= 0) {
            return SamplingResult.Drop(DropReason.NO_MATCHING_PATTERN)
        }

        if (!shouldRescueEvent(matchResult.sampleRate, sessionSampleRate, random)) {
            return SamplingResult.Drop(DropReason.RESCUE_PROBABILITY_FAILED)
        }

        return SamplingResult.Include(
            effectiveSampleRate = matchResult.sampleRate,
            matchedPattern = matchResult.matchedPattern,
            samplingMode = SamplingMode.EVENT_RESCUED_BY_ADAPTIVE
        )
    }

    /**
     * Determines if an unsampled event should be "rescued" by adaptive sampling.
     *
     * PRECONDITION: This function is only called when the session was NOT selected
     * for sampling (Tier 1 failed). The caller must ensure this condition.
     *
     * Uses conditional probability to ensure the overall sampling rate
     * for matched events equals the pattern's target rate, without double-counting
     * events that would have been included via session sampling.
     *
     * The math:
     * - We want P(event included) = 1/patternSampleRate
     * - P(session sampled) = 1/sessionSampleRate already contributes
     * - We need P(rescue | not session sampled) such that:
     *     P(included) = P(session) + P(not session) * P(rescue)
     *     1/pattern = 1/session + (1 - 1/session) * P(rescue)
     *     P(rescue) = (1/pattern - 1/session) / (1 - 1/session)
     *
     * @param patternSampleRate Target sample rate from matched pattern
     * @param sessionSampleRate Session's default sample rate
     * @param random Random instance for probability check
     * @return true if event should be rescued, false otherwise
     */
    internal fun shouldRescueEvent(
        patternSampleRate: Int,
        sessionSampleRate: Int,
        random: Random = Random.Default
    ): Boolean {
        // Guard: session sample rate must be positive
        if (sessionSampleRate <= 0) return false

        val sessionProbability = 1.0 / sessionSampleRate

        // If session sampling is 100% or invalid, no events need rescuing
        // (they would all be included via session sampling)
        if (sessionProbability >= 1.0) return false

        // Guard: pattern sample rate must be positive
        if (patternSampleRate <= 0) return false

        val targetProbability = 1.0 / patternSampleRate

        // Pattern rate must provide higher probability than session rate
        // (lower sample rate number = higher probability)
        // e.g., patternSampleRate=5 (20%) must be < sessionSampleRate=10 (10%)
        // If pattern probability <= session probability, rescuing won't increase sampling
        if (targetProbability <= sessionProbability) return false

        // Calculate rescue probability using conditional probability formula
        // Use coerceIn to handle floating-point precision issues near boundaries
        val rescueProbability = ((targetProbability - sessionProbability) / (1.0 - sessionProbability))
            .coerceIn(0.0, 1.0)

        return random.nextDouble() < rescueProbability
    }
}
