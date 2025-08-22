package com.trackingplan.client.sdk;

import com.trackingplan.client.sdk.util.Time;

import org.junit.Assert;
import org.junit.Test;

public class SamplingRateInstrumentedTest extends BaseInstrumentedTest {

    @Test
    public void given_EmptySamplingRate_when_SdkStarts_then_SamplingRateIsDownloaded() {
        // Given
        // Clear state

        // When
        logger.expectExactMessage("Sampling rate expired. Downloading...");
        logger.expectExactMessage("Sampling rate downloaded and saved");
        startTrackingplan();

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_PreviousSamplingRate_when_SdkStartsBefore24h_then_SamplingRateIsKept() {
        // Given
        startTrackingplan();
        stopTrackingplan();
        fakeTime.forwardTime(23 * Time.HOUR);

        // When
        logger.reset();
        logger.expectExactMessage("Previous sampling rate found and is still valid");
        startTrackingplan();

        // Then
        logger.assertExpectationsMatch();
        Assert.assertFalse(logger.containsExactMessage("Sampling rate expired. Downloading..."));
    }

    @Test
    public void given_PreviousSamplingRate_when_SdkStartsAfter24h_then_SamplingRateIsDownloaded() {
        // Given
        startTrackingplan();
        stopTrackingplan();
        fakeTime.forwardTime(24 * Time.HOUR);

        // When
        logger.expectExactMessage("Sampling rate expired. Downloading...");
        logger.expectExactMessage("Sampling rate downloaded and saved");
        startTrackingplan();

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_SessionSpanning2Days_when_SdkStartsEveryDay_then_SamplingRateIsDownloaded() {
        logger.expectExactMessage("Sampling rate expired. Downloading...");
        logger.expectExactMessage("Sampling rate downloaded and saved");
        logger.expectMessageStartsWith("New session started");
        logger.expectExactMessage("New session");
        logger.expectExactMessage("New daily active user");
        logger.expectExactMessage("First time execution");
        logger.expectExactMessage("Queue processed (3 requests). Batch 0 scheduled for sending");
        logger.expectExactMessage("Sampling rate expired. Downloading...");
        logger.expectExactMessage("Sampling rate downloaded and saved");
        logger.expectMessageStartsWith("Session resumed");
        logger.expectExactMessage("New daily active user");
        logger.expectExactMessage("Queue processed (1 requests). Batch 0 scheduled for sending");

        // First day
        startTrackingplan();
        fakeTime.forwardTime(28 * Time.HOUR);
        stopTrackingplan(); // Session duration is 28 hours (it didn't expire)

        // Second day, resume session after 5 minutes since stop
        fakeTime.forwardTime(5 * Time.MINUTE);
        startTrackingplan();

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_FakeSamplingRateDisabled_when_SdkStartsWithProvisionedTpId_then_SamplingRateIsDownloaded() {
        // Given
        startTrackingplanInitializer();
        TrackingplanInstance.getInstance().setFakeSamplingEnabled(false);

        // When
        logger.expectExactMessage("Sampling rate expired. Downloading...");
        logger.expectExactMessage("Sampling rate downloaded and saved");
        logger.expectMessageStartsWith("Sampling: SamplingRate{samplingRate=180.0");
        startTrackingplan("TP944306", "PRODUCTION");

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_FakeSamplingRateDisabled_when_SdkStartsWithNotProvisionedTpId_then_SamplingRateIsDownloaded() {
        // Test workaround for response 404 when downloading sampling rate

        // Given
        startTrackingplanInitializer();
        TrackingplanInstance.getInstance().setFakeSamplingEnabled(false);

        // When
        logger.expectExactMessage("Sampling rate expired. Downloading...");
        logger.expectExactMessage("Sampling rate downloaded and saved");
        logger.expectMessageStartsWith("Sampling: SamplingRate{samplingRate=1.0");
        startTrackingplan("TP000000", "PRODUCTION");

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_FakeSamplingRateDisabled_when_SdkStartsWithEnv_then_SamplingRateFromThatEnvIsDownloaded() {
        // Given
        startTrackingplanInitializer();
        TrackingplanInstance.getInstance().setFakeSamplingEnabled(false);

        // When
        logger.expectExactMessage("Sampling rate expired. Downloading...");
        logger.expectExactMessage("Sampling rate downloaded and saved");
        logger.expectMessageStartsWith("Sampling: SamplingRate{samplingRate=1.0");
        startTrackingplan("TP944306", "staging");

        // Then
        logger.assertExpectationsMatch();
    }
}
