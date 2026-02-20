// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import kotlinx.cinterop.ExperimentalForeignApi
import oslog.tp_os_log
import oslog.tp_os_log_create
import oslog.tp_os_log_type_debug
import oslog.tp_os_log_type_default
import oslog.tp_os_log_type_error
import oslog.tp_os_log_type_info

/**
 * iOS implementation using Apple's unified logging (os_log) with proper levels.
 * Similar to Android's PlatformLogger using android.util.Log.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformLogger : Logger {
    private val log = tp_os_log_create("com.trackingplan.sdk", "Trackingplan")

    actual override fun v(msg: String) = tp_os_log(log, tp_os_log_type_debug(), "[VERBOSE] $msg")
    actual override fun d(msg: String) = tp_os_log(log, tp_os_log_type_info(), "[DEBUG] $msg")
    actual override fun i(msg: String) = tp_os_log(log, tp_os_log_type_default(), "[INFO] $msg")
    actual override fun w(msg: String) = tp_os_log(log, tp_os_log_type_default(), "[WARN] $msg")
    actual override fun e(msg: String) = tp_os_log(log, tp_os_log_type_error(), "[ERROR] $msg")
}
