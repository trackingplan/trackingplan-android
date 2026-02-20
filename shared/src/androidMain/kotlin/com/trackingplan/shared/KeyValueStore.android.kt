// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import android.content.Context
import android.content.SharedPreferences

/**
 * Android implementation of KeyValueStore using SharedPreferences.
 */
actual class KeyValueStore private constructor(
    private val preferences: SharedPreferences
) {
    actual companion object {
        @Throws(Exception::class)
        actual fun create(name: String): KeyValueStore {
            val prefs = ContextProvider.applicationContext
                .getSharedPreferences(name, Context.MODE_PRIVATE)
            return KeyValueStore(prefs)
        }
    }

    actual fun getString(key: String, defaultValue: String?): String? {
        return try {
            preferences.getString(key, defaultValue)
        } catch (e: ClassCastException) {
            defaultValue
        }
    }

    actual fun setString(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return try {
            preferences.getInt(key, defaultValue)
        } catch (e: ClassCastException) {
            defaultValue
        }
    }

    actual fun setInt(key: String, value: Int) {
        preferences.edit().putInt(key, value).apply()
    }

    actual fun getLong(key: String, defaultValue: Long): Long {
        return try {
            preferences.getLong(key, defaultValue)
        } catch (e: ClassCastException) {
            defaultValue
        }
    }

    actual fun setLong(key: String, value: Long) {
        preferences.edit().putLong(key, value).apply()
    }

    actual fun getFloat(key: String, defaultValue: Float): Float {
        return try {
            preferences.getFloat(key, defaultValue)
        } catch (e: ClassCastException) {
            defaultValue
        }
    }

    actual fun setFloat(key: String, value: Float) {
        preferences.edit().putFloat(key, value).apply()
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return try {
            preferences.getBoolean(key, defaultValue)
        } catch (e: ClassCastException) {
            defaultValue
        }
    }

    actual fun setBoolean(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    actual fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }

    actual fun clear() {
        preferences.edit().clear().apply()
    }

    actual fun contains(key: String): Boolean {
        return preferences.contains(key)
    }
}
