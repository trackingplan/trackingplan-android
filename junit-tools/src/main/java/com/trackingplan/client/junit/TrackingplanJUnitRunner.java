package com.trackingplan.client.junit;

import android.app.Application;
import android.util.Log;

import androidx.test.runner.AndroidJUnitRunner;

import com.trackingplan.client.sdk.TrackingplanInstance;

public class TrackingplanJUnitRunner extends AndroidJUnitRunner {
    @Override
    public void callApplicationOnCreate(Application app) {
        Log.i("TrackingplanJUnitRunner", "Launching application...");
        final var instance = TrackingplanInstance.getInstance();
        if (instance != null) {
            instance.setRuntimeEnvironment(TrackingplanInstance.RuntimeEnvironment.AndroidJUnit);
        } else {
            Log.e("TrackingplanJUnitRunner", "Couldn't set up the runtime environment because TrackingplanInstance is not registered.");
        }
        super.callApplicationOnCreate(app);
    }
}
