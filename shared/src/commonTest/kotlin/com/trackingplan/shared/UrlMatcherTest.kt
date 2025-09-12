package com.trackingplan.shared

import kotlin.test.*

class UrlMatcherTest {
    
    @BeforeTest
    fun setup() {
        UrlMatcher.clearRegexCache()
    }
    
    @Test
    fun testSimpleStringMatching() {
        val providers = mapOf(
            "api.amplitude.com" to "amplitude",
            "graph.facebook.com/v" to "facebook",
            "api.mixpanel.com" to "mixpanel"
        )
        
        assertEquals("amplitude", UrlMatcher.matchProvider(providers, "https://api.amplitude.com/batch"))
        assertEquals("facebook", UrlMatcher.matchProvider(providers, "https://graph.facebook.com/v18.0/123456/activities"))
        assertEquals("mixpanel", UrlMatcher.matchProvider(providers, "https://api.mixpanel.com/track"))
        assertNull(UrlMatcher.matchProvider(providers, "https://api.segment.io/v1/batch"))
    }
    
    @Test
    fun testWildcardMatching() {
        val providers = mapOf(
            "api-*.amplitude.com" to "amplitude",
            "graph.facebook.com/v*/*/activities" to "facebook"
        )
        
        // Test * wildcard
        assertEquals("amplitude", UrlMatcher.matchProvider(providers, "https://api-eu.amplitude.com/batch"))
        assertEquals("amplitude", UrlMatcher.matchProvider(providers, "https://api-us-west.amplitude.com/batch"))
        
        // Test specific Facebook activities endpoint
        assertEquals("facebook", UrlMatcher.matchProvider(providers, "https://graph.facebook.com/v18.0/123456/activities"))
        assertEquals("facebook", UrlMatcher.matchProvider(providers, "https://graph.facebook.com/v20.0/789012/activities"))
        
        // Should NOT match other Facebook endpoints
        assertNull(UrlMatcher.matchProvider(providers, "https://graph.facebook.com/v18.0/me/posts"))
        assertNull(UrlMatcher.matchProvider(providers, "https://graph.facebook.com/v18.0/123456/friends"))
    }
    
    @Test
    fun testRegexMatching() {
        val providers = mapOf(
            "regex:analytics\\.[a-z]+\\.tiktok\\.com/api/v\\d+/app_sdk/batch" to "tiktok",
            "regex:graph\\.facebook\\.com/v\\d+/\\d+/activities" to "facebook"
        )
        
        // Test TikTok regex
        assertEquals("tiktok", UrlMatcher.matchProvider(providers, "https://analytics.us.tiktok.com/api/v1/app_sdk/batch"))
        assertEquals("tiktok", UrlMatcher.matchProvider(providers, "https://analytics.eu.tiktok.com/api/v2/app_sdk/batch"))
        assertNull(UrlMatcher.matchProvider(providers, "https://analytics.us.tiktok.com/api/v1/app_sdk/monitor"))
        
        // Test Facebook precise regex
        assertEquals("facebook", UrlMatcher.matchProvider(providers, "https://graph.facebook.com/v18/123456/activities"))
        assertEquals("facebook", UrlMatcher.matchProvider(providers, "https://graph.facebook.com/v20/789012/activities"))
        assertNull(UrlMatcher.matchProvider(providers, "https://graph.facebook.com/v18/me/activities"))
    }
    
    @Test
    fun testPatternPriority() {
        // First match should win
        val providers = mapOf(
            "graph.facebook.com/v" to "facebook_general",
            "graph.facebook.com/v*/*/activities" to "facebook_activities"
        )
        
        val url = "https://graph.facebook.com/v18.0/123456/activities"
        assertEquals("facebook_general", UrlMatcher.matchProvider(providers, url))
    }
    
    @Test
    fun testBackwardCompatibility() {
        // Test that existing patterns still work exactly as before
        val legacyProviders = mapOf(
            "api.amplitude.com" to "amplitude",
            "api2.amplitude.com" to "amplitude", 
            "bat.bing.com" to "bing",
            "api2.branch.io" to "branch",
            "api3.branch.io" to "branch",
            "ping.chartbeat.net" to "chartbeat",
            "track-sdk-eu.customer.io/api" to "customerio",
            "track-sdk.customer.io/api" to "customerio",
            "facebook.com/tr/" to "facebook",
            "graph.facebook.com/v" to "facebook",
            "api.intercom.io" to "intercom",
            "kissmetrics.com" to "kissmetrics",
            "trk.kissmetrics.io" to "kissmetrics",
            "px.ads.linkedin.com" to "linkedin",
            "api.mixpanel.com" to "mixpanel",
            "logx.optimizely.com/v1/events" to "optimizely",
            "ct.pinterest.com" to "pinterest",
            "pdst.fm" to "podsights",
            "quantserve.com" to "quantserve",
            "sb.scorecardresearch.com" to "scorecardresearch",
            "api.segment.io" to "segment",
            "api.segment.com" to "segment",
            "analytics.us.tiktok.com/api/v1/app_sdk/batch" to "tiktok",
            "analytics.us.tiktok.com/api/v1/app_sdk/monitor" to "tiktok"
        )
        
        assertEquals("amplitude", UrlMatcher.matchProvider(legacyProviders, "https://api.amplitude.com/batch"))
        assertEquals("facebook", UrlMatcher.matchProvider(legacyProviders, "https://facebook.com/tr/?id=123"))
        assertEquals("facebook", UrlMatcher.matchProvider(legacyProviders, "https://graph.facebook.com/v18.0/123456/activities"))
        assertEquals("facebook", UrlMatcher.matchProvider(legacyProviders, "https://graph.facebook.com/v18.0/me/posts"))
        assertEquals("tiktok", UrlMatcher.matchProvider(legacyProviders, "https://analytics.us.tiktok.com/api/v1/app_sdk/batch"))
        assertEquals("segment", UrlMatcher.matchProvider(legacyProviders, "https://api.segment.io/v1/track"))
    }
    
