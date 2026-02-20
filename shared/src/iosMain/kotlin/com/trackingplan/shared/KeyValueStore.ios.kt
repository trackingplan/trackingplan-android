// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of KeyValueStore using NSUserDefaults.
 */
actual class KeyValueStore private constructor(
    private val name: String,
    private val userDefaults: NSUserDefaults
) {
    actual companion object {
        @Throws(Exception::class)
        actual fun create(name: String): KeyValueStore {
            val defaults = NSUserDefaults(suiteName = name)
                ?: throw IllegalStateException("KeyValueStore: Failed to create NSUserDefaults with suite name: $name")
            return KeyValueStore(name, defaults)
        }
    }

    actual fun getString(key: String, defaultValue: String?): String? {
        return userDefaults.stringForKey(key) ?: defaultValue
    }

    actual fun setString(key: String, value: String) {
        userDefaults.setObject(value, forKey = key)
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        val obj = userDefaults.objectForKey(key) ?: return defaultValue
        return (obj as? Number)?.toInt() ?: defaultValue
    }

    actual fun setInt(key: String, value: Int) {
        userDefaults.setInteger(value.toLong(), forKey = key)
    }

    actual fun getLong(key: String, defaultValue: Long): Long {
        val obj = userDefaults.objectForKey(key) ?: return defaultValue
        return (obj as? Number)?.toLong() ?: defaultValue
    }

    actual fun setLong(key: String, value: Long) {
        userDefaults.setObject(value, forKey = key)
    }

    actual fun getFloat(key: String, defaultValue: Float): Float {
        val obj = userDefaults.objectForKey(key) ?: return defaultValue
        return (obj as? Number)?.toFloat() ?: defaultValue
    }

    actual fun setFloat(key: String, value: Float) {
        userDefaults.setFloat(value, forKey = key)
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val obj = userDefaults.objectForKey(key) ?: return defaultValue
        return when (obj) {
            is Boolean -> obj
            is Number -> obj.toInt() != 0
            else -> defaultValue
        }
    }

    actual fun setBoolean(key: String, value: Boolean) {
        userDefaults.setBool(value, forKey = key)
    }

    actual fun remove(key: String) {
        userDefaults.removeObjectForKey(key)
    }

    actual fun clear() {
        userDefaults.removePersistentDomainForName(name)
    }

    actual fun contains(key: String): Boolean {
        return userDefaults.objectForKey(key) != null
    }
}
