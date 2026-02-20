// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared.adaptive

import com.trackingplan.shared.BaseTest
import com.trackingplan.shared.SamplingOptions
import com.trackingplan.shared.TrackingplanSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for adaptive sampling pattern matching through TrackingplanSession.
 * Tests the full flow: config options → session → pattern parsing → matching → sampling decision.
 */
class AdaptiveSamplingIntegrationTest : BaseTest() {

    // =============================================================
    // Basic Pattern Matching
    // =============================================================

    @Test
    fun testFullMatchingWorkflow() {
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            patterns = listOf(
                """
                {
                    "provider": "amplitude",
                    "match": {"event_type": "session_start"},
                    "sample_rate": 5
                }
                """.trimIndent()
            )
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"session_start","user_id":"123"}"""
        )

        val result = session.evaluateSamplingDecision(request)

        assertIs<SamplingResult.Include>(result)
        assertEquals(5, result.effectiveSampleRate)
        val matchedPattern = result.matchedPattern
        assertNotNull(matchedPattern)
        assertEquals("amplitude", matchedPattern.provider)
        assertEquals(SamplingMode.SESSION_SAMPLED_PATTERN_MATCHED, result.samplingMode)
    }

    @Test
    fun testProviderOnlyPattern() {
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            patterns = listOf(
                """
                {
                    "provider": "hotjar",
                    "sample_rate": 10
                }
                """.trimIndent()
            )
        )

        // Any Hotjar request should match (no match condition)
        val request = Request(
            provider = "hotjar",
            endpoint = "https://api.hotjar.com/track",
            payload = """{"event":"anything","data":"whatever"}"""
        )

        val result = session.evaluateSamplingDecision(request)
        assertIs<SamplingResult.Include>(result)
        // effectiveSampleRate is min(patternRate, sessionRate) = min(10, 10) = 10
        assertEquals(10, result.effectiveSampleRate)
    }

    @Test
    fun testNoMatchingProvider() {
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            patterns = listOf(
                """
                {
                    "provider": "amplitude",
                    "sample_rate": 5
                }
                """.trimIndent()
            )
        )

        // Different provider - should not match pattern
        val request = Request(
            provider = "mixpanel",
            endpoint = "https://api.mixpanel.com/track",
            payload = """{"event":"test"}"""
        )

        val result = session.evaluateSamplingDecision(request)
        // Session is sampled, no pattern matched
        assertIs<SamplingResult.Include>(result)
        assertEquals(SamplingMode.SESSION_SAMPLED_NO_PATTERN, result.samplingMode)
        assertEquals(10, result.effectiveSampleRate)
    }

    // =============================================================
    // Multiple Patterns (Priority / First Match Wins)
    // =============================================================

    @Test
    fun testMultiplePatternsWithPriority() {
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            patterns = listOf(
                """
                {
                    "provider": "amplitude",
                    "match": {"event_type": "session_start", "plan": "premium"},
                    "sample_rate": 1
                }
                """.trimIndent(),
                """
                {
                    "provider": "amplitude",
                    "match": {"event_type": "session_start"},
                    "sample_rate": 5
                }
                """.trimIndent()
            )
        )

        // First pattern should match (more specific)
        val request1 = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"session_start","plan":"premium"}"""
        )

        val result1 = session.evaluateSamplingDecision(request1)
        assertIs<SamplingResult.Include>(result1)
        assertEquals(1, result1.effectiveSampleRate)

        // Second pattern should match (less specific)
        val request2 = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"session_start","plan":"free"}"""
        )

        val result2 = session.evaluateSamplingDecision(request2)
        assertIs<SamplingResult.Include>(result2)
        assertEquals(5, result2.effectiveSampleRate)
    }

    // =============================================================
    // Complex Patterns (Boolean Logic: and/or/not)
    // =============================================================

    @Test
    fun testComplexRealWorldPattern() {
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            patterns = listOf(
                """
                {
                    "provider": "mixpanel",
                    "match": {
                        "and": [
                            {
                                "or": [
                                    {"event": "App Install"},
                                    {"event": "App Open"}
                                ]
                            },
                            {
                                "or": [
                                    {"platform": "iOS"},
                                    {"platform": "Android"}
                                ]
                            },
                            {
                                "not": {
                                    "or": [
                                        {"debug": "true"},
                                        {"internal_user": "true"}
                                    ]
                                }
                            }
                        ]
                    },
                    "sample_rate": 5
                }
                """.trimIndent()
            )
        )

        // Should match: (App Install) AND (iOS) AND NOT (debug OR internal_user)
        val request = Request(
            provider = "mixpanel",
            endpoint = "https://api.mixpanel.com/track",
            payload = """{"event":"App Install","platform":"iOS","debug":"false"}"""
        )

        val result = session.evaluateSamplingDecision(request)
        assertIs<SamplingResult.Include>(result)
        assertEquals(5, result.effectiveSampleRate)
    }

    // =============================================================
    // Data Format Matching (GA Batching, Query Params, Special Keys)
    // =============================================================

    @Test
    fun testGoogleAnalyticsBatchingFormat() {
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            patterns = listOf(
                """
                {
                    "provider": "googleanalytics",
                    "match": {"en": "Scroll"},
                    "sample_rate": 1
                }
                """.trimIndent()
            )
        )

        // Google Analytics batching format: newline-separated query strings
        val request = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect",
            payload = "en=Scroll&param1=value1\nen=Click&param2=value2"
        )

        val result = session.evaluateSamplingDecision(request)
        assertIs<SamplingResult.Include>(result)
        assertEquals(1, result.effectiveSampleRate)
    }

    @Test
    fun testEndpointQueryParamsMatching() {
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            patterns = listOf(
                """
                {
                    "provider": "segment",
                    "match": {"api_key": "test_key_123"},
                    "sample_rate": 2
                }
                """.trimIndent()
            )
        )

        // Match based on endpoint query parameter
        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track?api_key=test_key_123",
            payload = """{"event":"test"}"""
        )

        val result = session.evaluateSamplingDecision(request)
        assertIs<SamplingResult.Include>(result)
        assertEquals(2, result.effectiveSampleRate)
    }

    @Test
    fun testSpecialKeysMatching() {
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            patterns = listOf(
                """
                {
                    "provider": "segment",
                    "match": {
                        "@TP_ENDPOINT_PATH@CONTAINS": "/track",
                        "event_name@CONTAINS": "purchase"
                    },
                    "sample_rate": 1
                }
                """.trimIndent()
            )
        )

        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track",
            payload = """{"event_name":"successful_purchase"}"""
        )

        val result = session.evaluateSamplingDecision(request)
        assertIs<SamplingResult.Include>(result)
        assertEquals(1, result.effectiveSampleRate)
    }

    // =============================================================
    // Exception Safety (iOS Compatibility)
    // =============================================================
    // These tests verify that evaluateSamplingDecision never throws exceptions,
    // which is critical for iOS where uncaught Kotlin exceptions crash the app.

    @Test
    fun testNoExceptionWithAllMalformedPatterns() {
        // All patterns are invalid JSON - should not throw
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            patterns = listOf(
                """{invalid json""",
                """not json at all""",
                """{""",
                """}""",
                ""
            )
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"session_start"}"""
        )

        // Should not throw - returns result with no pattern matched
        val result = session.evaluateSamplingDecision(request)
        assertIs<SamplingResult.Include>(result)
        assertEquals(SamplingMode.SESSION_SAMPLED_NO_PATTERN, result.samplingMode)
    }

    @Test
    fun testNoExceptionWithMalformedPayload() {
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            patterns = listOf(
                """
                {
                    "provider": "amplitude",
                    "match": {"event_type": "session_start"},
                    "sample_rate": 5
                }
                """.trimIndent()
            )
        )

        // Malformed JSON payload - should not throw
        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{invalid payload"""
        )

        val result = session.evaluateSamplingDecision(request)
        assertIs<SamplingResult.Include>(result)
    }

    @Test
    fun testNoExceptionWithMalformedEndpoint() {
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            patterns = listOf(
                """
                {
                    "provider": "amplitude",
                    "match": {"@TP_ENDPOINT_PATH@CONTAINS": "/track"},
                    "sample_rate": 5
                }
                """.trimIndent()
            )
        )

        // Malformed URL - should not throw
        val request = Request(
            provider = "amplitude",
            endpoint = "not a valid url at all ://???",
            payload = """{"event_type":"session_start"}"""
        )

        val result = session.evaluateSamplingDecision(request)
        assertIs<SamplingResult.Include>(result)
    }

    @Test
    fun testNoExceptionWithEmptyInputs() {
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            patterns = listOf(
                """
                {
                    "provider": "amplitude",
                    "match": {"event_type": "session_start"},
                    "sample_rate": 5
                }
                """.trimIndent()
            )
        )

        // All empty strings - should not throw
        val request = Request(
            provider = "",
            endpoint = "",
            payload = ""
        )

        val result = session.evaluateSamplingDecision(request)
        assertIs<SamplingResult.Include>(result)
    }

    @Test
    fun testNoExceptionWithEmptyPatterns() {
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            patterns = emptyList()
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"session_start"}"""
        )

        val result = session.evaluateSamplingDecision(request)
        assertIs<SamplingResult.Include>(result)
        assertEquals(SamplingMode.SESSION_SAMPLED_NO_PATTERN, result.samplingMode)
    }

    @Test
    fun testNoExceptionWithBinaryPayload() {
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            patterns = listOf(
                """
                {
                    "provider": "amplitude",
                    "match": {"event_type": "session_start"},
                    "sample_rate": 5
                }
                """.trimIndent()
            )
        )

        // Binary-like payload with null bytes, control characters, and non-printable data
        val binaryPayload = buildString {
            append('\u0000')  // null byte
            append('\u0001')  // SOH
            append('\u001F')  // unit separator
            append('\u007F')  // DEL
            append("some text in between")
            append('\u0000')
            append('\uFFFF')  // max unicode
            append('\u200B')  // zero-width space
        }

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = binaryPayload
        )

        // Should not throw - binary data won't match patterns but shouldn't crash
        val result = session.evaluateSamplingDecision(request)
        assertIs<SamplingResult.Include>(result)
    }

    @Test
    fun testNoExceptionWithBinaryEndpoint() {
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            patterns = listOf(
                """
                {
                    "provider": "amplitude",
                    "match": {"@TP_ENDPOINT_PATH@CONTAINS": "/track"},
                    "sample_rate": 5
                }
                """.trimIndent()
            )
        )

        // Binary-like endpoint with control characters
        val binaryEndpoint = "https://api.amplitude.com/\u0000track\u0001?key=\u007Fvalue"

        val request = Request(
            provider = "amplitude",
            endpoint = binaryEndpoint,
            payload = """{"event_type":"session_start"}"""
        )

        // Should not throw
        val result = session.evaluateSamplingDecision(request)
        assertIs<SamplingResult.Include>(result)
    }

    // =============================================================
    // Edge Cases (Malformed Patterns)
    // =============================================================

    @Test
    fun testMalformedPatternIsSkipped() {
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            patterns = listOf(
                """{invalid json""",  // Malformed - should be skipped
                """
                {
                    "provider": "amplitude",
                    "match": {"event_type": "session_start"},
                    "sample_rate": 5
                }
                """.trimIndent()
            )
        )

        // Should still match with the valid pattern
        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"session_start"}"""
        )

        val result = session.evaluateSamplingDecision(request)
        assertIs<SamplingResult.Include>(result)
        assertEquals(5, result.effectiveSampleRate)
    }

    // =============================================================
    // Tracking Disabled (Session Rate Zero)
    // =============================================================

    @Test
    fun testSessionRateZeroReturnsTrackingDisabled() {
        val session = createSession(
            sampleRate = 0,  // Tracking completely disabled
            trackingEnabled = false,
            patterns = listOf(
                """
                {
                    "provider": "amplitude",
                    "match": {"event_type": "critical_event"},
                    "sample_rate": 1
                }
                """.trimIndent()
            )
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"critical_event"}"""
        )

        val result = session.evaluateSamplingDecision(request)

        // Even with matching pattern, tracking disabled means no sampling
        assertIs<SamplingResult.Drop>(result)
        assertEquals(DropReason.TRACKING_DISABLED, result.reason)
    }

    // =============================================================
    // Adaptive Sampling Disabled
    // =============================================================

    @Test
    fun testAdaptiveSamplingDisabled() {
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = true,
            useAdaptiveSampling = false,
            patterns = listOf(
                """
                {
                    "provider": "amplitude",
                    "match": {"event_type": "session_start"},
                    "sample_rate": 5
                }
                """.trimIndent()
            )
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"session_start"}"""
        )

        val result = session.evaluateSamplingDecision(request)
        // Session is sampled but no pattern matched (adaptive sampling disabled)
        assertIs<SamplingResult.Include>(result)
        assertEquals(SamplingMode.DEFAULT, result.samplingMode)
        assertEquals(10, result.effectiveSampleRate)
    }

    // =============================================================
    // Unsampled Session Rescue (Statistical Test)
    // =============================================================

    @Test
    fun testUnsampledSessionWithMatchingPattern() {
        // Session NOT sampled, but has matching pattern - should try to rescue
        val session = createSession(
            sampleRate = 10,
            trackingEnabled = false,  // Session not sampled
            patterns = listOf(
                """
                {
                    "provider": "amplitude",
                    "match": {"event_type": "critical_event"},
                    "sample_rate": 2
                }
                """.trimIndent()
            )
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"critical_event"}"""
        )

        // Run multiple times to check probability-based rescue
        var rescuedCount = 0
        val iterations = 1000
        repeat(iterations) {
            val result = session.evaluateSamplingDecision(request)
            if (result is SamplingResult.Include) {
                rescuedCount++
                assertEquals(SamplingMode.EVENT_RESCUED_BY_ADAPTIVE, result.samplingMode)
                assertEquals(2, result.effectiveSampleRate)
            }
        }

        // Rescue probability = (1/2 - 1/10) / (1 - 1/10) = 0.4/0.9 ≈ 0.444
        // Allow for statistical variance
        val expectedRescueRate = 0.444
        val actualRescueRate = rescuedCount.toDouble() / iterations
        assertTrue(
            actualRescueRate > expectedRescueRate - 0.1 && actualRescueRate < expectedRescueRate + 0.1,
            "Rescue rate $actualRescueRate should be close to $expectedRescueRate"
        )
    }

    // =============================================================
    // Helper Functions
    // =============================================================

    private fun createSession(
        sampleRate: Int = 10,
        trackingEnabled: Boolean = true,
        useAdaptiveSampling: Boolean = true,
        patterns: List<String> = emptyList()
    ): TrackingplanSession {
        return TrackingplanSession.newSession(
            samplingRate = sampleRate,
            trackingEnabled = trackingEnabled,
            samplingOptions = SamplingOptions(
                useAdaptiveSampling = useAdaptiveSampling,
                adaptiveSamplingPatterns = patterns
            )
        )
    }
}
