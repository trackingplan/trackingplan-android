package com.example.gtm;

import android.app.Application;

import com.trackingplan.client.sdk.Trackingplan;

import java.util.HashMap;
public class ExampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Trackingplan.init("YOUR_TP_ID")
                // .environment("PRODUCTION")
                .tags(new HashMap<>() {{
                    put("tag1", "value1");
                }})
                .enableDebug()
                .dryRun()
                .start(this);
        // Trackingplan.stop();
    }
}
