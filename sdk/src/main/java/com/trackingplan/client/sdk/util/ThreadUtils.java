package com.trackingplan.client.sdk.util;

import android.os.Looper;

public class ThreadUtils {
    public static void checkRunningInMainThread() {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) return;
        throw new IllegalThreadStateException("Method must be called from UI main thread");
    }
}
