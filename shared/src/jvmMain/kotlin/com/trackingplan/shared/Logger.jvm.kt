// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

actual class PlatformLogger : Logger {
    actual override fun v(msg: String) {
        println("VERBOSE Trackingplan: $msg")
    }

    actual override fun d(msg: String) {
        println("DEBUG Trackingplan: $msg")
    }

    actual override fun i(msg: String) {
        println("INFO Trackingplan: $msg")
    }

    actual override fun w(msg: String) {
        println("WARN Trackingplan: $msg")
    }

    actual override fun e(msg: String) {
        println("ERROR Trackingplan: $msg")
    }
}
