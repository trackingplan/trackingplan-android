// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import kotlin.test.*

class IngestConfigCacheTest : BaseTest() {

    private lateinit var cacheStorage: CacheStorage
    private lateinit var cache: IngestConfigCache
    private val testTpId = "test-tp-id"

    @BeforeTest
    fun setup() {
        cacheStorage = CacheStorage()
        cache = IngestConfigCache(cacheStorage, testTpId)
    }

    @AfterTest
    fun cleanup() {
        cache.clear()
        CacheTimestampHelper.clearAll()
    }

    // Basic Operations

    @Test
    fun testSaveAndLoadValidConfig() {
        val jsonContent = """{"sample_rate": 2, "environment_rates": {"PRODUCTION": 1}}"""

        cache.save(jsonContent)
        val loaded = cache.loadIfValid()

        assertNotNull(loaded)
        assertEquals(2, loaded.sampleRate)
        assertEquals(1, loaded.getSamplingRate("PRODUCTION"))
    }

    @Test
    fun testLoadReturnsNullWhenEmpty() {
        val loaded = cache.loadIfValid()
        assertNull(loaded)
    }

    @Test
    fun testLoadReturnsNullWhenCorrupted() {
        cache.save("not valid json {{{")
        val loaded = cache.loadIfValid()
        assertNull(loaded)
    }

    @Test
    fun testClearRemovesCache() {
        cache.save("""{"sample_rate": 1}""")
        cache.clear()
        val loaded = cache.loadIfValid()
        assertNull(loaded)
    }

    @Test
    fun testGetDownloadedAt() {
        val fakeTime = TestTimeProvider()
        val expectedTime = fakeTime.currentTimeMillis()
        ServiceLocator.setTimeProvider(fakeTime)

        try {
            cache.save("""{"sample_rate": 1}""")
            val downloadedAt = cache.getDownloadedAt()
            assertEquals(expectedTime, downloadedAt)
        } finally {
            ServiceLocator.reset()
        }
    }

    @Test
    fun testGetDownloadedAtReturnsMinusOneWhenEmpty() {
        val downloadedAt = cache.getDownloadedAt()
        assertEquals(-1, downloadedAt)
    }

    // Expiration

    @Test
    fun testConfigMaxAgeIs24Hours() {
        // Verify the constant is set to 24 hours
        assertEquals(24 * 60 * 60 * 1000L, IngestConfigCache.CONFIG_MAX_AGE_MS)
    }

    @Test
    fun testLoadReturnsConfigWhenNotExpired() {
        cache.save("""{"sample_rate": 1}""")
        val loaded = cache.loadIfValid()
        assertNotNull(loaded)
    }

    @Test
    fun testLoadReturnsNullWhenExpired() {
        val fakeTime = TestTimeProvider()
        ServiceLocator.setTimeProvider(fakeTime)

        try {
            cache.save("""{"sample_rate": 1}""")

            // Advance time past 24 hours
            fakeTime.advanceTime(25 * 60 * 60 * 1000L)

            val loaded = cache.loadIfValid()
            assertNull(loaded)
        } finally {
            ServiceLocator.reset()
        }
    }

    // Different tpId creates separate files (cache is unique per tpId, not per environment)

    @Test
    fun testDifferentTpIdCreatesSeperateFiles() {
        val cache1 = IngestConfigCache(cacheStorage, "tp-1")
        val cache2 = IngestConfigCache(cacheStorage, "tp-2")

        try {
            cache1.save("""{"sample_rate": 1}""")
            cache2.save("""{"sample_rate": 2}""")

            assertEquals(1, cache1.loadIfValid()?.sampleRate)
            assertEquals(2, cache2.loadIfValid()?.sampleRate)
        } finally {
            cache1.clear()
            cache2.clear()
        }
    }

    // Complex config

    @Test
    fun testLoadConfigWithAllFields() {
        val jsonContent = """
            {
                "sample_rate": 5,
                "environment_rates": {"PRODUCTION": 1, "STAGING": 10},
                "options": {
                    "useAdaptiveSampling": true,
                    "adaptiveSamplingPatterns": ["pattern1"]
                }
            }
        """.trimIndent()

        cache.save(jsonContent)
        val loaded = cache.loadIfValid()

        assertNotNull(loaded)
        assertEquals(5, loaded.sampleRate)
        assertEquals(1, loaded.getSamplingRate("PRODUCTION"))
        assertEquals(10, loaded.getSamplingRate("STAGING"))
        assertEquals(5, loaded.getSamplingRate("UNKNOWN"))
        assertTrue(loaded.isAdaptiveSamplingEnabled())
    }
}
