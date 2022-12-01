// Copyright (c) 2022 Trackingplan
package com.trackingplan.client.sdk;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.startup.Initializer;

import com.trackingplan.client.sdk.util.AndroidLogger;

import java.util.Collections;
import java.util.List;

public class TrackingplanInitializer implements Initializer<TrackingplanInstance> {
    @NonNull
    @Override
    public TrackingplanInstance create(@NonNull Context context) {

        var logger = AndroidLogger.getInstance();

        logger.info("Starting initialization...");

        var instance = new TrackingplanInstance(context.getApplicationContext());
        instance.attachToLifeCycle(ProcessLifecycleOwner.get().getLifecycle());

        TrackingplanInstance.registerInstance(instance);

        logger.info("Trackingplan v" + BuildConfig.SDK_VERSION + " initialized");

        return instance;
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.emptyList();
    }
}
