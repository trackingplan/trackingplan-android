// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared.adaptive.integration

import com.trackingplan.shared.adaptive.AdaptiveSamplingMatcher
import com.trackingplan.shared.adaptive.AdaptiveSamplingPatternParser
import com.trackingplan.shared.adaptive.Request
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveSamplingFrozenMatchTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun generatedPatternsMatchFrozenMobilePayloads() {
        val config = readJsonResource("sdk_config.json").jsonObject
        val patternElements = config["options"]!!
            .jsonObject["adaptiveSamplingPatterns"]!!
            .jsonArray
            .toList()
        val patterns = AdaptiveSamplingPatternParser.parsePatterns(patternElements)
        val payloads = readJsonResource("input/payloads_mobile.json").jsonArray
        val expected = readJsonResource("output/expected_matches_mobile.json").jsonObject

        payloads.forEach { payloadElement ->
            val payload = payloadElement.jsonObject
            val id = payload.string("id")
            val expectedMatch = expected[id]!!.jsonObject
            val request = Request(
                provider = payload.string("provider"),
                endpoint = payload.string("endpoint"),
                payload = payload.stringOrNull("payload")
            )

            val result = AdaptiveSamplingMatcher.matchRequest(request, patterns)

            assertEquals(
                expectedMatch.intOrNull("sample_rate"),
                result.sampleRate,
                "sample_rate mismatch for [$id]"
            )
            assertEquals(
                expectedMatch.stringOrNull("matched_provider"),
                result.matchedPattern?.provider,
                "matched_provider mismatch for [$id]"
            )
        }
    }

    private fun readJsonResource(name: String) =
        json.parseToJsonElement(
            checkNotNull(javaClass.classLoader.getResourceAsStream(name)) {
                "Missing test resource [$name]"
            }.bufferedReader().use { it.readText() }
        )

    private fun JsonObject.string(key: String): String {
        return checkNotNull(this[key]?.jsonPrimitive?.contentOrNull) {
            "Missing string key [$key]"
        }
    }

    private fun JsonObject.stringOrNull(key: String): String? {
        val value = this[key] ?: return null
        if (value is JsonNull) return null
        return value.jsonPrimitive.contentOrNull
    }

    private fun JsonObject.intOrNull(key: String): Int? {
        val value = this[key] ?: return null
        if (value is JsonNull) return null
        return value.jsonPrimitive.intOrNull
    }
}
