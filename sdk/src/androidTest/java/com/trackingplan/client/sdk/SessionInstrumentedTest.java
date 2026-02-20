package com.trackingplan.client.sdk;

import com.trackingplan.shared.TimeProvider;

import org.junit.Assert;
import org.junit.Test;


public class SessionInstrumentedTest extends BaseInstrumentedTest {

    @Test
    public void given_SdkWasNeverExecutedBefore_when_SdkStarts_then_NewSessionStarted() throws Exception {

        // Given
        // First time execution
        startTrackingplanInitializer();
        final var trackingplan = TrackingplanInstance.getInstance();
        var sessionBeforeInit = trackingplan.getSession();

        // When
        logger.expectExactMessage("Previous session expired or doesn't exist. Creating a new session...");
        logger.expectMessageStartsWith("New session started");
        logger.expectExactMessage("Queued Trackingplan new_user event");
        logger.expectExactMessage("Queue processed (3 requests). Batch 0 scheduled for sending");
        startTrackingplan();
        var sessionAfterInit = trackingplan.getSession();

        // Then
        Assert.assertNotEquals(sessionBeforeInit, sessionAfterInit);

        Assert.assertFalse(sessionBeforeInit.getTrackingEnabled());
        Assert.assertTrue(sessionBeforeInit.getSessionId().isEmpty());

        Assert.assertTrue(sessionAfterInit.getTrackingEnabled());
        Assert.assertFalse(sessionAfterInit.getSessionId().isEmpty());

        logger.assertExpectationsMatch();
    }

    @Test
    public void given_SessionWithActivity5MinutesAgo_when_SdkStarts_then_KeepSession() {
        // Given
        startTrackingplan();
        var previousSession = TrackingplanInstance.getInstance().getSession();
        stopTrackingplan();
        fakeTime.advanceTime(5 * TimeProvider.MINUTE);

        // When
        logger.expectMessageStartsWith("Session resumed");
        // TODO: Zero events
        startTrackingplan();
        var currentSession = TrackingplanInstance.getInstance().getSession();

        // Then
        Assert.assertEquals(previousSession, currentSession);
    }

    @Test
    public void given_SessionWithActivity30MinutesAgo_when_SdkStarts_then_CreateNewSession() throws Exception {
        // Given
        startTrackingplan();
        var previousSession = TrackingplanInstance.getInstance().getSession();
        stopTrackingplan();
        fakeTime.advanceTime(30 * TimeProvider.MINUTE);

        // When
        logger.expectExactMessage("Previous ingest config found and is still valid");
        logger.expectMessageStartsWith("New session started");
        logger.expectExactMessage("Queue processed (1 requests). Batch 0 scheduled for sending");
        logger.expectExactMessage("Trackingplan stopped");
        startTrackingplan();
        var currentSession = TrackingplanInstance.getInstance().getSession();
        stopTrackingplan();

        // Then
        Assert.assertNotEquals(previousSession, currentSession);
        logger.assertExpectationsMatch();
    }
}
