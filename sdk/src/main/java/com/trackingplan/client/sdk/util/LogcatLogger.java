// Copyright (c) 2024 Trackingplan
package com.trackingplan.client.sdk.util;

import android.util.Log;

import androidx.annotation.NonNull;

public class LogcatLogger implements Logger {

    private final String tag;

    public LogcatLogger(@NonNull final String tag) {
        this.tag = tag;
    }

    public void v(@NonNull String msg) {
        Log.v(tag, msg);
    }

    public void d(@NonNull String msg) {
        Log.d(tag, msg);
    }

    public void i(@NonNull String msg) {
        Log.i(tag, msg);
    }

    public void w(@NonNull String msg) {
        Log.w(tag, msg);
    }

    public void e(@NonNull String msg) {
        Log.e(tag, msg);
    }
}