    @Test
    fun testSpecialCharacterEscaping() {
        val providers = mapOf(
            "api.amplitude.com" to "amplitude",
            "*.example.com" to "wildcard"
        )
        
        // Dot should be treated literally in simple strings
        assertNull(UrlMatcher.matchProvider(providers, "https://apixamplitude.com/batch"))  // Should NOT match because dot is literal
        assertEquals("amplitude", UrlMatcher.matchProvider(providers, "https://api.amplitude.com/batch"))  // Should match exact substring
        
        // Dot should be escaped in wildcard patterns
        assertEquals("wildcard", UrlMatcher.matchProvider(providers, "https://api.example.com"))
        assertNull(UrlMatcher.matchProvider(providers, "https://apiXexample.com"))
    }
    
    @Test
    fun testEmptyAndNullInputs() {
        val providers = mapOf("api.test.com" to "test")
        
        assertNull(UrlMatcher.matchProvider(emptyMap(), "https://api.test.com"))
        assertNull(UrlMatcher.matchProvider(providers, ""))
    }
    
    @Test
    fun testRegexCaching() {
        val providers = mapOf(
            "regex:test\\d+" to "test",
            "api-*" to "wildcard"
        )
        
        // First call should compile and cache regex
        assertEquals("test", UrlMatcher.matchProvider(providers, "https://test123"))
        assertEquals("wildcard", UrlMatcher.matchProvider(providers, "https://api-us"))
        
        // Second call should use cached regex
        assertEquals("test", UrlMatcher.matchProvider(providers, "https://test456"))
        assertEquals("wildcard", UrlMatcher.matchProvider(providers, "https://api-eu"))
        
        // Clear cache and verify it still works
        UrlMatcher.clearRegexCache()
        assertEquals("test", UrlMatcher.matchProvider(providers, "https://test789"))
    }
    
    @Test
    fun testInvalidRegexPatterns() {
        val providers = mapOf(
            "regex:[unclosed" to "invalid1",
            "regex:*{invalid}" to "invalid2", 
            "regex:(?invalid)" to "invalid3",
            "api.valid.com" to "valid",
            "regex:test\\d+" to "validRegex"
        )
        
        val testUrl = "https://api.valid.com/test"
        
        // Should skip invalid patterns and continue to valid ones
        assertEquals("valid", UrlMatcher.matchProvider(providers, testUrl))
        
        // Invalid regex patterns should not match anything
        assertNull(UrlMatcher.matchProvider(providers, "https://unclosed"))
        assertNull(UrlMatcher.matchProvider(providers, "https://invalid"))
        
        // Valid regex should still work
        assertEquals("validRegex", UrlMatcher.matchProvider(providers, "https://test123"))
    }
    
    @Test
    fun testInvalidWildcardPatternsFromRegexConversion() {
        val providers = mapOf(
            "api[invalid*pattern" to "invalid1",
            "api{invalid}*test" to "invalid2",
            "api.valid*.com" to "valid"
        )
        
        val testUrl = "https://api.valid-test.com/endpoint"
        
        // Should skip invalid wildcard patterns that fail regex conversion and continue to valid ones
        assertEquals("valid", UrlMatcher.matchProvider(providers, testUrl))
        
        // Invalid patterns should not match
        assertNull(UrlMatcher.matchProvider(providers, "https://api[invalid"))
        assertNull(UrlMatcher.matchProvider(providers, "https://api{invalid}"))
    }
    
    @Test
    fun testFailedPatternCaching() {
        val providers = mapOf(
            "regex:[invalid" to "invalid",
            "api.test.com" to "valid"
        )
        
        // First call with invalid regex should fail and cache the failure
        assertEquals("valid", UrlMatcher.matchProvider(providers, "https://api.test.com/test"))
        
        // Second call should use cached failure result (not attempt regex compilation again)
        assertEquals("valid", UrlMatcher.matchProvider(providers, "https://api.test.com/test2"))
        
        // Clear cache should reset both successful and failed patterns
        UrlMatcher.clearRegexCache()
        assertEquals("valid", UrlMatcher.matchProvider(providers, "https://api.test.com/test3"))
    }
}