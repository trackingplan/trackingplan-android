// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import android.os.SystemClock

/**
 * System time provider using Android platform APIs.
 */
actual class SystemTimeProvider : TimeProvider {
    actual override fun currentTimeMillis(): Long = System.currentTimeMillis()
    actual override fun elapsedRealTime(): Long = SystemClock.elapsedRealtime()
}

/**
 * Test time provider for dependency injection in tests.
 * Allows setting and advancing time values for deterministic testing.
 */
actual class TestTimeProvider : TimeProvider {
    private var _elapsedRealTime: Long = 0
    private var _currentTimeMillis: Long = System.currentTimeMillis()

    actual override fun currentTimeMillis(): Long = _currentTimeMillis
    actual override fun elapsedRealTime(): Long = _elapsedRealTime

    actual fun setCurrentTimeMillis(time: Long) {
        _currentTimeMillis = time
    }

    actual fun setElapsedRealTime(time: Long) {
        _elapsedRealTime = time
    }

    actual fun advanceTime(ms: Long) {
        _currentTimeMillis += ms
        _elapsedRealTime += ms
    }

    actual fun simulateReboot() {
        _elapsedRealTime = 0
    }
}
