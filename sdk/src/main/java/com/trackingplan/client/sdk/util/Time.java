// Copyright (c) 2024 Trackingplan
package com.trackingplan.client.sdk.util;

public interface Time {

    long MILLISECOND = 1;
    long SECOND = 1000 * MILLISECOND;
    long MINUTE = 60 * SECOND;
    long HOUR = 60 * MINUTE;

    long elapsedRealTime();
    long currentTimeMillis();
}
