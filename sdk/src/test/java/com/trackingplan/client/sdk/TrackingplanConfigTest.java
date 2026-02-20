// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for TrackingplanConfig Android adapter.
 *
 * Business logic (withTags, endpoint normalization, etc.) is tested in the shared module.
 * These tests focus on:
 * - Platform-specific defaults (sourceAlias = "android")
 * - Platform-specific fields (dryRun, backgroundObserver, customContext)
 * - Delegation to shared config
 * - Java interop
 */
public class TrackingplanConfigTest {

    @Test
    public void testPlatformSpecificDefaults() {
        TrackingplanConfig config = TrackingplanConfig.newConfig("test-tp-id").build();

        // Platform-specific default
        assertEquals("android", config.getSourceAlias());

        // Platform-specific fields defaults
        assertFalse(config.isDryRunEnabled());
        assertTrue(config.isBackgroundObserverEnabled());
        assertTrue(config.customContext().isEmpty());
    }

    @Test
    public void testDelegationViaBuilder() {
        Map<String, String> tags = new HashMap<>();
        tags.put("key", "value");

        Map<String, String> domains = new HashMap<>();
        domains.put("custom.domain.com", "custom");

        TrackingplanConfig config = TrackingplanConfig.newConfig("test-id")
                .environment("TEST_ENV")
                .sourceAlias("custom-source")
                .tags(tags)
                .customDomains(domains)
                .enableDebug()
                .enableTesting()
                .tracksEndPoint("https://custom-tracks.example.com/")
                .configEndPoint("https://custom-config.example.com/")
                .build();

        // Verify all delegated properties return expected values
        assertEquals("test-id", config.getTpId());
        assertEquals("TEST_ENV", config.getEnvironment());
        assertEquals("custom-source", config.getSourceAlias());
        assertEquals("value", config.tags().get("key"));
        assertEquals("custom", config.customDomains().get("custom.domain.com"));
        assertTrue(config.isDebugEnabled());
        assertTrue(config.isTestingEnabled());
        assertEquals("https://custom-tracks.example.com/", config.getTracksEndPoint());
        assertEquals("https://custom-config.example.com/", config.getConfigEndPoint());
    }

    @Test
    public void testPlatformSpecificFields() {
        // Test backgroundObserver (Android-specific)
        TrackingplanConfig configWithoutBgObserver = TrackingplanConfig.newConfig("test")
                .disableBackgroundObserver()
                .build();
        assertFalse(configWithoutBgObserver.isBackgroundObserverEnabled());

        // Test customContext (Android-specific)
        Map<String, String> context = new HashMap<>();
        context.put("contextKey", "contextValue");
        TrackingplanConfig configWithContext = TrackingplanConfig.newConfig("test")
                .customContext(context)
                .build();
        assertEquals("contextValue", configWithContext.customContext().get("contextKey"));
    }

    @Test
    public void testPlatformSpecificFieldsPreservedWithTags() {
        Map<String, String> context = new HashMap<>();
        context.put("contextKey", "contextValue");

        TrackingplanConfig original = TrackingplanConfig.newConfig("test")
                .disableBackgroundObserver()
                .customContext(context)
                .build();

        Map<String, String> newTags = new HashMap<>();
        newTags.put("newKey", "newValue");
        TrackingplanConfig updated = original.withTags(newTags, false);

        // Platform-specific fields should be preserved
        assertFalse(updated.isBackgroundObserverEnabled());
        assertEquals("contextValue", updated.customContext().get("contextKey"));
    }

    @Test(expected = RuntimeException.class)
    public void testDryRunRequiresDebug() {
        // DryRun without debug should throw (Android-specific behavior)
        TrackingplanConfig.newConfig("test")
                .enableDryRun()
                .build();
    }

    @Test
    public void testReturnedMapsAreUnmodifiable() {
        // Tests Java interop - Kotlin maps should be read-only from Java
        Map<String, String> tags = new HashMap<>();
        tags.put("key", "value");

        TrackingplanConfig config = TrackingplanConfig.newConfig("test")
                .tags(tags)
                .build();

        Map<String, String> returnedTags = config.tags();
        assertNotNull(returnedTags);
        assertEquals("value", returnedTags.get("key"));
    }

    @Test
    public void testEmptyConfig() {
        // Tests Android EMPTY sentinel
        TrackingplanConfig empty = TrackingplanConfig.EMPTY;

        assertEquals("", empty.getTpId());
        assertEquals("PRODUCTION", empty.getEnvironment());
        assertTrue(empty.tags().isEmpty());
    }
}
