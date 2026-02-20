package com.trackingplan.client.sdk;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.trackingplan.shared.TestLogger;
import com.trackingplan.shared.Storage;
import com.trackingplan.client.sdk.util.AndroidLog;
import com.trackingplan.shared.ContextProvider;
import com.trackingplan.shared.ServiceLocator;
import com.trackingplan.shared.TestTimeProvider;
import com.trackingplan.shared.TrackingplanSession;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
abstract class BaseInstrumentedTest {

    private final static String LOG_TAG = "TrackingplanTest";

    protected static final String TEST_TP_ID = "TP000000";
    protected static final String TEST_ENVIRONMENT = "PRODUCTION";

    protected TestLogger logger;
    protected TestTimeProvider fakeTime;

    protected Context context;

    @Before
    public void setUp() {
        logger = new TestLogger(20);
        AndroidLog.getInstance().addLogger(logger);
        AndroidLog.setLogLevel(AndroidLog.LogLevel.VERBOSE);

        fakeTime = new TestTimeProvider();
        ServiceLocator.INSTANCE.setTimeProvider(fakeTime);

        context = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();
        ContextProvider.INSTANCE.init(context);

        try {
            final var storage = Storage.Companion.create(TEST_TP_ID, TEST_ENVIRONMENT);
            storage.clear();
            // Clear storage for other test configurations used in IngestConfigCacheInstrumentedTest
            final var storage2 = Storage.Companion.create("TP873633", "PRODUCTION");
            storage2.clear();
            final var storage3 = Storage.Companion.create("TP873633", "preproduction");
            storage3.clear();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create storage", e);
        }
    }

    @After
    public void tearDown() {
        stopTrackingplan();
        AndroidLog.getInstance().removeLogger(logger);
        ServiceLocator.INSTANCE.reset();
        // Do not clear storage for inspection when debugging
    }

    protected void startTrackingplan() {
        startTrackingplan(TEST_TP_ID, TEST_ENVIRONMENT, true);
    }

    protected void startTrackingplan(
            @NonNull final String tpId,
            @NonNull final String environment
    ) {
        startTrackingplan(tpId, environment, true);
    }

    // Helpers
    protected void startTrackingplan(
            @NonNull final String tpId,
            @NonNull final String environment,
            final boolean fakeSampling
    ) {
        Log.i(LOG_TAG, "start trackingplan");

        // Pre-populate cache before initialize to avoid network downloads in tests
        if (fakeSampling) {
            try {
                var storage = Storage.Companion.create(tpId, environment);
                storage.getIngestConfigCache().save("{\"sample_rate\": 1}");
                storage.saveTrackingEnabled(true);
            } catch (Exception e) {
                throw new RuntimeException("Failed to pre-populate cache", e);
            }
        }

        var instance = TrackingplanInstance.getInstance();

        if (instance == null) {
            startTrackingplanInitializer();
            instance = TrackingplanInstance.getInstance();
        }

        Trackingplan.init(tpId)
                .environment(environment)
                .tags(new HashMap<>() {{
                    put("tag1", "value1");
                }})
//               .tracksEndPoint("http://192.168.0.24:9090")
                .enableDebug()
                .dryRun()
                .start(context);

        if (instance != null) {
            if (instance.waitForRunSync()) {
                Assert.fail("Wait for Trackingplan start timed out");
            }
            Assert.assertNotEquals(TrackingplanSession.Companion.getEMPTY(), instance.getSession());
        } else {
            Assert.fail("Trackingplan service not registered");
        }
    }

    protected void stopTrackingplan() {
        Log.i(LOG_TAG, "stop trackingplan");

        final var instance = TrackingplanInstance.getInstance();
        if (instance == null) {
            Log.w(LOG_TAG, "Stop failed. Trackingplan service not registered");
            return;
        }

        instance.flushQueue();
        Trackingplan.stop();
        if (instance.waitForRunSync()) {
            Assert.fail("Wait for Trackingplan stop timed out");
        }

        Log.i(LOG_TAG, "trackingplan stopped");
    }

    protected void startTrackingplanInitializer() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            final var initializer = new TrackingplanInitializer();
            initializer.create(context);
            Assert.assertFalse(TrackingplanInstance.getInstance().getSession().getTrackingEnabled());
            Assert.assertEquals(TrackingplanSession.Companion.getEMPTY(), TrackingplanInstance.getInstance().getSession());
        });
    }
}
