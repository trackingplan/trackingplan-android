// MIT License
//
// Copyright (c) 2021 Trackingplan
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
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
