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
package com.trackingplan.client.sdk;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.delivery.TaskRunnerBatchSender;
import com.trackingplan.client.sdk.interception.HttpRequest;
import com.trackingplan.client.sdk.util.AndroidLogger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;


final class RequestProcessor {

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    private static final long BATCH_TIMEOUT_MS = 30 * 1000; // TODO: Change to 30 seconds
    private static final int MAX_NUM_REQUESTS_IN_BATCH = 10;

    private final Queue<HttpRequest> queue = new LinkedList<>();

    private final TrackingplanInstance tpInstance;

    private final Map<Long, Runnable> activeTimers = new HashMap<>();
    private final BatchSender batchSender;

    private int lastBatchId = 0;

    public RequestProcessor(TrackingplanInstance instance) {
        this.tpInstance = instance;
        // this.batchSender = new WorkManagerBatchSender(tpInstance.getConfig(), tpInstance.getContext());
        this.batchSender = new TaskRunnerBatchSender(instance.getClient(), instance.getTaskRunner());
    }

    public void enqueueRequest(@NonNull HttpRequest request) {
        queue.add(request);
        logger.verbose("Request enqueued");
    }

    /**
     * @see RequestProcessor#processQueue(float, boolean)
     */
    public void processQueue(float samplingRate) {
        processQueue(samplingRate, false);
    }

    /**
     * Process the queue of intercepted requests and send them to Trackingplan in batches of
     * MAX_NUM_REQUESTS_IN_BATCH requests. More than one batch can be scheduled as a result of
     * processing the queue.
     * <p>
     * In order for a batch to be scheduled, there must be enough requests to make the batch full.
     * When allowPartialBatches is true and there are no enough requests in the queue to complete
     * a batch, a partial batch will be scheduled with less than MAX_NUM_REQUESTS_IN_BATCH.
     * <p>
     * NOTE: This method must be called from Trackingplan thread.
     */
    public void processQueue(float samplingRate, boolean allowPartialBatches) {

        logger.verbose("Processing queue...");

        if (queue.isEmpty()) {
            logger.debug("Process queue ignored. Queue is empty");
            return;
        }

        // Most of the times processQueue is called just after enqueueRequest. However, after
        // initialization the queue may have more than MAX_NUM_REQUESTS_IN_BATCH requests. In that
        // case, processQueue has to prepare and schedule more than one batch.

        int numBatchesToSend = queue.size() / MAX_NUM_REQUESTS_IN_BATCH;
        int numOrphanRequests = queue.size() % MAX_NUM_REQUESTS_IN_BATCH;

        if (allowPartialBatches && numOrphanRequests > 0) {
            numBatchesToSend += 1;
        }

        for (int i = 0; i < numBatchesToSend; i++) {
            final List<HttpRequest> batch = takeElements(queue);
            stopWatchTimer(lastBatchId);
            if (!tpInstance.getConfig().isDryRunEnabled()) {
                batchSender.send(batch, samplingRate, lastBatchId);
                logger.info("Queue processed (" + batch.size() + " requests). Batch " + lastBatchId + " scheduled for sending");
            } else {
                logger.info("Queue processed (" + batch.size() + " requests). Dry run mode enabled. No batch scheduled");
            }

            lastBatchId = (lastBatchId + 1) % 10000;
        }

        if (!queue.isEmpty()) {
            // Queue has requests that are not included in a batch. If there are no further calls
            // to this method, these requests will never be sent. Set a timer to forcibly send all
            // these requests if BATCH_TIMEOUT_MS have passed and no batch was sent in between.
            logger.verbose("Queue not full yet (" + numOrphanRequests + " requests).");
            startWatchTimer(lastBatchId, samplingRate);
        }
    }

    /**
     * Sets a timer that calls processQueue after BATCH_TIMEOUT_MS
     * if no batch was sent in between.
     * <p>
     * If a timer already existed, it is cancelled before starting
     * the new one.
     */
    private void startWatchTimer(long batchId, float samplingRate) {

        if (activeTimers.containsKey(batchId)) return;

        logger.verbose("Start watcher " + batchId);

        Runnable timer = tpInstance.runSyncDelayed(BATCH_TIMEOUT_MS, () -> {
            activeTimers.remove(batchId);
            if (batchId == this.lastBatchId) {
                logger.debug("Watcher " + batchId + " timed out. Forcing a queue processing...");
                processQueue(samplingRate, true);
            } else {
                logger.debug("Watcher " + batchId + " timed out. Nothing to do here (" + queue.size() + " requests in queue)");
            }
        });

        activeTimers.put(batchId, timer);
    }

    private void stopWatchTimer(long batchId) {
        if (!activeTimers.containsKey(batchId)) return;
        Runnable timer = activeTimers.get(batchId);
        if (timer != null) {
            tpInstance.cancelDelayedTask(timer);
        }
        activeTimers.remove(batchId);
        logger.debug("Watcher " + batchId + " stopped");
    }

    /**
     * Get max MAX_NUM_REQUESTS_IN_BATCH requests from queue. Queue is modified.
     */
    @NonNull
    private List<HttpRequest> takeElements(Queue<HttpRequest> queue) {

        final List<HttpRequest> batch = new LinkedList<>();

        while (batch.size() < RequestProcessor.MAX_NUM_REQUESTS_IN_BATCH && !queue.isEmpty()) {
            batch.add(queue.remove());
        }

        return batch;
    }

    public void discardPendingRequests() {
        queue.clear();
    }
}
