// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared.adaptive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RequestDataExtractorTest {

    @Test
    fun testParseUrlWithoutQueryParams() {
        val url = "https://api.amplitude.com/v1/batch"
        val (path, params) = RequestDataExtractor.parseUrl(url)

        assertEquals("/v1/batch", path)
        assertTrue(params.isEmpty())
    }

    @Test
    fun testParseUrlWithQueryParams() {
        val url = "https://api.segment.io/v1/track?api_key=123&user_id=456"
        val (path, params) = RequestDataExtractor.parseUrl(url)

        assertEquals("/v1/track", path)
        assertEquals(2, params.size)
        assertEquals("123", params["api_key"])
        assertEquals("456", params["user_id"])
    }

    @Test
    fun testParseUrlWithEncodedQueryParams() {
        val url = "https://api.example.com/track?name=John+Doe&email=test%40example.com"
        val (path, params) = RequestDataExtractor.parseUrl(url)

        assertEquals("/track", path)
        assertEquals("John Doe", params["name"])
        assertEquals("test@example.com", params["email"])
    }

    @Test
    fun testUrlDecodeWithEncodedPlusSign() {
        // Test Issue #4: %2B (encoded plus sign) should decode to +, not space
        // Regular + should decode to space
        val url = "https://api.example.com/track?key=%2Bvalue&test=a+b"
        val (path, params) = RequestDataExtractor.parseUrl(url)

        assertEquals("/track", path)
        // %2B should become + (plus sign), not space
        assertEquals("+value", params["key"])
        // + should become space
        assertEquals("a b", params["test"])
    }

    @Test
    fun testParseQueryString() {
        val queryString = "key1=value1&key2=value2&key3="
        val params = RequestDataExtractor.parseQueryString(queryString)

        assertEquals(3, params.size)
        assertEquals("value1", params["key1"])
        assertEquals("value2", params["key2"])
        assertEquals("", params["key3"])
    }

    @Test
    fun testParseEmptyQueryString() {
        val params = RequestDataExtractor.parseQueryString("")
        assertTrue(params.isEmpty())
    }

    @Test
    fun testParseJsonPayload() {
        val payload = """{"event_type":"session_start","user_id":"123"}"""
        val data = RequestDataExtractor.parseJsonPayload(payload)

        assertNotNull(data)
        assertEquals("session_start", data["event_type"])
        assertEquals("123", data["user_id"])
    }

    @Test
    fun testParseNestedJsonPayload() {
        val payload = """{"user":{"name":"John","age":"30"},"event":"purchase"}"""
        val data = RequestDataExtractor.parseJsonPayload(payload)

        assertNotNull(data)
        assertEquals("purchase", data["event"])
        val user = data["user"] as? Map<*, *>
        assertNotNull(user)
        assertEquals("John", user["name"])
        assertEquals("30", user["age"])
    }

    @Test
    fun testParseJsonArrayPayload() {
        val payload = """{"events":[{"name":"event1"},{"name":"event2"}]}"""
        val data = RequestDataExtractor.parseJsonPayload(payload)

        assertNotNull(data)
        val events = data["events"] as? List<*>
        assertNotNull(events)
        assertEquals(2, events.size)
    }

    @Test
    fun testParseInvalidJsonReturnsNull() {
        val payload = """{invalid json"""
        val data = RequestDataExtractor.parseJsonPayload(payload)
        assertNull(data)
    }

    @Test
    fun testExtractPayloadVariationsWithEndpointParams() {
        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch?api_key=123",
            payload = null
        )

        val variations = RequestDataExtractor.extractPayloadVariations(request)

        assertEquals(1, variations.size)
        val queryString = variations[0] as PayloadVariation.QueryString
        assertEquals("123", queryString.params["api_key"])
    }

    @Test
    fun testExtractPayloadVariationsWithJsonPayload() {
        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch",
            payload = """{"event_type":"session_start","user_id":"123"}"""
        )

        val variations = RequestDataExtractor.extractPayloadVariations(request)

        assertEquals(1, variations.size)
        val json = variations[0] as PayloadVariation.Json
        assertEquals("session_start", json.data["event_type"])
    }

    @Test
    fun testExtractPayloadVariationsWithMerged() {
        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/batch?api_key=123",
            payload = """{"event_type":"session_start"}"""
        )

        val variations = RequestDataExtractor.extractPayloadVariations(request)

        // Should have: endpoint params, JSON, and merged
        assertEquals(3, variations.size)
        assertTrue(variations.any { it is PayloadVariation.QueryString })
        assertTrue(variations.any { it is PayloadVariation.Json })
        assertTrue(variations.any { it is PayloadVariation.Merged })
    }

    @Test
    fun testExtractPayloadVariationsWithQueryStringBatching() {
        // Google Analytics batching format: multiple query strings separated by newlines
        val request = Request(
            provider = "googleanalytics",
            endpoint = "https://www.google-analytics.com/g/collect",
            payload = "en=Scroll&param1=value1\nen=Click&param2=value2"
        )

        val variations = RequestDataExtractor.extractPayloadVariations(request)

        // Should have 2 variations (one for each line)
        assertEquals(2, variations.size)
        val variation1 = variations[0] as PayloadVariation.QueryString
        assertEquals("Scroll", variation1.params["en"])
        assertEquals("value1", variation1.params["param1"])

        val variation2 = variations[1] as PayloadVariation.QueryString
        assertEquals("Click", variation2.params["en"])
        assertEquals("value2", variation2.params["param2"])
    }

    @Test
    fun testCreateMatchContext() {
        val request = Request(
            provider = "amplitude",
            endpoint = "https://api.amplitude.com/v1/batch?api_key=123",
            payload = """{"event":"test"}"""
        )

        val context = RequestDataExtractor.createMatchContext(request)

        assertEquals("/v1/batch", context.path)
        assertTrue(context.endpoint.contains("api.amplitude.com"))
        assertTrue(context.payload.contains("event"))
    }

    @Test
    fun testCreateMatchContextTruncatesLongStrings() {
        val longPayload = "x".repeat(100000)
        val request = Request(
            provider = "test",
            endpoint = "https://example.com/endpoint",
            payload = longPayload
        )

        val context = RequestDataExtractor.createMatchContext(request)

        // Should be truncated to MAX_CONTEXT_LENGTH (65536)
        assertTrue(context.payload.length <= 65536)
    }
}
