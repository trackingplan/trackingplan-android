// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import kotlin.concurrent.Volatile

/**
 * Service locator for dependency injection.
 * Used for injecting TimeProvider and Logger in tests.
 */
object ServiceLocator {
    @Volatile
    private var timeProvider: TimeProvider = SystemTimeProvider()

    @Volatile
    private var logger: Logger = PlatformLogger()

    fun getTimeProvider(): TimeProvider = timeProvider

    fun getLogger(): Logger = logger

    fun setTimeProvider(provider: TimeProvider) {
        timeProvider = provider
    }

    fun setLogger(logger: Logger) {
        this.logger = logger
    }

    fun reset() {
        timeProvider = SystemTimeProvider()
        logger = PlatformLogger()
    }
}
