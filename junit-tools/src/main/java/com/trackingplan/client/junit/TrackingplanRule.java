package com.trackingplan.client.junit;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.trackingplan.client.sdk.util.AndroidLogger;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.HashMap;

@VisibleForTesting
public class TrackingplanRule implements TestRule {

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    private final TrackingplanJUnit.TrackingplanInitializer initializer;

    TrackingplanRule(TrackingplanJUnit.TrackingplanInitializer initializer) {
        logger.verbose("TrackingplanRule instanced");
        this.initializer = initializer;
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
                logger.verbose("TrackingplanRule before test");
                initializer.start();
                try {
                    // Test
                    logger.verbose("TrackingplanRule evaluating test...");
                    base.evaluate();
                } finally {
                    // After
                    logger.verbose("TrackingplanRule after test");
                    TrackingplanJUnit.doSendAndStop();
                }
            }
        };
    }
}
