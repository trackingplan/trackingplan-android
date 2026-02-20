// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared.adaptive

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an adaptive sampling pattern rule from the configuration.
 *
 * Adaptive sampling allows fine-grained control over which events are sampled
 * based on their characteristics (provider, event type, properties, etc.).
 *
 * @property provider The analytics provider this rule applies to (e.g., "amplitude", "mixpanel")
 * @property match Optional matching conditions. If null, rule applies to all requests for this provider
 * @property sampleRate The sampling rate to apply if this rule matches (1/X probability, where 1 = 100%, 2 = 50%, etc.)
 */
@Serializable
data class AdaptiveSamplingPattern(
    val provider: String,
    val match: MatchCondition? = null,
    @SerialName("sample_rate") val sampleRate: Int
)

/**
 * Sealed class representing matching conditions with support for boolean operators
 * and field matching. Uses sealed class hierarchy for type safety and exhaustive
 * when expressions.
 *
 * This class hierarchy enables complex matching logic such as:
 * - Simple field matching: {"event_type": "page_view"}
 * - Boolean operators: {"and": [...], "or": [...], "not": {...}}
 * - Array values (OR logic): {"plan": ["premium", "enterprise"]}
 */
@Serializable
sealed class MatchCondition {

    /**
     * AND operator - all conditions must match.
     *
     * Example: {"and": [{"event": "purchase"}, {"currency": "USD"}]}
     */
    @Serializable
    @SerialName("and")
    data class And(val conditions: List<MatchCondition>) : MatchCondition()

    /**
     * OR operator - at least one condition must match.
     *
     * Example: {"or": [{"platform": "iOS"}, {"platform": "Android"}]}
     */
    @Serializable
    @SerialName("or")
    data class Or(val conditions: List<MatchCondition>) : MatchCondition()

    /**
     * NOT operator - negates the nested condition.
     *
     * Example: {"not": {"debug": "true"}}
     */
    @Serializable
    @SerialName("not")
    data class Not(val condition: MatchCondition) : MatchCondition()

    /**
     * Field matching - key-value comparisons with support for special operators.
     * Multiple fields in the map use implicit AND logic (all must match).
     *
     * Example: {"event_type": "session_start", "plan": "premium"}
     */
    @Serializable
    @SerialName("fields")
    data class Fields(val fields: Map<String, MatchValue>) : MatchCondition()
}

/**
 * Represents a value to match against in a field condition.
 * Can be a single value or array of values (OR logic for arrays).
 *
 * - Single: Matches if the field equals this value
 * - Multiple: Matches if the field equals any value in the list (OR logic)
 *
 * Example:
 * - Single: "premium" matches if field == "premium"
 * - Multiple: ["premium", "enterprise"] matches if field == "premium" OR field == "enterprise"
 */
@Serializable
sealed class MatchValue {
    /**
     * Single value to match exactly.
     */
    @Serializable
    @SerialName("single")
    data class Single(val value: String) : MatchValue()

    /**
     * Multiple values - matches if field equals any value (OR logic).
     */
    @Serializable
    @SerialName("multiple")
    data class Multiple(val values: List<String>) : MatchValue()
}

/**
 * Constants for special field keys used in adaptive sampling patterns.
 *
 * These special keys enable advanced matching beyond simple field equality:
 * - ENDPOINT_PATH_CONTAINS: Match substring in URL path (e.g., "/v1/batch")
 * - ENDPOINT_OR_PAYLOAD_CONTAINS: Match substring anywhere in request
 * - ANY_KEY: Match value in any field of the payload
 * - CONTAINS_SUFFIX: When appended to field name (e.g., "event_name@CONTAINS"), enables substring matching
 */
object SpecialKeys {
    /**
     * Special key for matching substrings in the endpoint path.
     * Example: {"@TP_ENDPOINT_PATH@CONTAINS": "/track"} matches URLs like "https://api.segment.io/v1/track"
     */
    const val ENDPOINT_PATH_CONTAINS = "@TP_ENDPOINT_PATH@CONTAINS"

    /**
     * Special key for matching substrings in either endpoint or payload.
     * Example: {"@TP_ENDPOINT_OR_PAYLOAD@CONTAINS": "purchase"} matches if "purchase" appears anywhere
     */
    const val ENDPOINT_OR_PAYLOAD_CONTAINS = "@TP_ENDPOINT_OR_PAYLOAD@CONTAINS"

    /**
     * Special key for matching a value in any field.
     * Example: {"@TP_ANY_KEY": "test"} matches if any field contains "test"
     */
    const val ANY_KEY = "@TP_ANY_KEY"

    /**
     * Suffix for enabling substring matching on specific fields.
     * Example: {"event_name@CONTAINS": "purchase"} matches if event_name contains "purchase"
     */
    const val CONTAINS_SUFFIX = "@CONTAINS"
}
