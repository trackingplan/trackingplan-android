// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.delivery;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.BatchSender;
import com.trackingplan.client.sdk.TrackingplanClient;
import com.trackingplan.client.sdk.interception.HttpRequest;
import com.trackingplan.client.sdk.session.TrackingplanSession;
import com.trackingplan.client.sdk.util.AndroidLog;
import com.trackingplan.client.sdk.util.TaskRunner;

import java.util.List;

/**
 * TaskRunner implementation of a BatchSender
 */
final public class TaskRunnerBatchSender implements BatchSender {

    private static final AndroidLog logger = AndroidLog.getInstance();

    private final TrackingplanClient client;
    private final TaskRunner taskRunner;

    public TaskRunnerBatchSender(@NonNull TrackingplanClient client, @NonNull TaskRunner taskRunner) {
        this.client = client;
        this.taskRunner = taskRunner;
    }

    @Override
    public void send(@NonNull final List<HttpRequest> batch, @NonNull final TrackingplanSession session, final long batchId, SendCallback callback) {
        SendBatchTask task = new SendBatchTask(batch, client, session);
        taskRunner.executeTask(task, (batchResult, error) -> {
            if (error == null) {
                logger.debug(batchResult.numRequestsSent + " raw tracks of batch " + batchId + " sent to Trackingplan (" + batchResult.numFailedRequests + " failed)");
            } else {
                logger.error("Send failed and batch " + batchId + " will be discarded: " + error.getMessage());
            }
            if (callback != null) {
                callback.onBatchSent(batchId);
            }
        });
    }
}
