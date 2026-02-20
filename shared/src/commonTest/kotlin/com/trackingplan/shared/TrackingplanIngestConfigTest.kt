// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TrackingplanIngestConfigTest {

    @Test
    fun testParseBasicConfig() {
        val jsonConfig = """
            {
                "sample_rate": 1,
                "environment_rates": {
                    "PRODUCTION": 1,
                    "STAGING": 2
                }
            }
        """.trimIndent()

        val config = TrackingplanIngestConfigParser.parse(jsonConfig)

        assertNotNull(config)
        assertEquals(1, config.sampleRate)
        assertEquals(1, config.environmentRates["PRODUCTION"])
        assertEquals(2, config.environmentRates["STAGING"])
    }

    @Test
    fun testGetSamplingRateForEnvironment() {
        val jsonConfig = """
            {
                "sample_rate": 10,
                "environment_rates": {
                    "PRODUCTION": 1,
                    "STAGING": 5
                }
            }
        """.trimIndent()

        val config = TrackingplanIngestConfigParser.parse(jsonConfig)

        // Should return environment-specific rate
        assertEquals(1, config.getSamplingRate("PRODUCTION"))
        assertEquals(5, config.getSamplingRate("STAGING"))

        // Should fall back to default sample_rate for unknown environments
        assertEquals(10, config.getSamplingRate("DEVELOPMENT"))
    }

    @Test
    fun testParseFullConfig() {
        val jsonConfig = """
            {
                "sample_rate": 1,
                "environment_rates": {
                    "PRODUCTION": 1,
                    "STAGING": 2
                },
                "options": {
                    "useAdaptiveSampling": true,
                    "adaptiveSamplingPatterns": ["pattern1", "pattern2"]
                }
            }
        """.trimIndent()

        val config = TrackingplanIngestConfigParser.parse(jsonConfig)

        assertNotNull(config)
        assertEquals(1, config.sampleRate)
        assertEquals(2, config.environmentRates["STAGING"])
        assertTrue(config.isAdaptiveSamplingEnabled())
        assertEquals(2, config.options.adaptiveSamplingPatterns.size)
    }

    @Test
    fun testParseConfigWithDefaults() {
        val jsonConfig = """
            {
                "sample_rate": 5
            }
        """.trimIndent()

        val config = TrackingplanIngestConfigParser.parse(jsonConfig)

        assertNotNull(config)
        assertEquals(5, config.sampleRate)
        assertTrue(config.environmentRates.isEmpty())
        assertEquals(false, config.isAdaptiveSamplingEnabled())
    }

    @Test
    fun testParseConfigIgnoresUnknownFields() {
        val jsonConfig = """
            {
                "sample_rate": 3,
                "environment_rates": {
                    "PRODUCTION": 1
                },
                "tracksEndpoint": "https://custom.endpoint.com/",
                "privacy_config": {
                    "mode": "strict"
                },
                "unknown_field": "should be ignored"
            }
        """.trimIndent()

        val config = TrackingplanIngestConfigParser.parse(jsonConfig)

        // Should parse successfully ignoring unknown fields
        assertNotNull(config)
        assertEquals(3, config.sampleRate)
        assertEquals(1, config.getSamplingRate("PRODUCTION"))
    }

    @Test
    fun testShouldEnableTrackingAlwaysForRateOne() {
        val jsonConfig = """
            {
                "sample_rate": 1
            }
        """.trimIndent()

        val config = TrackingplanIngestConfigParser.parse(jsonConfig)

        // With rate=1, tracking should always be enabled
        repeat(10) {
            assertTrue(config.shouldEnableTracking(1))
        }
    }

    @Test
    fun testShouldEnableTrackingNeverForZeroOrNegative() {
        val jsonConfig = """
            {
                "sample_rate": 1
            }
        """.trimIndent()

        val config = TrackingplanIngestConfigParser.parse(jsonConfig)

        // With rate=0 or negative, tracking should never be enabled
        repeat(10) {
            assertEquals(false, config.shouldEnableTracking(0))
            assertEquals(false, config.shouldEnableTracking(-1))
        }
    }

    @Test
    fun testShouldEnableTrackingWithEnvironment() {
        // Test rate=1 always enables regardless of random value
        val configRate1 = TrackingplanIngestConfig(
            sampleRate = 1,
            environmentRates = mapOf("PRODUCTION" to 1),
            random = FakeRandom(0.99f)
        )
        assertTrue(configRate1.shouldEnableTracking("PRODUCTION"))

        // Test rate=2 (threshold=0.5): random=0.4 should enable
        val configEnabled = TrackingplanIngestConfig(
            sampleRate = 2,
            environmentRates = mapOf("STAGING" to 2),
            random = FakeRandom(0.4f)
        )
        assertTrue(configEnabled.shouldEnableTracking("STAGING"))

        // Test rate=2 (threshold=0.5): random=0.6 should disable
        val configDisabled = TrackingplanIngestConfig(
            sampleRate = 2,
            environmentRates = mapOf("STAGING" to 2),
            random = FakeRandom(0.6f)
        )
        assertFalse(configDisabled.shouldEnableTracking("STAGING"))

        // Test fallback to default rate: UNKNOWN uses sampleRate=100 (threshold=0.01)
        val configUnknown = TrackingplanIngestConfig(
            sampleRate = 100,
            environmentRates = mapOf("STAGING" to 2),
            random = FakeRandom(0.02f)
        )
        assertFalse(configUnknown.shouldEnableTracking("UNKNOWN"))
    }

    @Test
    fun testParseEmptyJsonObject() {
        val jsonConfig = "{}"

        val config = TrackingplanIngestConfigParser.parse(jsonConfig)

        // Should parse successfully with all defaults
        assertNotNull(config)
        assertEquals(1, config.sampleRate)
        assertTrue(config.environmentRates.isEmpty())
        assertFalse(config.isAdaptiveSamplingEnabled())
    }

    @Test
    fun testParseMalformedJsonThrowsException() {
        val malformedJson = "{ invalid json }"

        try {
            TrackingplanIngestConfigParser.parse(malformedJson)
            throw AssertionError("Expected parsing to fail for malformed JSON")
        } catch (e: Exception) {
            // Expected - parsing should throw an exception
            assertNotNull(e)
        }
    }

    private class FakeRandom(private val value: Float) : Random() {
        override fun nextBits(bitCount: Int): Int = 0
        override fun nextFloat(): Float = value
    }
}
