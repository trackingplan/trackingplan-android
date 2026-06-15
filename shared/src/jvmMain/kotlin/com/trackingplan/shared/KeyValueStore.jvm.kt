// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import java.util.concurrent.ConcurrentHashMap

actual class KeyValueStore private constructor(
    private val values: MutableMap<String, Any>
) {
    actual companion object {
        private val stores = ConcurrentHashMap<String, MutableMap<String, Any>>()

        @Throws(Exception::class)
        actual fun create(name: String): KeyValueStore {
            return KeyValueStore(stores.getOrPut(name) { ConcurrentHashMap() })
        }
    }

    actual fun getString(key: String, defaultValue: String?): String? {
        return values[key] as? String ?: defaultValue
    }

    actual fun setString(key: String, value: String) {
        values[key] = value
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return values[key] as? Int ?: defaultValue
    }

    actual fun setInt(key: String, value: Int) {
        values[key] = value
    }

    actual fun getLong(key: String, defaultValue: Long): Long {
        return values[key] as? Long ?: defaultValue
    }

    actual fun setLong(key: String, value: Long) {
        values[key] = value
    }

    actual fun getFloat(key: String, defaultValue: Float): Float {
        return values[key] as? Float ?: defaultValue
    }

    actual fun setFloat(key: String, value: Float) {
        values[key] = value
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return values[key] as? Boolean ?: defaultValue
    }

    actual fun setBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    actual fun remove(key: String) {
        values.remove(key)
    }

    actual fun clear() {
        values.clear()
    }

    actual fun contains(key: String): Boolean {
        return values.containsKey(key)
    }
}
