package com.trackingplan.client.sdk;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.trackingplan.client.sdk.session.Storage;
import com.trackingplan.client.sdk.session.TrackingplanSession;
import com.trackingplan.client.sdk.test.TestLogger;
import com.trackingplan.client.sdk.test.TestTime;
import com.trackingplan.client.sdk.util.AndroidLog;
import com.trackingplan.client.sdk.util.ServiceLocator;
import com.trackingplan.client.sdk.util.Time;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
abstract class BaseInstrumentedTest {

    private final static String LOG_TAG = "TrackingplanTest";

    protected TestLogger logger;
    protected TestTime fakeTime;

    protected Context context;

    @Before
    public void setUp() {
        logger = new TestLogger(20);
        AndroidLog.getInstance().addLogger(logger);

        fakeTime = new TestTime();
        ServiceLocator.registerSharedInstance(Time.class, fakeTime, true);

        context = InstrumentationRegistry.getInstrumentation().getTargetContext().getApplicationContext();

        final var storage = new Storage("TP000000", "PRODUCTION", context);
        storage.clear();
    }

    @After
    public void tearDown() {
        stopTrackingplan();
        AndroidLog.getInstance().removeLogger(logger);
        // Do not clear storage for inspection when debugging
    }

    protected void startTrackingplan() {
        startTrackingplan("TP000000", "PRODUCTION");
    }

    // Helpers
    protected void startTrackingplan(
            @NonNull final String tpId,
            @NonNull final String environment
    ) {
        Log.i(LOG_TAG, "start trackingplan");

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
                .enableDebug()
                .dryRun()
                .start(context);

        if (instance != null) {
            if (instance.waitForRunSync()) {
                Assert.fail("Wait for Trackingplan start timed out");
            }
            Assert.assertNotEquals(instance.getSession(), TrackingplanSession.EMPTY);
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
            TrackingplanInstance.getInstance().setFakeSamplingEnabled(true);
            Assert.assertFalse(TrackingplanInstance.getInstance().getSession().isTrackingEnabled());
            Assert.assertEquals(TrackingplanInstance.getInstance().getSession(), TrackingplanSession.EMPTY);
        });
    }
}
