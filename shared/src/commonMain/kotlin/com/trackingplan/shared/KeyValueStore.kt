// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

/**
 * Platform-agnostic key-value storage interface.
 * Each platform implements this using its native persistence mechanism
 * (SharedPreferences on Android, UserDefaults on iOS).
 *
 * Use [KeyValueStore.create] to instantiate.
 */
expect class KeyValueStore {

    companion object {
        /**
         * Creates a KeyValueStore instance.
         * @param name The name/identifier for this storage (suite name on iOS, prefs name on Android)
         * @throws Exception if the storage cannot be created
         */
        @Throws(Exception::class)
        fun create(name: String): KeyValueStore
    }

    // String operations
    fun getString(key: String, defaultValue: String?): String?
    fun setString(key: String, value: String)

    // Int operations
    fun getInt(key: String, defaultValue: Int): Int
    fun setInt(key: String, value: Int)

    // Long operations
    fun getLong(key: String, defaultValue: Long): Long
    fun setLong(key: String, value: Long)

    // Float operations
    fun getFloat(key: String, defaultValue: Float): Float
    fun setFloat(key: String, value: Float)

    // Boolean operations
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun setBoolean(key: String, value: Boolean)

    // Utility operations
    fun remove(key: String)
    fun clear()
    fun contains(key: String): Boolean
}
