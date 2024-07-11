package com.trackingplan.client.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.trackingplan.client.sdk.session.SamplingRate;
import com.trackingplan.client.sdk.session.Storage;
import com.trackingplan.client.sdk.session.TrackingplanSession;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StorageUnitTest extends BaseInstrumentedTest {

    @Test
    public void saveAndLoadSessionFromStorage() {
        final var session = TrackingplanSession.newSession(1.0f, true);
        final var storage = new Storage("TP000000", "PRODUCTION", context);
        storage.saveSession(session);
        final var loadedSession = storage.loadSession();
        assertEquals(session, loadedSession);
    }

    @Test
    public void saveAndLoadSamplingRateFromStorage() {
        final var samplingRate = new SamplingRate(1.0f);
        final var storage = new Storage("TP000000", "PRODUCTION", context);
        storage.saveSamplingRate(samplingRate);
        final var loadedSamplingRate = storage.loadSamplingRate();
        assertEquals(samplingRate, loadedSamplingRate);
    }

    @Test
    public void saveAndLoadFirstTimeExecution() {
        final var storage = new Storage("TP000000", "PRODUCTION", context);
        assertTrue(storage.isFirstTimeExecution());
        storage.saveFirstTimeExecution();
        assertFalse(storage.isFirstTimeExecution());
    }
}
