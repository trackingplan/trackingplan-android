// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import kotlin.test.*

class StorageTest : BaseTest() {

    private val testTpId = "test-tp-id"
    private val testEnvironment1 = "env-1"
    private val testEnvironment2 = "env-2"

    @AfterTest
    fun cleanup() {
        try {
            Storage.create(testTpId, testEnvironment1).clear()
        } catch (_: Exception) {}
        try {
            Storage.create(testTpId, testEnvironment2).clear()
        } catch (_: Exception) {}
        try {
            Storage.create("other-tp-id", testEnvironment1).clear()
        } catch (_: Exception) {}
        CacheTimestampHelper.clearAll()
    }

    @Test
    fun testConfigCachePreservedWhenOnlyEnvironmentChanges() {
        // Given: storage with cached config for tpId + env1
        val storage1 = Storage.create(testTpId, testEnvironment1)
        storage1.ingestConfigCache.save("""{"sample_rate": 1}""")
        val originalTimestamp = storage1.ingestConfigCache.getDownloadedAt()
        assertTrue(originalTimestamp >= 0, "Config should be cached")

        // When: creating storage with same tpId but different environment
        val storage2 = Storage.create(testTpId, testEnvironment2)

        // Then: config cache should be preserved (timestamp should still be valid)
        val preservedTimestamp = storage2.ingestConfigCache.getDownloadedAt()
        assertEquals(originalTimestamp, preservedTimestamp,
            "Config cache timestamp should be preserved when only environment changes")
    }

    @Test
    fun testConfigCacheClearedWhenTpIdChanges() {
        // Given: storage with cached config for tpId1
        val storage1 = Storage.create(testTpId, testEnvironment1)
        storage1.ingestConfigCache.save("""{"sample_rate": 1}""")
        val originalTimestamp = storage1.ingestConfigCache.getDownloadedAt()
        assertTrue(originalTimestamp >= 0, "Config should be cached")

        // When: creating storage with different tpId
        val storage2 = Storage.create("other-tp-id", testEnvironment1)

        // Then: config cache should be cleared (different tpId = different config)
        val newTimestamp = storage2.ingestConfigCache.getDownloadedAt()
        assertEquals(-1, newTimestamp,
            "Config cache should be cleared when tpId changes")
    }

    @Test
    fun testSessionDataClearedWhenEnvironmentChanges() {
        // Given: storage with session data for env1
        val storage1 = Storage.create(testTpId, testEnvironment1)
        storage1.saveTrackingEnabled(true)
        assertTrue(storage1.loadTrackingEnabled())

        // When: creating storage with same tpId but different environment
        val storage2 = Storage.create(testTpId, testEnvironment2)

        // Then: session data should be cleared
        assertFalse(storage2.loadTrackingEnabled(),
            "Session data should be cleared when environment changes")
    }

    @Test
    fun testSaveAndLoadSessionFromStorage() {
        val storage = Storage.create(testTpId, testEnvironment1)
        val session = TrackingplanSession.newSession(
            samplingRate = 10,
            trackingEnabled = true,
            samplingOptions = SamplingOptions(
                useAdaptiveSampling = true,
                adaptiveSamplingPatterns = listOf("""{"provider":"amplitude","sample_rate":5}""")
            )
        )

        storage.saveSession(session)
        val loadedSession = storage.loadSession()

        assertEquals(session.sessionId, loadedSession.sessionId)
        assertEquals(session.samplingRate, loadedSession.samplingRate)
        assertEquals(session.trackingEnabled, loadedSession.trackingEnabled)
        assertEquals(session.samplingOptions, loadedSession.samplingOptions)
    }

    @Test
    fun testSaveAndLoadSessionWithDefaultSamplingOptions() {
        val storage = Storage.create(testTpId, testEnvironment1)
        val session = TrackingplanSession.newSession(
            samplingRate = 1,
            trackingEnabled = true,
            samplingOptions = SamplingOptions()
        )

        storage.saveSession(session)
        val loadedSession = storage.loadSession()

        assertEquals(session.sessionId, loadedSession.sessionId)
        assertEquals(SamplingOptions(), loadedSession.samplingOptions)
        assertFalse(loadedSession.samplingOptions.useAdaptiveSampling)
    }

    @Test
    fun testSaveAndLoadTrackingEnabled() {
        val storage = Storage.create(testTpId, testEnvironment1)

        // Default should be false
        assertFalse(storage.loadTrackingEnabled())

        storage.saveTrackingEnabled(true)
        assertTrue(storage.loadTrackingEnabled())

        storage.saveTrackingEnabled(false)
        assertFalse(storage.loadTrackingEnabled())
    }

    @Test
    fun testFirstTimeExecution() {
        val storage = Storage.create(testTpId, testEnvironment1)

        assertTrue(storage.isFirstTimeExecution())
        storage.saveFirstTimeExecutionNow()
        assertFalse(storage.isFirstTimeExecution())
    }
}
