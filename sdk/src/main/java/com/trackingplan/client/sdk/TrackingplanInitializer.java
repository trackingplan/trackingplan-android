// Copyright (c) 2022 Trackingplan
package com.trackingplan.client.sdk;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.startup.Initializer;

import com.trackingplan.client.sdk.util.AndroidLog;
import com.trackingplan.client.sdk.util.ServiceLocator;
import com.trackingplan.client.sdk.util.SystemTime;
import com.trackingplan.client.sdk.util.Time;

import java.util.Collections;
import java.util.List;


public class TrackingplanInitializer implements Initializer<TrackingplanInstance> {
    @NonNull
    @Override
    public TrackingplanInstance create(@NonNull Context context) {

        registerSharedInstances();

        final var log = AndroidLog.getInstance();

        log.debug("Launching Trackingplan service...");

        var instance = new TrackingplanInstance(context.getApplicationContext());
        instance.attachToLifeCycle(ProcessLifecycleOwner.get().getLifecycle());
        
        TrackingplanInstance.registerInstance(instance);

        log.info("Trackingplan v" + BuildConfig.SDK_VERSION + " running");

        return instance;
    }

    @NonNull
    @Override
    public List<Class<? extends Initializer<?>>> dependencies() {
        return Collections.emptyList();
    }

    private void registerSharedInstances() {
        ServiceLocator.registerSharedInstance(Time.class, new SystemTime());
    }
}
