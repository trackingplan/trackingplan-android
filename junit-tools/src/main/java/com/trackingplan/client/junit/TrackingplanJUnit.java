package com.trackingplan.client.junit;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.test.platform.app.InstrumentationRegistry;

import com.trackingplan.client.sdk.Trackingplan;
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
            logger.verbose("TrackingplanJUnit starting on main thread...");
            Trackingplan.clearInitState();
            Trackingplan.startTest(config);
            logger.verbose("TrackingplanJUnit started");
        });
    }

    /**
     * Send collected requests to Trackingplan and stops collecting requests
     */
    public static void doSendAndStop() throws InterruptedException {
        doSendAndStop(1500);
    }

    /**
     * Send collected requests to Trackingplan and stops collecting requests
     */
    public static void doSendAndStop(long waitTimeMs) throws InterruptedException {
        logger.verbose("TrackingplanJUnit do send and stop...");
        // Wait for analytic requests to be triggered
        if (waitTimeMs > 0) {
            Thread.sleep(waitTimeMs);
        }
        // Ensures that all of the intercepted analytic requests are sent to Trackingplan
        Trackingplan.flushQueue();
        // Clear initialization state so that the next init call works
        InstrumentationRegistry.getInstrumentation().runOnMainSync(Trackingplan::clearInitState);
    }

    public static class TrackingplanInitializer {

        private final TrackingplanConfig.Builder configBuilder;
        private long waitTimeMs = 1500;

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

        public TrackingplanInitializer waitTimeMs(long waitTimeMs) {
            this.waitTimeMs = waitTimeMs;
            return this;
        }

        @NonNull
        public TrackingplanRule newRule() {
            return new TrackingplanRule(this, waitTimeMs);
        }

        public void start() {
            TrackingplanJUnit.start(configBuilder.build());
        }
    }
}
