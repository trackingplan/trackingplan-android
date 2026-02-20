// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.interception.HttpRequest;
import com.trackingplan.shared.TrackingplanSession;

import java.util.List;

public interface BatchSender {
    interface SendCallback {
        void onBatchSent(long batchId);
    }
    void send(
            @NonNull final List<HttpRequest> batch,
            @NonNull final TrackingplanSession session,
            final long batchId,
            SendCallback callback
    );
}
