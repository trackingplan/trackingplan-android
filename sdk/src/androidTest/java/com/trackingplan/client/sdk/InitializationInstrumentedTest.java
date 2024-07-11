package com.trackingplan.client.sdk;

import static org.junit.Assert.assertTrue;

import org.junit.Test;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class InitializationInstrumentedTest extends BaseInstrumentedTest {
    @Test
    public void given_SdkWasNeverExecutedBefore_when_SdkStart_then_SdkStarts() {
        // Given
        // First time execution

        // When
        logger.expectExactMessage("Trackingplan initialized");
        logger.expectExactMessage("Queue processed (3 requests). Batch 0 scheduled for sending");
        startTrackingplan();

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_SdkAlreadyStarted_when_CallingSdkStartAgain_then_StartIgnored() {
        // Given
        startTrackingplan();

        // When
        logger.expectExactMessage("Trackingplan already initialized. Start ignored");
        startTrackingplan();

        // Then
        logger.assertExpectationsMatch();
    }
}
