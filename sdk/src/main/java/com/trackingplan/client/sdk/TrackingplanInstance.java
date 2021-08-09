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

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.core.os.HandlerCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.trackingplan.client.sdk.interception.HttpRequest;
import com.trackingplan.client.sdk.session.FetchSessionDataTask;
import com.trackingplan.client.sdk.session.SessionData;
import com.trackingplan.client.sdk.session.SessionDataStorage;
import com.trackingplan.client.sdk.util.AndroidLogger;
import com.trackingplan.client.sdk.util.TaskRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Trackingplan singleton instance.
 * <p>
 * This class is responsible of refreshing session data, manage intercepted requests, initialize
 * and provide services to other components and holding important information like configuration and
 * session data (sampling rate, tracking enabled, etc.).
 * <p>
 * Regarding the thread model, this class uses an event loop provided by {@link HandlerThread} in
 * order to process intercepted requests synchronously. There are four kind of threads to take into
 * consideration: Consumer, Network, Trackingplan and Trackingplan Worker.
 * <p>
 * Trackingplan initialization happens in the Consumer thread, the one calling
 * {@link Trackingplan.Initializer#start(Context)}. Often this thread is the application main
 * thread (aka UI thread).
 * <p>
 * Trackingplan interception happens in Network threads (one or more). This is from where
 * InstrumentRequestBuilder#build() is called. However, after a request is
 * considered of interest for Trackingplan, it is managed synchronously in Trackingplan thread
 * (see {@link TrackingplanInstance#processRequest})
 * <p>
 * All the rest of the Trackingplan code runs in Trackingplan thread. So you only have to think in
 * single-thread model once {@link TrackingplanInstance#processRequest} is called. There are, however,
 * some cases where tasks like session refreshing or batch sending are sent to Trackingplan Worker
 * thread using {@link TaskRunner}, our async task executor. Note that the task's callbacks invoked
 * upon finalization of the task are executed also in Trackingplan thread.
 */
final public class TrackingplanInstance implements LifecycleObserver {

    private static final long FETCH_CONFIG_RETRY_INTERVAL_MS = 5 * 60 * 1000; // TODO: Change to 5 min

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    private static TrackingplanInstance instance;

    public static TrackingplanInstance getInstance() {
        return instance;
    }

    public static void registerInstance(TrackingplanInstance instance) {
        TrackingplanInstance.instance = instance;
    }

    private final TaskRunner taskRunner;
    private final HandlerThread handlerThread; // TODO: Research how it affects performance. Research a non-blocking implementation
    private final Handler handler;

    private final TrackingplanConfig config;
    private final TrackingplanClient client;

    private final RequestProcessor requestProcessor;
    private final Map<String, String> providers;

    private final SessionDataStorage sessionStore;

    // Note: Application context has the same lifecycle as the app. So no leak is possible
    private final Context context;

    private SessionData currentSessionData;
    private boolean downloadingSessionData = false;

    // processRequest will work again when current time >= suspendedUntilMs
    private long suspendedUntilMs = 0;

    public TrackingplanInstance(
            final TrackingplanConfig config,
            final Context context,
            final Lifecycle lifecycle
    ) {
        // Initialization is done in UI thread.

        this.context = context.getApplicationContext();

        this.config = config;
        sessionStore = new SessionDataStorage();
        providers = makeDefaultProviders();
        providers.putAll(config.customDomains());
        client = new TrackingplanClient(config);

        // Start Trackingplan thread. Intercepted requests will be routed through it
        handlerThread = new HandlerThread("Trackingplan");
        handlerThread.start(); // TODO: Should I call .quit() later somewhere?

        handler = HandlerCompat.createAsync(handlerThread.getLooper());
        taskRunner = new TaskRunner(this.handler);

        // Must be initialized after taskRunner and client
        requestProcessor = new RequestProcessor(this);

        SessionData sessionData = sessionStore.load(config.getTpId(), context);

        if (!sessionStore.hasExpired(sessionData)) {
            // Start session in Trackingplan thread
            runSync(() -> startSession(sessionData));
        }
        // else Session initialization will be triggered in processRequest

        logger.info("Configuration: " + config.toString());

        lifecycle.addObserver(this);
    }

    public final TrackingplanConfig getConfig() {
        return config;
    }

    public final Context getContext() {
        return context;
    }

    public final Map<String, String> getProviders() {
        return Collections.unmodifiableMap(providers);
    }

    public final TrackingplanClient getClient() {
        return client;
    }

    public final TaskRunner getTaskRunner() {
        return taskRunner;
    }

    /**
     * Execute task inside Trackingplan main thread
     *
     * @param task Task
     */
    public void runSync(@NonNull Runnable task) {
        this.handler.post(() -> {
            try {
                task.run();
            } catch (Exception ex) {
                logger.warn("RunSync failed: " + ex.getMessage());
            }
        });
    }

    public Runnable runSyncDelayed(long delayMillis, @NonNull Runnable task) {

        final Runnable wrapper = () -> {
            try {
                task.run();
            } catch (Exception ex) {
                logger.warn("RunSync failed: " + ex.getMessage());
            }
        };

        this.handler.postDelayed(wrapper, delayMillis);

        return wrapper;
    }

    public void cancelDelayedTask(@NonNull Runnable callback) {
        this.handler.removeCallbacks(callback);
    }

    /**
     * This method is the entry point to Trackingplan. It's called from InstrumentRequestBuilder
     */
    public void processRequest(@NonNull final HttpRequest request) {

        checkRunningInTrackingplanThread();

        if (isSuspended()) {
            logger.warn("Request ignored. Request processing is disabled temporarily");
            return;
        }

        boolean sessionExpired = sessionStore.hasExpired(currentSessionData);
        boolean shouldEnqueue = sessionExpired || currentSessionData.isTrackingEnabled();

        if (sessionExpired && !downloadingSessionData) {

            // Refresh session data downloading it from backend. Note
            // that request processing continues so that it can get enqueued.
            // This request will get processed once session is refreshed. If
            // tracking is enabled, they will be sent to trackingplan. Otherwise,
            // they will be discarded.

            refreshSessionDataAsync();
        }

        if (!shouldEnqueue) {
            logger.verbose("Request ignored. Tracking disabled");
            return;
        }

        try {
            requestProcessor.enqueueRequest(request);

            if (!sessionExpired && currentSessionData.isTrackingEnabled()) {
                requestProcessor.processQueue(currentSessionData.getSamplingRate());
            }
        } catch (Exception ex) {
            logger.error("Request processing failed: " + ex.getMessage());
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void processQueue() {

        if (sessionStore.hasExpired(currentSessionData) || !currentSessionData.isTrackingEnabled()) {
            return;
        }

        logger.info("Processing all queue before going to background");
        requestProcessor.processQueue(currentSessionData.getSamplingRate(), true);
    }

    private void startSession(SessionData sessionData) {

        this.currentSessionData = sessionData;

        logger.info("Session initialized with data: " + sessionData.toString());

        if (!sessionData.isTrackingEnabled()) {
            long remainingTime = sessionStore.remainingTimeTillExpiration(sessionData);
            suspendRequestProcessingTemporarily(remainingTime);
            return;
        }

        // Process any pending requests that happened before session was started
        requestProcessor.processQueue(currentSessionData.getSamplingRate());
    }

    /**
     * Stop accepting new requests so that new requests are ignored. Any pending
     * requests that weren't sent yet are discarded
     *
     * @param duration Duration
     */
    private void suspendRequestProcessingTemporarily(long duration) {
        suspendedUntilMs = SystemClock.elapsedRealtime() + duration;
        requestProcessor.discardPendingRequests();
        logger.warn("Request processing is suspended temporarily for " + (duration / 1000) + " seconds");
        logger.info("Tracking is disabled for this session.");
        logger.info("All pending intercepted requests were discarded");
    }

    private boolean isSuspended() {
        return SystemClock.elapsedRealtime() < suspendedUntilMs;
    }

    private void refreshSessionDataAsync() {
        if (downloadingSessionData) return;
        logger.verbose("Session data expired. Downloading ...");
        downloadingSessionData = true;
        FetchSessionDataTask task = new FetchSessionDataTask(config.getTpId(), client);
        taskRunner.executeTask(task, (newSessionData, error) -> {
            if (error == null) {
                sessionStore.save(newSessionData, context);
                logger.verbose("Session data downloaded and saved");
                startSession(newSessionData);
                downloadingSessionData = false;
            } else {
                suspendRequestProcessingTemporarily(FETCH_CONFIG_RETRY_INTERVAL_MS);
                downloadingSessionData = false;
                logger.warn("Fetching session data failed: " + error.getMessage());
            }
        });
    }

    private Map<String, String> makeDefaultProviders() {
        return new HashMap<String, String>() {{
            put("google-analytics.com", "googleanalytics");
            put("www.google-analytics.com", "googleanalytics");
            put("ssl.google-analytics.com", "googleanalytics");
            put("analytics.google.com", "googleanalytics");
            put("api.segment.io", "segment");
            put("api.segment.com", "segment");
            put("quantserve.com", "quantserve");
            put("api.intercom.io", "intercom");
            put("api.amplitude.com", "amplitude");
            put("ping.chartbeat.net", "chartbeat");
            put("api.mixpanel.com", "mixpanel");
            put("kissmetrics.com", "kissmetrics");
            put("sb.scorecardresearch.com", "scorecardresearch");
            // put("firebaseinstallations.googleapis.com", "firebaseinstallations");
        }};
    }

    private void checkRunningInTrackingplanThread() {
        if (Thread.currentThread() == handlerThread) return;
        throw new IllegalThreadStateException("Method must be called from Trackingplan main thread");
    }
}
