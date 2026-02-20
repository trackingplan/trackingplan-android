// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import kotlin.test.*

class TrackingplanConfigTest {

    @Test
    fun testDefaultValues() {
        val config = TrackingplanConfig(tpId = "test-tp-id")

        assertEquals("test-tp-id", config.tpId)
        assertEquals("PRODUCTION", config.environment)
        assertEquals(defaultSourceAlias(), config.sourceAlias)
        assertTrue(config.tags.isEmpty())
        assertTrue(config.providerDomains.isEmpty())
        assertFalse(config.debug)
        assertFalse(config.testing)
        assertFalse(config.dryRun)
        assertEquals("https://eu-tracks.trackingplan.com/v1/", config.tracksEndpoint)
        assertEquals("https://config.trackingplan.com/", config.configEndpoint)
    }

    @Test
    fun testBuilderPattern() {
        val config = TrackingplanConfigBuilder()
            .tpId("test-tp-id")
            .environment("STAGING")
            .sourceAlias("custom-source")
            .tags(mapOf("key1" to "value1"))
            .providerDomains(mapOf("custom.domain.com" to "custom"))
            .debug(true)
            .testing(true)
            .tracksEndpoint("https://custom-tracks.example.com")
            .configEndpoint("https://custom-config.example.com")
            .build()

        assertEquals("test-tp-id", config.tpId)
        assertEquals("STAGING", config.environment)
        assertEquals("custom-source", config.sourceAlias)
        assertEquals(mapOf("key1" to "value1"), config.tags)
        assertEquals(mapOf("custom.domain.com" to "custom"), config.providerDomains)
        assertTrue(config.debug)
        assertTrue(config.testing)
        assertEquals("https://custom-tracks.example.com/", config.tracksEndpoint)
        assertEquals("https://custom-config.example.com/", config.configEndpoint)
    }

    @Test
    fun testEndpointNormalization() {
        val configWithSlash = TrackingplanConfigBuilder()
            .tpId("test")
            .tracksEndpoint("https://example.com/v1/")
            .build()
        val configWithoutSlash = TrackingplanConfigBuilder()
            .tpId("test")
            .tracksEndpoint("https://example.com/v1")
            .build()

        assertEquals("https://example.com/v1/", configWithSlash.tracksEndpoint)
        assertEquals("https://example.com/v1/", configWithoutSlash.tracksEndpoint)
    }

    @Test
    fun testEmptyTpIdInBuilderThrows() {
        assertFailsWith<IllegalArgumentException> {
            TrackingplanConfigBuilder().build()
        }
    }

    @Test
    fun testEmptyTpIdInDataClassAllowed() {
        // Empty tpId is allowed in data class for sentinel/EMPTY values
        val config = TrackingplanConfig(tpId = "")
        assertEquals("", config.tpId)
    }

    @Test
    fun testDryRunViaBuilder() {
        val config = TrackingplanConfigBuilder()
            .tpId("test-tp-id")
            .debug(true)
            .dryRun(true)
            .build()

        assertTrue(config.dryRun)
        assertTrue(config.debug)
    }

    @Test
    fun testDryRunRequiresDebug() {
        assertFailsWith<IllegalArgumentException> {
            TrackingplanConfigBuilder()
                .tpId("test-tp-id")
                .dryRun(true)
                .build()
        }
    }

    @Test
    fun testDryRunWithDebugDisabledThrows() {
        assertFailsWith<IllegalArgumentException> {
            TrackingplanConfigBuilder()
                .tpId("test-tp-id")
                .debug(false)
                .dryRun(true)
                .build()
        }
    }

    @Test
    fun testWithTagsMerge() {
        val original = TrackingplanConfig(
            tpId = "test",
            tags = mapOf("existing" to "value1")
        )

        val updated = original.withTags(mapOf("new" to "value2"), replace = false)

        assertEquals(mapOf("existing" to "value1", "new" to "value2"), updated.tags)
        assertEquals(mapOf("existing" to "value1"), original.tags) // Original unchanged (immutable)
    }

    @Test
    fun testWithTagsReplace() {
        val original = TrackingplanConfig(
            tpId = "test",
            tags = mapOf("existing" to "value1")
        )

        val updated = original.withTags(mapOf("new" to "value2"), replace = true)

        assertEquals(mapOf("new" to "value2"), updated.tags)
        assertEquals(mapOf("existing" to "value1"), original.tags) // Original unchanged (immutable)
    }

    @Test
    fun testWithTagsOverwrite() {
        val original = TrackingplanConfig(
            tpId = "test",
            tags = mapOf("key" to "original")
        )

        val updated = original.withTags(mapOf("key" to "updated"), replace = false)

        assertEquals(mapOf("key" to "updated"), updated.tags)
        assertEquals(mapOf("key" to "original"), original.tags) // Original unchanged
    }

    @Test
    fun testSampleRateUrl() {
        val config = TrackingplanConfig(
            tpId = "my-tp-id",
            configEndpoint = "https://config.example.com/"
        )

        assertEquals("https://config.example.com/config-my-tp-id.json", config.sampleRateUrl())
    }

    @Test
    fun testDataClassEquality() {
        val config1 = TrackingplanConfig(tpId = "test", environment = "STAGING")
        val config2 = TrackingplanConfig(tpId = "test", environment = "STAGING")
        val config3 = TrackingplanConfig(tpId = "test", environment = "PRODUCTION")

        assertEquals(config1, config2)
        assertNotEquals(config1, config3)
    }

    @Test
    fun testDataClassCopy() {
        val original = TrackingplanConfig(tpId = "test", environment = "STAGING")
        val copied = original.copy(environment = "PRODUCTION")

        assertEquals("STAGING", original.environment)
        assertEquals("PRODUCTION", copied.environment)
        assertEquals(original.tpId, copied.tpId)
    }

    @Test
    fun testImmutability() {
        val config = TrackingplanConfig(
            tpId = "test",
            tags = mapOf("key" to "value")
        )

        // Attempt to get tags and verify it's not modifiable
        val tags = config.tags
        assertIs<Map<String, String>>(tags)

        // Original should still have the same value
        assertEquals(mapOf("key" to "value"), config.tags)
    }

    @Test
    fun testMaxRequestBodySizeConstant() {
        assertEquals(100 * 1024, TrackingplanConfig.MAX_REQUEST_BODY_SIZE_IN_BYTES)
    }

    @Test
    fun testDefaultEndpointConstants() {
        assertEquals("https://eu-tracks.trackingplan.com/v1/", TrackingplanConfig.DEFAULT_TRACKS_ENDPOINT)
        assertEquals("https://config.trackingplan.com/", TrackingplanConfig.DEFAULT_CONFIG_ENDPOINT)
    }

    @Test
    fun testBuilderMakesDefensiveCopies() {
        // Create mutable maps
        val mutableTags = mutableMapOf("key1" to "value1")
        val mutableDomains = mutableMapOf("domain.com" to "provider")

        // Build config with mutable maps
        val config = TrackingplanConfigBuilder()
            .tpId("test-tp-id")
            .tags(mutableTags)
            .providerDomains(mutableDomains)
            .build()

        // Mutate the original maps after building
        mutableTags["key1"] = "modified"
        mutableTags["key2"] = "value2"
        mutableDomains["domain.com"] = "modified"

        // Config should NOT be affected by mutations to original maps
        assertEquals("value1", config.tags["key1"])
        assertNull(config.tags["key2"])
        assertEquals("provider", config.providerDomains["domain.com"])
    }
}
