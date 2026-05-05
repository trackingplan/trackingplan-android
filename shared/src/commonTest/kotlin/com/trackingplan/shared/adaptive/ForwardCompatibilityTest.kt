// Copyright (c) 2026 Trackingplan
package com.trackingplan.shared.adaptive

import com.trackingplan.shared.TrackingplanIngestConfigParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Pins the SDK's tolerance for ingest format evolution. The wire format is allowed to
 * grow (new pattern fields, new MatchCondition variants, new value shapes); existing SDKs
 * already on user devices must not crash and must continue to apply whatever rules they
 * still understand.
 */
class ForwardCompatibilityTest {

    @Test
    fun unknownTopLevelFieldOnPatternIsTolerated() {
        // Cron may add new optional pattern fields (e.g. priority, weight) ahead of an SDK
        // release. The SDK must ignore them and parse the rest of the pattern normally.
        val configJson = """
            {
                "sample_rate": 1,
                "options": {
                    "useAdaptiveSampling": true,
                    "adaptiveSamplingPatterns": [
                        {
                            "provider": "amplitude",
                            "match": {"event_type": "purchase"},
                            "sample_rate": 1,
                            "priority": 100,
                            "experimentalRule": {"foo": "bar"}
                        }
                    ]
                }
            }
        """.trimIndent()

        val config = TrackingplanIngestConfigParser.parse(configJson)
        assertEquals(1, config.options.adaptiveSamplingPatterns.size)

        val parsed = AdaptiveSamplingPatternParser.parsePatterns(
            config.options.adaptiveSamplingPatterns
        )
        assertEquals(1, parsed.size, "Pattern with unknown fields must still parse")
        assertEquals("amplitude", parsed[0].provider)
        assertEquals(1, parsed[0].sampleRate)
    }

    @Test
    fun unknownMatchConditionVariantDoesNotCrash() {
        // If a future cron emits a new boolean operator (e.g. "xor" with array-of-conditions
        // semantics), older SDKs don't recognise it. The parser falls through to the
        // implicit-Fields branch and tries to extract MatchValues from the array — that
        // fails because the array contains objects, not primitives. The pattern is skipped,
        // but the SDK does not crash and other patterns continue to apply.
        val configJson = """
            {
                "sample_rate": 1,
                "options": {
                    "useAdaptiveSampling": true,
                    "adaptiveSamplingPatterns": [
                        {
                            "provider": "amplitude",
                            "match": {"xor": [{"event_type": "a"}, {"event_type": "b"}]},
                            "sample_rate": 1
                        },
                        {
                            "provider": "mixpanel",
                            "match": {"event": "purchase"},
                            "sample_rate": 2
                        }
                    ]
                }
            }
        """.trimIndent()

        val config = TrackingplanIngestConfigParser.parse(configJson)
        // The wire layer keeps both patterns regardless of shape.
        assertEquals(2, config.options.adaptiveSamplingPatterns.size)

        // Lazy parse must not crash. The xor pattern is silently skipped; the mixpanel
        // pattern (which uses a known shape) survives.
        val parsed = AdaptiveSamplingPatternParser.parsePatterns(
            config.options.adaptiveSamplingPatterns
        )
        assertEquals(
            1,
            parsed.size,
            "Unknown variant pattern is skipped; the known-shape pattern still applies"
        )
        assertEquals("mixpanel", parsed[0].provider)
        assertTrue(parsed[0].match is MatchCondition.Fields)
    }

    @Test
    fun onePatternCorruptionDoesNotKillTheBatch() {
        // A single malformed pattern in an otherwise-valid array must be skipped, not
        // propagated. The other patterns must still apply.
        val configJson = """
            {
                "sample_rate": 1,
                "options": {
                    "useAdaptiveSampling": true,
                    "adaptiveSamplingPatterns": [
                        {"provider": "amplitude", "sample_rate": 1},
                        {"missing_provider": true},
                        "not even an object",
                        {"provider": "mixpanel", "sample_rate": 2}
                    ]
                }
            }
        """.trimIndent()

        val config = TrackingplanIngestConfigParser.parse(configJson)
        // Wire layer keeps every element regardless of shape (4 entries).
        assertEquals(4, config.options.adaptiveSamplingPatterns.size)

        // Lazy parse drops the two invalid entries (missing provider, not-an-object).
        val parsed = AdaptiveSamplingPatternParser.parsePatterns(
            config.options.adaptiveSamplingPatterns
        )
        assertEquals(2, parsed.size, "Two valid patterns survive, two invalid are skipped")
        assertEquals("amplitude", parsed[0].provider)
        assertEquals("mixpanel", parsed[1].provider)
    }

