// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.os.HandlerCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.trackingplan.client.sdk.interception.HttpRequest;
import com.trackingplan.client.sdk.interception.InterceptionContext;
import com.trackingplan.client.sdk.session.FetchSessionDataTask;
import com.trackingplan.client.sdk.session.SessionData;
import com.trackingplan.client.sdk.session.SessionDataStorage;
import com.trackingplan.client.sdk.util.AndroidLogger;
import com.trackingplan.client.sdk.util.TaskRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
 * {@link Trackingplan.ConfigInitializer#start(Context)}. Often this thread is the application main
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
final public class TrackingplanInstance {

    public enum RuntimeEnvironment {
        AndroidDefault,
        AndroidJUnit
    }

    private static final long FETCH_CONFIG_RETRY_INTERVAL_MS = 5 * 60 * 1000;

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    private static volatile TrackingplanInstance instance;

    public static TrackingplanInstance getInstance() {
        return instance;
    }

    static void registerInstance(TrackingplanInstance instance) {
        TrackingplanInstance.instance = instance;
    }

    // Trackingplan main thread
    private final HandlerThread handlerThread;
    private final TaskRunner taskRunner;
    private final Handler handler;

    private final Map<String, String> providers;
    private final RequestQueue requestQueue;

    // NOTE: Application context has the same lifecycle as the app. So no leak is possible
    private final Context context;

    private TrackingplanConfig config;
    private TrackingplanClient client;

    private SessionData currentSessionData;
    private boolean downloadingSessionData = false;

    // processRequest will work again when current time >= suspendedUntilMs
    private long suspendedUntilMs = 0;

    private final MyLifecycleObserver lifecycleObserver;
    private Lifecycle appLifeCycle;

    private RuntimeEnvironment runtimeEnvironment = RuntimeEnvironment.AndroidDefault;

    @MainThread
    TrackingplanInstance(@NonNull final Context context) {
        checkRunningInMainThread();
        this.context = context.getApplicationContext();
        lifecycleObserver = new MyLifecycleObserver();
        providers = makeDefaultProviders();
        config = TrackingplanConfig.emptyConfig;
        requestQueue = new RequestQueue(this);
        // Start Trackingplan thread. Intercepted requests will be routed through it
        handlerThread = new HandlerThread("Trackingplan");
        handlerThread.start();
        handler = HandlerCompat.createAsync(handlerThread.getLooper());
        taskRunner = new TaskRunner(this.handler);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        handlerThread.quitSafely();
        if (appLifeCycle != null) {
            appLifeCycle.removeObserver(lifecycleObserver);
        }
        logger.verbose("TrackingplanInstance destroyed");
    }

    @VisibleForTesting
    public void setRuntimeEnvironment(RuntimeEnvironment runtime) {
        this.runtimeEnvironment = runtime;
    }

    @MainThread
    public void attachToLifeCycle(final Lifecycle lifecycle) {
        if (appLifeCycle != null) {
            appLifeCycle.removeObserver(lifecycleObserver);
        }
        if (lifecycle != null) {
            lifecycle.addObserver(lifecycleObserver);
        }
        this.appLifeCycle = lifecycle;
    }

    @MainThread
    void start(@NonNull TrackingplanConfig config) throws IllegalArgumentException {

        if (config.equals(TrackingplanConfig.emptyConfig)) {
            throw new IllegalArgumentException("Empty config");
        }

        if (!config.isBackgroundObserverEnabled()) {
            attachToLifeCycle(null);
        }

        // Start in Trackingplan thread
        runSync(() -> {

            if (instance.isConfigured()) {
                logger.info("Trackingplan is already initialized. Start ignored");
                return;
            }

            if (config.isDebugEnabled()) {
                logger.info("Debug mode enabled");
            }

            if (config.isDryRunEnabled()) {
                logger.info("DryRun mode enabled");
            }


            logger.debug("Configuration: " + config);

            this.config = config;

            providers.clear();
            providers.putAll(makeDefaultProviders());
            providers.putAll(config.customDomains());

            requestQueue.start();

            client = new TrackingplanClient(config, context);

            // Get session data. Note that session data is bypassed for AndroidJUnit
            SessionData sessionData;
            if (runtimeEnvironment == RuntimeEnvironment.AndroidJUnit) {
                sessionData = new SessionData(config.getTpId(), 1, true, System.currentTimeMillis());
            } else {
                sessionData = SessionDataStorage.load(config.getTpId(), context);
            }

            if (!SessionDataStorage.hasExpired(sessionData)) {
                startSession(sessionData);
            }
            // else Session initialization will be triggered in processRequest

            logger.info("Trackingplan started");
        });
    }

    @MainThread
    void stop() {
        attachToLifeCycle(null);
        // Stop in Trackingplan thread
        runSync(() -> {
            requestQueue.stop();
            this.config = TrackingplanConfig.emptyConfig;
        });
    }

    public Context getContext() {
        return context;
    }

    TrackingplanClient getClient() {
        return client;
    }

    TaskRunner getTaskRunner() {
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
                logger.error("RunSync failed: " + ex.getMessage());
            }
        });
    }

    Runnable runSyncDelayed(long delayMillis, @NonNull Runnable task) {

        final Runnable wrapper = () -> {
            try {
                task.run();
            } catch (Exception ex) {
                logger.error("RunSync failed: " + ex.getMessage());
            }
        };

        this.handler.postDelayed(wrapper, delayMillis);

        return wrapper;
    }

    void cancelDelayedTask(@NonNull Runnable callback) {
        this.handler.removeCallbacks(callback);
    }

    /**
     * This method is the entry point to Trackingplan. It's called from InstrumentRequestBuilder.
     * Note that a queue is used for intercepted requests so that they are grouped into batches
     * before sending to Trackingplan. Regarding this, request are enqueued while the SDK isn't
     * configured. So if it never gets configured the intercepted requests might end up filling all
     * the memory available. To avoid this problem client code should call Trackingplan.stop(Context).
     * to stop any interception.
     */
    public void processRequest(
            @NonNull final HttpRequest request,
            @NonNull final InterceptionContext interceptionContext
    ) {
        checkRunningInTrackingplanThread();

        if (isSuspended()) {
            logger.warn("Request ignored. Request processing is disabled temporarily");
            return;
        }

        boolean isConfigured = isConfigured();
        boolean sessionExpired = SessionDataStorage.hasExpired(currentSessionData);
        boolean shouldQueue = !isConfigured || sessionExpired || currentSessionData.isTrackingEnabled();

        if (isConfigured && sessionExpired && !downloadingSessionData) {

            // Refresh session data downloading it from backend. Note
            // that request processing continues so that it can get enqueued.
            // This request will get processed once session is refreshed. If
            // tracking is enabled, they will be sent to trackingplan. Otherwise,
            // they will be discarded.

            refreshSessionDataAsync(this.config);
        }

        if (!shouldQueue) {
            logger.verbose("Request ignored. Tracking disabled");
            return;
        }

        try {
            // Note that initRequestContext depends on TrackingplanConfig#ignoreContext setting.
            // Since configuration is not available during interception, context initialization has
            // been moved from the interception stage to the process stage.
            initRequestContext(request, interceptionContext);

            // Note that initRequestDestination depends on TrackingplanConfig#customDomains setting.
            // For this reason, setting the provider of a request has been moved from the interception
            // stage to the process stage (configuration is not available during interception).
            initRequestDestination(request);

            if (!isTargetedToSupportedDestination(request)) {
                logger.verbose("Request ignored. Doesn't belong to a supported destination");
                return;
            }

            requestQueue.queueRequest(request);

            if (isConfigured && !sessionExpired && currentSessionData.isTrackingEnabled()) {
                requestQueue.processQueue(currentSessionData.getSamplingRate());
            }
        } catch (Exception ex) {
            logger.error("Request processing failed: " + ex.getMessage());
        }
    }

    /**
     * Flush the requests queue. It times out after 10s.
     */
    @VisibleForTesting
    void flushQueue() {
        flushQueue(10000);
    }

    private void flushQueue(long timeout) {
        final CountDownLatch lock = new CountDownLatch(1);
        runSync(() -> {
            if (!isConfigured() || SessionDataStorage.hasExpired(currentSessionData) || !currentSessionData.isTrackingEnabled()) {
                logger.debug("Processing queue ignored because of missing configuration");
                lock.countDown();
                return;
            }
            requestQueue.processQueue(currentSessionData.getSamplingRate(), true, lock::countDown);
        });
        try {
            if (Thread.currentThread() != handlerThread && timeout > 0) {
                var counterReachedZero = lock.await(timeout, TimeUnit.MILLISECONDS);
                if (!counterReachedZero) {
                    logger.debug("Queue flushing took longer than 10 seconds (timeout)");
                }
            }
        } catch (InterruptedException e) {
            logger.debug("Current thread interrupted while waiting for queue flushing");
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isTargetedToSupportedDestination(HttpRequest request) {
        return !request.getProvider().isEmpty();
    }

    boolean isConfigured() {
        return config != null && !TrackingplanConfig.emptyConfig.equals(config);
    }

    RuntimeEnvironment getRuntimeEnvironment() {
        return runtimeEnvironment;
    }

    private void startSession(SessionData sessionData) {

        this.currentSessionData = sessionData;

        logger.debug("Session initialized with data: " + sessionData.toString());

        if (!sessionData.isTrackingEnabled()) {
            long remainingTime = SessionDataStorage.remainingTimeTillExpiration(sessionData);
            suspendRequestProcessingTemporarily(remainingTime);
            return;
        }

        // Process any pending requests that happened before session was started
        requestQueue.processQueue(currentSessionData.getSamplingRate());
    }

    /**
     * Stop accepting new requests so that new requests are ignored. Any pending
     * requests that weren't sent yet are discarded
     *
     * @param duration Duration
     */
    private void suspendRequestProcessingTemporarily(long duration) {
        suspendedUntilMs = SystemClock.elapsedRealtime() + duration;
        int numPendingRequests = requestQueue.discardPendingRequests();
        logger.warn("Request processing is suspended temporarily for " + (duration / 1000) + " seconds");
        logger.info("Tracking is disabled for this session.");
        if (numPendingRequests > 0) {
            logger.debug(numPendingRequests + " pending intercepted requests were discarded");
        }
    }

    private boolean isSuspended() {
        return SystemClock.elapsedRealtime() < suspendedUntilMs;
    }

    private void refreshSessionDataAsync(TrackingplanConfig config) {
        if (downloadingSessionData) return;

        logger.verbose("Session data expired. Downloading ...");
        downloadingSessionData = true;

        taskRunner.executeTask(new FetchSessionDataTask(config.getTpId(), client), (newSessionData, error) -> {
            if (error == null) {
                SessionDataStorage.save(newSessionData, context);
                logger.verbose("Session data downloaded and saved");
                startSession(newSessionData);
                downloadingSessionData = false;
            } else {
                suspendRequestProcessingTemporarily(FETCH_CONFIG_RETRY_INTERVAL_MS);
                downloadingSessionData = false;
                logger.error("Fetching session data failed: " + error.getMessage());
            }
        });
    }

    private void initRequestContext(HttpRequest request, InterceptionContext interceptionContext) {
        if (config.ignoreContext()) {
            return;
        }

        if (!interceptionContext.activityName.isEmpty()) {
            request.addContextField("activity", interceptionContext.activityName);
        }
    }

    private void initRequestDestination(HttpRequest request) {

        var whitelistInstruments = Arrays.asList("okhttp", "urlconnection");

        if (!whitelistInstruments.contains(request.getInterceptionModule())) {
            return;
        }

        for (String partialUrl : providers.keySet()) {
            if (!request.getUrl().contains(partialUrl)) {
                continue;
            }
            var provider = providers.get(partialUrl);
            if (provider != null) {
                request.setProvider(provider);
            }
        }
    }

    private Map<String, String> makeDefaultProviders() {
        return new HashMap<>() {{
            put("api.amplitude.com", "amplitude");
            put("api2.amplitude.com", "amplitude");
            // Disabled as payloads intercepted by urlconnection are encrypted
            // put("inapps.appsflyer.com/api/v", "appsflyer");
            // put("launches.appsflyer.com/api/v", "appsflyer");
            put("bat.bing.com", "bing");
            put("ping.chartbeat.net", "chartbeat");
            put("track-sdk-eu.customer.io/api", "customerio"); // Europe Region
            put("track-sdk.customer.io/api", "customerio"); // USA Region
            put("facebook.com/tr/", "facebook");
            // put("firebaseinstallations.googleapis.com", "firebase");
            put("google-analytics.com", "googleanalytics");
            put("analytics.google.com", "googleanalytics");
            put("api.intercom.io", "intercom");
            put("kissmetrics.com", "kissmetrics");
            put("trk.kissmetrics.io", "kissmetrics");
            put("px.ads.linkedin.com", "linkedin");
            put("api.mixpanel.com", "mixpanel");
            put("logx.optimizely.com/v1/events", "optimizely");
            put("ct.pinterest.com", "pinterest");
            put("pdst.fm", "podsights");
            put("quantserve.com", "quantserve");
            put("sb.scorecardresearch.com", "scorecardresearch");
            put("api.segment.io", "segment");
            put("api.segment.com", "segment");
        }};
    }

    private void checkRunningInTrackingplanThread() {
        if (Thread.currentThread() == handlerThread) return;
        throw new IllegalThreadStateException("Method must be called from Trackingplan main thread");
    }

    private void checkRunningInMainThread() {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) return;
        throw new IllegalThreadStateException("Method must be called from UI main thread");
    }

    private class MyLifecycleObserver implements DefaultLifecycleObserver {
        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            DefaultLifecycleObserver.super.onStop(owner);
            logger.debug("Processing queue before going to background");
            // Do not wait as this is called from MainThread
            flushQueue(0);
        }
    }
}
