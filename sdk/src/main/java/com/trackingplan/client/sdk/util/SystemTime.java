// Copyright (c) 2024 Trackingplan
package com.trackingplan.client.sdk.util;

import android.os.SystemClock;

public class SystemTime implements Time {
    public long elapsedRealTime() {
        return SystemClock.elapsedRealtime();
    }
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
