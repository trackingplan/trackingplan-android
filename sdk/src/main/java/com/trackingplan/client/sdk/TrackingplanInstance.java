// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.core.os.HandlerCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.trackingplan.client.sdk.interception.HttpRequest;
import com.trackingplan.client.sdk.interception.InterceptionContext;
import com.trackingplan.client.sdk.session.FetchSessionDataTask;
import com.trackingplan.client.sdk.session.SessionData;
import com.trackingplan.client.sdk.session.SessionDataStorage;
import com.trackingplan.client.sdk.util.AndroidLogger;
import com.trackingplan.client.sdk.util.TaskRunner;
import com.trackingplan.client.sdk.util.URLUtils;

import java.util.Arrays;
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
final public class TrackingplanInstance implements LifecycleObserver {

    private static final long FETCH_CONFIG_RETRY_INTERVAL_MS = 5 * 60 * 1000; // TODO: Change to 5 min

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    private static volatile TrackingplanInstance instance;

    @NonNull
    public static TrackingplanInstance getInstance() {
        if (instance == null) {
            throw new RuntimeException("Instance not registered during app startup");
        }
        return instance;
    }

    public static void registerInstance(TrackingplanInstance instance) {
        TrackingplanInstance.instance = instance;
    }

    private final TaskRunner taskRunner;
    private final HandlerThread handlerThread; // TODO: Research how it affects performance. Research a non-blocking implementation
    private final Handler handler;

    private final Map<String, String> providers;
    private final RequestQueue requestQueue;

    // Note: Application context has the same lifecycle as the app. So no leak is possible
    private final Context context;

    private TrackingplanConfig config;
    private TrackingplanClient client;

    private SessionData currentSessionData;
    private boolean downloadingSessionData = false;

    // processRequest will work again when current time >= suspendedUntilMs
    private long suspendedUntilMs = 0;

    public TrackingplanInstance(final Context context, final Lifecycle lifecycle) {

        checkRunningInMainThread();

        this.context = context.getApplicationContext();
        config = TrackingplanConfig.emptyConfig;
        providers = makeDefaultProviders();

        // Start Trackingplan thread. Intercepted requests will be routed through it
        handlerThread = new HandlerThread("Trackingplan");
        handlerThread.start(); // TODO: Should I call .quit() later somewhere?

        handler = HandlerCompat.createAsync(handlerThread.getLooper());
        taskRunner = new TaskRunner(this.handler);

        requestQueue = new RequestQueue(this);

        lifecycle.addObserver(this);
    }

    public void setConfig(@NonNull TrackingplanConfig config) {

        if (config.equals(TrackingplanConfig.emptyConfig)) {
            throw new RuntimeException("Empty config");
        }

        this.config = config;

        providers.clear();
        providers.putAll(makeDefaultProviders());
        providers.putAll(config.customDomains());

        client = new TrackingplanClient(config);

        SessionData sessionData = SessionDataStorage.load(config.getTpId(), context);
        if (!SessionDataStorage.hasExpired(sessionData)) {
            // Start session in Trackingplan thread
            runSync(() -> startSession(sessionData));
        }
        // else Session initialization will be triggered in processRequest

        logger.info("Configuration: " + config);
    }

    public Context getContext() {
        return context;
    }

    public TrackingplanClient getClient() {
        return client;
    }

