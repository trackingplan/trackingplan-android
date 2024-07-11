package com.trackingplan.client.junit;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.trackingplan.client.sdk.util.AndroidLog;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.HashMap;

@VisibleForTesting
public class TrackingplanRule implements TestRule {

    private static final AndroidLog logger = AndroidLog.getInstance();

    private final TrackingplanJUnit.TrackingplanInitializer initializer;
    private final long waitTimeMs;

    TrackingplanRule(TrackingplanJUnit.TrackingplanInitializer initializer, long waitTimeMs) {
        logger.verbose("TrackingplanRule instanced");
        this.initializer = initializer;
        this.waitTimeMs = waitTimeMs;
    }

    @NonNull
    @Override
    public Statement apply(@NonNull Statement base, @NonNull Description description) {
        logger.verbose("Applying TrackingplanRule...");
        initializer.customContext(new HashMap<>() {{
            put("junit_method_name", description.getMethodName());
        }});
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // Before
                initializer.start();
                try {
                    // Test
                    base.evaluate();
                } finally {
                    // After
                    TrackingplanJUnit.doSendAndStop(waitTimeMs);
                }
            }
        };
    }
}
