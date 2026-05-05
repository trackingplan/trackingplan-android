// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared.adaptive

import com.trackingplan.shared.ServiceLocator
import kotlinx.serialization.json.*

/**
 * Parser for adaptive sampling patterns using kotlinx.serialization.
 *
 * Converts raw JsonElement values from SamplingOptions into strongly-typed
 * AdaptiveSamplingPattern objects. Handles complex nested structures including
 * boolean operators (and, or, not) and field matching conditions.
 *
 * The parser is lenient and gracefully handles malformed patterns by logging
 * warnings and skipping invalid patterns, ensuring that other valid patterns
 * continue to work even as the wire format evolves.
 */
object AdaptiveSamplingPatternParser {

    /**
     * Parses a list of pattern JsonElements into AdaptiveSamplingPattern objects.
     * Skips any malformed patterns, returning only valid ones; per-pattern parse failures
     * are logged inside [parsePattern].
     *
     * @param elements List of JsonElements representing adaptive sampling patterns
     * @return List of successfully parsed patterns (malformed patterns are skipped)
     */
    fun parsePatterns(elements: List<JsonElement>): List<AdaptiveSamplingPattern> {
        return elements.mapNotNull { parsePattern(it) }
    }

    /**
     * Parses a single pattern JsonElement into an AdaptiveSamplingPattern.
     *
     * Expected JSON format:
     * ```json
     * {
     *   "provider": "amplitude",
     *   "match": {
     *     "event_type": "session_start",
     *     "plan": ["premium", "enterprise"]
     *   },
     *   "sample_rate": 5
     * }
     * ```
     *
     * @param element JsonElement representing a single pattern
     * @return Parsed AdaptiveSamplingPattern, or null on any of the following:
     * - element is not a JsonObject
     * - `provider` is missing or not a string
     * - `match` is present but its shape is neither JsonNull nor a JsonObject (this case
     *   drops the whole pattern rather than collapsing to a catch-all rule)
     *
     * All structural early returns above are silent — the wire format is allowed to
     * evolve and unrecognised shapes degrade silently rather than spamming logs. The
     * outer catch is defensive only: any throw from inside the body (e.g. from
     * parseMatchCondition on a malformed operator body) is unexpected and gets logged
     * at WARN before returning null.
     */
    fun parsePattern(element: JsonElement): AdaptiveSamplingPattern? {
        return try {
            if (element !is JsonObject) return null

            val provider = (element["provider"] as? JsonPrimitive)
                ?.takeIf { it.isString }?.content ?: return null
            val sampleRate = (element["sample_rate"] as? JsonPrimitive)?.intOrNull ?: 1
            // Distinguish absent / JsonNull (catch-all) from malformed shape (drop the
            // whole pattern). A null `match` is interpreted by AdaptiveSamplingMatcher as
            // "applies to every request for this provider" — treating an unrecognised
            // shape as null would silently widen the rule's reach.
            val matchElement: JsonObject? = when (val m = element["match"]) {
                null, is JsonNull -> null
                is JsonObject -> m
                else -> return null
            }

            val match = matchElement?.let { parseMatchCondition(it) }

            AdaptiveSamplingPattern(provider, match, sampleRate)
        } catch (e: Exception) {
            // Defense in depth: a secondary throw from the logger here would propagate
            // through parsedPatterns (lazy) into evaluateSamplingDecision and crash the
            // host app at the Kotlin/Native boundary on iOS. Swallow any logger failure.
            try {
                ServiceLocator.getLogger().w(
                    "Failed to parse adaptive sampling pattern: ${e.message}"
                )
            } catch (_: Throwable) {
            }
            null
        }
    }

    /**
     * Recursively parses match conditions from JsonObject.
     *
     * Supports:
     * - Boolean operators: "and", "or", "not"
     * - Field matching: Simple key-value pairs (implicit AND)
     * - Array values: Treated as OR logic
     *
     * @param element JsonObject representing the match condition
     * @return Parsed MatchCondition
     */
    internal fun parseMatchCondition(element: JsonObject): MatchCondition {
        // Check for boolean operators first
        when {
            element.containsKey("and") -> {
                val conditions = element["and"]!!.jsonArray.map {
                    parseMatchCondition(it.jsonObject)
                }
                return MatchCondition.And(conditions)
            }

            element.containsKey("or") -> {
                val conditions = element["or"]!!.jsonArray.map {
                    parseMatchCondition(it.jsonObject)
                }
                return MatchCondition.Or(conditions)
            }

            element.containsKey("not") -> {
                val condition = parseMatchCondition(element["not"]!!.jsonObject)
                return MatchCondition.Not(condition)
            }

            else -> {
                // Simple field matching (implicit AND for multiple fields)
                val fields = element.mapNotNull { (key, value) ->
                    val matchValue = when (value) {
                        is JsonArray -> {
                            val values = value.mapNotNull { it.jsonPrimitive.contentOrNull }
                            if (values.isNotEmpty()) {
                                MatchValue.Multiple(values)
                            } else {
                                null
                            }
                        }
                        is JsonPrimitive -> {
                            val content = value.contentOrNull
                            if (content != null) {
                                MatchValue.Single(content)
                            } else {
                                null
                            }
                        }
                        else -> null
                    }
                    matchValue?.let { key to it }
                }.toMap()

                return MatchCondition.Fields(fields)
            }
        }
    }
}