    @Test
    fun malformedMatchShapeDropsPatternNotPromotesToCatchAll() {
        // A `match` field that is present but neither absent nor a JsonObject (e.g. a
        // string from a hypothetical future wire format) must NOT silently collapse to a
        // null match condition — AdaptiveSamplingMatcher treats null match as "applies to
        // every request for this provider", which would broaden the rule's reach. The
        // whole pattern must be dropped instead.
        val configJson = """
            {
                "sample_rate": 1,
                "options": {
                    "useAdaptiveSampling": true,
                    "adaptiveSamplingPatterns": [
                        {
                            "provider": "amplitude",
                            "match": "this should be a JsonObject",
                            "sample_rate": 1
                        },
                        {
                            "provider": "mixpanel",
                            "match": {"event_type": "purchase"},
                            "sample_rate": 2
                        }
                    ]
                }
            }
        """.trimIndent()

        val config = TrackingplanIngestConfigParser.parse(configJson)
        val parsed = AdaptiveSamplingPatternParser.parsePatterns(
            config.options.adaptiveSamplingPatterns
        )
        assertEquals(
            1,
            parsed.size,
            "Pattern with malformed match shape must be dropped, not promoted to catch-all"
        )
        assertEquals("mixpanel", parsed[0].provider)
    }

    @Test
    fun knownOperatorWithNonArrayBodyDropsPattern() {
        // {"and": "<string>"} — the SDK recognises `and` as a boolean operator but the
        // body is not the JsonArray of conditions it expects. parseMatchCondition's
        // `.jsonArray` accessor throws on a primitive; the outer catch in parsePattern
        // logs at WARN and drops the pattern. A sibling valid pattern must still apply.
        val configJson = """
            {
                "sample_rate": 1,
                "options": {
                    "useAdaptiveSampling": true,
                    "adaptiveSamplingPatterns": [
                        {
                            "provider": "amplitude",
                            "match": {"and": "not an array"},
                            "sample_rate": 1
                        },
                        {
                            "provider": "mixpanel",
                            "match": {"event_type": "purchase"},
                            "sample_rate": 2
                        }
                    ]
                }
            }
        """.trimIndent()

        val config = TrackingplanIngestConfigParser.parse(configJson)
        val parsed = AdaptiveSamplingPatternParser.parsePatterns(
            config.options.adaptiveSamplingPatterns
        )
        assertEquals(1, parsed.size, "Malformed `and` body must drop the whole pattern")
        assertEquals("mixpanel", parsed[0].provider)
    }

    @Test
    fun knownOperatorWithNonObjectArrayElementsDropsPattern() {
        // {"and": [1, 2, 3]} — the body is a JsonArray (right shape) but its elements are
        // primitives, not condition objects. parseMatchCondition recurses into each
        // element via `.jsonObject`, which throws on a primitive; the outer catch in
        // parsePattern drops the whole pattern. Sibling valid pattern still applies.
        val configJson = """
            {
                "sample_rate": 1,
                "options": {
                    "useAdaptiveSampling": true,
                    "adaptiveSamplingPatterns": [
                        {
                            "provider": "amplitude",
                            "match": {"and": [1, 2, 3]},
                            "sample_rate": 1
                        },
                        {
                            "provider": "mixpanel",
                            "match": {"event_type": "purchase"},
                            "sample_rate": 2
                        }
                    ]
                }
            }
        """.trimIndent()

        val config = TrackingplanIngestConfigParser.parse(configJson)
        val parsed = AdaptiveSamplingPatternParser.parsePatterns(
            config.options.adaptiveSamplingPatterns
        )
        assertEquals(
            1,
            parsed.size,
            "Malformed `and` array elements must drop the whole pattern"
        )
        assertEquals("mixpanel", parsed[0].provider)
    }
}
