// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.delivery.TaskRunnerBatchSender;
import com.trackingplan.client.sdk.interception.HttpRequest;
import com.trackingplan.client.sdk.util.AndroidLogger;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

final class RequestQueue {

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    // TODO: Expose this through Advanced Options
    private static final long BATCH_TIMEOUT_MS = 30 * 1000;
    private static final int MAX_NUM_REQUESTS_IN_BATCH = 10;

    private final Queue<HttpRequest> queue = new LinkedList<>();

    private final TrackingplanInstance tpInstance;

    private int lastBatchId = 0;

    private boolean shuttingDown = false;
    private Runnable watcher = null;

    public RequestQueue(TrackingplanInstance instance) {
        tpInstance = instance;
    }

    /**
     * This method must be called from Trackingplan thread
     */
    public void queueRequest(@NonNull HttpRequest request) {
        if (shuttingDown) {
            logger.verbose("Couldn't queue request because queue is stopped");
            return;
        }
        queue.add(request);
        logger.debug("Request queued: " + request);
    }

    /**
     * @see RequestQueue#processQueue(float, boolean, Runnable)
     */
    public void processQueue(float samplingRate) {
        processQueue(samplingRate, false, null);
    }

    /**
     * Process the queue of intercepted requests and send them to Trackingplan in batches
     * of MAX_NUM_REQUESTS_IN_BATCH requests. More than one batch can be scheduled as a result
     * of processing the queue but full batches are enforced.
     * <p>
     * In order for a batch to be scheduled, there must be enough requests to make the batch full.
     * When forceSendBatch is true and there are no enough requests in the queue to complete a batch,
     * a batch will be scheduled with less than MAX_NUM_REQUESTS_IN_BATCH.
     * <p>
     * NOTE: This method must be called from Trackingplan thread.
     */
    void processQueue(float samplingRate, boolean forceSendBatch, Runnable callback) {

        if (shuttingDown) {
            logger.debug("Process queue ignored. Queue is stopped");
            if (callback != null) callback.run();
            return;
        }

        if (!tpInstance.isConfigured()) {
            logger.debug("Process queue ignored. Configuration not provided");
            if (callback != null) callback.run();
            return;
        }

        if (queue.isEmpty()) {
            logger.debug("Queue is empty. Nothing to do");
            if (callback != null) callback.run();
            return;
        }

        logger.verbose("Processing queue...");

        // Most of the times processQueue is called just after enqueueRequest. However,
        // after initialization the queue may have more than MAX_NUM_REQUESTS_IN_BATCH
        // requests. In that case, processQueue has to prepare and schedule more than one batch.

        final int numBatchesToSend = getNumBatchesToSend(forceSendBatch);
        final AtomicInteger batchesSentCounter = new AtomicInteger(0);

        // Stop watcher because a new batch will be sent.
        if (numBatchesToSend > 0) {
            stopWatcher();
        }

        // Prepare batch and send
        for (int i = 0; i < numBatchesToSend; i++) {
            final List<HttpRequest> batch = takeElements(queue);
            final var batchSender = new TaskRunnerBatchSender(tpInstance.getClient(), tpInstance.getTaskRunner());
            batchSender.send(batch, samplingRate, lastBatchId, () -> {
                // Note: This callback is executed in Trackingplan thread
                int numBatchesSent = batchesSentCounter.addAndGet(1);
                if (numBatchesSent == numBatchesToSend) {
                    logger.info("Batch sender finished (" + numBatchesSent + " batches sent)");
                    if (callback != null) callback.run();
                }
            });
            logger.info("Queue processed (" + batch.size() + " requests). Batch " + lastBatchId + " scheduled for sending");

            lastBatchId = (lastBatchId + 1) % 10000;
        }

        if (!queue.isEmpty()) {
            // Queue has requests that are not included in a batch. If there are no further
            // calls to this method, these requests will never be sent. Set a timer to forcibly
            // send all these requests if BATCH_TIMEOUT_MS have passed and no batch was sent in
            // between.
            logger.verbose("Queue not full yet (" + queue.size() + " requests).");
            startWatcher(samplingRate);
        }
    }

    private int getNumBatchesToSend(boolean forceSendBatch) {
        int numBatchesToSend = queue.size() / MAX_NUM_REQUESTS_IN_BATCH;
        int numOrphanRequests = queue.size() % MAX_NUM_REQUESTS_IN_BATCH;
        if (forceSendBatch && numOrphanRequests > 0) {
            numBatchesToSend += 1;
        }
        return numBatchesToSend;
    }

    /**
     * Sets a watcher that calls processQueue after BATCH_TIMEOUT_MS.
     * <p>
     * If a watcher already existed, no watcher is started and the deadline of the previous
     * one is not changed.
     */
    private void startWatcher(float samplingRate) {

        if (watcher != null) {
            logger.verbose("Watcher is already started");
            return;
        }

        logger.debug("Watcher started");

        watcher = tpInstance.runSyncDelayed(BATCH_TIMEOUT_MS, () -> {
            watcher = null;
            logger.debug("Watcher timed out. Forcing the processing of the queue...");
            processQueue(samplingRate, true, null);
        });
    }

    private void stopWatcher() {
        if (watcher == null) {
            logger.verbose("Watcher is already stopped");
            return;
        }
        tpInstance.cancelDelayedTask(watcher);
        watcher = null;
        logger.debug("Watcher stopped");
    }

    /**
     * Get max MAX_NUM_REQUESTS_IN_BATCH requests from queue. Queue is modified.
     */
    @NonNull
    private List<HttpRequest> takeElements(Queue<HttpRequest> queue) {

        final List<HttpRequest> batch = new LinkedList<>();

        while (batch.size() < RequestQueue.MAX_NUM_REQUESTS_IN_BATCH && !queue.isEmpty()) {
            batch.add(queue.remove());
        }

        return batch;
    }

    /**
     * This method must be called from Trackingplan thread
     */
    public void start() {
        shuttingDown = false;
    }

    /**
     * This method must be called from Trackingplan thread
     */
    public void stop() {
        shuttingDown = true;
        stopWatcher();
        int numPendingRequests = discardPendingRequests();
        if (numPendingRequests > 0) {
            logger.info(numPendingRequests + " pending intercepted requests were discarded");
        }
    }

    /**
     * This method must be called from Trackingplan thread
     */
    public int discardPendingRequests() {
        int numRequests = queue.size();
        queue.clear();
        return numRequests;
    }
}
