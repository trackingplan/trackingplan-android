package com.trackingplan.client.sdk;

import com.trackingplan.client.sdk.interception.HttpRequest;
import com.trackingplan.client.sdk.interception.InterceptionContext;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;

/**
 * Instrumented test for updateTags functionality.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class TagsInstrumentedTest extends BaseInstrumentedTest {

    @Test
    public void given_TrackingplanInitialized_when_UpdateTags_then_TagsAreUpdated() throws Exception {
        // Given
        startTrackingplan();

        // When
        logger.expectExactMessage("Tags updated: {tag2=value2, tag3=value3}");
        logger.expectMessageStartingWithAndContaining("Batch: [", List.of("\"tag2\": \"value2\"", "\"tag3\": \"value3\""));
        Trackingplan.updateTags(new HashMap<String, String>() {{
            put("tag2", "value2");
            put("tag3", "value3");
        }});

        // Process a fake request to trigger batch creation with updated tags
        final var instance = TrackingplanInstance.getInstance();
        instance.runSync(() -> {
            instance.processRequest(
                createFakeRequest(), 
                InterceptionContext.createInterceptionContext(context)
            );
        });
        instance.flushQueue();

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_TrackingplanNotInitialized_when_UpdateTags_then_ErrorLogged() throws Exception {
        // Given
        // Trackingplan is not initialized

        // When
        logger.expectExactMessage("Cannot update tags. Trackingplan was not initialized");
        Trackingplan.updateTags(new HashMap<String, String>() {{
            put("tag2", "value2");
        }});

        // Then
        logger.assertExpectationsMatch();
    }

    @Test
    public void given_ExistingTags_when_UpdateTagsWithOverlappingKeys_then_TagsAreMerged() throws Exception {
        // Given
        startTrackingplan(); // BaseInstrumentedTest starts with tag1=value1

        // When
        logger.expectMessageStartsWith("Tags updated: {");
        logger.expectMessageStartingWithAndContaining("Batch: [", List.of("\"tag1\": \"newvalue1\"", "\"tag2\": \"value2\""));
        Trackingplan.updateTags(new HashMap<String, String>() {{
            put("tag1", "newvalue1"); // Overwrite existing key
            put("tag2", "value2");    // Add new key
        }});

        // Process a fake request to trigger batch creation with updated tags
        final var instance = TrackingplanInstance.getInstance();
        instance.runSync(() -> {
            instance.processRequest(
                createFakeRequest(), 
                InterceptionContext.createInterceptionContext(context)
            );
        });
        instance.flushQueue();

        // Then  
        logger.assertExpectationsMatch();
    }

    private HttpRequest createFakeRequest() {
        return new HttpRequest.Builder()
                .setUrl("https://api.amplitude.com/batch")
                .setHttpMethod("POST")
                .setProvider("amplitude")
                .setInterceptionModule("test")
                .build();
    }
}