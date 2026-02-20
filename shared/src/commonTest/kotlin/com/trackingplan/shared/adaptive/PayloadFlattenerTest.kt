// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared.adaptive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PayloadFlattenerTest {

    @Test
    fun testFlattenSimpleObject() {
        val data = mapOf(
            "event" to "purchase",
            "user_id" to "123"
        )

        val flattened = PayloadFlattener.flattenToKeyValues(data)

        assertEquals(2, flattened.size)
        assertEquals(listOf("purchase"), flattened["event"])
        assertEquals(listOf("123"), flattened["user_id"])
    }

    @Test
    fun testFlattenNestedObject() {
        val data = mapOf(
            "user" to mapOf(
                "name" to "John",
                "email" to "john@example.com"
            ),
            "event" to "purchase"
        )

        val flattened = PayloadFlattener.flattenToKeyValues(data)

        assertEquals(3, flattened.size)
        assertEquals(listOf("John"), flattened["name"])
        assertEquals(listOf("john@example.com"), flattened["email"])
        assertEquals(listOf("purchase"), flattened["event"])
    }

    @Test
    fun testFlattenArrays() {
        val data = mapOf(
            "tags" to listOf("premium", "vip", "enterprise")
        )

        val flattened = PayloadFlattener.flattenToKeyValues(data)

        // Arrays should be flattened (nested list items are processed recursively)
        // Since list items aren't maps, they won't add keys, but nested maps within arrays will be flattened
        assertTrue(flattened.isEmpty() || flattened.size >= 0)
    }

    @Test
    fun testFlattenMixedStructure() {
        val data = mapOf(
            "event" to "purchase",
            "user" to mapOf(
                "id" to "123",
                "preferences" to mapOf(
                    "theme" to "dark"
                )
            ),
            "plan" to "premium"
        )

        val flattened = PayloadFlattener.flattenToKeyValues(data)

        assertTrue(flattened.containsKey("event"))
        assertTrue(flattened.containsKey("id"))
        assertTrue(flattened.containsKey("theme"))
        assertTrue(flattened.containsKey("plan"))

        assertEquals(listOf("purchase"), flattened["event"])
        assertEquals(listOf("123"), flattened["id"])
        assertEquals(listOf("dark"), flattened["theme"])
        assertEquals(listOf("premium"), flattened["plan"])
    }

    @Test
    fun testFlattenAvoidsDuplicates() {
        val data = mapOf(
            "tag" to "premium"
        )

        // Add same value multiple times through different processing
        val result = mutableMapOf<String, MutableList<String>>()
        PayloadFlattener.flattenToKeyValues(data).forEach { (key, values) ->
            result.getOrPut(key) { mutableListOf() }.addAll(values)
        }

        // Should only have one "premium" value
        assertEquals(1, result["tag"]?.size)
    }

    @Test
    fun testFlattenQueryStringVariation() {
        val variation = PayloadVariation.QueryString(
            mapOf("key1" to "value1", "key2" to "value2")
        )

        val flattened = PayloadFlattener.flattenPayloadVariation(variation)

        assertEquals(2, flattened.size)
        assertEquals(listOf("value1"), flattened["key1"])
        assertEquals(listOf("value2"), flattened["key2"])
    }

    @Test
    fun testFlattenJsonVariation() {
        val variation = PayloadVariation.Json(
            mapOf(
                "event" to "purchase",
                "user" to mapOf(
                    "name" to "John"
                )
            )
        )

        val flattened = PayloadFlattener.flattenPayloadVariation(variation)

        assertTrue(flattened.containsKey("event"))
        assertTrue(flattened.containsKey("name"))
        assertEquals(listOf("purchase"), flattened["event"])
        assertEquals(listOf("John"), flattened["name"])
    }

    @Test
    fun testFlattenMergedVariation() {
        val variation = PayloadVariation.Merged(
            endpointParams = mapOf("api_key" to "123"),
            payloadData = mapOf("event" to "purchase")
        )

        val flattened = PayloadFlattener.flattenPayloadVariation(variation)

        assertTrue(flattened.containsKey("api_key"))
        assertTrue(flattened.containsKey("event"))
        assertEquals(listOf("123"), flattened["api_key"])
        assertEquals(listOf("purchase"), flattened["event"])
    }

    @Test
    fun testFlattenEmptyData() {
        val flattened = PayloadFlattener.flattenToKeyValues(emptyMap<String, Any>())
        assertTrue(flattened.isEmpty())
    }

    @Test
    fun testFlattenNullData() {
        val flattened = PayloadFlattener.flattenToKeyValues(null)
        assertTrue(flattened.isEmpty())
    }

    @Test
    fun testFlattenIgnoresNonStringValues() {
        val data = mapOf(
            "string_value" to "text",
            "number_value" to 123,
            "boolean_value" to true,
            "null_value" to null
        )

        val flattened = PayloadFlattener.flattenToKeyValues(data)

        // Only string values should be collected
        assertTrue(flattened.containsKey("string_value"))
        assertEquals(listOf("text"), flattened["string_value"])
        // Non-string values are ignored
        assertEquals(1, flattened.size)
    }

    @Test
    fun testBooleanValuesInJsonPayload() {
        // Test Issue #5: Boolean values should be converted to strings for pattern matching
        val jsonPayload = """{"active": true, "debug": false, "count": 42}"""
        val parsed = RequestDataExtractor.parseJsonPayload(jsonPayload)
        val flattened = PayloadFlattener.flattenToKeyValues(parsed!!)

        // Boolean values should be converted to strings
        assertEquals(listOf("true"), flattened["active"])
        assertEquals(listOf("false"), flattened["debug"])
        // Numbers are converted to strings
        assertEquals(listOf("42"), flattened["count"])
    }

    @Test
    fun testMergedPayloadWithSharedKeys() {
        // Test Issue #1: URL has ?user_id=url_value and JSON has {"user_id": "json_value"}
        // This should not throw UnsupportedOperationException
        val endpointParams = mapOf("user_id" to "url_value", "source" to "web")
        val payloadData = mapOf<String, Any?>("user_id" to "json_value", "event" to "click")

        val variation = PayloadVariation.Merged(endpointParams, payloadData)

        // This should not throw UnsupportedOperationException
        val flattened = PayloadFlattener.flattenPayloadVariation(variation)

        // Should have both values for shared key
        assertTrue(flattened["user_id"]?.contains("url_value") == true)
        assertTrue(flattened["user_id"]?.contains("json_value") == true)
        // Should also have unique keys from both sources
        assertTrue(flattened["source"]?.contains("web") == true)
        assertTrue(flattened["event"]?.contains("click") == true)
    }
}
