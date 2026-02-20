package com.trackingplan.client.sdk;

import com.trackingplan.shared.TimeProvider;

import org.junit.Assert;
import org.junit.Test;

public class IngestConfigCacheInstrumentedTest extends BaseInstrumentedTest {

    @Test
    public void given_EmptyIngestConfigCache_when_SdkStarts_then_IngestConfigIsDownloaded() throws Exception {
        // Given
        startTrackingplanInitializer();

        // When
        logger.expectExactMessage("Ingest config expired or not found. Downloading...");
        logger.expectExactMessage("Ingest config downloaded and saved");
        startTrackingplan(TEST_TP_ID, TEST_ENVIRONMENT, false);

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_CachedIngestConfig_when_SdkStarts_then_IngestConfigIsFound() throws Exception {
        // Given - Cache is pre-populated by startTrackingplan helper

        // When
        logger.expectExactMessage("Previous ingest config found and is still valid");
        startTrackingplan();

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_CachedIngestConfig_when_SdkRestarts_then_CacheIsUsed() throws Exception {
        // Given - first download caches the config
        startTrackingplanInitializer();
        logger.expectExactMessage("Ingest config expired or not found. Downloading...");
        logger.expectExactMessage("Ingest config downloaded and saved");
        logger.expectMessageStartsWith("Sampling rate:");
        startTrackingplan("TP873633", "PRODUCTION", false);
        stopTrackingplan();

        // When - second start should use cached config
        logger.expectExactMessage("Previous ingest config found and is still valid");
        logger.expectMessageStartsWith("Sampling rate:");
        startTrackingplan("TP873633", "PRODUCTION", false);

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_PreviousIngestConfig_when_SdkStartsBefore24h_then_IngestConfigIsKept() throws Exception {
        // Given
        startTrackingplan();
        stopTrackingplan();
        fakeTime.advanceTime(23 * TimeProvider.HOUR);

        // When
        logger.reset();
        logger.expectExactMessage("Previous ingest config found and is still valid");
        startTrackingplan();

        // Then
        logger.assertExpectationsMatch();
        Assert.assertFalse(logger.containsExactMessage("Ingest config expired or not found. Downloading..."));
    }

    @Test
    public void given_PreviousIngestConfig_when_SdkStartsAfter24h_then_IngestConfigIsDownloaded() throws Exception {
        // Given
        startTrackingplan();
        stopTrackingplan();
        fakeTime.advanceTime(24 * TimeProvider.HOUR);

        // When
        logger.reset();
        logger.expectExactMessage("Ingest config expired or not found. Downloading...");
        logger.expectExactMessage("Ingest config downloaded and saved");
        startTrackingplan(TEST_TP_ID, TEST_ENVIRONMENT, false);

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_ActiveSession_when_SdkRestartsWithinSessionTimeout_then_SessionResumed() throws Exception {
        // First start - new session is created
        logger.expectExactMessage("Previous ingest config found and is still valid");
        logger.expectMessageStartsWith("New session started");
        logger.expectExactMessage("Queued Trackingplan new_session event");
        logger.expectExactMessage("Queued Trackingplan new_dau event");
        logger.expectExactMessage("Queued Trackingplan new_user event");
        logger.expectExactMessage("Queue processed (3 requests). Batch 0 scheduled for sending");
        // Second start - session is resumed (within 30 minute timeout)
        // Note: no new_dau event since only 15 min elapsed, not 24h
        logger.expectExactMessage("Previous ingest config found and is still valid");
        logger.expectMessageStartsWith("Session resumed");

        // First start
        startTrackingplan();
        fakeTime.advanceTime(10 * TimeProvider.MINUTE);
        stopTrackingplan();

        // Second start - resume session after 5 minutes (total 15 min, within 30 min timeout)
        fakeTime.advanceTime(5 * TimeProvider.MINUTE);
        startTrackingplan();

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_SessionSpanning2Days_when_SdkStartsEveryDay_then_IngestConfigIsDownloaded() throws Exception {
        // First day: start session, download config, queue events
        logger.expectExactMessage("Ingest config expired or not found. Downloading...");
        logger.expectExactMessage("Ingest config downloaded and saved");
        logger.expectMessageStartsWith("New session started");
        logger.expectExactMessage("Queued Trackingplan new_session event");
        logger.expectExactMessage("Queued Trackingplan new_dau event");
        logger.expectExactMessage("Queued Trackingplan new_user event");
        logger.expectExactMessage("Queue processed (3 requests). Batch 0 scheduled for sending");

        // Second day: config re-downloaded, session RESUMED (not new), new DAU event
        logger.expectExactMessage("Ingest config expired or not found. Downloading...");
        logger.expectExactMessage("Ingest config downloaded and saved");
        logger.expectMessageStartsWith("Session resumed");
        logger.expectExactMessage("Queued Trackingplan new_dau event");
        logger.expectExactMessage("Queue processed (1 requests). Batch 0 scheduled for sending");

        // First day: start tracking (no cache pre-population = real download)
        startTrackingplanInitializer();
        startTrackingplan(TEST_TP_ID, TEST_ENVIRONMENT, false);

        // Advance 28 hours while session is running (session stays active)
        fakeTime.advanceTime(28 * TimeProvider.HOUR);
        stopTrackingplan(); // Session duration is 28h, still active (timeout is 30min after stop)

        // Second day: resume after 5 minutes since stop (within 30min session timeout)
        fakeTime.advanceTime(5 * TimeProvider.MINUTE);
        startTrackingplan(TEST_TP_ID, TEST_ENVIRONMENT, false);

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_FakeSamplingDisabled_when_SdkStartsWithProvisionedTpId_then_IngestConfigIsDownloaded() throws Exception {
        // Given
        startTrackingplanInitializer();

        // When
        logger.expectExactMessage("Ingest config expired or not found. Downloading...");
        logger.expectExactMessage("Ingest config downloaded and saved");
        logger.expectMessageStartsWith("Sampling rate: 15");
        startTrackingplan("TP873633", "PRODUCTION", false);

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_FakeSamplingDisabled_when_SdkStartsWithNotProvisionedTpId_then_IngestConfigIsDownloaded() throws Exception {
        // Test workaround for response 404 when downloading ingest config

        // Given
        startTrackingplanInitializer();

        // When
        logger.expectExactMessage("Ingest config expired or not found. Downloading...");
        logger.expectExactMessage("Ingest config downloaded and saved");
        logger.expectMessageStartsWith("Sampling rate: 1");
        startTrackingplan("TP000000", "PRODUCTION", false);

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_FakeSamplingDisabled_when_SdkStartsWithEnv_then_IngestConfigFromThatEnvIsDownloaded() throws Exception {
        // Given
        startTrackingplanInitializer();

        // When
        logger.expectExactMessage("Ingest config expired or not found. Downloading...");
        logger.expectExactMessage("Ingest config downloaded and saved");
        logger.expectMessageStartsWith("Sampling rate: 1");
        startTrackingplan("TP873633", "preproduction", false);

        // Then
        logger.assertExpectationsMatch();
    }
}
