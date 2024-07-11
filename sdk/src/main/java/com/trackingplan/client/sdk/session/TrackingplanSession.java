// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.session;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.util.ServiceLocator;
import com.trackingplan.client.sdk.util.Time;

import java.util.Objects;
import java.util.UUID;

final public class TrackingplanSession {

    public final static TrackingplanSession EMPTY = new TrackingplanSession();
    public static final long MAX_IDLE_DURATION = 30 * Time.MINUTE;
    private final String sessionId;
    private final float samplingRate;
    private final boolean trackingEnabled;
    // Timestamp in milliseconds since 1st Jan 1970
    private final long createdAt;
    // Timestamp in milliseconds since boot time
    private long lastActivityTime;
    private final boolean isNew;

    @NonNull
    public static TrackingplanSession newSession(
            final float samplingRate,
            final boolean trackingEnabled
    ) {
        return new TrackingplanSession(samplingRate, trackingEnabled);
    }

    private TrackingplanSession(
            final float samplingRate,
            final boolean trackingEnabled
    ) {
        final var time = ServiceLocator.getSharedInstance(Time.class);
        this.sessionId = UUID.randomUUID().toString();
        this.samplingRate = samplingRate;
        this.trackingEnabled = trackingEnabled;
        this.createdAt = time.currentTimeMillis();
        this.lastActivityTime = time.elapsedRealTime();
        this.isNew = true;

    }

    // This constructor is used to restore session objects from storage
    TrackingplanSession(
            @NonNull final String sessionId,
            final float samplingRate,
            final boolean trackingEnabled,
            final long createdAt,
            final long lastActivityTime
    ) {
        this.sessionId = sessionId;
        this.samplingRate = samplingRate;
        this.trackingEnabled = trackingEnabled;
        this.createdAt = createdAt;
        this.lastActivityTime = lastActivityTime;
        this.isNew = false;
    }

    // This constructor is used to create an empty session
    private TrackingplanSession() {
        this.sessionId = "";
        this.samplingRate = Float.MAX_VALUE;
        this.trackingEnabled = false;
        this.createdAt = 0;
        this.lastActivityTime = Long.MAX_VALUE;
        this.isNew = false;
    }

    @NonNull
    public String getSessionId() {
        return sessionId;
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

    public boolean isNew() { return isNew; }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    private long getIdleDuration() {

        final var time = ServiceLocator.getSharedInstance(Time.class);

        var elapsedTimeSinceBoot = time.elapsedRealTime();

        // Session expires when device reboots since SystemClock.elapsedRealTime gets restarted
        if (lastActivityTime > elapsedTimeSinceBoot) {
            return Long.MAX_VALUE;
        }

        return elapsedTimeSinceBoot - lastActivityTime;
    }

    public boolean hasExpired() {
        return getIdleDuration() >= MAX_IDLE_DURATION;
    }

    public boolean updateLastActivity() {
        final var time = ServiceLocator.getSharedInstance(Time.class);
        final var elapsedRealtime = time.elapsedRealTime();

        if (lastActivityTime > elapsedRealtime || elapsedRealtime > lastActivityTime + Time.MINUTE) {
            this.lastActivityTime = elapsedRealtime;
            return true;
        }

        return false;
    }

    @Override
    @NonNull
    public String toString() {
        return "Session{" +
                "sessionId=" + sessionId +
                ", samplingRate=" + samplingRate +
                ", trackingEnabled=" + trackingEnabled +
                ", createdAt=" + createdAt +
                ", lastActivityTime=" + lastActivityTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackingplanSession that = (TrackingplanSession) o;
        return sessionId.equals(that.sessionId)
                && createdAt == that.createdAt
                && trackingEnabled == that.trackingEnabled
                && Float.compare(samplingRate, that.samplingRate) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sessionId, samplingRate, trackingEnabled, createdAt, lastActivityTime);
    }
}
