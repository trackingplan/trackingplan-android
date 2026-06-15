// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import java.io.File

actual class CacheStorage actual constructor() {
    private val cacheDirectory = File(
        System.getProperty("java.io.tmpdir"),
        "trackingplan-shared-cache"
    )

    private fun getCacheFile(filename: String): File {
        require(!filename.contains('/') && !filename.contains('\\')) {
            "CacheStorage: Invalid filename - path separators not allowed"
        }
        return File(cacheDirectory, filename)
    }

    @Throws(Exception::class)
    actual fun save(filename: String, content: String) {
        cacheDirectory.mkdirs()
        getCacheFile(filename).writeText(content)
        CacheTimestampHelper.saveTimestamp(filename)
    }

    @Throws(Exception::class)
    actual fun loadIfValid(filename: String, maxAgeMs: Long): String? {
        val file = getCacheFile(filename)
        if (!file.exists()) return null
        if (CacheTimestampHelper.isExpired(filename, maxAgeMs)) return null
        return file.readText()
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
