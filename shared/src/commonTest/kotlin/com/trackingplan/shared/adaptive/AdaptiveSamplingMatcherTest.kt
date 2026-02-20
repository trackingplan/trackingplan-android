// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared.adaptive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdaptiveSamplingMatcherTest {

    @Test
    fun testMatchExactValue() {
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.Fields(
                mapOf("event_type" to MatchValue.Single("session_start"))
            ),
            sampleRate = 5
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"session_start","user_id":"123"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertTrue(result.matched)
        assertEquals(5, result.sampleRate)
        assertEquals(pattern, result.matchedPattern)
    }

    @Test
    fun testMatchArrayValues() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.Fields(
                mapOf("plan" to MatchValue.Multiple(listOf("premium", "enterprise")))
            ),
            sampleRate = 2
        )

        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track",
            payload = """{"plan":"enterprise"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertTrue(result.matched)
        assertEquals(2, result.sampleRate)
    }

    @Test
    fun testMatchWithAndOperator() {
        val pattern = AdaptiveSamplingPattern(
            provider = "mixpanel",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("event" to MatchValue.Single("App Install"))),
                    MatchCondition.Fields(mapOf("platform" to MatchValue.Single("iOS")))
                )
            ),
            sampleRate = 3
        )

        val request = Request(
            provider = "mixpanel",
            endpoint = "https://api.mixpanel.com/track",
            payload = """{"event":"App Install","platform":"iOS"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertTrue(result.matched)
        assertEquals(3, result.sampleRate)
    }

    @Test
    fun testMatchWithOrOperator() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.Or(
                listOf(
                    MatchCondition.Fields(mapOf("event_name" to MatchValue.Single("page_view"))),
                    MatchCondition.Fields(mapOf("event_name" to MatchValue.Single("screen_view")))
                )
            ),
            sampleRate = 4
        )

        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track",
            payload = """{"event_name":"screen_view"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertTrue(result.matched)
    }

    @Test
    fun testMatchWithNotOperator() {
        val pattern = AdaptiveSamplingPattern(
            provider = "googleanalytics",
            match = MatchCondition.Not(
                MatchCondition.Fields(mapOf("event_name" to MatchValue.Single("test")))
            ),
            sampleRate = 1
        )

        val request = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect",
            payload = """{"event_name":"purchase"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertTrue(result.matched)
    }

    @Test
    fun testMatchEndpointPathContains() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.Fields(
                mapOf("@TP_ENDPOINT_PATH@CONTAINS" to MatchValue.Single("/track"))
            ),
            sampleRate = 2
        )

        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track?api_key=123",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertTrue(result.matched)
    }

    @Test
    fun testMatchEndpointOrPayloadContains() {
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.Fields(
                mapOf("@TP_ENDPOINT_OR_PAYLOAD@CONTAINS" to MatchValue.Single("purchase"))
            ),
            sampleRate = 1
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event":"purchase","amount":100}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertTrue(result.matched)
    }

    @Test
    fun testMatchAnyKey() {
        val pattern = AdaptiveSamplingPattern(
            provider = "mixpanel",
            match = MatchCondition.Fields(
                mapOf("@TP_ANY_KEY" to MatchValue.Single("premium"))
            ),
            sampleRate = 5
        )

        val request = Request(
            provider = "mixpanel",
            endpoint = "https://api.mixpanel.com/track",
            payload = """{"user":{"plan":"premium"},"event":"test"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertTrue(result.matched)
    }

    @Test
    fun testMatchFieldContains() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.Fields(
                mapOf("event_name@CONTAINS" to MatchValue.Single("purchase"))
            ),
            sampleRate = 2
        )

        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track",
            payload = """{"event_name":"successful_purchase"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertTrue(result.matched)
    }

    @Test
    fun testNoMatchReturnsNonMatchedResult() {
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.Fields(
                mapOf("event_type" to MatchValue.Single("session_end"))
            ),
            sampleRate = 5
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"session_start"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertFalse(result.matched)
        assertNull(result.sampleRate)
        assertNull(result.matchedPattern)
    }

    @Test
    fun testFirstMatchingRuleWins() {
        val patterns = listOf(
            AdaptiveSamplingPattern(
                provider = "amplitude",
                match = MatchCondition.Fields(
                    mapOf("event_type" to MatchValue.Single("session_start"))
                ),
                sampleRate = 5
            ),
            AdaptiveSamplingPattern(
                provider = "amplitude",
                match = MatchCondition.Fields(
                    mapOf("event_type" to MatchValue.Single("session_start"))
                ),
                sampleRate = 10
            )
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"session_start"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, patterns)

        assertTrue(result.matched)
        assertEquals(5, result.sampleRate)  // First pattern should match
    }

    @Test
    fun testPatternWithoutMatchConditionAlwaysMatches() {
        val pattern = AdaptiveSamplingPattern(
            provider = "hotjar",
            match = null,
            sampleRate = 10
        )

        val request = Request(
            provider = "hotjar",
            endpoint = "https://api.hotjar.com/track",
            payload = """{"event":"anything"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertTrue(result.matched)
        assertEquals(10, result.sampleRate)
    }

    @Test
    fun testNoMatchingProviderReturnsNonMatchedResult() {
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = null,
            sampleRate = 5
        )

        val request = Request(
            provider = "mixpanel",
            endpoint = "https://api.mixpanel.com/track",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertFalse(result.matched)
    }

    @Test
    fun testComplexNestedConditions() {
        val pattern = AdaptiveSamplingPattern(
            provider = "mixpanel",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Or(
                        listOf(
                            MatchCondition.Fields(mapOf("event" to MatchValue.Single("App Install"))),
                            MatchCondition.Fields(mapOf("event" to MatchValue.Single("App Open")))
                        )
                    ),
                    MatchCondition.Not(
                        MatchCondition.Fields(mapOf("debug" to MatchValue.Single("true")))
                    )
                )
            ),
            sampleRate = 5
        )

        val request = Request(
            provider = "mixpanel",
            endpoint = "https://api.mixpanel.com/track",
            payload = """{"event":"App Install","debug":"false"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertTrue(result.matched)
        assertEquals(5, result.sampleRate)
    }

    @Test
    fun testRealWorldFacebookPattern() {
        val pattern = AdaptiveSamplingPattern(
            provider = "facebook",
            match = MatchCondition.Fields(
                mapOf(
                    "event_name" to MatchValue.Multiple(listOf("Purchase", "CompleteRegistration")),
                    "currency" to MatchValue.Multiple(listOf("USD", "EUR", "GBP"))
                )
            ),
            sampleRate = 1
        )

        val request = Request(
            provider = "facebook",
            endpoint = "https://facebook.com/tr/",
            payload = """{"event_name":"Purchase","currency":"USD","value":100}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertTrue(result.matched)
        assertEquals(1, result.sampleRate)
    }

    @Test
    fun testMultipleFieldsMustAllMatch() {
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.Fields(
                mapOf(
                    "event_type" to MatchValue.Single("session_start"),
                    "plan" to MatchValue.Single("premium")
                )
            ),
            sampleRate = 2
        )

        // Missing "plan" field - should not match
        val request1 = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"session_start"}"""
        )

        val result1 = AdaptiveSamplingMatcher.matchRequest(request1, listOf(pattern))
        assertFalse(result1.matched)

        // Has both fields - should match
        val request2 = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"session_start","plan":"premium"}"""
        )

        val result2 = AdaptiveSamplingMatcher.matchRequest(request2, listOf(pattern))
        assertTrue(result2.matched)
    }

    @Test
    fun testMatchWithEndpointQueryParams() {
        val pattern = AdaptiveSamplingPattern(
            provider = "googleanalytics",
            match = MatchCondition.Fields(
                mapOf("tid" to MatchValue.Single("UA-123456"))
            ),
            sampleRate = 2
        )

        val request = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect?tid=UA-123456&en=Scroll",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertTrue(result.matched)
    }

    @Test
    fun testSpecialKeysWithNoPayload() {
        // Test Issue #3: Pattern with special key should match even when there are
        // no query params and no payload (empty variations list)
        val pattern = AdaptiveSamplingPattern(
            provider = "test_provider",
            match = MatchCondition.Fields(
                mapOf("@TP_ENDPOINT_PATH@CONTAINS" to MatchValue.Single("/v1/batch"))
            ),
            sampleRate = 5
        )

        // Request with matching path but NO query params and NO payload
        val request = Request(
            provider = "test_provider",
            endpoint = "https://api.example.com/v1/batch",
            payload = ""  // Empty payload
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        // Should match based on special key, even with no payload
        assertTrue(result.matched)
        assertEquals(5, result.sampleRate)
    }

    @Test
    fun testEmptyAndConditionReturnsTrue() {
        // Empty AND condition should return true (vacuous truth)
        // This documents the mathematically correct behavior where
        // "all conditions in an empty set are satisfied"
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.And(emptyList()),
            sampleRate = 10
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"session_start"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        // Should match because empty AND is vacuously true
        assertTrue(result.matched)
        assertEquals(10, result.sampleRate)
    }

    @Test
    fun testRealWorldGoogleAnalyticsPattern() {
        // Real-world pattern: Match specific tid, exclude certain tids, and require clickout event
        val pattern = AdaptiveSamplingPattern(
            provider = "googleanalytics",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("tid" to MatchValue.Single("G-FDQTHHLP9T"))),
                    MatchCondition.Not(
                        MatchCondition.Fields(mapOf("tid" to MatchValue.Multiple(listOf("G-6V6V4N23GF", "G-0HX60ZSQXP", "G-CNDDB7KNXH"))))
                    ),
                    MatchCondition.Or(
                        listOf(
                            MatchCondition.Fields(mapOf("en" to MatchValue.Single("clickout")))
                        )
                    )
                )
            ),
            sampleRate = 1
        )

        // Should match: tid matches, not in exclusion list, and en is clickout
        val request1 = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect?tid=G-FDQTHHLP9T&en=clickout",
            payload = null
        )
        val result1 = AdaptiveSamplingMatcher.matchRequest(request1, listOf(pattern))
        assertTrue(result1.matched)
        assertEquals(1, result1.sampleRate)

        // Should NOT match: tid is in exclusion list (even though en is clickout)
        val request2 = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect?tid=G-6V6V4N23GF&en=clickout",
            payload = null
        )
        val result2 = AdaptiveSamplingMatcher.matchRequest(request2, listOf(pattern))
        assertFalse(result2.matched)

        // Should NOT match: en is not clickout (OR condition fails)
        val request3 = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect?tid=G-FDQTHHLP9T&en=page_view",
            payload = null
        )
        val result3 = AdaptiveSamplingMatcher.matchRequest(request3, listOf(pattern))
        assertFalse(result3.matched)

        // Should NOT match: tid is different (first AND condition fails)
        val request4 = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect?tid=G-DIFFERENT&en=clickout",
            payload = null
        )
        val result4 = AdaptiveSamplingMatcher.matchRequest(request4, listOf(pattern))
        assertFalse(result4.matched)

        // Should NOT match: another excluded tid
        val request5 = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect?tid=G-CNDDB7KNXH&en=clickout",
            payload = null
        )
        val result5 = AdaptiveSamplingMatcher.matchRequest(request5, listOf(pattern))
        assertFalse(result5.matched)
    }

    // ============================================================================
    // Special Keys - Extended Coverage Tests
    // ============================================================================

    @Test
    fun testEndpointPathContainsSubstring() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.Fields(
                mapOf("@TP_ENDPOINT_PATH@CONTAINS" to MatchValue.Single("v1"))
            ),
            sampleRate = 3
        )

        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
        assertEquals(3, result.sampleRate)
    }

    @Test
    fun testEndpointPathContainsNoMatch() {
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.Fields(
                mapOf("@TP_ENDPOINT_PATH@CONTAINS" to MatchValue.Single("v2"))
            ),
            sampleRate = 2
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/v1/httpapi",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    @Test
    fun testEndpointPathContainsArrayWithMatch() {
        val pattern = AdaptiveSamplingPattern(
            provider = "mixpanel",
            match = MatchCondition.Fields(
                mapOf("@TP_ENDPOINT_PATH@CONTAINS" to MatchValue.Multiple(listOf("/track", "/batch")))
            ),
            sampleRate = 4
        )

        val request = Request(
            provider = "mixpanel",
            endpoint = "https://api.mixpanel.com/batch",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testEndpointPathContainsArrayWithNoMatch() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.Fields(
                mapOf("@TP_ENDPOINT_PATH@CONTAINS" to MatchValue.Multiple(listOf("/identify", "/group")))
            ),
            sampleRate = 2
        )

        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    @Test
    fun testEndpointPathContainsCaseSensitive() {
        val pattern = AdaptiveSamplingPattern(
            provider = "custom",
            match = MatchCondition.Fields(
                mapOf("@TP_ENDPOINT_PATH@CONTAINS" to MatchValue.Single("Track"))
            ),
            sampleRate = 5
        )

        // Should not match "track" (lowercase)
        val request = Request(
            provider = "custom",
            endpoint = "https://api.example.com/track",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    @Test
    fun testEndpointPathShouldNotMatchDomain() {
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.Fields(
                mapOf("@TP_ENDPOINT_PATH@CONTAINS" to MatchValue.Single("amplitude"))
            ),
            sampleRate = 3
        )

        // "amplitude" is in domain, not path - should not match
        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/httpapi",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    @Test
    fun testEndpointOrPayloadContainsMatchInEndpoint() {
        val pattern = AdaptiveSamplingPattern(
            provider = "googleanalytics",
            match = MatchCondition.Fields(
                mapOf("@TP_ENDPOINT_OR_PAYLOAD@CONTAINS" to MatchValue.Single("collect"))
            ),
            sampleRate = 2
        )

        val request = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect?v=2",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testEndpointOrPayloadContainsMatchInPayloadJson() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.Fields(
                mapOf("@TP_ENDPOINT_OR_PAYLOAD@CONTAINS" to MatchValue.Single("premium"))
            ),
            sampleRate = 4
        )

        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track",
            payload = """{"user":{"plan":"premium"},"event":"signup"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testEndpointOrPayloadContainsNoMatch() {
        val pattern = AdaptiveSamplingPattern(
            provider = "mixpanel",
            match = MatchCondition.Fields(
                mapOf("@TP_ENDPOINT_OR_PAYLOAD@CONTAINS" to MatchValue.Single("enterprise"))
            ),
            sampleRate = 3
        )

        val request = Request(
            provider = "mixpanel",
            endpoint = "https://api.mixpanel.com/track",
            payload = """{"event":"page_view","plan":"basic"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    @Test
    fun testAnyKeyNoMatch() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.Fields(
                mapOf("@TP_ANY_KEY" to MatchValue.Single("vip"))
            ),
            sampleRate = 3
        )

        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track",
            payload = """{"event":"page_view","user":"standard"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    @Test
    fun testFieldContainsSubstringInNestedProperty() {
        val pattern = AdaptiveSamplingPattern(
            provider = "mixpanel",
            match = MatchCondition.Fields(
                mapOf("event@CONTAINS" to MatchValue.Single("purchase"))
            ),
            sampleRate = 2
        )

        val request = Request(
            provider = "mixpanel",
            endpoint = "https://api.mixpanel.com/track",
            payload = """{"properties":{"event":"successful_purchase"}}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testFieldContainsArrayOfValues() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.Fields(
                mapOf("event_name@CONTAINS" to MatchValue.Multiple(listOf("click", "tap")))
            ),
            sampleRate = 4
        )

        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track",
            payload = """{"event_name":"button_click"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testFieldContainsWithAndOperator() {
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("event@CONTAINS" to MatchValue.Single("purchase"))),
                    MatchCondition.Fields(mapOf("currency" to MatchValue.Single("USD")))
                )
            ),
            sampleRate = 3
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event":"successful_purchase","currency":"USD"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testFieldContainsWithNotOperator() {
        val pattern = AdaptiveSamplingPattern(
            provider = "mixpanel",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("event@CONTAINS" to MatchValue.Single("page"))),
                    MatchCondition.Not(
                        MatchCondition.Fields(mapOf("event@CONTAINS" to MatchValue.Single("test")))
                    )
                )
            ),
            sampleRate = 2
        )

        val request = Request(
            provider = "mixpanel",
            endpoint = "https://api.mixpanel.com/track",
            payload = """{"event":"page_view"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    // ============================================================================
    // Google Analytics Batching Tests
    // ============================================================================

    @Test
    fun testGoogleAnalyticsBatchMatchInFirstLine() {
        val pattern = AdaptiveSamplingPattern(
            provider = "googleanalytics",
            match = MatchCondition.Fields(
                mapOf("en" to MatchValue.Single("click"))
            ),
            sampleRate = 3
        )

        // GA batch format: multiple events separated by newlines
        val request = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect",
            payload = """v=2&tid=UA-123456&en=click&cid=123
v=2&tid=UA-123456&en=page_view&cid=456"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
        assertEquals(3, result.sampleRate)
    }

    @Test
    fun testGoogleAnalyticsBatchMatchInSecondLine() {
        val pattern = AdaptiveSamplingPattern(
            provider = "googleanalytics",
            match = MatchCondition.Fields(
                mapOf("en" to MatchValue.Single("scroll"))
            ),
            sampleRate = 2
        )

        val request = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect",
            payload = """v=2&tid=UA-123456&en=click&cid=123
v=2&tid=UA-123456&en=scroll&cid=456"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testGoogleAnalyticsBatchNoMatchInAnyLine() {
        val pattern = AdaptiveSamplingPattern(
            provider = "googleanalytics",
            match = MatchCondition.Fields(
                mapOf("en" to MatchValue.Single("purchase"))
            ),
            sampleRate = 5
        )

        val request = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect",
            payload = """v=2&tid=UA-123456&en=click&cid=123
v=2&tid=UA-123456&en=scroll&cid=456
v=2&tid=UA-123456&en=page_view&cid=789"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    @Test
    fun testGoogleAnalyticsBatchMultipleMatches() {
        val pattern = AdaptiveSamplingPattern(
            provider = "googleanalytics",
            match = MatchCondition.Fields(
                mapOf("en" to MatchValue.Multiple(listOf("click", "scroll")))
            ),
            sampleRate = 4
        )

        // Both events in batch match - should match on first one
        val request = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect",
            payload = """v=2&tid=UA-123456&en=click&cid=123
v=2&tid=UA-123456&en=scroll&cid=456"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testGoogleAnalyticsBatchWithEndpointParams() {
        val pattern = AdaptiveSamplingPattern(
            provider = "googleanalytics",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("tid" to MatchValue.Single("UA-123456"))),
                    MatchCondition.Fields(mapOf("en" to MatchValue.Single("click")))
                )
            ),
            sampleRate = 3
        )

        // tid in endpoint, en in batch payload
        val request = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect?tid=UA-123456",
            payload = """v=2&en=click&cid=123
v=2&en=page_view&cid=456"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    // ============================================================================
    // Real-World Provider Pattern Tests
    // ============================================================================

    @Test
    fun testGoogleAnalyticsScrollEvent() {
        val pattern = AdaptiveSamplingPattern(
            provider = "googleanalytics",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Or(
                        listOf(
                            MatchCondition.Fields(mapOf("en" to MatchValue.Single("scroll"))),
                            MatchCondition.Fields(mapOf("en" to MatchValue.Single("Scroll")))
                        )
                    ),
                    MatchCondition.Not(
                        MatchCondition.Fields(mapOf("tid" to MatchValue.Single("G-TEST123")))
                    )
                )
            ),
            sampleRate = 10
        )

        val request = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect?tid=G-PROD456&en=Scroll",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
        assertEquals(10, result.sampleRate)
    }

    @Test
    fun testGoogleAnalyticsMultipleTIDExclusions() {
        val pattern = AdaptiveSamplingPattern(
            provider = "googleanalytics",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("en" to MatchValue.Single("page_view"))),
                    MatchCondition.Not(
                        MatchCondition.Fields(mapOf("tid" to MatchValue.Multiple(listOf("G-TEST1", "G-TEST2", "G-INTERNAL"))))
                    )
                )
            ),
            sampleRate = 5
        )

        // Should not match - tid is in exclusion list
        val request = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect?tid=G-TEST2&en=page_view",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    @Test
    fun testFacebookMultipleEventTypes() {
        val pattern = AdaptiveSamplingPattern(
            provider = "facebook",
            match = MatchCondition.Fields(
                mapOf("event_name" to MatchValue.Multiple(listOf("Purchase", "CompleteRegistration", "Lead", "AddToCart")))
            ),
            sampleRate = 1
        )

        val request = Request(
            provider = "facebook",
            endpoint = "https://www.facebook.com/tr/",
            payload = """{"event_name":"Lead","value":50}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
        assertEquals(1, result.sampleRate)
    }

    @Test
    fun testFacebookWithCurrencyFilter() {
        val pattern = AdaptiveSamplingPattern(
            provider = "facebook",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("event_name" to MatchValue.Single("Purchase"))),
                    MatchCondition.Fields(mapOf("currency" to MatchValue.Multiple(listOf("USD", "EUR", "GBP"))))
                )
            ),
            sampleRate = 2
        )

        val request = Request(
            provider = "facebook",
            endpoint = "https://www.facebook.com/tr/",
            payload = """{"event_name":"Purchase","currency":"GBP","value":99.99}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testFacebookPageViewExcluded() {
        val pattern = AdaptiveSamplingPattern(
            provider = "facebook",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("event_name" to MatchValue.Multiple(listOf("Purchase", "Lead")))),
                    MatchCondition.Not(
                        MatchCondition.Fields(mapOf("test_mode" to MatchValue.Single("true")))
                    )
                )
            ),
            sampleRate = 1
        )

        // PageView not in the list - should not match
        val request = Request(
            provider = "facebook",
            endpoint = "https://www.facebook.com/tr/",
            payload = """{"event_name":"PageView"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    @Test
    fun testMixpanelAppInstallEvents() {
        val pattern = AdaptiveSamplingPattern(
            provider = "mixpanel",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Or(
                        listOf(
                            MatchCondition.Fields(mapOf("event" to MatchValue.Single("App Install"))),
                            MatchCondition.Fields(mapOf("event" to MatchValue.Single("App Open")))
                        )
                    ),
                    MatchCondition.Fields(mapOf("platform" to MatchValue.Multiple(listOf("iOS", "Android"))))
                )
            ),
            sampleRate = 5
        )

        val request = Request(
            provider = "mixpanel",
            endpoint = "https://api.mixpanel.com/track",
            payload = """{"event":"App Install","platform":"iOS"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
        assertEquals(5, result.sampleRate)
    }

    @Test
    fun testMixpanelWithDebugExclusion() {
        val pattern = AdaptiveSamplingPattern(
            provider = "mixpanel",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("event" to MatchValue.Single("Purchase"))),
                    MatchCondition.Not(
                        MatchCondition.Fields(mapOf("debug" to MatchValue.Single("true")))
                    )
                )
            ),
            sampleRate = 3
        )

        // Should not match - debug is true
        val request = Request(
            provider = "mixpanel",
            endpoint = "https://api.mixpanel.com/track",
            payload = """{"event":"Purchase","debug":"true"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    @Test
    fun testSegmentPageViewWithCategory() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Or(
                        listOf(
                            MatchCondition.Fields(mapOf("event_name" to MatchValue.Single("page_view"))),
                            MatchCondition.Fields(mapOf("event_name" to MatchValue.Single("screen_view")))
                        )
                    ),
                    MatchCondition.Fields(mapOf("category" to MatchValue.Multiple(listOf("ecommerce", "retail"))))
                )
            ),
            sampleRate = 2
        )

        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track",
            payload = """{"event_name":"screen_view","category":"retail"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testSegmentWithTestEmailExclusion() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("event_name" to MatchValue.Single("signup"))),
                    MatchCondition.Not(
                        MatchCondition.Fields(mapOf("email@CONTAINS" to MatchValue.Single("test")))
                    )
                )
            ),
            sampleRate = 4
        )

        // Should not match - email contains "test"
        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track",
            payload = """{"event_name":"signup","email":"test@company.com"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    @Test
    fun testGoogleAdsConversionWithLabel() {
        val pattern = AdaptiveSamplingPattern(
            provider = "googleads",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("google_conversion_id" to MatchValue.Single("123456789"))),
                    MatchCondition.Fields(mapOf("google_conversion_label" to MatchValue.Single("AbCdEf")))
                )
            ),
            sampleRate = 1
        )

        val request = Request(
            provider = "googleads",
            endpoint = "https://www.googleadservices.com/pagead/conversion/",
            payload = """{"google_conversion_id":"123456789","google_conversion_label":"AbCdEf"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testGoogleAdsMissingLabel() {
        val pattern = AdaptiveSamplingPattern(
            provider = "googleads",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("google_conversion_id" to MatchValue.Single("123456789"))),
                    MatchCondition.Fields(mapOf("google_conversion_label" to MatchValue.Single("AbCdEf")))
                )
            ),
            sampleRate = 1
        )

        // Missing label - should not match
        val request = Request(
            provider = "googleads",
            endpoint = "https://www.googleadservices.com/pagead/conversion/",
            payload = """{"google_conversion_id":"123456789"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    @Test
    fun testAmplitudeSessionEvents() {
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("event_type" to MatchValue.Multiple(listOf("session_start", "session_end")))),
                    MatchCondition.Fields(mapOf("platform" to MatchValue.Single("iOS")))
                )
            ),
            sampleRate = 10
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/2/httpapi",
            payload = """{"event_type":"session_start","platform":"iOS"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testTikTokCompletePayment() {
        val pattern = AdaptiveSamplingPattern(
            provider = "tiktok",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("event" to MatchValue.Single("CompletePayment"))),
                    MatchCondition.Fields(mapOf("currency" to MatchValue.Multiple(listOf("USD", "EUR"))))
                )
            ),
            sampleRate = 2
        )

        val request = Request(
            provider = "tiktok",
            endpoint = "https://analytics.tiktok.com/api/v2/pixel/track",
            payload = """{"event":"CompletePayment","currency":"USD","value":49.99}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testPinterestCheckoutEvent() {
        val pattern = AdaptiveSamplingPattern(
            provider = "pinterest",
            match = MatchCondition.Fields(
                mapOf("event_name" to MatchValue.Multiple(listOf("checkout", "Checkout", "purchase", "Purchase")))
            ),
            sampleRate = 3
        )

        val request = Request(
            provider = "pinterest",
            endpoint = "https://ct.pinterest.com/v3/",
            payload = """{"event_name":"Purchase"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    // ============================================================================
    // Edge Case Tests
    // ============================================================================

    @Test
    fun testNestedFieldMatch() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.Fields(
                mapOf("plan" to MatchValue.Single("premium"))
            ),
            sampleRate = 5
        )

        // Field is nested in user.subscription.plan
        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track",
            payload = """{"user":{"subscription":{"plan":"premium"}}}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testDeeplyNestedFieldMatch() {
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.Fields(
                mapOf("country" to MatchValue.Single("US"))
            ),
            sampleRate = 3
        )

        // Field is deeply nested: user.location.address.country
        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"user":{"location":{"address":{"country":"US"}}}}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testMatchInArrayElement() {
        val pattern = AdaptiveSamplingPattern(
            provider = "mixpanel",
            match = MatchCondition.Fields(
                mapOf("item_id" to MatchValue.Single("PROD-123"))
            ),
            sampleRate = 4
        )

        // item_id is in array element
        val request = Request(
            provider = "mixpanel",
            endpoint = "https://api.mixpanel.com/track",
            payload = """{"items":[{"item_id":"PROD-456"},{"item_id":"PROD-123"}]}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testNullPayload() {
        val pattern = AdaptiveSamplingPattern(
            provider = "hotjar",
            match = MatchCondition.Fields(
                mapOf("event" to MatchValue.Single("session_start"))
            ),
            sampleRate = 5
        )

        // null payload - should not match (no data to match against)
        val request = Request(
            provider = "hotjar",
            endpoint = "https://api.hotjar.com/track",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    @Test
    fun testSpecialCharactersInPath() {
        val pattern = AdaptiveSamplingPattern(
            provider = "custom",
            match = MatchCondition.Fields(
                mapOf("@TP_ENDPOINT_PATH@CONTAINS" to MatchValue.Single("track-v2"))
            ),
            sampleRate = 3
        )

        val request = Request(
            provider = "custom",
            endpoint = "https://api.example.com/track-v2/events?foo=bar",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testNumericValuesInAnyKey() {
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.Fields(
                mapOf("@TP_ANY_KEY" to MatchValue.Single("12345"))
            ),
            sampleRate = 2
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"user_id":"12345","event":"login"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testDuplicateKeyInEndpointAndPayload() {
        val pattern = AdaptiveSamplingPattern(
            provider = "googleanalytics",
            match = MatchCondition.Fields(
                mapOf("tid" to MatchValue.Single("UA-123456"))
            ),
            sampleRate = 3
        )

        // tid appears in both endpoint and payload - should match (OR logic)
        val request = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/collect?tid=UA-123456",
            payload = """tid=UA-999999&en=page_view"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testDuplicateKeyAtDifferentNestingLevels() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.Fields(
                mapOf("status" to MatchValue.Single("active"))
            ),
            sampleRate = 4
        )

        // status appears at multiple nesting levels - flattening should find it
        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track",
            payload = """{"status":"pending","user":{"status":"active"}}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testEmptyOrCondition() {
        // Empty OR condition should return false (no conditions to satisfy)
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.Or(emptyList()),
            sampleRate = 10
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"session_start"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        // Empty OR should return false (no alternatives satisfied)
        assertFalse(result.matched)
    }

    @Test
    fun testArrayElementMatchingWithMultipleItems() {
        val pattern = AdaptiveSamplingPattern(
            provider = "facebook",
            match = MatchCondition.Fields(
                mapOf("content_id" to MatchValue.Multiple(listOf("ITEM-001", "ITEM-002")))
            ),
            sampleRate = 2
        )

        // Array contains one of the target IDs
        val request = Request(
            provider = "facebook",
            endpoint = "https://www.facebook.com/tr/",
            payload = """{"contents":[{"content_id":"ITEM-999"},{"content_id":"ITEM-002"},{"content_id":"ITEM-888"}]}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    // ============================================================================
    // Additional @CONTAINS Suffix Edge Cases
    // ============================================================================

    @Test
    fun testFieldContainsInQueryParameters() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.Fields(
                mapOf("category@CONTAINS" to MatchValue.Single("shop"))
            ),
            sampleRate = 3
        )

        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track?category=shopping&source=web",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testFieldContainsInDeeplyNestedObject() {
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.Fields(
                mapOf("user_type@CONTAINS" to MatchValue.Single("prem"))
            ),
            sampleRate = 4
        )

        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"user":{"subscription":{"user_type":"premium_user"}}}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testFieldContainsInArrayElement() {
        val pattern = AdaptiveSamplingPattern(
            provider = "mixpanel",
            match = MatchCondition.Fields(
                mapOf("item_name@CONTAINS" to MatchValue.Single("Laptop"))
            ),
            sampleRate = 5
        )

        val request = Request(
            provider = "mixpanel",
            endpoint = "https://api.mixpanel.com/track",
            payload = """{"items":[{"item_name":"MacBook Laptop"},{"item_name":"iPad"}]}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testFieldContainsCaseSensitive() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.Fields(
                mapOf("event@CONTAINS" to MatchValue.Single("PUR"))
            ),
            sampleRate = 2
        )

        // "PUR" (uppercase) should not match "purchase" (lowercase)
        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track",
            payload = """{"event":"purchase"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    @Test
    fun testFieldContainsEmptySubstringMatchesEverything() {
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.Fields(
                mapOf("event@CONTAINS" to MatchValue.Single(""))
            ),
            sampleRate = 3
        )

        // Empty substring should match any value
        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event":"purchase"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testFieldContainsCombinedWithRegularKeyNoMatch() {
        val pattern = AdaptiveSamplingPattern(
            provider = "facebook",
            match = MatchCondition.Fields(
                mapOf(
                    "event@CONTAINS" to MatchValue.Single("pur"),
                    "currency" to MatchValue.Single("EUR")
                )
            ),
            sampleRate = 4
        )

        // event contains "pur" but currency doesn't match - implicit AND should fail
        val request = Request(
            provider = "facebook",
            endpoint = "https://www.facebook.com/tr/",
            payload = """{"event":"purchase","currency":"USD"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    // ============================================================================
    // Combined Special Keys with Operators
    // ============================================================================

    @Test
    fun testFieldContainsWithOrOperator() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.Or(
                listOf(
                    MatchCondition.Fields(mapOf("event@CONTAINS" to MatchValue.Single("pur"))),
                    MatchCondition.Fields(mapOf("event@CONTAINS" to MatchValue.Single("add")))
                )
            ),
            sampleRate = 3
        )

        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/v1/track",
            payload = """{"event":"add_to_cart"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testEndpointPathContainsAndRegularKeyNoMatchOnPath() {
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("@TP_ENDPOINT_PATH@CONTAINS" to MatchValue.Single("/api/events"))),
                    MatchCondition.Fields(mapOf("event_type" to MatchValue.Single("purchase")))
                )
            ),
            sampleRate = 2
        )

        // Path doesn't contain "/api/events" - should not match even though event_type matches
        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/api/track",
            payload = """{"event_type":"purchase","amount":100}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    @Test
    fun testEndpointPathContainsAndRegularKeyNoMatchOnKey() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("@TP_ENDPOINT_PATH@CONTAINS" to MatchValue.Single("/api/events"))),
                    MatchCondition.Fields(mapOf("event_type" to MatchValue.Single("purchase")))
                )
            ),
            sampleRate = 2
        )

        // Path matches but event_type doesn't - should not match
        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/api/events",
            payload = """{"event_type":"checkout","amount":100}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    @Test
    fun testEndpointOrPayloadContainsOrAnyKey() {
        val pattern = AdaptiveSamplingPattern(
            provider = "mixpanel",
            match = MatchCondition.Or(
                listOf(
                    MatchCondition.Fields(mapOf("@TP_ENDPOINT_OR_PAYLOAD@CONTAINS" to MatchValue.Single("checkout"))),
                    MatchCondition.Fields(mapOf("@TP_ANY_KEY" to MatchValue.Single("premium")))
                )
            ),
            sampleRate = 4
        )

        // Doesn't contain "checkout" but has "premium" - should match via OR
        val request = Request(
            provider = "mixpanel",
            endpoint = "https://api.mixpanel.com/track",
            payload = """{"plan":"premium","event":"login"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testEndpointPathContainsAndEndpointOrPayloadContains() {
        val pattern = AdaptiveSamplingPattern(
            provider = "segment",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("@TP_ENDPOINT_PATH@CONTAINS" to MatchValue.Single("/checkout"))),
                    MatchCondition.Fields(mapOf("@TP_ENDPOINT_OR_PAYLOAD@CONTAINS" to MatchValue.Single("complete")))
                )
            ),
            sampleRate = 5
        )

        val request = Request(
            provider = "segment",
            endpoint = "https://api.segment.io/checkout/confirm",
            payload = """status=complete&order=123"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testNotEndpointPathContainsMatch() {
        val pattern = AdaptiveSamplingPattern(
            provider = "amplitude",
            match = MatchCondition.Not(
                MatchCondition.Fields(mapOf("@TP_ENDPOINT_PATH@CONTAINS" to MatchValue.Single("/test")))
            ),
            sampleRate = 3
        )

        // Path doesn't contain "/test" - NOT should make this match
        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/api/events",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testNotEndpointPathContainsNoMatch() {
        val pattern = AdaptiveSamplingPattern(
            provider = "mixpanel",
            match = MatchCondition.Not(
                MatchCondition.Fields(mapOf("@TP_ENDPOINT_PATH@CONTAINS" to MatchValue.Single("/test")))
            ),
            sampleRate = 3
        )

        // Path contains "/test" - NOT should make this NOT match
        val request = Request(
            provider = "mixpanel",
            endpoint = "https://api.mixpanel.com/test/path",
            payload = null
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    // ============================================================================
    // Provider-Specific Variations
    // ============================================================================

    @Test
    fun testHotjarProviderOnlyRule() {
        val pattern = AdaptiveSamplingPattern(
            provider = "hotjar",
            match = null,
            sampleRate = 5
        )

        // No match condition - should match any request for this provider
        val request = Request(
            provider = "hotjar",
            endpoint = "https://ws.hotjar.com/api/v2/client/ws/identify_user",
            payload = """{"user_id":"12345"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
        assertEquals(5, result.sampleRate)
    }

    @Test
    fun testSnowplowProviderOnlyRule() {
        val pattern = AdaptiveSamplingPattern(
            provider = "snowplow",
            match = null,
            sampleRate = 10
        )

        // No match condition - should match any request for this provider
        val request = Request(
            provider = "snowplow",
            endpoint = "https://snowplow.com/collect",
            payload = """{"user_id":"12345","session_id":"abc123"}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
        assertEquals(10, result.sampleRate)
    }

    @Test
    fun testBingAdsConversionWithGoalId() {
        val pattern = AdaptiveSamplingPattern(
            provider = "bing",
            match = MatchCondition.Fields(
                mapOf("goal_id" to MatchValue.Multiple(listOf("123456", "789012")))
            ),
            sampleRate = 2
        )

        val request = Request(
            provider = "bing",
            endpoint = "https://bat.bing.com/action/0",
            payload = """{"goal_id":"123456","revenue":99.99}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertTrue(result.matched)
    }

    @Test
    fun testBingAdsConversionWrongGoalId() {
        val pattern = AdaptiveSamplingPattern(
            provider = "bing",
            match = MatchCondition.Fields(
                mapOf("goal_id" to MatchValue.Multiple(listOf("123456", "789012")))
            ),
            sampleRate = 2
        )

        // Wrong goal_id - should not match
        val request = Request(
            provider = "bing",
            endpoint = "https://bat.bing.com/action/0",
            payload = """{"goal_id":"999999","revenue":99.99}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))
        assertFalse(result.matched)
    }

    // ============================================================================
    // Firebase Android (lib-firebase) Tests
    // ============================================================================
    // The Android SDK intercepts Firebase Analytics calls at the bytecode level.
    // The payload format is a JSON object with "name" nested inside "params".
    // The config merger generates patterns using the "name" key from
    // lib-firebase_parser.get_event_name_keys(). The flattener must find "name"
    // despite it being nested.

    @Test
    fun testFirebaseAndroidLogEventV2MatchByName() {
        // Pattern as generated by config_merger for lib-firebase events
        val pattern = AdaptiveSamplingPattern(
            provider = "lib-firebase",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("method" to MatchValue.Single("logEvent"))),
                    MatchCondition.Fields(mapOf("name" to MatchValue.Single("purchase")))
                )
            ),
            sampleRate = 5
        )

        // Android SDK v2 payload: "name" is nested inside "params"
        val request = Request(
            provider = "lib-firebase",
            endpoint = "code://com.google.firebase.analytics.FirebaseAnalytics",
            payload = """{"version":2,"method":"logEvent","params":{"name":"purchase","params":{"value":"100","currency":"USD"}}}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertTrue(result.matched)
        assertEquals(5, result.sampleRate)
    }

    @Test
    fun testFirebaseAndroidLogEventV1NoMethodFieldDoesNotMatch() {
        // v1 payloads don't have "method" in JSON, so they won't match the new
        // pattern shape that requires method=logEvent. This is fine since adaptive
        // sampling is only enabled on modern SDKs (>= 1.4.1) which use v2 format.
        val pattern = AdaptiveSamplingPattern(
            provider = "lib-firebase",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("method" to MatchValue.Single("logEvent"))),
                    MatchCondition.Fields(mapOf("name" to MatchValue.Single("add_to_cart")))
                )
            ),
            sampleRate = 3
        )

        // Android SDK v1 payload: "name" is at the top level, no "method" field
        val request = Request(
            provider = "lib-firebase",
            endpoint = "code://com.google.firebase.analytics.FirebaseAnalytics",
            payload = """{"name":"add_to_cart","params":{"item_id":"SKU-123","quantity":"1"}}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertFalse(result.matched)
    }

    @Test
    fun testFirebaseAndroidNoMatchWrongEventName() {
        val pattern = AdaptiveSamplingPattern(
            provider = "lib-firebase",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("method" to MatchValue.Single("logEvent"))),
                    MatchCondition.Fields(mapOf("name" to MatchValue.Single("purchase")))
                )
            ),
            sampleRate = 5
        )

        val request = Request(
            provider = "lib-firebase",
            endpoint = "code://com.google.firebase.analytics.FirebaseAnalytics",
            payload = """{"version":2,"method":"logEvent","params":{"name":"screen_view","params":{"screen_name":"Home"}}}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertFalse(result.matched)
    }

    @Test
    fun testFirebaseAndroidMultipleEventNames() {
        // Each event name gets its own pattern with method=logEvent condition
        val patterns = listOf(
            AdaptiveSamplingPattern(
                provider = "lib-firebase",
                match = MatchCondition.And(
                    listOf(
                        MatchCondition.Fields(mapOf("method" to MatchValue.Single("logEvent"))),
                        MatchCondition.Fields(mapOf("name" to MatchValue.Single("purchase")))
                    )
                ),
                sampleRate = 2
            ),
            AdaptiveSamplingPattern(
                provider = "lib-firebase",
                match = MatchCondition.And(
                    listOf(
                        MatchCondition.Fields(mapOf("method" to MatchValue.Single("logEvent"))),
                        MatchCondition.Fields(mapOf("name" to MatchValue.Single("add_to_cart")))
                    )
                ),
                sampleRate = 2
            ),
            AdaptiveSamplingPattern(
                provider = "lib-firebase",
                match = MatchCondition.And(
                    listOf(
                        MatchCondition.Fields(mapOf("method" to MatchValue.Single("logEvent"))),
                        MatchCondition.Fields(mapOf("name" to MatchValue.Single("begin_checkout")))
                    )
                ),
                sampleRate = 2
            )
        )

        val requestPurchase = Request(
            provider = "lib-firebase",
            endpoint = "code://com.google.firebase.analytics.FirebaseAnalytics",
            payload = """{"version":2,"method":"logEvent","params":{"name":"purchase","params":{"value":"50"}}}"""
        )

        val requestCheckout = Request(
            provider = "lib-firebase",
            endpoint = "code://com.google.firebase.analytics.FirebaseAnalytics",
            payload = """{"version":2,"method":"logEvent","params":{"name":"begin_checkout","params":{}}}"""
        )

        val requestPageView = Request(
            provider = "lib-firebase",
            endpoint = "code://com.google.firebase.analytics.FirebaseAnalytics",
            payload = """{"version":2,"method":"logEvent","params":{"name":"screen_view","params":{}}}"""
        )

        assertTrue(AdaptiveSamplingMatcher.matchRequest(requestPurchase, patterns).matched)
        assertTrue(AdaptiveSamplingMatcher.matchRequest(requestCheckout, patterns).matched)
        assertFalse(AdaptiveSamplingMatcher.matchRequest(requestPageView, patterns).matched)
    }

    @Test
    fun testFirebaseAndroidCaseSensitiveEventName() {
        val pattern = AdaptiveSamplingPattern(
            provider = "lib-firebase",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("method" to MatchValue.Single("logEvent"))),
                    MatchCondition.Fields(mapOf("name" to MatchValue.Single("Purchase")))
                )
            ),
            sampleRate = 5
        )

        // Firebase event names are case-sensitive: "purchase" != "Purchase"
        val request = Request(
            provider = "lib-firebase",
            endpoint = "code://com.google.firebase.analytics.FirebaseAnalytics",
            payload = """{"version":2,"method":"logEvent","params":{"name":"purchase","params":{}}}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertFalse(result.matched)
    }

    @Test
    fun testFirebaseAndroidProviderMismatchDoesNotMatch() {
        val pattern = AdaptiveSamplingPattern(
            provider = "lib-firebase",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("method" to MatchValue.Single("logEvent"))),
                    MatchCondition.Fields(mapOf("name" to MatchValue.Single("purchase")))
                )
            ),
            sampleRate = 5
        )

        // Same payload but different provider (iOS Firebase uses "googleanalyticsfirebase")
        val request = Request(
            provider = "googleanalyticsfirebase",
            endpoint = "https://app-measurement.com/a",
            payload = """{"version":2,"method":"logEvent","params":{"name":"purchase","params":{}}}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertFalse(result.matched)
    }

    @Test
    fun testFirebaseAndroidSetUserPropertyNotMatchedByEventPattern() {
        // Pattern targets logEvent events by name — includes method=logEvent condition
        // generated by lib-firebase_parser.get_match_conditions()
        val pattern = AdaptiveSamplingPattern(
            provider = "lib-firebase",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("method" to MatchValue.Single("logEvent"))),
                    MatchCondition.Fields(mapOf("name" to MatchValue.Single("premium")))
                )
            ),
            sampleRate = 5
        )

        // setUserProperty has "name" in params but refers to a user property, not an event.
        // The method=logEvent condition prevents this from matching.
        val request = Request(
            provider = "lib-firebase",
            endpoint = "code://com.google.firebase.analytics.FirebaseAnalytics",
            payload = """{"version":2,"method":"setUserProperty","params":{"name":"premium","value":"true"}}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertFalse(result.matched)
    }

    // ============================================================================
    // Firebase iOS (googleanalyticsfirebase) Tests
    // ============================================================================
    // The iOS SDK intercepts Firebase Analytics at the network level (URLProtocol).
    // The raw payload is protobuf, so the SDK decodes it and builds a synthetic JSON
    // payload: {"events":[{"event_name":"screen_view"},{"event_name":"purchase"}]}
    // The config merger generates patterns using the "event_name" key from
    // googleanalyticsfirebase_parser.get_event_name_keys(). The flattener collects
    // all "event_name" values from the nested events array.

    @Test
    fun testFirebaseIOSSyntheticPayloadMatchesByEventName() {
        val pattern = AdaptiveSamplingPattern(
            provider = "googleanalyticsfirebase",
            match = MatchCondition.Fields(
                mapOf("event_name" to MatchValue.Single("purchase"))
            ),
            sampleRate = 5
        )

        // Synthetic payload built by the iOS SDK after decoding the protobuf
        val request = Request(
            provider = "googleanalyticsfirebase",
            endpoint = "https://app-measurement.com/a",
            payload = """{"events":[{"event_name":"purchase"}]}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertTrue(result.matched)
        assertEquals(5, result.sampleRate)
    }

    @Test
    fun testFirebaseIOSSyntheticPayloadMultipleEventsMatchIfAnyMatches() {
        val pattern = AdaptiveSamplingPattern(
            provider = "googleanalyticsfirebase",
            match = MatchCondition.Fields(
                mapOf("event_name" to MatchValue.Single("purchase"))
            ),
            sampleRate = 3
        )

        // Batch with multiple events — match if any event matches
        val request = Request(
            provider = "googleanalyticsfirebase",
            endpoint = "https://app-measurement.com/a",
            payload = """{"events":[{"event_name":"screen_view"},{"event_name":"purchase"},{"event_name":"session_start"}]}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertTrue(result.matched)
        assertEquals(3, result.sampleRate)
    }

    @Test
    fun testFirebaseIOSSyntheticPayloadNoMatchWhenEventNotInPattern() {
        val pattern = AdaptiveSamplingPattern(
            provider = "googleanalyticsfirebase",
            match = MatchCondition.Fields(
                mapOf("event_name" to MatchValue.Single("purchase"))
            ),
            sampleRate = 5
        )

        val request = Request(
            provider = "googleanalyticsfirebase",
            endpoint = "https://app-measurement.com/a",
            payload = """{"events":[{"event_name":"screen_view"},{"event_name":"session_start"}]}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertFalse(result.matched)
    }

    @Test
    fun testFirebaseIOSProviderMismatchLibFirebasePatternsDoNotMatch() {
        // lib-firebase patterns should NOT match googleanalyticsfirebase requests
        val pattern = AdaptiveSamplingPattern(
            provider = "lib-firebase",
            match = MatchCondition.And(
                listOf(
                    MatchCondition.Fields(mapOf("method" to MatchValue.Single("logEvent"))),
                    MatchCondition.Fields(mapOf("name" to MatchValue.Single("purchase")))
                )
            ),
            sampleRate = 5
        )

        val request = Request(
            provider = "googleanalyticsfirebase",
            endpoint = "https://app-measurement.com/a",
            payload = """{"events":[{"event_name":"purchase"}]}"""
        )

        val result = AdaptiveSamplingMatcher.matchRequest(request, listOf(pattern))

        assertFalse(result.matched)
    }
}
