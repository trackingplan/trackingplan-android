package com.trackingplan.client.sdk.session;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.util.ServiceLocator;
import com.trackingplan.client.sdk.util.Time;

import java.util.Objects;

public final class SamplingRate {

    public final static SamplingRate EMPTY = new SamplingRate(Float.MAX_VALUE, 0, false);
    public static final long SAMPLING_RATE_LIFE_TIME = 24 * 3600 * 1000; // 24 hours

    private final float samplingRate;
    private final long downloadedAt;
    private final boolean trackingEnabled;

    // Used to create a new sampling rate after downloading it
    public SamplingRate(float samplingRate) {
        final var time = ServiceLocator.getSharedInstance(Time.class);
        this.samplingRate = samplingRate;
        downloadedAt = time.currentTimeMillis();
        trackingEnabled = Math.random() < (1 / samplingRate);
    }

    // Used to restore sampling rate from storage
    public SamplingRate(float samplingRate, long downloadedAt, boolean trackingEnabled) {
        this.samplingRate = samplingRate;
        this.downloadedAt = downloadedAt;
        this.trackingEnabled = trackingEnabled;
    }

    public float getValue() {
        return samplingRate;
    }

    public long getDownloadedAt() {
        return downloadedAt;
    }

    public boolean hasExpired() {
        final var time = ServiceLocator.getSharedInstance(Time.class);
        return time.currentTimeMillis() >= downloadedAt + SAMPLING_RATE_LIFE_TIME;
    }

    public boolean isTrackingEnabled() {
        return trackingEnabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SamplingRate that = (SamplingRate) o;
        return Float.compare(that.samplingRate, samplingRate) == 0 && downloadedAt == that.downloadedAt && trackingEnabled == that.trackingEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(samplingRate, downloadedAt, trackingEnabled);
    }

    @NonNull
    @Override
    public String toString() {
        return "SamplingRate{" +
                "samplingRate=" + samplingRate +
                ", downloadedAt=" + downloadedAt +
                ", trackingEnabled=" + trackingEnabled +
                '}';
    }
}