    public TaskRunner getTaskRunner() {
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
        boolean shouldEnqueue = !isConfigured || sessionExpired || currentSessionData.isTrackingEnabled();

        if (isConfigured && sessionExpired && !downloadingSessionData) {

            // Refresh session data downloading it from backend. Note
            // that request processing continues so that it can get enqueued.
            // This request will get processed once session is refreshed. If
            // tracking is enabled, they will be sent to trackingplan. Otherwise,
            // they will be discarded.

            refreshSessionDataAsync(this.config);
        }

        /*
        TODO: Request are enqueued while the SDK isn't configured. So if it never gets configured
         the intercepted requests might end up filling all the memory available. Suspend interception
         if no configuration is provided
         */

        if (!shouldEnqueue) {
            logger.verbose("Request ignored. Tracking disabled");
            return;
        }

        try {
            // Note that initRequestContext depends on TrackingplanConfig#ignoreContext setting.
            // For this reason, setting the context has been moved from the interception stage to
            // the process stage (configuration is not available during interception).
            initRequestContext(request, interceptionContext);

            // Note that initRequestProvider depends on TrackingplanConfig#customDomains setting.
            // For this reason, setting the provider of a request has been moved from the interception
            // stage to the process stage (configuration is not available during interception).
            initRequestProvider(request);

            if (!hasSupportedProvider(request)) {
                logger.verbose("Request ignored. Doesn't belong to a supported provider");
                return;
            }

            requestQueue.enqueueRequest(request);

            if (isConfigured && !sessionExpired && currentSessionData.isTrackingEnabled()) {
                requestQueue.processQueue(currentSessionData.getSamplingRate());
            }
        } catch (Exception ex) {
            logger.error("Request processing failed: " + ex.getMessage());
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean hasSupportedProvider(HttpRequest request) {
        return !request.getProvider().isEmpty();
    }

    public boolean isConfigured() {
        return config != TrackingplanConfig.emptyConfig;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private void processQueue() {

        if (!isConfigured() || SessionDataStorage.hasExpired(currentSessionData) || !currentSessionData.isTrackingEnabled()) {
            return;
        }

        logger.info("Processing all queue before going to background");
        requestQueue.processQueue(currentSessionData.getSamplingRate(), true);
    }

    private void startSession(SessionData sessionData) {

        this.currentSessionData = sessionData;

        logger.info("Session initialized with data: " + sessionData.toString());

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
        requestQueue.discardPendingRequests();
        logger.warn("Request processing is suspended temporarily for " + (duration / 1000) + " seconds");
        logger.info("Tracking is disabled for this session.");
        logger.info("All pending intercepted requests were discarded");
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
                logger.warn("Fetching session data failed: " + error.getMessage());
            }
        });
    }

    private void initRequestProvider(HttpRequest request) {
        var whitelist = Arrays.asList("okhttp", "urlconnection");

        if (!whitelist.contains(request.getInterceptionModule())) {
            return;
        }

        String provider = providers.get(URLUtils.getDomain(request.getUrl()));
        if (provider != null) {
            request.setProvider(provider);
        }
    }

    private void initRequestContext(HttpRequest request, InterceptionContext interceptionContext) {
        if (config.ignoreContext()) {
            return;
        }

        if (!interceptionContext.activityName.isEmpty()) {
            request.addContextField("activity", interceptionContext.activityName);
        }
    }

    private Map<String, String> makeDefaultProviders() {
        return new HashMap<>() {{
            put("google-analytics.com", "googleanalytics");
            put("www.google-analytics.com", "googleanalytics");
            put("analytics.google.com", "googleanalytics");
            put("ssl.google-analytics.com", "googleanalytics");
            put("api.segment.io", "segment");
            put("api.segment.com", "segment");
            put("api.intercom.io", "intercom");
            put("api.amplitude.com", "amplitude");
            put("api2.amplitude.com", "amplitude");
            put("ping.chartbeat.net", "chartbeat");
            put("api.mixpanel.com", "mixpanel");
            put("kissmetrics.com", "kissmetrics");
            put("trk.kissmetrics.io", "kissmetrics");
            put("ct.pinterest.com", "pinterest");
            // put("facebook.com/tr/", "facebook");
            put("px.ads.linkedin.com", "linkedin");
            put("bat.bing.com", "bing");
            put("pdst.fm", "podsights");
            put("firebaseinstallations.googleapis.com", "firebase");
            put("quantserve.com", "quantserve");
            put("sb.scorecardresearch.com", "scorecardresearch");
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
}
