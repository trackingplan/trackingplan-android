package com.trackingplan.client.junit;

import android.app.Application;
import android.util.Log;

import androidx.test.runner.AndroidJUnitRunner;

import com.trackingplan.client.sdk.Trackingplan;
import com.trackingplan.client.sdk.util.AndroidLog;

public class TrackingplanJUnitRunner extends AndroidJUnitRunner {
    @Override
    public void callApplicationOnCreate(Application app) {
        Log.i("TrackingplanJUnitRunner", "Launching application...");
        Trackingplan.enableInstrumentedTestMode();
        super.callApplicationOnCreate(app);
    }
}
