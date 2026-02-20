// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import android.util.Log

/**
 * Android implementation using android.util.Log directly.
 * Replaces LogcatLogger with a cleaner implementation.
 */
actual class PlatformLogger : Logger {
    private val tag = "Trackingplan"

    actual override fun v(msg: String) {
        Log.v(tag, msg)
    }

    actual override fun d(msg: String) {
        Log.d(tag, msg)
    }

    actual override fun i(msg: String) {
        Log.i(tag, msg)
    }

    actual override fun w(msg: String) {
        Log.w(tag, msg)
    }

    actual override fun e(msg: String) {
        Log.e(tag, msg)
    }
}
