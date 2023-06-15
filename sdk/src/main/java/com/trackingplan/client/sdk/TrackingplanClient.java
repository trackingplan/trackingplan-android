// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk;

import android.content.Context;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.delivery.TrackBuilder;
import com.trackingplan.client.sdk.exceptions.TrackingplanSendException;
import com.trackingplan.client.sdk.interception.HttpRequest;
import com.trackingplan.client.sdk.util.AndroidLogger;
import com.trackingplan.client.sdk.util.StreamUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

final public class TrackingplanClient {

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    private static final int TRACKS_CONNECT_TIMEOUT = 30 * 1000;

    private final TrackingplanConfig config;
    private final TrackBuilder builder;

    public TrackingplanClient(@NonNull TrackingplanConfig config, @NonNull final Context context) {
        this.config = config;
        this.builder = new TrackBuilder(config, context);
    }

    public float getSamplingRate() throws IOException, JSONException {

        URL url = new URL(config.getConfigEndPoint() + "config-" + config.getTpId() + ".json");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        try (InputStream in = new BufferedInputStream(urlConnection.getInputStream())) {
            String rawConfig = StreamUtils.convertInputStreamToString(in);
            var jsonObject = new JSONObject(rawConfig);
            float samplingRate = (float) jsonObject.getDouble("sample_rate");
            var environmentRates = jsonObject.optJSONObject("environment_rates");
            if (environmentRates != null) {
                samplingRate = (float) environmentRates.optDouble(config.getEnvironment(), samplingRate);
            }
            return samplingRate;
        } finally {
            urlConnection.disconnect();
        }
    }

    public int sendTracks(List<HttpRequest> requests, float samplingRate) throws IOException {

        JSONArray batchPayload;

        try {
            batchPayload = builder.createJsonPayload(requests, samplingRate);

            if (config.isDebugEnabled()) {
                String payloadString = batchPayload.toString(2);
                logPayload("Batch\n" + payloadString);
            }

        } catch (JSONException ex) {
            throw new TrackingplanSendException("Send failed", ex);
        }

        sendTracks(batchPayload);
        return batchPayload.length();
    }

    private void sendTracks(JSONArray batch) throws IOException {
        byte[] payload = batch.toString().getBytes(StandardCharsets.UTF_8);

        if (config.isDryRunEnabled()) {
            logger.info("Dry run mode enabled. No tracks sent");
            return;
        }

        sendTracks(payload);
    }

    private void sendTracks(byte[] payload) throws IOException {

        // TODO: Compress rawTracks before sending
        HttpURLConnection conn = makeNewTracksConnection(config.getTracksEndPoint() + config.getTpId());

        logger.verbose("Connecting to endpoint: " + conn.getURL().toString());

        try {
            try (OutputStream out = conn.getOutputStream()) {
                out.write(payload);
            }

            // Read response code explicitly to force the sending of the request.
            // Response is ignored. Tracks endpoint will return 204 if request was
            // parsed correctly. Otherwise it will return a != 204 code.
            int responseCode = conn.getResponseCode();

            logger.verbose("Raw tracks sent. Response code " + responseCode);

        } catch (SocketTimeoutException ex) {
            throw new TrackingplanSendException("Connection to tracks timed out", ex);
        } finally {
            conn.disconnect();
        }
    }

    private HttpURLConnection makeNewTracksConnection(@NonNull String tracksEndPoint) throws IOException {
        URL tracksEndPointURL = new URL(tracksEndPoint);
        HttpURLConnection conn = (HttpURLConnection) tracksEndPointURL.openConnection();
        conn.setConnectTimeout(TRACKS_CONNECT_TIMEOUT);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    private void logPayload(String payloadString) {
        String[] lines = payloadString.split("\\r?\\n");
        for (String line : lines) {
            logger.verbose(line);
        }
    }
}
