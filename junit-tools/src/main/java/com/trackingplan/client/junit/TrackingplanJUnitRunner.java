package com.trackingplan.client.junit;

import android.app.Application;

import androidx.test.runner.AndroidJUnitRunner;

import com.trackingplan.client.sdk.Trackingplan;
import com.trackingplan.client.sdk.TrackingplanInstance;
import com.trackingplan.client.sdk.util.AndroidLogger;

public class TrackingplanJUnitRunner extends AndroidJUnitRunner {

    private static final AndroidLogger logger = new AndroidLogger("TrackingplanJUnitRunner");

    @Override
    public void callApplicationOnCreate(Application app) {
        logger.info("Launching application...");
        Trackingplan.enableInstrumentedTestMode();
        super.callApplicationOnCreate(app);
    }
}
