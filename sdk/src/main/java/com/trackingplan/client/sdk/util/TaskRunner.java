// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.util;

import android.os.Handler;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class TaskRunner {

    public interface Callback<T> {
        void onComplete(T result, Exception error);
    }

    private final Executor executor = Executors.newSingleThreadExecutor();
    private final Handler handler;

    public TaskRunner(Handler handler) {
        this.handler = handler;
    }

    public <T> void executeTask(Callable<T> callable, Callback<T> callback) {
        executor.execute(() -> {
            try {
                final T result = callable.call();
                handler.post(() -> callback.onComplete(result, null));
            } catch (Exception e) {
                handler.post(() -> callback.onComplete(null, e));
            }
        });
    }
}
