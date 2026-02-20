// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

/**
 * Internal helper for cache timestamp operations.
 * Owns its own KeyValueStore separate from the main Storage store.
 * This allows cache timestamps to persist when session data is cleared.
 */
internal object CacheTimestampHelper {
    private const val CACHE_STORE_NAME = "com.trackingplan.sdk.cache"
    private val store: KeyValueStore by lazy { KeyValueStore.create(CACHE_STORE_NAME) }

    private fun timestampKey(filename: String) = "cache_timestamp_$filename"

    fun saveTimestamp(filename: String) {
        store.setLong(timestampKey(filename), ServiceLocator.getTimeProvider().currentTimeMillis())
    }

    fun getTimestamp(filename: String): Long {
        return store.getLong(timestampKey(filename), -1)
    }

    fun isExpired(filename: String, maxAgeMs: Long): Boolean {
        val cachedAt = getTimestamp(filename)
        if (cachedAt < 0) return true

        val age = ServiceLocator.getTimeProvider().currentTimeMillis() - cachedAt
        return age >= maxAgeMs
    }

    fun clearTimestamp(filename: String) {
        store.remove(timestampKey(filename))
    }

    /**
     * Clears all cache timestamps. Called when tpId changes.
     */
    fun clearAll() {
        store.clear()
    }
}

/**
 * Generic file-based cache storage with timestamp-based expiration.
 * Platform implementations use the system cache directory for file storage.
 * Timestamps are stored in a separate KeyValueStore via CacheTimestampHelper.
 */
expect class CacheStorage() {
    /**
     * Saves content to a cache file.
     * @param filename The name of the cache file
     * @param content The content to save
     * @throws Exception if the file cannot be written
     */
    @Throws(Exception::class)
    fun save(filename: String, content: String)

    /**
     * Loads content from a cache file if it exists and is not expired.
     * @param filename The name of the cache file
     * @param maxAgeMs Maximum age in milliseconds before considering expired
     * @return The content if valid, null if missing or expired
     * @throws Exception if the cache directory cannot be accessed
     */
    @Throws(Exception::class)
    fun loadIfValid(filename: String, maxAgeMs: Long): String?

    /**
     * Returns the cached timestamp.
     * @param filename The name of the cache file
     * @return Timestamp in milliseconds, or -1 if not cached
     * @throws Exception if the cache directory cannot be accessed
     */
    @Throws(Exception::class)
    fun getTimestamp(filename: String): Long

    /**
     * Deletes a cache file and its timestamp.
     * @param filename The name of the cache file
     * @throws Exception if the cache directory cannot be accessed
     */
    @Throws(Exception::class)
    fun clear(filename: String)
}
