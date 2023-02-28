// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk;

import android.content.Context;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.trackingplan.client.sdk.interception.InstrumentRequestBuilder;
import com.trackingplan.client.sdk.util.AndroidLogger;
import com.trackingplan.client.sdk.util.LogWrapper;

import java.util.Map;

final public class Trackingplan {

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    private Trackingplan() {
        // Empty
    }

    @SuppressWarnings("unused")
    @MainThread
    public static ConfigInitializer init(@NonNull String tpId) {
        return new ConfigInitializer(tpId);
    }

    @VisibleForTesting
    public static void clearInitState() {
        stop(false);
    }

    @MainThread
    public static void start(@NonNull TrackingplanConfig config) {
        try {
            var instance = TrackingplanInstance.getInstance();

            if (instance == null) {
                throw new RuntimeException("Instance not registered during app startup");
            }

            if (instance.isConfigured()) {
                logger.info("Trackingplan is already initialized. Start ignored");
                return;
            }

            // Enable instrument in case SDK was stopped before
            InstrumentRequestBuilder.setDisabled(false);

            if (config.isDebugEnabled()) {
                logger.info("Debug mode enabled");
            }

            if (config.isDryRunEnabled()) {
                logger.info("DryRun mode enabled");
            }

            if (!config.isBackgroundObserverEnabled()) {
                instance.attachToLifeCycle(null);
            }

            instance.setConfig(config);

        } catch (Exception e) {
            // Use Log because AndroidLogger may not be enabled
            Log.w(LogWrapper.LOG_TAG, "Trackingplan start failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    public static void stop() {
        stop(true);
    }

    private static void stop(boolean unregisterInstance) {

        InstrumentRequestBuilder.setDisabled(true);

        try {
            var instance = TrackingplanInstance.getInstance();
            if (instance != null) {
                instance.stop();
                if (unregisterInstance) {
                    TrackingplanInstance.registerInstance(null);
                }
            } else {
                logger.warn(LogWrapper.LOG_TAG, "Instance not registered during app startup");
            }
        } catch (Exception e) {
            // Use Log because AndroidLogger may not be enabled
            Log.w(LogWrapper.LOG_TAG, "Stop instance failed: " + e.getMessage());
        }

        if (unregisterInstance) {
            logger.info("Trackingplan v" + BuildConfig.SDK_VERSION + " disabled. To enable it again remove the stop() call and restart the app.");
        } else {
            logger.info("Trackingplan v" + BuildConfig.SDK_VERSION + " stopped. To start it again use Trackingplan.init(string).start()");
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
            AndroidLogger.getInstance().setLogcatEnabled(true);
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
            Trackingplan.start(config);
        }
    }
}
