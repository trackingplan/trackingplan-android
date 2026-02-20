// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

/**
 * Cache for the ingest configuration JSON file.
 * Uses CacheStorage for file-based persistence with 24-hour expiration.
 *
 * - save() stores the raw JSON without validation
 * - loadIfValid() parses and validates on read
 */
class IngestConfigCache(
    private val cacheStorage: CacheStorage,
    private val tpId: String
) {
    companion object {
        private const val FILENAME_PREFIX = "ingest_config_"
        private const val FILENAME_SUFFIX = ".json"
        const val CONFIG_MAX_AGE_MS: Long = 24 * 3600 * 1000 // 24 hours
    }

    private val filename: String = "$FILENAME_PREFIX$tpId$FILENAME_SUFFIX"

    /**
     * Saves the raw JSON configuration to cache.
     * Does not validate or parse the content.
     *
     * @param jsonContent The raw JSON string from the config endpoint
     * @throws Exception if saving fails
     */
    @Throws(Exception::class)
    fun save(jsonContent: String) {
        cacheStorage.save(filename, jsonContent)
    }

    /**
     * Loads and parses the cached configuration if valid (not expired).
     *
     * @return The parsed config, or null if cache is missing, expired, or corrupted
     */
    fun loadIfValid(): TrackingplanIngestConfig? {
        return try {
            val json = cacheStorage.loadIfValid(filename, CONFIG_MAX_AGE_MS)
            json?.let { TrackingplanIngestConfigParser.parse(it) }
        } catch (e: Exception) {
            // Cache corrupted or parse failed - return null to trigger fresh download
            null
        }
    }

    /**
     * Returns the timestamp when the config was cached.
     *
     * @return Timestamp in milliseconds, or -1 if not cached
     */
    fun getDownloadedAt(): Long {
        return try {
            cacheStorage.getTimestamp(filename)
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * Checks if the cached config has expired (older than 24 hours).
     *
     * @return true if expired or not cached, false if still valid
     */
    fun hasExpired(): Boolean {
        val downloadedAt = getDownloadedAt()
        if (downloadedAt < 0) return true
        val timeProvider = ServiceLocator.getTimeProvider()
        return timeProvider.currentTimeMillis() >= downloadedAt + CONFIG_MAX_AGE_MS
    }

    /**
     * Clears the cached configuration and its timestamp.
     */
    fun clear() {
        try {
            cacheStorage.clear(filename)
        } catch (e: Exception) {
            // Ignore clear failures
        }
    }
}
