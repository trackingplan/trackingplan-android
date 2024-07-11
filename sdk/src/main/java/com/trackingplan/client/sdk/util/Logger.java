// Copyright (c) 2024 Trackingplan
package com.trackingplan.client.sdk.util;

import androidx.annotation.NonNull;

/** Wrapper that handles Android logcat logging. */
public interface Logger {
    void v(@NonNull String msg);
    void d(@NonNull String msg);
    void i(@NonNull String msg);
    void w(@NonNull String msg);
    void e(@NonNull String msg);
}
