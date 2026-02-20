// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

/**
 * Log levels for the unified logging system.
 */
enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}

/**
 * Common logging interface used across shared module and platform SDKs.
 * Platform-specific implementations (PlatformLogger) handle actual logging.
 */
interface Logger {
    fun v(msg: String)  // verbose
    fun d(msg: String)  // debug
    fun i(msg: String)  // info
    fun w(msg: String)  // warn
    fun e(msg: String)  // error
}

/**
 * Platform-specific logger implementation.
 * Android uses android.util.Log, iOS uses NSLog.
 */
expect class PlatformLogger() : Logger {
    override fun v(msg: String)
    override fun d(msg: String)
    override fun i(msg: String)
    override fun w(msg: String)
    override fun e(msg: String)
}
