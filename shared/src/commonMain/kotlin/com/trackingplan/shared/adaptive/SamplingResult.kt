// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared.adaptive

/**
 * Represents the outcome of a sampling decision for a request.
 *
 * This is a sealed class that explicitly handles both inclusion and dropping of requests,
 * making the decision outcome type-safe and self-documenting.
 */
sealed class SamplingResult {
    /**
     * Request should be included in sampling.
     *
     * @property effectiveSampleRate The sample rate to report (1 = 100%, 10 = 10%, etc.)
     * @property matchedPattern The adaptive pattern that matched (null if none matched)
     * @property samplingMode Describes how the decision was made (for analytics/debugging)
     */
    data class Include(
        val effectiveSampleRate: Int,
        val matchedPattern: AdaptiveSamplingPattern?,
        val samplingMode: SamplingMode
    ) : SamplingResult()

    /**
     * Request should be dropped (not included in sampling).
     *
     * @property reason The reason why the request was dropped
     */
    data class Drop(
        val reason: DropReason
    ) : SamplingResult()
}

/**
 * Reasons why a request may be dropped by adaptive sampling.
 *
 * Each reason has a string value for logging and debugging purposes.
 */
enum class DropReason(val value: String) {
    /** Session sample rate is 0 - tracking completely disabled */
    TRACKING_DISABLED("tracking-disabled"),

    /** Session not sampled and adaptive sampling is disabled in config */
    ADAPTIVE_SAMPLING_DISABLED("adaptive-sampling-disabled"),

    /** Session not sampled and no adaptive pattern matched the request */
    NO_MATCHING_PATTERN("no-matching-pattern"),

    /** Session not sampled, pattern matched, but rescue probability check failed */
    RESCUE_PROBABILITY_FAILED("rescue-probability-failed")
}

/**
 * Describes how a sampling decision was made.
 *
 * The string values are used for analytics and debugging, maintaining
 * compatibility with the JS SDK's sampling mode identifiers.
 */
enum class SamplingMode(val value: String) {
    /** No adaptive sampling - using default session sampling only */
    DEFAULT("NOT_ADAPTIVE"),

    /** Session was sampled, adaptive rule matched */
    SESSION_SAMPLED_PATTERN_MATCHED("ADAPTIVE/DEFAULT_DICE/EVENT_MATCHED"),

    /** Session was sampled, no adaptive rule matched */
    SESSION_SAMPLED_NO_PATTERN("ADAPTIVE/DEFAULT_DICE/EVENT_NOT_MATCHED"),

    /** Session not sampled, but event rescued by adaptive sampling */
    EVENT_RESCUED_BY_ADAPTIVE("ADAPTIVE/EVENT_DICE/EVENT_MATCHED")
}
