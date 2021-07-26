// MIT License
//
// Copyright (c) 2021 Trackingplan
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package com.trackingplan.client.sdk;

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

    public static final String TRACKS_END_POINT = "https://tracks.trackingplan.com/v1/";
    private static final String CONFIG_END_POINT = "https://config.trackingplan.com/";
    private static final int TRACKS_CONNECT_TIMEOUT = 30 * 1000;

    private final TrackingplanConfig config;
    private final TrackBuilder builder;

    public TrackingplanClient(@NonNull TrackingplanConfig config) {
        this.config = config;
        this.builder = new TrackBuilder(config);
    }

    public float getSamplingRate() throws IOException, JSONException {

        URL url = new URL(CONFIG_END_POINT + "config-" + config.getTpId() + ".json");
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        try (InputStream in = new BufferedInputStream(urlConnection.getInputStream())) {
            String rawConfig = StreamUtils.convertInputStreamToString(in);
            JSONObject jsonObject = new JSONObject(rawConfig);
            return (float) jsonObject.getDouble("sample_rate");
        } finally {
            urlConnection.disconnect();
        }
    }

    public int sendTracks(List<HttpRequest> requests, float samplingRate) throws IOException {

        JSONArray batchPayload;

        try {
            batchPayload = builder.createJsonPayload(requests, samplingRate);
        } catch (JSONException ex) {
            throw new TrackingplanSendException("Send failed", ex);
        }

        sendTracks(batchPayload);
        return batchPayload.length();
    }

    private void sendTracks(JSONArray batch) throws IOException {
        byte[] payload = batch.toString().getBytes(StandardCharsets.UTF_8);
        sendTracks(payload);
    }

    private void sendTracks(byte[] payload) throws IOException {

        HttpURLConnection conn = makeNewTracksConnection();

        try {
            try (OutputStream out = conn.getOutputStream()) {
                out.write(payload);
            }

            // Read response code explicitly to force the sending of the request.
            // Response is ignored. Tracks endpoint will return 204 if request was
            // parsed correctly. Otherwise it will return a != 204 code.
            int responseCode = conn.getResponseCode();

            logger.verbose("Client: Raw tracks sent. Response code " + responseCode);

        } catch (SocketTimeoutException ex) {
            throw new TrackingplanSendException("Connection to tracks timed out", ex);
        } finally {
            conn.disconnect();
        }
    }

    private HttpURLConnection makeNewTracksConnection() throws IOException {
        URL tracksEndPoint = new URL(TRACKS_END_POINT);
        HttpURLConnection conn = (HttpURLConnection) tracksEndPoint.openConnection();
        conn.setConnectTimeout(TRACKS_CONNECT_TIMEOUT);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }
}
