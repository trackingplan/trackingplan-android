// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.interception.HttpRequest;

import java.util.List;

public interface BatchSender {
    void send(@NonNull List<HttpRequest> batch, float samplingRate, long batchId, Runnable callback);
}
