// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk;

import android.content.Context;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.trackingplan.client.sdk.interception.InstrumentRequestBuilder;
import com.trackingplan.client.sdk.util.AndroidLog;

import java.util.Map;

final public class Trackingplan {

    private static final AndroidLog logger = AndroidLog.getInstance();

    private Trackingplan() {
        // Empty
    }

    public static ConfigInitializer init(@NonNull String tpId) {
        return new ConfigInitializer(tpId);
    }

    @VisibleForTesting
    public static void clearInitState() {
        stop(false);
        // Wait for all task to finish
        final var instance = TrackingplanInstance.getInstance();
        if (instance != null && instance.waitForRunSync()) {
            logger.warn("Wait for Trackingplan stop timed out");
        }
    }

    /**
     * Tells Trackingplan SDK that it is running as part of an instrumented test. This mode inhibits
     * Trackingplan initialization from app (see {@link Trackingplan#init#start})
     */
    @VisibleForTesting
    public static void enableInstrumentedTestMode() {

        final var instance = TrackingplanInstance.getInstance();

        if (instance == null) {
            logger.error("Couldn't set up the runtime environment because TrackingplanInstance is not registered.");
            return;
        }

        instance.setRuntimeEnvironment(TrackingplanInstance.RuntimeEnvironment.AndroidJUnit);
    }

    @MainThread
    public static void start(@NonNull final TrackingplanConfig config) {

        final var instance = TrackingplanInstance.getInstance();

        if (instance == null) {
            logger.error("Trackingplan was not registered during app startup");
            return;
        }

        if (instance.getRuntimeEnvironment() == TrackingplanInstance.RuntimeEnvironment.AndroidJUnit) {
            logger.info("Trackingplan running in JUnit. Start from application ignored");
            return;
        }

        startTrackingplan(instance, config);
    }

    @MainThread
    @VisibleForTesting
    public static void startTest(@NonNull final TrackingplanConfig config) {

        final var instance = TrackingplanInstance.getInstance();

        if (instance == null) {
            logger.error("Trackingplan was not registered during app startup");
            return;
        }

        startTrackingplan(instance, config);
    }

    private static void startTrackingplan(
            @NonNull final TrackingplanInstance instance,
            @NonNull final TrackingplanConfig config
    ) {
        try {
            // Enable instrument
            InstrumentRequestBuilder.setDisabled(false);
            instance.start(config);
        } catch (Exception e) {
            logger.error("Trackingplan start failed: " + e.getMessage());
        }
    }

    public static void stop() {
        stop(true);
    }

    private static void stop(boolean unregisterInstance) {

        // Disable instrument
        InstrumentRequestBuilder.setDisabled(true);

        try {
            var instance = TrackingplanInstance.getInstance();
            if (instance != null) {
                instance.stop();
                if (unregisterInstance) {
                    instance.runSync(() -> {
                        TrackingplanInstance.registerInstance(null);
                    });
                }
            } else {
                throw new Exception("Trackingplan was not registered during app startup");
            }

            if (unregisterInstance) {
                logger.info("Trackingplan v" + BuildConfig.SDK_VERSION + " disabled. To enable it again remove the stop() call and restart the app.");
            } else {
                logger.info("Trackingplan v" + BuildConfig.SDK_VERSION + " stopped.");
            }

        } catch (Exception e) {
            logger.error("Trackingplan stop failed: " + e.getMessage());
        }
    }

    /**
     * Flush the queue of intercepted requests. This is a blocking operation and it should be
     * mainly used for testing purposes. This call times out after 10s.
     */
    @VisibleForTesting()
    public static void flushQueue() {
        var instance = TrackingplanInstance.getInstance();
        if (instance != null) {
            instance.flushQueue();
        }
    }

    public static class ConfigInitializer {

        private final TrackingplanConfig.Builder configBuilder;

        private ConfigInitializer(@NonNull String tpId) {
            configBuilder = new TrackingplanConfig.Builder(tpId);
        }

        @SuppressWarnings("unused")
        public ConfigInitializer configEndPoint(@NonNull String endPoint) {
            configBuilder.configEndPoint(endPoint);
            return this;
        }

        @SuppressWarnings("unused")
        public ConfigInitializer customDomains(@NonNull Map<String, String> customDomains) {
            configBuilder.customDomains(customDomains);
            return this;
        }

        @SuppressWarnings("unused")
        public ConfigInitializer dryRun() {
            configBuilder.enableDryRun();
            return this;
        }

        @SuppressWarnings("unused")
        public ConfigInitializer enableDebug() {
            configBuilder.enableDebug();
            return this;
        }

        @SuppressWarnings("unused")
        public ConfigInitializer environment(@NonNull String environment) {
            configBuilder.environment(environment);
            return this;
        }

        @SuppressWarnings("unused")
        public ConfigInitializer ignoreContext() {
            configBuilder.ignoreContext();
            return this;
        }

        @SuppressWarnings("unused")
        public ConfigInitializer tags(@NonNull Map<String, String> tags) {
            configBuilder.tags(tags);
            return this;
        }

        @SuppressWarnings("unused")
        public ConfigInitializer tracksEndPoint(@NonNull String endPoint) {
            configBuilder.tracksEndPoint(endPoint);
            return this;
        }

        @SuppressWarnings("unused")
        public ConfigInitializer sourceAlias(@NonNull String alias) {
            configBuilder.sourceAlias(alias);
            return this;
        }

        @MainThread
        public void start(Context ignoredContext) {
            TrackingplanConfig config = configBuilder.build();
            if (config.isDebugEnabled()) {
                AndroidLog.setLogLevel(AndroidLog.LogLevel.VERBOSE);
            }
            Trackingplan.start(config);
        }
    }
}
