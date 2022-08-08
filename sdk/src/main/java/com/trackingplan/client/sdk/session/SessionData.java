// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.session;

import androidx.annotation.NonNull;

/**
 * Immutable class
 */
final public class SessionData {

    private final String tpId;
    private final float samplingRate;
    private final boolean trackingEnabled;
    private final long createdAt;

    public SessionData(
            @NonNull String tpId,
            float samplingRate,
            boolean trackingEnabled,
            long createdAt
    ) {
        this.tpId = tpId;
        this.samplingRate = samplingRate;
        this.trackingEnabled = trackingEnabled;
        this.createdAt = createdAt;
    }

    public String getTpId() {
        return tpId;
    }

    public float getSamplingRate() {
        return samplingRate;
    }

    public boolean isTrackingEnabled() {
        return trackingEnabled;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    @NonNull
    public String toString() {
        return "SessionData{" +
                "tpId='" + tpId + '\'' +
                ", samplingRate=" + samplingRate +
                ", trackingEnabled=" + trackingEnabled +
                ", createdAt=" + createdAt +
                '}';
    }
}
