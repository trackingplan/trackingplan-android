// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.session;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.TrackingplanClient;

import java.util.concurrent.Callable;

final public class FetchSessionDataTask implements Callable<SessionData> {

    private final String tpId;
    private final TrackingplanClient client;

    public FetchSessionDataTask(@NonNull String tpId, @NonNull TrackingplanClient client) {
        this.tpId = tpId;
        this.client = client;
    }

    @Override
    public SessionData call() throws Exception {
        final float samplingRate = client.getSamplingRate();
        final boolean isTrackedUser = shouldTrackUser(samplingRate);
        return new SessionData(tpId, samplingRate, isTrackedUser, System.currentTimeMillis());
    }

    private boolean shouldTrackUser(float samplingRate) {
        return Math.random() < (1 / samplingRate);
    }
}
