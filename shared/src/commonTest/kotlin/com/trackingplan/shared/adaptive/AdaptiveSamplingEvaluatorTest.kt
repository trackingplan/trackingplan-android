// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared.adaptive

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class AdaptiveSamplingEvaluatorTest {

    // =============================================================
    // Tracking Disabled (Early Exit)
    // =============================================================

    @Test
    fun testSessionRateZeroSkipsAdaptiveSampling() {
        val request = createRequest("amplitude", """{"event_type":"session_start"}""")
        val pattern = createPattern("amplitude", "event_type", "session_start", 1)

        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = 0,  // Tracking disabled
            sessionTrackingEnabled = false,
            adaptiveSamplingEnabled = true,
            patterns = listOf(pattern)
        )

        assertIs<SamplingResult.Drop>(result)
        assertEquals(DropReason.TRACKING_DISABLED, result.reason)
    }

    @Test
    fun testSessionRateNegativeSkipsAdaptiveSampling() {
        val request = createRequest("amplitude", """{"event_type":"session_start"}""")
        val pattern = createPattern("amplitude", "event_type", "session_start", 1)

        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = -1,  // Invalid rate
            sessionTrackingEnabled = false,
            adaptiveSamplingEnabled = true,
            patterns = listOf(pattern)
        )

        assertIs<SamplingResult.Drop>(result)
        assertEquals(DropReason.TRACKING_DISABLED, result.reason)
    }

    // =============================================================
    // Session Rate 1 (100% Tracking)
    // =============================================================

    @Test
    fun testSessionRateOneAllRequestsIncluded() {
        // Session rate of 1 means 100% of requests are tracked
        val request = createRequest("amplitude", """{"event_type":"session_start"}""")

        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = 1,  // 100% tracking
            sessionTrackingEnabled = true,
            adaptiveSamplingEnabled = false,
            patterns = emptyList()
        )

        assertIs<SamplingResult.Include>(result)
        assertEquals(1, result.effectiveSampleRate)
        assertEquals(SamplingMode.DEFAULT, result.samplingMode)
    }

    @Test
    fun testSessionRateOneWithPatternMatching() {
        // Session rate of 1 with pattern matching - pattern rate should be min(pattern, session)
        val request = createRequest("amplitude", """{"event_type":"session_start"}""")
        val pattern = createPattern("amplitude", "event_type", "session_start", 5)

        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = 1,  // 100% tracking
            sessionTrackingEnabled = true,
            adaptiveSamplingEnabled = true,
            patterns = listOf(pattern)
        )

        assertIs<SamplingResult.Include>(result)
        // min(5, 1) = 1, session rate wins
        assertEquals(1, result.effectiveSampleRate)
        assertEquals(pattern, result.matchedPattern)
        assertEquals(SamplingMode.SESSION_SAMPLED_PATTERN_MATCHED, result.samplingMode)
    }

    // =============================================================
    // Session Sampled + Adaptive Disabled
    // =============================================================

    @Test
    fun testSessionSampledAdaptiveDisabled() {
        val request = createRequest("amplitude")
        val patterns = emptyList<AdaptiveSamplingPattern>()

        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = 10,
            sessionTrackingEnabled = true,
            adaptiveSamplingEnabled = false,
            patterns = patterns
        )

        assertIs<SamplingResult.Include>(result)
        assertEquals(10, result.effectiveSampleRate)
        assertNull(result.matchedPattern)
        assertEquals(SamplingMode.DEFAULT, result.samplingMode)
    }

    // =============================================================
    // Session Sampled + Adaptive Enabled + Pattern Matches
    // =============================================================

    @Test
    fun testSessionSampledPatternMatches() {
        val request = createRequest("amplitude", """{"event_type":"session_start"}""")
        val pattern = createPattern("amplitude", "event_type", "session_start", 5)

        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = 10,
            sessionTrackingEnabled = true,
            adaptiveSamplingEnabled = true,
            patterns = listOf(pattern)
        )

        assertIs<SamplingResult.Include>(result)
        assertEquals(5, result.effectiveSampleRate)  // min(5, 10) = 5
        assertEquals(pattern, result.matchedPattern)
        assertEquals(SamplingMode.SESSION_SAMPLED_PATTERN_MATCHED, result.samplingMode)
    }

    @Test
    fun testSessionSampledPatternMatchesWithHigherPatternRate() {
        // When pattern rate is higher than session rate (lower probability),
        // we use the session rate (higher probability)
        val request = createRequest("amplitude", """{"event_type":"session_start"}""")
        val pattern = createPattern("amplitude", "event_type", "session_start", 20)

        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = 10,
            sessionTrackingEnabled = true,
            adaptiveSamplingEnabled = true,
            patterns = listOf(pattern)
        )

        assertIs<SamplingResult.Include>(result)
        assertEquals(10, result.effectiveSampleRate)  // min(20, 10) = 10
        assertEquals(pattern, result.matchedPattern)
        assertEquals(SamplingMode.SESSION_SAMPLED_PATTERN_MATCHED, result.samplingMode)
    }

    // =============================================================
    // Session Sampled + Adaptive Enabled + No Pattern Matches
    // =============================================================

    @Test
    fun testSessionSampledNoPatternMatches() {
        val request = createRequest("amplitude", """{"event_type":"page_view"}""")
        val pattern = createPattern("amplitude", "event_type", "session_start", 5)

        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = 10,
            sessionTrackingEnabled = true,
            adaptiveSamplingEnabled = true,
            patterns = listOf(pattern)
        )

        assertIs<SamplingResult.Include>(result)
        assertEquals(10, result.effectiveSampleRate)
        assertNull(result.matchedPattern)
        assertEquals(SamplingMode.SESSION_SAMPLED_NO_PATTERN, result.samplingMode)
    }

    @Test
    fun testSessionSampledEmptyPatterns() {
        val request = createRequest("amplitude")

        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = 10,
            sessionTrackingEnabled = true,
            adaptiveSamplingEnabled = true,
            patterns = emptyList()
        )

        assertIs<SamplingResult.Include>(result)
        assertEquals(10, result.effectiveSampleRate)
        assertNull(result.matchedPattern)
        assertEquals(SamplingMode.SESSION_SAMPLED_NO_PATTERN, result.samplingMode)
    }

    // =============================================================
    // Session Sampled + Multiple Patterns (First Match Wins)
    // =============================================================

    @Test
    fun testMultiplePatternsFirstMatchWins() {
        val request = createRequest("amplitude", """{"event_type":"session_start"}""")
        val pattern1 = createPattern("amplitude", "event_type", "session_start", 5)
        val pattern2 = createPattern("amplitude", "event_type", "session_start", 3)

        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = 10,
            sessionTrackingEnabled = true,
            adaptiveSamplingEnabled = true,
            patterns = listOf(pattern1, pattern2)
        )

        assertIs<SamplingResult.Include>(result)
        assertEquals(5, result.effectiveSampleRate)  // First pattern's rate
        assertEquals(pattern1, result.matchedPattern)
    }

    // =============================================================
    // Session NOT Sampled + Adaptive Disabled
    // =============================================================

    @Test
    fun testSessionNotSampledAdaptiveDisabled() {
        val request = createRequest("amplitude")

        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = 10,
            sessionTrackingEnabled = false,
            adaptiveSamplingEnabled = false,
            patterns = emptyList()
        )

        assertIs<SamplingResult.Drop>(result)
        assertEquals(DropReason.ADAPTIVE_SAMPLING_DISABLED, result.reason)
    }

    // =============================================================
    // Session NOT Sampled + Adaptive Enabled + No Pattern Matches
    // =============================================================

    @Test
    fun testSessionNotSampledNoPatternMatches() {
        val request = createRequest("amplitude", """{"event_type":"page_view"}""")
        val pattern = createPattern("amplitude", "event_type", "session_start", 5)

        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = 10,
            sessionTrackingEnabled = false,
            adaptiveSamplingEnabled = true,
            patterns = listOf(pattern)
        )

        assertIs<SamplingResult.Drop>(result)
        assertEquals(DropReason.NO_MATCHING_PATTERN, result.reason)
    }

    @Test
    fun testSessionNotSampledEmptyPatterns() {
        val request = createRequest("amplitude")

        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = 10,
            sessionTrackingEnabled = false,
            adaptiveSamplingEnabled = true,
            patterns = emptyList()
        )

        assertIs<SamplingResult.Drop>(result)
        assertEquals(DropReason.NO_MATCHING_PATTERN, result.reason)
    }

    // =============================================================
    // Session NOT Sampled + Pattern Matches + Rescue Succeeds
    // =============================================================

    @Test
    fun testSessionNotSampledPatternMatchesRescueSucceeds() {
        val request = createRequest("amplitude", """{"event_type":"session_start"}""")
        val pattern = createPattern("amplitude", "event_type", "session_start", 5)

        // Use deterministic random that always succeeds (returns 0.0)
        val alwaysSucceedsRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextDouble(): Double = 0.0
        }

        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = 10,
            sessionTrackingEnabled = false,
            adaptiveSamplingEnabled = true,
            patterns = listOf(pattern),
            random = alwaysSucceedsRandom
        )

        assertIs<SamplingResult.Include>(result)
        assertEquals(5, result.effectiveSampleRate)
        assertEquals(pattern, result.matchedPattern)
        assertEquals(SamplingMode.EVENT_RESCUED_BY_ADAPTIVE, result.samplingMode)
    }

    // =============================================================
    // Session NOT Sampled + Pattern Matches + Rescue Fails
    // =============================================================

    @Test
    fun testSessionNotSampledPatternMatchesRescueFails() {
        val request = createRequest("amplitude", """{"event_type":"session_start"}""")
        val pattern = createPattern("amplitude", "event_type", "session_start", 5)

        // Use deterministic random that always fails (returns 0.99)
        val alwaysFailsRandom = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
            override fun nextDouble(): Double = 0.99
        }

        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = 10,
            sessionTrackingEnabled = false,
            adaptiveSamplingEnabled = true,
            patterns = listOf(pattern),
            random = alwaysFailsRandom
        )

        assertIs<SamplingResult.Drop>(result)
        assertEquals(DropReason.RESCUE_PROBABILITY_FAILED, result.reason)
    }

    // =============================================================
    // Invalid Pattern Sample Rate
    // =============================================================

    @Test
    fun testPatternWithZeroSampleRate() {
        val request = createRequest("amplitude", """{"event_type":"session_start"}""")
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.Fields(mapOf("event_type" to MatchValue.Single("session_start"))),
            sampleRate = 0
        )

        // Session sampled - should return NO_PATTERN mode since pattern rate is invalid
        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = 10,
            sessionTrackingEnabled = true,
            adaptiveSamplingEnabled = true,
            patterns = listOf(pattern)
        )

        assertIs<SamplingResult.Include>(result)
        assertEquals(SamplingMode.SESSION_SAMPLED_NO_PATTERN, result.samplingMode)
    }

    @Test
    fun testPatternWithNegativeSampleRate() {
        val request = createRequest("amplitude", """{"event_type":"session_start"}""")
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.Fields(mapOf("event_type" to MatchValue.Single("session_start"))),
            sampleRate = -5
        )

        // Session not sampled - should return Drop since pattern rate is invalid
        val result = AdaptiveSamplingEvaluator.evaluate(
            request = request,
            sessionSampleRate = 10,
            sessionTrackingEnabled = false,
            adaptiveSamplingEnabled = true,
            patterns = listOf(pattern)
        )

        assertIs<SamplingResult.Drop>(result)
        assertEquals(DropReason.NO_MATCHING_PATTERN, result.reason)
    }

    // =============================================================
    // shouldRescueEvent - Edge Cases
    // =============================================================

    @Test
    fun testShouldRescueEventSessionRateZero() {
        assertFalse(AdaptiveSamplingEvaluator.shouldRescueEvent(5, 0))
    }

    @Test
    fun testShouldRescueEventSessionRateOne() {
        // Session rate of 1 means 100% sampling - no rescue needed
        assertFalse(AdaptiveSamplingEvaluator.shouldRescueEvent(5, 1))
    }

    @Test
    fun testShouldRescueEventPatternRateZero() {
        assertFalse(AdaptiveSamplingEvaluator.shouldRescueEvent(0, 10))
    }

    @Test
    fun testShouldRescueEventPatternRateNegative() {
        assertFalse(AdaptiveSamplingEvaluator.shouldRescueEvent(-1, 10))
    }

    @Test
    fun testShouldRescueEventPatternRateEqualToSessionRate() {
        // Same rate means no additional probability needed
        assertFalse(AdaptiveSamplingEvaluator.shouldRescueEvent(10, 10))
    }

    @Test
    fun testShouldRescueEventPatternRateGreaterThanSessionRate() {
        // Pattern rate 20 (5%) is less likely than session rate 10 (10%)
        // No rescue can increase probability in this case
        assertFalse(AdaptiveSamplingEvaluator.shouldRescueEvent(20, 10))
    }

    // =============================================================
    // shouldRescueEvent - Probability Calculation
    // =============================================================

    @Test
    fun testShouldRescueEventProbabilityCalculation() {
        // Session rate: 10 (P = 0.1)
        // Pattern rate: 5 (P = 0.2)
        // Expected rescue probability: (0.2 - 0.1) / (1 - 0.1) = 0.1 / 0.9 ≈ 0.111

        val rescueProbability = calculateRescueProbability(5, 10)
        assertTrue(rescueProbability > 0.0)
        assertTrue(rescueProbability < 1.0)

        // Approximately 0.111
        assertTrue(rescueProbability > 0.1)
        assertTrue(rescueProbability < 0.15)
    }

    @Test
    fun testShouldRescueEventHighRescueProbability() {
        // Session rate: 100 (P = 0.01)
        // Pattern rate: 2 (P = 0.5)
        // Expected rescue probability: (0.5 - 0.01) / (1 - 0.01) ≈ 0.495

        val rescueProbability = calculateRescueProbability(2, 100)
        assertTrue(rescueProbability > 0.4)
        assertTrue(rescueProbability < 0.6)
    }

    // =============================================================
    // shouldRescueEvent - Statistical Distribution
    // =============================================================

    @Test
    fun testRescueProbabilityDistribution() {
        // Test that shouldRescueEvent produces expected distribution over many samples
        val sessionSampleRate = 10  // 10% session probability
        val patternSampleRate = 5   // 20% target probability

        // Expected rescue probability: (0.2 - 0.1) / (1 - 0.1) ≈ 0.111
        val expectedRescueProbability = (1.0/patternSampleRate - 1.0/sessionSampleRate) /
                (1.0 - 1.0/sessionSampleRate)

        val numSamples = 10000
        var rescueCount = 0

        repeat(numSamples) {
            if (AdaptiveSamplingEvaluator.shouldRescueEvent(patternSampleRate, sessionSampleRate)) {
                rescueCount++
            }
        }

        val actualProbability = rescueCount.toDouble() / numSamples

        // Allow 3% tolerance for statistical variance
        val tolerance = 0.03
        assertTrue(
            actualProbability > expectedRescueProbability - tolerance,
            "Actual probability $actualProbability should be close to expected $expectedRescueProbability"
        )
        assertTrue(
            actualProbability < expectedRescueProbability + tolerance,
            "Actual probability $actualProbability should be close to expected $expectedRescueProbability"
        )
    }

    @Test
    fun testCombinedProbabilityMatchesTargetRate() {
        // Test that the combined probability of session + rescue equals target rate
        val sessionSampleRate = 10  // 10% session probability
        val patternSampleRate = 5   // 20% target probability

        val numSamples = 10000
        var includedCount = 0

        repeat(numSamples) {
            val random = Random.Default
            val sessionSampled = random.nextDouble() < (1.0 / sessionSampleRate)

            if (sessionSampled) {
                includedCount++
            } else {
                if (AdaptiveSamplingEvaluator.shouldRescueEvent(patternSampleRate, sessionSampleRate, random)) {
                    includedCount++
                }
            }
        }

        val actualProbability = includedCount.toDouble() / numSamples
        val expectedProbability = 1.0 / patternSampleRate  // 0.2

        // Allow 3% tolerance for statistical variance
        val tolerance = 0.03
        assertTrue(
            actualProbability > expectedProbability - tolerance,
            "Combined probability $actualProbability should be close to expected $expectedProbability"
        )
        assertTrue(
            actualProbability < expectedProbability + tolerance,
            "Combined probability $actualProbability should be close to expected $expectedProbability"
        )
    }

    // =============================================================
    // Enum Value Validation
    // =============================================================

    @Test
    fun testDropReasonValues() {
        // Verify drop reason string values for logging
        assertEquals("tracking-disabled", DropReason.TRACKING_DISABLED.value)
        assertEquals("adaptive-sampling-disabled", DropReason.ADAPTIVE_SAMPLING_DISABLED.value)
        assertEquals("no-matching-pattern", DropReason.NO_MATCHING_PATTERN.value)
        assertEquals("rescue-probability-failed", DropReason.RESCUE_PROBABILITY_FAILED.value)
    }

    @Test
    fun testSamplingModeValues() {
        // Verify sampling mode string values for analytics/debugging
        assertEquals("NOT_ADAPTIVE", SamplingMode.DEFAULT.value)
        assertEquals("ADAPTIVE/DEFAULT_DICE/EVENT_MATCHED", SamplingMode.SESSION_SAMPLED_PATTERN_MATCHED.value)
        assertEquals("ADAPTIVE/DEFAULT_DICE/EVENT_NOT_MATCHED", SamplingMode.SESSION_SAMPLED_NO_PATTERN.value)
        assertEquals("ADAPTIVE/EVENT_DICE/EVENT_MATCHED", SamplingMode.EVENT_RESCUED_BY_ADAPTIVE.value)
    }

    // =============================================================
    // Helper Functions
    // =============================================================

    private fun createRequest(
        provider: String,
        payload: String = """{"test":"data"}"""
    ): Request {
        return Request(
            provider = provider,
            endpoint = "https://api.$provider.com/track",
            payload = payload
        )
    }

    private fun createPattern(
        provider: String,
        fieldKey: String,
        fieldValue: String,
        sampleRate: Int
    ): AdaptiveSamplingPattern {
        return AdaptiveSamplingPattern(
            provider = provider,
            match = MatchCondition.Fields(
                mapOf(fieldKey to MatchValue.Single(fieldValue))
            ),
            sampleRate = sampleRate
        )
    }

    private fun calculateRescueProbability(patternSampleRate: Int, sessionSampleRate: Int): Double {
        val sessionProbability = 1.0 / sessionSampleRate
        val targetProbability = 1.0 / patternSampleRate
        return (targetProbability - sessionProbability) / (1.0 - sessionProbability)
    }
}
