package com.trackingplan.client.junit;

import android.content.Context;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.test.platform.app.InstrumentationRegistry;

import com.trackingplan.client.sdk.Trackingplan;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.HashMap;

@VisibleForTesting
public class TrackingplanJUnitRule implements TestRule {

    private final String tpId;
    private final String environment;
    private final String testName;

    private final long SLEEP_TIME_MS = 1500;

    public TrackingplanJUnitRule(@NonNull String tpId, @NonNull String environment) {
        this.tpId = tpId;
        this.environment = environment;
        this.testName = "";
    }

    public TrackingplanJUnitRule(
            @NonNull String tpId,
            @NonNull String environment,
            @NonNull String testName
    ) {
        this.tpId = tpId;
        this.environment = environment;
        this.testName = testName;
    }

    @NonNull
    @Override
    public Statement apply(@NonNull Statement base, @NonNull Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // Before
                InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                    initTrackingplan(tpId, environment, testName, description);
                });
                try {
                    base.evaluate();
                } finally {
                    // After
                    sendCollectedRequests();
                }
            }
        };
    }

    /**
     * Initialize Trackingplan Android SDK with test environment settings
     */
    @MainThread
    private void initTrackingplan(
            @NonNull String tpId,
            @NonNull String environment,
            @NonNull String testName,
            @NonNull Description description
    ) {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        final var initializer = Trackingplan.init(tpId)
                .customContext(new HashMap<>() {{
                    put("junit_method_name", description.getMethodName());
                }})
                .environment(environment)
                .enableDebug()
                .disableBackgroundObserver();
                // .dryRun()

        if (!testName.isEmpty()) {
            initializer.tags(new HashMap<>() {{
                put("test_title", testName);
            }});
        }

        initializer.start(appContext);
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
