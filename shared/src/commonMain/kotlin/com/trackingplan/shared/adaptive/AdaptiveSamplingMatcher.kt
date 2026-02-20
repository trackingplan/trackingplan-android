// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared.adaptive

/**
 * Result of matching a request against adaptive sampling patterns.
 *
 * @property matched Whether a pattern matched
 * @property sampleRate The sample rate from the matched pattern (null if no match)
 * @property matchedPattern The full pattern that matched (null if no match)
 */
data class MatchResult(
    val matched: Boolean,
    val sampleRate: Int?,
    val matchedPattern: AdaptiveSamplingPattern?
)

/**
 * Core pattern matching engine for adaptive sampling.
 *
 * Evaluates requests against a list of adaptive sampling patterns to determine
 * if a special sampling rate should be applied based on the request characteristics.
 *
 * Algorithm:
 * 1. Filter patterns by provider
 * 2. Extract and pre-flatten all payload variations (endpoint params, JSON, batched queries)
 * 3. Evaluate each pattern in order
 * 4. Return first matching pattern (or null if none match)
 *
 * Optimization: Pre-flattening all data variations once before matching avoids repeated
 * recursive traversals, significantly improving performance with many patterns.
 */
object AdaptiveSamplingMatcher {

    /**
     * Matches a request against a list of adaptive sampling patterns.
     *
     * Returns the first pattern that matches, or a non-matched result if none match.
     * Patterns are evaluated in order, so more specific patterns should come first.
     *
     * @param request The request to match
     * @param patterns List of adaptive sampling patterns
     * @return MatchResult with match status, sample rate, and matched pattern
     */
    fun matchRequest(
        request: Request,
        patterns: List<AdaptiveSamplingPattern>
    ): MatchResult {
        // Filter patterns for this provider
        val providerPatterns = patterns.filter { it.provider == request.provider }
        if (providerPatterns.isEmpty()) {
            return MatchResult(matched = false, sampleRate = null, matchedPattern = null)
        }

        // Extract payload variations (endpoint params, JSON, batched queries)
        val variations = RequestDataExtractor.extractPayloadVariations(request)

        // Pre-flatten all variations for efficient matching
        val flattenedVariations = variations.map { variation ->
            PayloadFlattener.flattenPayloadVariation(variation)
        }

        // Create context for special keys
        val context = RequestDataExtractor.createMatchContext(request)

        // Evaluate each pattern in order
        for (pattern in providerPatterns) {
            // If no match condition, pattern applies automatically
            if (pattern.match == null) {
                return MatchResult(
                    matched = true,
                    sampleRate = pattern.sampleRate,
                    matchedPattern = pattern
                )
            }

            // Try to match against each flattened variation
            // Ensure at least one iteration for special key evaluation (Issue #3)
            val variationsToCheck = flattenedVariations.ifEmpty { listOf(emptyMap()) }
            for (flattened in variationsToCheck) {
                if (evaluateCondition(pattern.match, flattened, context)) {
                    return MatchResult(
                        matched = true,
                        sampleRate = pattern.sampleRate,
                        matchedPattern = pattern
                    )
                }
            }
        }

        return MatchResult(matched = false, sampleRate = null, matchedPattern = null)
    }

    /**
     * Recursively evaluates a match condition against flattened data.
     *
     * Supports:
     * - Boolean operators: And, Or, Not
     * - Field matching: exact, contains, special keys
     * - Array values: OR logic (match if any value matches)
     *
     * @param condition The condition to evaluate
     * @param flatMap Flattened payload data (key -> list of string values)
     * @param context Match context for special key operations
     * @return true if condition matches, false otherwise
     */
    private fun evaluateCondition(
        condition: MatchCondition,
        flatMap: Map<String, List<String>>,
        context: MatchContext
    ): Boolean {
        return when (condition) {
            is MatchCondition.And -> {
                // ALL conditions must match
                condition.conditions.all { evaluateCondition(it, flatMap, context) }
            }

            is MatchCondition.Or -> {
                // ANY condition must match
                condition.conditions.any { evaluateCondition(it, flatMap, context) }
            }

            is MatchCondition.Not -> {
                // Condition must NOT match
                !evaluateCondition(condition.condition, flatMap, context)
            }

            is MatchCondition.Fields -> {
                // All fields must match (implicit AND)
                condition.fields.all { (key, matchValue) ->
                    evaluateFieldMatch(key, matchValue, flatMap, context)
                }
            }
        }
    }

    /**
     * Evaluates a single field match against the data.
     *
     * Handles:
     * - Special keys: @TP_ENDPOINT_PATH@CONTAINS, @TP_ENDPOINT_OR_PAYLOAD@CONTAINS, @TP_ANY_KEY
     * - Contains suffix: field@CONTAINS for substring matching
     * - Regular field: exact matching
     *
     * @param key The field key (may be special key or regular field)
     * @param matchValue The value(s) to match against
     * @param flatMap Flattened payload data
     * @param context Match context for special operations
     * @return true if field matches, false otherwise
     */
    private fun evaluateFieldMatch(
        key: String,
        matchValue: MatchValue,
        flatMap: Map<String, List<String>>,
        context: MatchContext
    ): Boolean {
        return when {
            // Special key: @TP_ENDPOINT_PATH@CONTAINS
            key == SpecialKeys.ENDPOINT_PATH_CONTAINS -> {
                matchAnyValue(matchValue) { value ->
                    context.path.contains(value)
                }
            }

            // Special key: @TP_ENDPOINT_OR_PAYLOAD@CONTAINS
            key == SpecialKeys.ENDPOINT_OR_PAYLOAD_CONTAINS -> {
                val fullText = "${context.endpoint} ${context.payload}"
                matchAnyValue(matchValue) { value ->
                    fullText.contains(value)
                }
            }

            // Special key: @TP_ANY_KEY
            key == SpecialKeys.ANY_KEY -> {
                matchAnyValue(matchValue) { value ->
                    flatMap.values.any { values -> values.contains(value) }
                }
            }

            // Key with @CONTAINS suffix (substring matching)
            key.endsWith(SpecialKeys.CONTAINS_SUFFIX) -> {
                val actualKey = key.substring(0, key.length - SpecialKeys.CONTAINS_SUFFIX.length)
                val fieldValues = flatMap[actualKey] ?: return false

                matchAnyValue(matchValue) { value ->
                    fieldValues.any { fieldValue -> fieldValue.contains(value) }
                }
            }

            // Regular key matching (exact)
            else -> {
                val fieldValues = flatMap[key] ?: return false
                matchAnyValue(matchValue) { value ->
                    fieldValues.contains(value)
                }
            }
        }
    }

    /**
     * Helper to match against single or multiple values (OR logic for arrays).
     *
     * For Single: Tests the predicate with the single value
     * For Multiple: Tests if ANY value in the array matches (OR logic)
     *
     * @param matchValue The value(s) to test
     * @param predicate Function to test each value
     * @return true if any value matches, false otherwise
     */
    private fun matchAnyValue(matchValue: MatchValue, predicate: (String) -> Boolean): Boolean {
        return when (matchValue) {
            is MatchValue.Single -> predicate(matchValue.value)
            is MatchValue.Multiple -> matchValue.values.any(predicate)
        }
    }
}
