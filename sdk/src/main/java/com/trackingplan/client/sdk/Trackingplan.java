// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.util.AndroidLogger;
import com.trackingplan.client.sdk.util.LogWrapper;

import java.util.Map;

final public class Trackingplan {

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    @SuppressWarnings("unused")
    public static ConfigInitializer init(String tpId) {
        return new ConfigInitializer(tpId);
    }

    @SuppressWarnings("unused")
    public static void stop(Context context) {
        try {
            var instance = TrackingplanInstance.getInstance();
            if (instance == null) {
                logger.warn(LogWrapper.LOG_TAG, "Instance not registered during app startup");
                return;
            }
            instance.stop();
            TrackingplanInstance.registerInstance(null);
            logger.info("Trackingplan v" + BuildConfig.SDK_VERSION + " disabled");
        } catch (Exception e) {
            // Use Log because AndroidLogger may not be enabled
            Log.w(LogWrapper.LOG_TAG, "Trackingplan stop failed: " + e.getMessage());
        }
    }

    public static class ConfigInitializer {

        private final TrackingplanConfig.Builder configBuilder;

        private ConfigInitializer(@NonNull String tpId) {
            configBuilder = new TrackingplanConfig.Builder(tpId);
        }

        @SuppressWarnings("unused")
        public ConfigInitializer enableDebug() {
            AndroidLogger.getInstance().setLogcatEnabled(true);
            configBuilder.enableDebug();
            return this;
        }

        @SuppressWarnings("unused")
        public ConfigInitializer ignoreContext() {
            configBuilder.ignoreContext();
            return this;
        }

        @SuppressWarnings("unused")
        public ConfigInitializer dryRun() {
            configBuilder.enableDryRun();
            return this;
        }

        @SuppressWarnings("unused")
        public ConfigInitializer sourceAlias(@NonNull String alias) {
            configBuilder.sourceAlias(alias);
            return this;
        }

        @SuppressWarnings("unused")
        public ConfigInitializer environment(@NonNull String environment) {
            configBuilder.environment(environment);
            return this;
        }

        @SuppressWarnings("unused")
        public ConfigInitializer customDomains(@NonNull Map<String, String> customDomains) {
            configBuilder.customDomains(customDomains);
            return this;
        }

        @SuppressWarnings("unused")
        public void start(Context context) {
            try {
                var instance = TrackingplanInstance.getInstance();
                if (instance == null) {
                    throw new RuntimeException("Instance not registered during app startup");
                }

                TrackingplanConfig config = configBuilder.build();

                if (config.isDebugEnabled()) {
                    logger.info("Debug mode enabled");
                }

                if (config.isDryRunEnabled()) {
                    logger.info("DryRun mode enabled");
                }

                instance.setConfig(config);

            } catch (Exception e) {
                // Use Log because AndroidLogger may not be enabled
                Log.w(LogWrapper.LOG_TAG, "Trackingplan initialization failed: " + e.getMessage());
            }
        }
    }
}
