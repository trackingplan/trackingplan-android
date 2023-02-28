package com.trackingplan.client.junit;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.test.platform.app.InstrumentationRegistry;

import com.trackingplan.client.sdk.TrackingplanConfig;
import com.trackingplan.client.sdk.util.AndroidLogger;

import java.util.Map;

@VisibleForTesting
public class TrackingplanJUnit {

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    /**
     * Initializes Trackingplan for JUnit
     */
    @NonNull
    public static TrackingplanInitializer init(@NonNull String tpId, @NonNull String environment) {
        // By default, enable debug and disable the background observer
        return new TrackingplanInitializer(tpId, environment);
    }

    /**
     * Starts collecting requests
     */
    @MainThread
    private static void start(@NonNull TrackingplanConfig config) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            logger.verbose("TrackingplanRule initialization started on main thread...");
            com.trackingplan.client.sdk.Trackingplan.start(config);
            logger.verbose("TrackingplanRule initialization finished");
        });
    }

    /**
     * Send collected requests to Trackingplan and stops collecting requests
     */
    public static void doSendAndStop() throws InterruptedException {
        // Wait for analytic requests to be triggered
        long SLEEP_TIME_MS = 1500;
        Thread.sleep(SLEEP_TIME_MS);
        // Ensures that all of the intercepted analytic requests are sent to Trackingplan
        com.trackingplan.client.sdk.Trackingplan.flushQueue();
        // Clear initialization state so that the next init call works
        com.trackingplan.client.sdk.Trackingplan.clearInitState();
    }

    public static class TrackingplanInitializer {

        private final TrackingplanConfig.Builder configBuilder;

        private TrackingplanInitializer(@NonNull String tpId, @NonNull String environment) {
            configBuilder = TrackingplanConfig.newConfig(tpId)
                    .environment(environment)
                    .enableDebug()
                    .disableBackgroundObserver();
        }

        public TrackingplanInitializer configEndPoint(@NonNull String endPoint) {
            configBuilder.configEndPoint(endPoint);
            return this;
        }

        public TrackingplanInitializer customContext(@NonNull Map<String, String> customContext) {
            configBuilder.customContext(customContext);
            return this;
        }

        public TrackingplanInitializer customDomains(@NonNull Map<String, String> customDomains) {
            configBuilder.customDomains(customDomains);
            return this;
        }

        public TrackingplanInitializer dryRun() {
            configBuilder.enableDryRun();
            return this;
        }

        public TrackingplanInitializer tags(@NonNull Map<String, String> tags) {
            configBuilder.tags(tags);
            return this;
        }

        public TrackingplanInitializer tracksEndPoint(@NonNull String endPoint) {
            configBuilder.tracksEndPoint(endPoint);
            return this;
        }

        @NonNull
        public TrackingplanRule newRule() {
            return new TrackingplanRule(this);
        }

        public void start() {
            TrackingplanJUnit.start(configBuilder.build());
        }
    }
}
