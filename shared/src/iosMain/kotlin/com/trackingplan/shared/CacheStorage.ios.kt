// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*

/**
 * iOS implementation of CacheStorage using the app's Caches directory.
 * Timestamps are stored in a separate KeyValueStore via CacheTimestampHelper.
 */
@OptIn(ExperimentalForeignApi::class)
actual class CacheStorage actual constructor() {

    private fun getCacheFileURL(filename: String): NSURL {
        require(!filename.contains('/') && !filename.contains('\\')) {
            "CacheStorage: Invalid filename - path separators not allowed"
        }
        val paths = NSSearchPathForDirectoriesInDomains(
            NSCachesDirectory, NSUserDomainMask, true
        )
        val cachesDir = paths.firstOrNull() as? String
            ?: throw IllegalStateException("CacheStorage: Unable to get cache directory")
        return NSURL.fileURLWithPath("$cachesDir/$filename")
    }

    @Throws(Exception::class)
    actual fun save(filename: String, content: String) {
        val url = getCacheFileURL(filename)
        val nsString = content as NSString
        val success = nsString.writeToURL(
            url,
            atomically = true,
            encoding = NSUTF8StringEncoding,
            error = null
        )
        if (!success) {
            throw Exception("CacheStorage: Failed to write file: $filename")
        }
        CacheTimestampHelper.saveTimestamp(filename)
    }

    @Throws(Exception::class)
    actual fun loadIfValid(filename: String, maxAgeMs: Long): String? {
        val url = getCacheFileURL(filename)
        val path = url.path ?: return null

        if (!NSFileManager.defaultManager.fileExistsAtPath(path)) return null

        if (CacheTimestampHelper.isExpired(filename, maxAgeMs)) return null

        return NSString.stringWithContentsOfURL(url, encoding = NSUTF8StringEncoding, error = null)
    }

    @Throws(Exception::class)
    actual fun getTimestamp(filename: String): Long {
        return CacheTimestampHelper.getTimestamp(filename)
    }

    @Throws(Exception::class)
    actual fun clear(filename: String) {
        val path = getCacheFileURL(filename).path ?: return
        NSFileManager.defaultManager.removeItemAtPath(path, error = null)
        CacheTimestampHelper.clearTimestamp(filename)
    }
}
