// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

/**
 * Interface for time operations, allowing injection of fake time in tests.
 */
interface TimeProvider {
    /** Returns current wall-clock time in milliseconds since Unix epoch. */
    fun currentTimeMillis(): Long

    /** Returns elapsed time since device boot in milliseconds. Not affected by wall-clock changes. */
    fun elapsedRealTime(): Long

    companion object {
        const val MILLISECOND: Long = 1
        const val SECOND: Long = 1000 * MILLISECOND
        const val MINUTE: Long = 60 * SECOND
        const val HOUR: Long = 60 * MINUTE
    }
}

/**
 * System time provider using platform-specific implementations.
 */
expect class SystemTimeProvider() : TimeProvider {
    override fun currentTimeMillis(): Long
    override fun elapsedRealTime(): Long
}

/**
 * Test time provider for dependency injection in tests.
 * Allows setting and advancing time values for deterministic testing.
 */
expect class TestTimeProvider() : TimeProvider {
    override fun currentTimeMillis(): Long
    override fun elapsedRealTime(): Long
    fun setCurrentTimeMillis(time: Long)
    fun setElapsedRealTime(time: Long)
    fun advanceTime(ms: Long)
    fun simulateReboot()
}
