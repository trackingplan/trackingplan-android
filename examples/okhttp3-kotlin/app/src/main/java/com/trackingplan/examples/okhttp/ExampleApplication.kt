package com.trackingplan.examples.okhttp

import android.app.Application
import com.trackingplan.client.sdk.Trackingplan

class ExampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Trackingplan.init("YOUR_TP_ID")
            .enableDebug()
            .dryRun()
            .start(this)
    }
}