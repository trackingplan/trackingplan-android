// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

actual class SystemTimeProvider : TimeProvider {
    actual override fun currentTimeMillis(): Long = System.currentTimeMillis()

    actual override fun elapsedRealTime(): Long = System.nanoTime() / 1_000_000
}

actual class TestTimeProvider : TimeProvider {
    private var elapsedRealTime: Long = 0
    private var currentTimeMillis: Long = System.currentTimeMillis()

    actual override fun currentTimeMillis(): Long = currentTimeMillis

    actual override fun elapsedRealTime(): Long = elapsedRealTime

    actual fun setCurrentTimeMillis(time: Long) {
        currentTimeMillis = time
    }

    actual fun setElapsedRealTime(time: Long) {
        elapsedRealTime = time
    }

    actual fun advanceTime(ms: Long) {
        currentTimeMillis += ms
        elapsedRealTime += ms
    }

    actual fun simulateReboot() {
        elapsedRealTime = 0
    }
}
