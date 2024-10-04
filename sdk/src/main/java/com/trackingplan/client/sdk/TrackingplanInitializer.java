// Copyright (c) 2022 Trackingplan
package com.trackingplan.client.sdk;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.startup.Initializer;

import com.trackingplan.client.sdk.util.AndroidLog;
import com.trackingplan.client.sdk.util.ScreenViewTracker;
import com.trackingplan.client.sdk.util.ServiceLocator;
import com.trackingplan.client.sdk.util.SystemTime;
import com.trackingplan.client.sdk.util.Time;

import java.util.Collections;
import java.util.List;


public class TrackingplanInitializer implements Initializer<TrackingplanInstance> {
    @NonNull
    @Override
    public TrackingplanInstance create(@NonNull final Context context) {

        final var logger = AndroidLog.getInstance();

        logger.debug("Launching Trackingplan service...");

        ServiceLocator.registerSharedInstance(Time.class, new SystemTime());

        // Initialize TrackingplanInstance
        var instance = new TrackingplanInstance(context.getApplicationContext());
        instance.attachToLifeCycle(ProcessLifecycleOwner.get().getLifecycle());

        if (context instanceof Application) {
            final var screenViewTracker = new ScreenViewTracker();
            instance.attachToScreenViewTracker(screenViewTracker);
            screenViewTracker.start((Application) context);
            ServiceLocator.registerSharedInstance(ScreenViewTracker.class, screenViewTracker);
        }

        TrackingplanInstance.registerInstance(instance);

        logger.info("Trackingplan v" + BuildConfig.SDK_VERSION + " running");

        return instance;
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.emptyList();
    }
}
