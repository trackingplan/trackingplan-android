// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared.adaptive

import com.trackingplan.shared.ServiceLocator
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

/**
 * Parser for adaptive sampling patterns using kotlinx.serialization.
 *
 * Converts JSON pattern strings from the configuration into strongly-typed
 * AdaptiveSamplingPattern objects. Handles complex nested structures including
 * boolean operators (and, or, not) and field matching conditions.
 *
 * The parser is lenient and gracefully handles malformed patterns by logging
 * warnings and skipping invalid patterns, ensuring that other valid patterns
 * continue to work.
 */
object AdaptiveSamplingPatternParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parses a list of JSON pattern strings into AdaptiveSamplingPattern objects.
     * Logs and skips any malformed patterns, returning only valid ones.
     *
     * @param jsonStrings List of JSON strings representing adaptive sampling patterns
     * @return List of successfully parsed patterns (malformed patterns are skipped)
     */
    fun parsePatterns(jsonStrings: List<String>): List<AdaptiveSamplingPattern> {
        return jsonStrings.mapNotNull { jsonString ->
            try {
                parsePattern(jsonString)
            } catch (e: Exception) {
                // Log error but continue with other patterns
                ServiceLocator.getLogger().w("Failed to parse adaptive sampling pattern: ${e.message}")
                null
            }
        }
    }

    /**
     * Parses a single JSON pattern string into an AdaptiveSamplingPattern.
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
     * @param jsonString JSON string representing a single pattern
     * @return Parsed AdaptiveSamplingPattern, or null if parsing fails
     */
    fun parsePattern(jsonString: String): AdaptiveSamplingPattern? {
        return try {
            val jsonElement = json.parseToJsonElement(jsonString)

            if (jsonElement !is JsonObject) {
                return null
            }

            val provider = jsonElement["provider"]?.jsonPrimitive?.content ?: return null
            val sampleRate = jsonElement["sample_rate"]?.jsonPrimitive?.intOrNull ?: 1
            val matchElement = jsonElement["match"]?.jsonObject

            val match = matchElement?.let { parseMatchCondition(it) }

            AdaptiveSamplingPattern(provider, match, sampleRate)
        } catch (e: Exception) {
            // Return null for any parsing errors (malformed JSON, invalid structure, etc.)
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
