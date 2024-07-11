package com.trackingplan.client.sdk.test;

import androidx.annotation.VisibleForTesting;

import com.trackingplan.client.sdk.util.Time;

@VisibleForTesting
public class TestTime implements Time {

    private volatile long elapsedRealTime = 0;
    private volatile long currentTimeMillis = System.currentTimeMillis();


    @Override
    public synchronized long elapsedRealTime() {
        return elapsedRealTime;
    }

    @Override
    public synchronized long currentTimeMillis() {
        return currentTimeMillis;
    }

    public synchronized void forwardTime(long durationMs) {
        elapsedRealTime += durationMs;
        currentTimeMillis += durationMs;
    }
}
