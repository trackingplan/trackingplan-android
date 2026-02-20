// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared.adaptive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdaptiveSamplingPatternParserTest {

    @Test
    fun testParseBasicPattern() {
        val patternJson = """
            {
                "provider": "amplitude",
                "sample_rate": 5
            }
        """.trimIndent()

        val pattern = AdaptiveSamplingPatternParser.parsePattern(patternJson)

        assertNotNull(pattern)
        assertEquals("amplitude", pattern.provider)
        assertEquals(5, pattern.sampleRate)
        assertNull(pattern.match)
    }

    @Test
    fun testParsePatternWithSimpleMatch() {
        val patternJson = """
            {
                "provider": "amplitude",
                "match": {
                    "event_type": "session_start"
                },
                "sample_rate": 10
            }
        """.trimIndent()

        val pattern = AdaptiveSamplingPatternParser.parsePattern(patternJson)

        assertNotNull(pattern)
        assertEquals("amplitude", pattern.provider)
        assertEquals(10, pattern.sampleRate)
        assertNotNull(pattern.match)

        val match = pattern.match as MatchCondition.Fields
        assertEquals(1, match.fields.size)
        assertTrue(match.fields.containsKey("event_type"))

        val value = match.fields["event_type"] as MatchValue.Single
        assertEquals("session_start", value.value)
    }

    @Test
    fun testParsePatternWithArrayValues() {
        val patternJson = """
            {
                "provider": "segment",
                "match": {
                    "plan": ["premium", "enterprise"]
                },
                "sample_rate": 2
            }
        """.trimIndent()

        val pattern = AdaptiveSamplingPatternParser.parsePattern(patternJson)

        assertNotNull(pattern)
        val match = pattern.match as MatchCondition.Fields
        val value = match.fields["plan"] as MatchValue.Multiple
        assertEquals(listOf("premium", "enterprise"), value.values)
    }

    @Test
    fun testParsePatternWithAndOperator() {
        val patternJson = """
            {
                "provider": "mixpanel",
                "match": {
                    "and": [
                        {"event": "App Install"},
                        {"platform": "iOS"}
                    ]
                },
                "sample_rate": 3
            }
        """.trimIndent()

        val pattern = AdaptiveSamplingPatternParser.parsePattern(patternJson)

        assertNotNull(pattern)
        val match = pattern.match as MatchCondition.And
        assertEquals(2, match.conditions.size)

        val condition1 = match.conditions[0] as MatchCondition.Fields
        val condition2 = match.conditions[1] as MatchCondition.Fields

        assertTrue(condition1.fields.containsKey("event"))
        assertTrue(condition2.fields.containsKey("platform"))
    }

    @Test
    fun testParsePatternWithOrOperator() {
        val patternJson = """
            {
                "provider": "segment",
                "match": {
                    "or": [
                        {"event_name": "page_view"},
                        {"event_name": "screen_view"}
                    ]
                },
                "sample_rate": 4
            }
        """.trimIndent()

        val pattern = AdaptiveSamplingPatternParser.parsePattern(patternJson)

        assertNotNull(pattern)
        val match = pattern.match as MatchCondition.Or
        assertEquals(2, match.conditions.size)
    }

    @Test
    fun testParsePatternWithNotOperator() {
        val patternJson = """
            {
                "provider": "googleanalytics",
                "match": {
                    "not": {
                        "event_name": "test"
                    }
                },
                "sample_rate": 1
            }
        """.trimIndent()

        val pattern = AdaptiveSamplingPatternParser.parsePattern(patternJson)

        assertNotNull(pattern)
        val match = pattern.match as MatchCondition.Not
        val innerCondition = match.condition as MatchCondition.Fields
        assertTrue(innerCondition.fields.containsKey("event_name"))
    }

    @Test
    fun testParseNestedBooleanOperators() {
        val patternJson = """
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
                            "not": {
                                "debug": "true"
                            }
                        }
                    ]
                },
                "sample_rate": 5
            }
        """.trimIndent()

        val pattern = AdaptiveSamplingPatternParser.parsePattern(patternJson)

        assertNotNull(pattern)
        val match = pattern.match as MatchCondition.And
        assertEquals(2, match.conditions.size)

        val orCondition = match.conditions[0] as MatchCondition.Or
        assertEquals(2, orCondition.conditions.size)

        val notCondition = match.conditions[1] as MatchCondition.Not
        assertNotNull(notCondition.condition)
    }

    @Test
    fun testParsePatternWithMultipleFields() {
        val patternJson = """
            {
                "provider": "amplitude",
                "match": {
                    "event_type": "session_start",
                    "plan": "premium",
                    "region": ["us", "eu"]
                },
                "sample_rate": 2
            }
        """.trimIndent()

        val pattern = AdaptiveSamplingPatternParser.parsePattern(patternJson)

        assertNotNull(pattern)
        val match = pattern.match as MatchCondition.Fields
        assertEquals(3, match.fields.size)
        assertTrue(match.fields.containsKey("event_type"))
        assertTrue(match.fields.containsKey("plan"))
        assertTrue(match.fields.containsKey("region"))
    }

    @Test
    fun testParsePatternWithSpecialKeys() {
        val patternJson = """
            {
                "provider": "segment",
                "match": {
                    "@TP_ENDPOINT_PATH@CONTAINS": "/track",
                    "event_name@CONTAINS": "purchase"
                },
                "sample_rate": 1
            }
        """.trimIndent()

        val pattern = AdaptiveSamplingPatternParser.parsePattern(patternJson)

        assertNotNull(pattern)
        val match = pattern.match as MatchCondition.Fields
        assertTrue(match.fields.containsKey("@TP_ENDPOINT_PATH@CONTAINS"))
        assertTrue(match.fields.containsKey("event_name@CONTAINS"))
    }

    @Test
    fun testParseMalformedPatternReturnsNull() {
        val patternJson = """{"invalid": "json}"""

        val pattern = AdaptiveSamplingPatternParser.parsePattern(patternJson)

        assertNull(pattern)
    }

    @Test
    fun testParsePatternWithoutProviderReturnsNull() {
        val patternJson = """
            {
                "match": {"event": "test"},
                "sample_rate": 5
            }
        """.trimIndent()

        val pattern = AdaptiveSamplingPatternParser.parsePattern(patternJson)

        assertNull(pattern)
    }

    @Test
    fun testParsePatternDefaultsSampleRateToOne() {
        val patternJson = """
            {
                "provider": "amplitude"
            }
        """.trimIndent()

        val pattern = AdaptiveSamplingPatternParser.parsePattern(patternJson)

        assertNotNull(pattern)
        assertEquals(1, pattern.sampleRate)
    }

    @Test
    fun testParseMultiplePatterns() {
        val patterns = listOf(
            """{"provider": "amplitude", "sample_rate": 5}""",
            """{"provider": "mixpanel", "sample_rate": 10}""",
            """{"invalid": "pattern}""",  // This one should be skipped
            """{"provider": "segment", "sample_rate": 2}"""
        )

        val parsedPatterns = AdaptiveSamplingPatternParser.parsePatterns(patterns)

        assertEquals(3, parsedPatterns.size)  // Only 3 valid patterns
        assertEquals("amplitude", parsedPatterns[0].provider)
        assertEquals("mixpanel", parsedPatterns[1].provider)
        assertEquals("segment", parsedPatterns[2].provider)
    }

    @Test
    fun testParseComplexRealWorldPattern() {
        // Real-world pattern from JavaScript SDK examples
        val patternJson = """
            {
                "provider": "facebook",
                "match": {
                    "event_name": ["Purchase", "CompleteRegistration"],
                    "currency": ["USD", "EUR", "GBP"]
                },
                "sample_rate": 1
            }
        """.trimIndent()

        val pattern = AdaptiveSamplingPatternParser.parsePattern(patternJson)

        assertNotNull(pattern)
        assertEquals("facebook", pattern.provider)
        assertEquals(1, pattern.sampleRate)

        val match = pattern.match as MatchCondition.Fields
        assertEquals(2, match.fields.size)

        val eventNames = match.fields["event_name"] as MatchValue.Multiple
        assertEquals(listOf("Purchase", "CompleteRegistration"), eventNames.values)

        val currencies = match.fields["currency"] as MatchValue.Multiple
        assertEquals(listOf("USD", "EUR", "GBP"), currencies.values)
    }
}
