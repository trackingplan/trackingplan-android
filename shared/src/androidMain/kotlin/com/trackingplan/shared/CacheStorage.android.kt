// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import java.io.File

/**
 * Android implementation of CacheStorage using the app's cache directory.
 * Timestamps are stored in a separate KeyValueStore via CacheTimestampHelper.
 */
actual class CacheStorage actual constructor() {

    private fun getCacheFile(filename: String): File {
        require(!filename.contains('/') && !filename.contains('\\')) {
            "CacheStorage: Invalid filename - path separators not allowed"
        }
        return File(ContextProvider.applicationContext.cacheDir, filename)
    }

    @Throws(Exception::class)
    actual fun save(filename: String, content: String) {
        getCacheFile(filename).writeText(content)
        CacheTimestampHelper.saveTimestamp(filename)
    }

    @Throws(Exception::class)
    actual fun loadIfValid(filename: String, maxAgeMs: Long): String? {
        val file = getCacheFile(filename)
        if (!file.exists()) return null

        if (CacheTimestampHelper.isExpired(filename, maxAgeMs)) return null

        return try {
            file.readText()
        } catch (e: Exception) {
            null
        }
    }

    @Throws(Exception::class)
    actual fun getTimestamp(filename: String): Long {
        return CacheTimestampHelper.getTimestamp(filename)
    }

    @Throws(Exception::class)
    actual fun clear(filename: String) {
        getCacheFile(filename).delete()
        CacheTimestampHelper.clearTimestamp(filename)
    }
}
