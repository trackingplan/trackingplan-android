// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.delivery;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.BatchSender;
import com.trackingplan.client.sdk.TrackingplanClient;
import com.trackingplan.client.sdk.interception.HttpRequest;
import com.trackingplan.client.sdk.util.AndroidLogger;
import com.trackingplan.client.sdk.util.TaskRunner;

import java.util.List;

/**
 * TaskRunner implementation of a BatchSender
 */
final public class TaskRunnerBatchSender implements BatchSender {

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    private final TrackingplanClient client;
    private final TaskRunner taskRunner;

    public TaskRunnerBatchSender(@NonNull TrackingplanClient client, @NonNull TaskRunner taskRunner) {
        this.client = client;
        this.taskRunner = taskRunner;
    }

    @Override
    public void send(@NonNull List<HttpRequest> batch, float samplingRate, long batchId) {
        SendBatchTask task = new SendBatchTask(batch, client, samplingRate);
        taskRunner.executeTask(task, (batchResult, error) -> {
            if (error == null) {
                logger.info(batchResult.numRequestsSent + " raw tracks of batch " + batchId + " sent to Trackingplan (" + batchResult.numFailedRequests + " failed)");
            } else {
                logger.warn("Send failed and batch " + batchId + " will be discarded: " + error.getMessage());
            }
        });
    }
}
