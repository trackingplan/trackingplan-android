// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import android.content.Context

/**
 * Provider for application context needed by Android implementations.
 * Initialized automatically by TrackingplanInitializer via App Startup Library.
 */
object ContextProvider {
    lateinit var applicationContext: Context
        private set

    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}
