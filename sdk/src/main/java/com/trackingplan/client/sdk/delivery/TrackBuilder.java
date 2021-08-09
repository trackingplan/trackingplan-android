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
package com.trackingplan.client.sdk.delivery;

import android.os.Build;
import android.util.Base64;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.BuildConfig;
import com.trackingplan.client.sdk.TrackingplanConfig;
import com.trackingplan.client.sdk.interception.HttpRequest;
import com.trackingplan.client.sdk.util.AndroidLogger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

final public class TrackBuilder {

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    private final TrackingplanConfig config;

    public TrackBuilder(@NonNull TrackingplanConfig config) {
        this.config = config;
    }

    public JSONArray createJsonPayload(List<HttpRequest> requests, float samplingRate) throws JSONException {

        JSONArray payload = new JSONArray();

        for (HttpRequest request : requests) {
            try {
                payload.put(createRawTrack(request, samplingRate));
            } catch (JSONException e) {
                logger.warn("Cannot convert request to raw track: " + e.getMessage());
                logger.info("Request information: " + request.toString());
            }
        }

        if (payload.length() == 0) {
            throw new JSONException("JSON conversion failed");
        }

        return payload;
    }

    private JSONObject createRawTrack(HttpRequest request, float samplingRate) throws JSONException {

        String device = Build.MANUFACTURER + " " + Build.MODEL;
        String platform = "Android " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";

        Pair<String, String> parsedPayload = parsePayload(request.getPayloadData());
        String payloadString = parsedPayload.first;
        String payloadType = parsedPayload.second;

        JSONObject rawTrack = new JSONObject();

        rawTrack.put("provider", request.getProvider());

        JSONObject requestJson = new JSONObject();
        rawTrack.put("request", requestJson);
        requestJson.put("endpoint", request.getUrl());
        requestJson.put("method", request.getMethod());
        requestJson.put("post_payload", payloadString);
        if (payloadString != null) {
            requestJson.put("post_payload_type", payloadType);
        }

        requestJson.put("response_code", request.getResponseCode());

        JSONObject context = new JSONObject();

        if (!config.ignoreContext()) {
            // TODO: App name
            // TODO: App version
            // TODO: App instance id
            context.put("device", device);
            context.put("platform", platform);
            for (Map.Entry<String, String> entry : request.getContext().entrySet()) {
                context.put(entry.getKey(), entry.getValue());
            }
        }

        rawTrack.put("context", context);

        rawTrack.put("tp_id", config.getTpId());
        rawTrack.put("source_alias", config.getSourceAlias());
        rawTrack.put("environment", config.getEnvironment());
        rawTrack.put("sdk", "android");
        rawTrack.put("sdk_version", BuildConfig.SDK_VERSION);

        rawTrack.put("sampling_rate", samplingRate);
        rawTrack.put("debug", false);

        return rawTrack;
    }

    private Pair<String, String> parsePayload(byte[] payload) {

        if (payload.length == 0) {
            return new Pair<>(null, "string");
        }

        String payloadStr;
        String type;

        try {
            payloadStr = new String(payload, StandardCharsets.UTF_8);
            type = "string";
        } catch (Exception e) {
            payloadStr = bytesTob64(payload).trim();
            type = "base64";
        }

        return new Pair<>(payloadStr, type);
    }

    private String bytesTob64(byte[] bytes) {
        byte[] encoded = Base64.encode(bytes, Base64.DEFAULT);
        return new String(encoded);
    }
}
