// Copyright (c) 2026 Trackingplan
package com.trackingplan.shared.adaptive

import com.trackingplan.shared.SamplingOptions
import com.trackingplan.shared.TrackingplanIngestConfigParser
import com.trackingplan.shared.TrackingplanSession
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Regression test for the ingest cron's wire format. The cron emits
 * `adaptiveSamplingPatterns` as a JSON array of objects (matching the JS SDK):
 *
 * ```json
 * "adaptiveSamplingPatterns": [
 *   {"provider": "lib-firebase", "match": {...}, "sample_rate": 1},
 *   ...
 * ]
 * ```
 *
 * Before this branch, the SDK declared the field as `List<String>` and the parser threw
 * `JsonDecodingException` when objects appeared instead of strings — silently disabling
 * adaptive sampling on every device. The fix retypes the field to `List<JsonElement>`,
 * which accepts any cron-emitted shape; per-pattern interpretation happens lazily in
 * `AdaptiveSamplingPatternParser`.
 *
 * This test feeds a verbatim subset of the actual TP3666183 cron output through
 * `TrackingplanIngestConfigParser.parse()` and asserts the patterns survive both
 * deserialization and the lazy parse step.
 */
class CronWireFormatCompatibilityTest {

    private val cronOutputJson = """
        {
            "sample_rate": 1,
            "environment_rates": {"PRODUCTION": 15},
            "options": {
                "useSmartSampling": false,
                "useAdaptiveSampling": true,
                "adaptiveSamplingPatterns": [
                    {
                        "provider": "lib-firebase",
                        "sample_rate": 1,
                        "match": {"and": [{"method": "logEvent"}, {"name": "purchase"}]}
                    },
                    {
                        "provider": "lib-firebase",
                        "sample_rate": 2,
                        "match": {"and": [{"method": "logEvent"}, {"name": "view_item"}]}
                    },
                    {
                        "provider": "trackingplan",
                        "sample_rate": 1,
                        "match": {"event_name": "pixels"}
                    }
                ]
            },
            "a": "Iberdrola Android"
        }
    """.trimIndent()

    @Test
    fun cron_objectShapedPatterns_areAcceptedByDeserializer() {
        // Smoke test: the wire format must deserialize without throwing.
        val config = TrackingplanIngestConfigParser.parse(cronOutputJson)

        assertTrue(config.options.useAdaptiveSampling, "useAdaptiveSampling round-tripped")
        assertEquals(15, config.getSamplingRate("PRODUCTION"))
        assertEquals(
            3,
            config.options.adaptiveSamplingPatterns.size,
            "All three cron-generated patterns must be preserved as JsonElements"
        )
        // Each preserved element is a JsonObject (not a string).
        config.options.adaptiveSamplingPatterns.forEach { element ->
            assertTrue(
                element is JsonObject,
                "Each pattern element must be a JsonObject, got [${element::class.simpleName}]"
            )
        }
    }

    @Test
    fun cron_objectShapedPatterns_resolveToTypedPatternsViaSession() {
        // End-to-end: the lazy session parser must turn each JsonElement into a typed
        // AdaptiveSamplingPattern with the expected provider/sampleRate/match shape.
        val config = TrackingplanIngestConfigParser.parse(cronOutputJson)
        val samplingOptions = SamplingOptions(
            useAdaptiveSampling = config.options.useAdaptiveSampling,
            adaptiveSamplingPatterns = config.options.adaptiveSamplingPatterns
        )
        val session = TrackingplanSession.newSession(
            samplingRate = 15,
            trackingEnabled = true,
            samplingOptions = samplingOptions
        )

        val parsed = session.parsedPatterns
        assertEquals(3, parsed.size, "All three patterns parse into typed instances")

        val purchase = parsed[0]
        assertEquals("lib-firebase", purchase.provider)
        assertEquals(1, purchase.sampleRate)
        val purchaseMatch = purchase.match as MatchCondition.And
        assertEquals(2, purchaseMatch.conditions.size)

        val viewItem = parsed[1]
        assertEquals("lib-firebase", viewItem.provider)
        assertEquals(2, viewItem.sampleRate)

        val pixels = parsed[2]
        assertEquals("trackingplan", pixels.provider)
        assertEquals(1, pixels.sampleRate)
        val pixelsMatch = pixels.match as MatchCondition.Fields
        assertNotNull(pixelsMatch.fields["event_name"])
    }
}
