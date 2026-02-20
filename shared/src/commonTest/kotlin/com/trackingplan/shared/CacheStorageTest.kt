// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import kotlin.test.*

class CacheStorageTest : BaseTest() {

    private lateinit var cacheStorage: CacheStorage
    private val testFiles = mutableListOf<String>()

    private fun testFilename(name: String): String {
        val filename = "test-cache-$name-${kotlin.random.Random.nextInt()}.json"
        testFiles.add(filename)
        return filename
    }

    @BeforeTest
    fun setup() {
        cacheStorage = CacheStorage()
    }

    @AfterTest
    fun cleanup() {
        testFiles.forEach { filename ->
            try { cacheStorage.clear(filename) } catch (_: Exception) {}
        }
        testFiles.clear()
        CacheTimestampHelper.clearAll()
    }

    // Basic Operations

    @Test
    fun testSaveAndLoad() {
        val filename = testFilename("save-load")
        val content = """{"key": "value"}"""

        cacheStorage.save(filename, content)
        val loaded = cacheStorage.loadIfValid(filename, Long.MAX_VALUE)

        assertEquals(content, loaded)
    }

    @Test
    fun testLoadNonExistentFile() {
        val filename = testFilename("non-existent")
        val loaded = cacheStorage.loadIfValid(filename, Long.MAX_VALUE)
        assertNull(loaded)
    }

    @Test
    fun testClear() {
        val filename = testFilename("clear")
        cacheStorage.save(filename, "content")
        cacheStorage.clear(filename)
        val loaded = cacheStorage.loadIfValid(filename, Long.MAX_VALUE)
        assertNull(loaded)
    }

    @Test
    fun testGetTimestamp() {
        val filename = testFilename("timestamp")
        val beforeSave = currentTimeMs()
        cacheStorage.save(filename, "content")
        val afterSave = currentTimeMs()

        val timestamp = cacheStorage.getTimestamp(filename)

        assertTrue(timestamp >= beforeSave - 1000) // Allow 1s tolerance
        assertTrue(timestamp <= afterSave + 1000)
    }

    @Test
    fun testGetTimestampNonExistentFile() {
        val filename = testFilename("timestamp-missing")
        val timestamp = cacheStorage.getTimestamp(filename)
        assertEquals(-1, timestamp)
    }

    // Expiration Logic

    @Test
    fun testLoadIfValidNotExpired() {
        val filename = testFilename("not-expired")
        cacheStorage.save(filename, "content")

        val loaded = cacheStorage.loadIfValid(filename, 60 * 60 * 1000) // 1 hour

        assertEquals("content", loaded)
    }

    @Test
    fun testLoadIfValidExpired() {
        val filename = testFilename("expired")
        cacheStorage.save(filename, "content")

        // Advance fake time by 2 seconds to make the file appear expired
        val fakeTime = TestTimeProvider()
        fakeTime.setCurrentTimeMillis(currentTimeMs() + 2000)
        ServiceLocator.setTimeProvider(fakeTime)

        try {
            // File should be expired with maxAge of 1000ms since fakeTime is 2s ahead
            val loaded = cacheStorage.loadIfValid(filename, 1000)
            assertNull(loaded)
        } finally {
            ServiceLocator.reset()
        }
    }

    // Edge Cases

    @Test
    fun testSaveEmptyContent() {
        val filename = testFilename("empty")
        cacheStorage.save(filename, "")
        val loaded = cacheStorage.loadIfValid(filename, Long.MAX_VALUE)
        assertEquals("", loaded)
    }

    @Test
    fun testSaveSpecialCharacters() {
        val filename = testFilename("special-chars")
        val content = """{"emoji": "🎉", "unicode": "日本語", "quotes": "\"hello\""}"""

        cacheStorage.save(filename, content)
        val loaded = cacheStorage.loadIfValid(filename, Long.MAX_VALUE)

        assertEquals(content, loaded)
    }

    @Test
    fun testMultipleFiles() {
        val file1 = testFilename("multi-1")
        val file2 = testFilename("multi-2")

        cacheStorage.save(file1, "content1")
        cacheStorage.save(file2, "content2")

        assertEquals("content1", cacheStorage.loadIfValid(file1, Long.MAX_VALUE))
        assertEquals("content2", cacheStorage.loadIfValid(file2, Long.MAX_VALUE))

        cacheStorage.clear(file1)

        assertNull(cacheStorage.loadIfValid(file1, Long.MAX_VALUE))
        assertEquals("content2", cacheStorage.loadIfValid(file2, Long.MAX_VALUE))
    }

    @Test
    fun testOverwriteExistingFile() {
        val filename = testFilename("overwrite")

        cacheStorage.save(filename, "original")
        cacheStorage.save(filename, "updated")

        val loaded = cacheStorage.loadIfValid(filename, Long.MAX_VALUE)
        assertEquals("updated", loaded)
    }

    private fun currentTimeMs(): Long {
        return ServiceLocator.getTimeProvider().currentTimeMillis()
    }
}
