package com.trackingplan.client.junit;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.test.platform.app.InstrumentationRegistry;

import com.trackingplan.client.sdk.Trackingplan;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

@VisibleForTesting
public class TrackingplanJUnitRule implements TestRule {

    private final String tpId;
    private final String environment;

    private final long SLEEP_TIME_MS = 1500;

    public TrackingplanJUnitRule(@NonNull String tpId, @NonNull String environment) {
        this.tpId = tpId;
        this.environment = environment;
    }

    @NonNull
    @Override
    public Statement apply(@NonNull Statement base, @NonNull Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // Before
                initTrackingplan(tpId, environment, description);
                try {
                    base.evaluate();
                } finally {
                    // After
                    sendCollectedRequests();
                }
            }
        };
    }

    private void initTrackingplan(
            @NonNull String tpId,
            @NonNull String environment,
            @NonNull Description description
    ) {
        // Initialize Trackingplan Android SDK with test environment settings
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Trackingplan.init(tpId)
                .environment(environment)
                // TODO: Add support to pass test name as a tag
                // .sourceAlias(description.getMethodName())
                .enableDebug()
                // .dryRun()
                .start(appContext);
    }

    private void sendCollectedRequests() throws InterruptedException {
        // Wait for analytic requests to be triggered
        Thread.sleep(SLEEP_TIME_MS);
        // Ensures that all of the intercepted analytic requests are sent to Trackingplan
        Trackingplan.flushQueue();
        // Clear initialization state so that the next init call works
        Trackingplan.clearInitState();
    }
}
