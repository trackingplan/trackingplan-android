package com.trackingplan.client.sdk;

import com.trackingplan.client.sdk.interception.HttpRequest;
import com.trackingplan.client.sdk.interception.InterceptionContext;
import com.trackingplan.shared.Storage;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Instrumented tests for preQueue and request processing functionality.
 *
 * Note: Direct testing of the preQueue mechanism is challenging because it requires
 * precise timing control between request interception and session initialization.
 * These tests verify the observable behavior of request processing.
 */
public class PreQueueInstrumentedTest extends BaseInstrumentedTest {

    @Test
    public void given_SessionStarted_when_RequestProcessed_then_RequestQueued() throws Exception  {
        // Given - Start trackingplan with session
        startTrackingplan();
        final var instance = TrackingplanInstance.getInstance();

        // Verify session is active
        Assert.assertNotEquals("Session should be active", "", instance.getSession().getSessionId());
        Assert.assertTrue("Tracking should be enabled", instance.getSession().getTrackingEnabled());

        // When - Process a request
        logger.reset();
        logger.expectMessageStartsWith("Queue processed (1 requests). Batch 1 scheduled for sending");
        logger.expectMessageStartingWithAndContaining("Batch:", List.of("\"endpoint\": \"https:\\/\\/api.amplitude.com\\/batch\""));

        instance.runSync(() -> {
            instance.processRequest(createFakeAmplitudeRequest(), createContext());
        });
        instance.flushQueue();
        instance.waitForRunSync();

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_TrackingplanInitButNoIngestConfig_when_RequestProcessed_then_RequestPreQueued() throws Exception  {
        // Given - Trackingplan initialized but no ingest config downloaded yet
        startTrackingplanInitializer();
        final var instance = TrackingplanInstance.getInstance();

        // When - Process a request before Trackingplan.init
        logger.reset();
        logger.expectExactMessage("Request pre-queued (session not ready)"); // amplify
        logger.expectExactMessage("Request pre-queued (session not ready)"); // trackingplan new-session
        logger.expectExactMessage("Request pre-queued (session not ready)"); // trackingplan new-dau
        logger.expectExactMessage("Request pre-queued (session not ready)"); // trackingplan new-user
        logger.expectExactMessage("Processing 4 pre-queued requests...");
        logger.expectExactMessage("Pre-queue processed");
        logger.expectExactMessage("Queue processed (4 requests). Batch 0 scheduled for sending");

        instance.runSync(() -> {
            instance.processRequest(createFakeAmplitudeRequest(), createContext());
        });

        startTrackingplan();
        instance.waitForRunSync();

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_TrackingDisabled_when_RequestProcessed_then_RequestDropped() throws Exception {
        // Given - Pre-populate cache with tracking disabled
        var storage = Storage.Companion.create(TEST_TP_ID, TEST_ENVIRONMENT);
        storage.getIngestConfigCache().save("{\"sample_rate\": 0}");
        storage.saveTrackingEnabled(false);

        startTrackingplan(TEST_TP_ID, TEST_ENVIRONMENT, false);
        final var instance = TrackingplanInstance.getInstance();

        // Verify tracking is disabled
        Assert.assertFalse("Tracking should be disabled", instance.getSession().getTrackingEnabled());

        // When - Process a request
        logger.reset();
        logger.expectMessageStartsWith("Request dropped (reason: tracking-disabled)");

        instance.runSync(() -> {
            instance.processRequest(createFakeAmplitudeRequest(), createContext());
        });
        instance.waitForRunSync();

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_CustomDomainConfigured_when_RequestForCustomDomain_then_RequestMatched() throws Exception {
        // Given - Start trackingplan with custom domains
        startTrackingplanInitializer();
        final var instance = TrackingplanInstance.getInstance();

        var storage = Storage.Companion.create(TEST_TP_ID, TEST_ENVIRONMENT);
        storage.getIngestConfigCache().save("{\"sample_rate\": 1}");
        storage.saveTrackingEnabled(true);

        Trackingplan.init(TEST_TP_ID)
                .environment(TEST_ENVIRONMENT)
                .customDomains(Map.of("analytics.mycompany.com", "custom_provider"))
                .enableDebug()
                .dryRun()
                .start(context);
        instance.waitForRunSync();

        // Verify session started
        Assert.assertNotEquals("Session should be active", "", instance.getSession().getSessionId());

        // When - Process request for custom domain
        logger.reset();
        logger.expectExactMessage("Processing request: https://analytics.mycompany.com/track");
        logger.expectMessageStartingWithAndContaining("Request queued", List.of("https://analytics.mycompany.com/track"));
        logger.expectMessageStartingWithAndContaining("Batch:", List.of("\"endpoint\": \"https:\\/\\/analytics.mycompany.com\\/track\""));

        instance.runSync(() -> {
            instance.processRequest(
                createRequestForUrl("https://analytics.mycompany.com/track"),
                createContext()
            );
        });
        instance.flushQueue();
        instance.waitForRunSync();

        // Then - Verify request was NOT ignored as unknown destination
        logger.assertExpectationsMatch();
        Assert.assertFalse(
            "Request should be matched to custom provider, not ignored",
            logger.containsExactMessage("Request ignored. Doesn't belong to a supported destination")
        );
    }

    @Test
    public void given_UnknownDomain_when_RequestProcessed_then_RequestIgnored() throws Exception {
        // Given - Start trackingplan without custom domains
        startTrackingplan();
        final var instance = TrackingplanInstance.getInstance();

        // When - Process request for unknown domain
        logger.reset();
        // The SDK logs at verbose level when request doesn't match any known destination
        logger.expectExactMessage("Processing request: https://unknown-analytics.example.com/track");
        logger.expectMessageStartsWith("Request ignored. Doesn't belong to a supported destination");

        instance.runSync(() -> {
            instance.processRequest(
                createRequestForUrl("https://unknown-analytics.example.com/track"),
                createContext()
            );
        });
        instance.waitForRunSync();

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_AdaptiveSamplingConfigured_when_RequestProcessed_then_SamplingEvaluated() throws Exception {
        // Given - Pre-populate cache with adaptive sampling config
        var storage = Storage.Companion.create(TEST_TP_ID, TEST_ENVIRONMENT);
        String configJson = """
            {
                "sample_rate": 100,
                "options": {
                    "useAdaptiveSampling": true,
                    "adaptiveSamplingPatterns": ["{\\"provider\\":\\"amplitude\\",\\"sample_rate\\":1}"]
                }
            }
            """;
        storage.getIngestConfigCache().save(configJson);

        startTrackingplan(TEST_TP_ID, TEST_ENVIRONMENT, false);
        final var instance = TrackingplanInstance.getInstance();

        // When - Process amplitude request (should be rescued by adaptive sampling)
        logger.reset();
        logger.expectExactMessage("Processing request: https://api.amplitude.com/batch");
        logger.expectMessageStartingWithAndContaining("Request queued", List.of("https://api.amplitude.com/batch"));
        logger.expectMessageStartingWithAndContaining("Batch:", List.of("\"endpoint\": \"https:\\/\\/api.amplitude.com\\/batch\"", "\"sampling_rate\": 1", "\"sampling_mode\": \"ADAPTIVE\\/EVENT_DICE\\/EVENT_MATCHED\""));

        instance.runSync(() -> {
            instance.processRequest(createFakeAmplitudeRequest(), createContext());
        });
        instance.flushQueue();
        instance.waitForRunSync();

        // Then - Request should be processed (either queued or dropped, but not ignored)
        logger.assertExpectationsMatch();
        Assert.assertFalse(
            "Request should not be ignored as unknown destination",
            logger.containsExactMessage("Request ignored (reason: unknown-destination)")
        );
    }

    private HttpRequest createFakeAmplitudeRequest() {
        return new HttpRequest.Builder()
                .setUrl("https://api.amplitude.com/batch")
                .setHttpMethod("POST")
                .setProvider("amplitude")
                .setInterceptionModule("test")
                .build();
    }

    private HttpRequest createRequestForUrl(String url) {
        return new HttpRequest.Builder()
                .setUrl(url)
                .setHttpMethod("POST")
                .setInterceptionModule("okhttp")
                .build();
    }

    private InterceptionContext createContext() {
        return InterceptionContext.createInterceptionContext(context);
    }
}
