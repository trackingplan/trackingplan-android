// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import platform.Foundation.NSDate
import platform.Foundation.NSProcessInfo
import platform.Foundation.timeIntervalSince1970

/**
 * System time provider using iOS platform APIs.
 */
actual class SystemTimeProvider : TimeProvider {
    actual override fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
    actual override fun elapsedRealTime(): Long = (NSProcessInfo.processInfo.systemUptime * 1000).toLong()
}

/**
 * Test time provider for dependency injection in tests.
 * Allows setting and advancing time values for deterministic testing.
 */
actual class TestTimeProvider : TimeProvider {
    private var _elapsedRealTime: Long = 0
    private var _currentTimeMillis: Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

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
