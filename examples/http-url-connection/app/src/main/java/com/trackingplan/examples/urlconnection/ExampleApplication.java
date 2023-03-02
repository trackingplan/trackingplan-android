package com.trackingplan.examples.urlconnection;

import android.app.Application;

import com.trackingplan.client.sdk.Trackingplan;

public class ExampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        System.out.println("ExampleApplication::onCreate");
        Trackingplan.init("YOUR_TP_ID")
                // .environment("PRODUCTION")
                .enableDebug()
                .dryRun()
                .start(this);
        // Trackingplan.stop();
    }
}
