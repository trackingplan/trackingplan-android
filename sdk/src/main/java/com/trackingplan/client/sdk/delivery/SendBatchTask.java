// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.delivery;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.TrackingplanClient;
import com.trackingplan.client.sdk.interception.HttpRequest;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * TaskRunner implementation of a batch worker
 */
final class SendBatchTask implements Callable<SendBatchTask.BatchResult> {

    public static class BatchResult {
        public int numRequestsSent;
        public int numFailedRequests;

        public BatchResult(int numRequestsSent, int numFailedRequests) {
            this.numRequestsSent = numRequestsSent;
            this.numFailedRequests = numFailedRequests;
        }
    }

    private final List<HttpRequest> batch;
    private final TrackingplanClient client;
    private final float samplingRate;

    public SendBatchTask(
            @NonNull List<HttpRequest> batch,
            @NonNull TrackingplanClient client,
            float samplingRate
    ) {
        this.batch = batch;
        this.client = client;
        this.samplingRate = samplingRate;
    }

    @Override
    public BatchResult call() throws Exception {
        int numRequestsSent = client.sendTracks(batch, samplingRate);
        int numFailedRequests = batch.size() - numRequestsSent;
        return new BatchResult(numRequestsSent, numFailedRequests);
    }
}
