// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.interception.HttpRequest;

import java.util.List;

public interface BatchSender {
    interface SendCallback {
        void onBatchSent(long batchId);
    }
    void send(@NonNull List<HttpRequest> batch, float samplingRate, long batchId, SendCallback callback);
}
