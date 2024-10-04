// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk;

import static com.trackingplan.client.sdk.TrackingplanInstance.RuntimeEnvironment.AndroidJUnit;

import android.content.Context;
import android.os.Bundle;
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
import com.trackingplan.client.sdk.session.SamplingRate;
import com.trackingplan.client.sdk.session.Storage;
import com.trackingplan.client.sdk.session.TrackingplanSession;
import com.trackingplan.client.sdk.util.AndroidLog;
import com.trackingplan.client.sdk.util.JSONUtils;
import com.trackingplan.client.sdk.util.ScreenViewTracker;
import com.trackingplan.client.sdk.util.TaskRunner;
import com.trackingplan.client.sdk.util.ThreadUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    private static final AndroidLog logger = AndroidLog.getInstance();

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
    private final AtomicInteger numActiveTasks;

    private final Map<String, String> providers;
    private final RequestQueue requestQueue;

    // NOTE: Application context has the same lifecycle as the app. So no leak is possible
    private final Context context;

    private volatile TrackingplanConfig config;
    private TrackingplanClient client;
    private Storage storage;

    @NonNull
    private TrackingplanSession currentSession;

    private final FlushQueueOnStopLifecycleObserver flushQueueLifeCycleObserver;
    private final SessionLifecycleObserver sessionLifecycleObserver;
    private Lifecycle appLifeCycle;

    final private ScreenViewTracker.ScreenViewListener screenViewListener;
    private ScreenViewTracker screenViewTracker;

    private RuntimeEnvironment runtimeEnvironment = RuntimeEnvironment.AndroidDefault;

    // For testing purposes
    private boolean fakeSamplingEnabled;

    @MainThread
    TrackingplanInstance(@NonNull final Context context) {
        ThreadUtils.checkRunningInMainThread();
        this.context = context.getApplicationContext();
        providers = makeDefaultProviders();
        config = TrackingplanConfig.EMPTY;
        requestQueue = new RequestQueue(this);
        currentSession = TrackingplanSession.EMPTY;

        flushQueueLifeCycleObserver = new FlushQueueOnStopLifecycleObserver();
        sessionLifecycleObserver = new SessionLifecycleObserver();
        screenViewListener = new MyScreenViewListener();

        // Start Trackingplan thread. Intercepted requests will be routed through it
        handlerThread = new HandlerThread("Trackingplan");
        handlerThread.start();
        handler = HandlerCompat.createAsync(handlerThread.getLooper());
        numActiveTasks = new AtomicInteger(0);
        taskRunner = new TaskRunner(this.handler);

        // For testing purposes
        fakeSamplingEnabled = false;
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        handlerThread.quitSafely();
        if (appLifeCycle != null) {
            appLifeCycle.removeObserver(flushQueueLifeCycleObserver);
            appLifeCycle.removeObserver(sessionLifecycleObserver);
        }
        logger.verbose("TrackingplanInstance destroyed");
    }

    @VisibleForTesting
    public void setRuntimeEnvironment(RuntimeEnvironment runtime) {
        this.runtimeEnvironment = runtime;
        if (!fakeSamplingEnabled && runtime == AndroidJUnit) {
            fakeSamplingEnabled = true;
        }
    }

    @VisibleForTesting
    public void setFakeSamplingEnabled(boolean enabled) {
        fakeSamplingEnabled = enabled;
    }

    @MainThread
    public void attachToLifeCycle(final Lifecycle lifecycle) {
        if (appLifeCycle != null) {
            appLifeCycle.removeObserver(flushQueueLifeCycleObserver);
            appLifeCycle.removeObserver(sessionLifecycleObserver);
        }
        if (lifecycle != null) {
            lifecycle.addObserver(flushQueueLifeCycleObserver);
            lifecycle.addObserver(sessionLifecycleObserver);
        }
        this.appLifeCycle = lifecycle;
    }

    @MainThread
    public void attachToScreenViewTracker(final ScreenViewTracker screenViewTracker) {
        if (this.screenViewTracker != null) {
            this.screenViewTracker.unregisterScreenViewListener(screenViewListener);
        }
        if (screenViewTracker != null) {
            screenViewTracker.registerScreenViewListener(screenViewListener);
        }
        this.screenViewTracker = screenViewTracker;
    }

    @MainThread
    void start(@NonNull final TrackingplanConfig config) throws IllegalArgumentException {

        if (config.equals(TrackingplanConfig.EMPTY)) {
            throw new IllegalArgumentException("Empty config");
        }

        if (!config.isBackgroundObserverEnabled()) {
            // This disables flushing queue data when app goes to background and session
            // activity tracking due to app moving from background to foreground and vice versa
            attachToLifeCycle(null);
        }

        // Start in Trackingplan thread
        runSync(() -> {
            if (isConfigured()) {
                logger.warn("Trackingplan already initialized. Start ignored");
                return;
            }

            logger.info("Trackingplan initialized");
            logger.debug("Configuration: " + config);

            if (config.isDebugEnabled()) {
                logger.info("Debug mode enabled");
            }

            if (config.isDryRunEnabled()) {
                logger.info("DryRun mode enabled");
            }

            if (screenViewTracker != null) {
                logger.info("Screen attribution enabled");
            }

            this.config = config;
            this.storage = new Storage(config.getTpId(), config.getEnvironment(), context);

            providers.clear();
            providers.putAll(makeDefaultProviders());
            providers.putAll(config.customDomains());

            requestQueue.start();

            client = new TrackingplanClient(config, context);

            logger.info("Trackingplan started");
            startSession();
        });
    }

    void stop() {
        final var mainHandler = new Handler(Looper.getMainLooper());

        mainHandler.post(() -> {
            attachToLifeCycle(null);
            attachToScreenViewTracker(null);
        });

        // Stop in Trackingplan thread
        runSync(() -> {
            requestQueue.stop();

            // Wait for pending taskRunner tasks to finish
            final CountDownLatch lock = new CountDownLatch(1);
            taskRunner.executeTask(() -> { lock.countDown(); return true; }, null);
            try {
                lock.await();
            } catch (InterruptedException e) {
                logger.warn("Current thread interrupted while waiting for Trackingplan.stop");
            }

            stopSession();
            this.currentSession = TrackingplanSession.EMPTY;
            this.config = TrackingplanConfig.EMPTY;

            logger.info("Trackingplan stopped");
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

    @NonNull
    @VisibleForTesting
    public TrackingplanSession getSession() {
        return currentSession;
    }

    /**
     * Execute task inside Trackingplan main thread
     *
     * @param task Task
     */
    public void runSync(@NonNull Runnable task) {
        numActiveTasks.incrementAndGet();
        this.handler.post(() -> {
            try {
                task.run();
            } catch (Exception ex) {
                logger.error("RunSync failed: " + ex.getMessage());
            } finally {
                numActiveTasks.decrementAndGet();
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

    @VisibleForTesting
    public boolean waitForRunSync() {
        try {
            while (numActiveTasks.get() > 0) {
                Thread.sleep(500);
            }
            return false;
        } catch (InterruptedException e) {
            logger.debug("Current thread interrupted while waiting for runSync");
            return true;
        }
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

        // Session is empty before Trackingplan.init(...).start() is called. So queue these requests
        // unless a valid session is started and tracking is disabled.
        if (!currentSession.getSessionId().isEmpty() && !currentSession.isTrackingEnabled()) {
            logger.verbose("Request ignored. Tracking disabled for current session");
            return;
        }

        try {
            initRequestContext(request, interceptionContext);

            // Note that initRequestDestination depends on TrackingplanConfig#customDomains setting.
            // This means that intercepted requests targeted to any customDomain are ignored
            // before Trackingplan.init(...).start() call.
            initRequestDestination(request);

            if (!isTargetedToSupportedDestination(request)) {
                logger.verbose("Request ignored. Doesn't belong to a supported destination");
                return;
            }

            requestQueue.queueRequest(request);

            // Do not process queue if there is no config or currentSession is not yet a valid
            if (!isConfigured() || !currentSession.isTrackingEnabled()) {
                return;
            }

            // Keep reference to session in case currentSession changes while processing the queue
            final var session = currentSession;

            requestQueue.processQueue(session, false, () -> {
                // Check that the session is still the same
                if (!currentSession.getSessionId().equals(session.getSessionId())) return;
                if (currentSession.updateLastActivity()) {
                    storage.saveSession(currentSession);
                    logger.verbose("Last session activity updated and saved");
                }
            });

        } catch (Exception ex) {
            logger.error("Request processing failed: " + ex.getMessage());
        }
    }

    /**
     * Creates a trackingplan event and adds it to the queue. Note that the event might end up
     * not being send if the session wasn't eligible for tracking or the app gets terminated
     * or killed before the queue is processed and sent to Trackingplan.
     *
     * @param eventName Name of the event
     * @param properties Properties of the event
     */
    private void queueTrackingplanEvent(@NonNull final String eventName, final Bundle properties) {

        final var builder = new HttpRequest.Builder()
                .setProvider("trackingplan")
                .setHttpMethod("POST")
                .setUrl("TRACKINGPLAN")
                .addHeaderField("Content-Type", "application/json");

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("event_name", eventName);

            if (properties != null && !properties.isEmpty()) {
                jsonObject.put("properties", JSONUtils.makeJSONObject(properties));
            }

            final byte[] payload = JSONUtils.encodeJsonPayload(jsonObject);
            builder.setRequestPayload(payload);
            builder.setRequestPayloadNumBytes(payload.length);
        } catch (JSONException e) {
            // Silent exceptions. No error should compromise host app
            logger.verbose("Failed to create Trackingplan " + eventName + " event");
            return;
        }

        final var request = builder.build();
        final var interceptionContext = InterceptionContext.createInterceptionContext(context);
        processRequest(request, interceptionContext);
    }

    /**
     * Flush the requests queue. It times out after 10s.
     */
    @VisibleForTesting
    public void flushQueue() {
        flushQueue(10000);
    }

    private void flushQueue(long timeout) {
        final CountDownLatch lock = new CountDownLatch(1);
        runSync(() -> {
            if (!isConfigured() || !currentSession.isTrackingEnabled()) {
                logger.debug("Processing queue ignored because of missing configuration");
                lock.countDown();
                return;
            }
            requestQueue.processQueue(currentSession, true, lock::countDown);
        });
        try {
            if (Thread.currentThread() != handlerThread && timeout > 0) {
                var counterReachedZero = lock.await(timeout, TimeUnit.MILLISECONDS);
                if (!counterReachedZero) {
                    logger.debug("Queue flushing took longer than " + timeout + " ms. (timeout)");
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
        return config != null && !TrackingplanConfig.EMPTY.equals(config);
    }

    RuntimeEnvironment getRuntimeEnvironment() {
        return runtimeEnvironment;
    }

    private void startSession() {

        checkRunningInTrackingplanThread();

        // Force getting the sampling rate in order to trigger download every 24 hour as it might not
        // be downloaded in case of long sessions (a session spanning over a period larger than 24 hours).
        getSamplingRate();

        // Note that new session is started (and previous one is expired) after a period of inactivity
        // Let's assume that queue was empty before the app received new activity so any pending
        // requests in the queue should be attributed to the new session. In other words, there is
        // no need to flush the queue before starting a new session.
        final var session = restoreOrCreateSession();

        if (session.getSessionId().equals(currentSession.getSessionId())) {
            logger.verbose("Session already started. Start ignored");
            return;
        }

        if (session.isNew()) {
            logger.debug("New session started: " + session);
        } else {
            logger.debug("Session resumed: " + session);
        }

        var forceSendBatch = false;

        // Trigger new_session event every time a new session is started
        if (session.isNew()) {
            logger.verbose("New session");
            queueTrackingplanEvent("new_session", null);
        }

        // Trigger new_dau event if last one was sent 24h ago
        if (storage.wasLastDauSent24hAgo()) {
            logger.verbose("New daily active user");
            queueTrackingplanEvent("new_dau", null);
            forceSendBatch = true;
        }

        // Trigger new_user event the first time it is executed.
        if (storage.isFirstTimeExecution()) {
            logger.verbose("First time execution");
            queueTrackingplanEvent("new_user", null);
            forceSendBatch = true;
        }

        // All events triggered during startSession should have been queued before this assignment
        // to avoid useless calls to requestQueue.processQueue.
        currentSession = session;

        if (!currentSession.isTrackingEnabled()) {
            int numPendingRequests = requestQueue.discardPendingRequests();
            logger.debug("Tracking is disabled for this session");
            if (numPendingRequests > 0) {
                logger.debug(numPendingRequests + " pending intercepted requests were discarded");
            }
            return;
        }

        // Reference used by processQueue callback
        final var storage = this.storage;

        // Process pending requests that happened before session was started
        requestQueue.processQueue(currentSession, forceSendBatch, () -> {
            if (storage.isFirstTimeExecution()) {
                storage.saveFirstTimeExecution();
            }
            if (storage.wasLastDauSent24hAgo()) {
                storage.saveLastDauEventSentTime();
            }
        });
    }

    private void stopSession() {

        checkRunningInTrackingplanThread();

        if (currentSession.getSessionId().isEmpty()) {
            return;
        }

        if (currentSession.updateLastActivity()) {
            storage.saveSession(currentSession);
            logger.verbose("Last session activity updated and saved");
        }
    }

    @NonNull
    private TrackingplanSession restoreOrCreateSession() {

        checkRunningInTrackingplanThread();

        TrackingplanSession session = storage.loadSession();

        // Restore session
        if (!session.hasExpired()) {
            logger.verbose("Previous session found and is still valid");
            if (session.updateLastActivity()) {
                storage.saveSession(session);
                logger.verbose("Last session activity updated and saved");
            }
            return session;
        }

        logger.verbose("Previous session expired or doesn't exist. Creating a new session...");

        final var samplingRate = getSamplingRate();

        // New session
        if (!samplingRate.hasExpired()) {
            session = TrackingplanSession.newSession(samplingRate.getValue(), samplingRate.isTrackingEnabled());
            logger.verbose("New session created with tracking " + (session.isTrackingEnabled() ? "enabled" : "disabled"));
        } else {
            // Create a new session with tracking disabled.
            session = TrackingplanSession.newSession(session.getSamplingRate(), false);
            logger.verbose("New session created with tracking disabled because sampling rate was outdated");
        }

        storage.saveSession(session);
        logger.verbose("Session saved");

        return session;
    }

    @NonNull
    private SamplingRate getSamplingRate() {

        checkRunningInTrackingplanThread();

        var samplingRate = storage.loadSamplingRate();

        if (!samplingRate.hasExpired()) {
            logger.verbose("Previous sampling rate found and is still valid");
            logger.verbose("Sampling: " + samplingRate);
            return samplingRate;
        }

        logger.verbose("Sampling rate expired. Downloading...");

        if (fakeSamplingEnabled) {
            samplingRate = new SamplingRate(1.0f);
            storage.saveSamplingRate(samplingRate);
            logger.debug("Sampling rate downloaded and saved");
            logger.verbose("Sampling: " + samplingRate);
        } else {

            var numAttempts = 2;
            Exception lastError;

            do {
                try {
                    samplingRate = new SamplingRate(client.getSamplingRate());
                    lastError = null;
                    storage.saveSamplingRate(samplingRate);
                    logger.debug("Sampling rate downloaded and saved");
                    logger.verbose("Sampling: " + samplingRate);
                } catch (Exception ex) {
                    numAttempts -= 1;
                    lastError = ex;
                    SystemClock.sleep(1000);
                }
            } while (lastError != null && numAttempts > 0);

            if (lastError != null) {
                // TODO: Cancel any previous retry
                logger.error("Sampling rate download failed:\n\t" + lastError);
                logger.verbose(samplingRate.toString());
                runSyncDelayed(FETCH_CONFIG_RETRY_INTERVAL_MS, this::getSamplingRate);
            }
        }

        return samplingRate;
    }

    private void initRequestContext(HttpRequest request, InterceptionContext interceptionContext) {
        request.addContextField("app_name", InterceptionContext.appName);
        request.addContextField("app_version", InterceptionContext.appVersion);
        request.addContextField("language", InterceptionContext.language);
        request.addContextField("device", InterceptionContext.device);
        request.addContextField("platform", InterceptionContext.platform);

        if (!interceptionContext.activityName.isEmpty()) {
            request.addContextField("activity", interceptionContext.activityName);
        }

        if (!interceptionContext.screenName.isEmpty()) {
            request.addContextField("screen", interceptionContext.screenName);
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
            put("bat.bing.com", "bing");
            put("ping.chartbeat.net", "chartbeat");
            put("track-sdk-eu.customer.io/api", "customerio"); // Europe Region
            put("track-sdk.customer.io/api", "customerio"); // USA Region
            put("facebook.com/tr/", "facebook");
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

    private class FlushQueueOnStopLifecycleObserver implements DefaultLifecycleObserver {
        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            // TODO: This is not called when app is terminated/killed because of using ProcessLifecycleOwner
            DefaultLifecycleObserver.super.onStop(owner);
            runSync(() -> {
                if (!isConfigured()) return;
                logger.verbose("onStop lifecycle called.");
                logger.debug("Processing queue before going to background");
                // Do not wait as this is called from TP thread
                flushQueue(0);
            });
        }
    }

    private class SessionLifecycleObserver implements DefaultLifecycleObserver {
        @Override
        public void onResume(@NonNull LifecycleOwner owner) {
            DefaultLifecycleObserver.super.onResume(owner);
            runSync(() -> {
                if (!isConfigured()) return;
                logger.verbose("onResume lifecycle called");
                startSession();
            });
        }

        @Override
        public void onPause(@NonNull LifecycleOwner owner) {
            // This is not called when app is terminated. As a workaround, activity is updated
            // after sending a batch of raw tracks in processRequest
            DefaultLifecycleObserver.super.onPause(owner);
            runSync(() -> {
                if (!isConfigured()) return;
                logger.verbose("onPause lifecycle called");
                stopSession();
            });
        }
    }

    private class MyScreenViewListener implements ScreenViewTracker.ScreenViewListener {
        @Override
        public void onScreenViewed(@NonNull final String screenName,
                                   @NonNull final String previousScreenName) {
            logger.debug("Screen View " + screenName);
            runSync(() -> {
                if (!isConfigured()) return;
                final var params = new Bundle();
                params.putString("screen", screenName);
                if (!previousScreenName.isEmpty()) {
                    params.putString("previous_screen", previousScreenName);
                }
                queueTrackingplanEvent("screen_view", params);
            });
        }
    }
}
