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

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.BuildConfig;
import com.trackingplan.client.sdk.TrackingplanConfig;
import com.trackingplan.client.sdk.interception.HttpRequest;
import com.trackingplan.client.sdk.util.AndroidLogger;
import com.trackingplan.client.sdk.util.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

final public class TrackBuilder {

    private static final String HEADER_CONTENT_TYPE = "content-type";
    private static final String HEADER_CONTENT_ENCODING = "content-encoding";

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

        JSONObject rawTrack = new JSONObject();

        rawTrack.put("provider", request.getProvider());

        JSONObject requestJson = new JSONObject();
        rawTrack.put("request", requestJson);
        requestJson.put("endpoint", request.getUrl());
        requestJson.put("method", request.getMethod());
        parsePayload(request, requestJson);

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

    private void parsePayload(HttpRequest request, JSONObject requestJson) throws JSONException {

        byte[] payload = request.getPayloadData();

        if (payload.length == 0) {
            requestJson.put("post_payload", null);
            return;
        }

        String contentType = request.getHeaders().get(HEADER_CONTENT_TYPE);
        String contentEncoding = request.getHeaders().get(HEADER_CONTENT_ENCODING);

        if (!StringUtils.isEmpty(contentEncoding) || isGzipCompressed(payload)) {
            requestJson.put("post_payload", bytesTob64(payload));
            requestJson.put("post_payload_encoding", StringUtils.getNonNullOrDefault(contentEncoding, "gzip") + ", b64");
        } else {

            String payloadStr = bytesToUtf8(payload);

            if ("application/json".equals(contentType)) {
                try {
                    requestJson.put("post_payload", new JSONObject(payloadStr));
                } catch (JSONException ex) {
                    requestJson.put("post_payload", payloadStr);
                }
            } else {
                requestJson.put("post_payload", payloadStr);
            }
        }

        requestJson.put("post_payload_type", StringUtils.getNonNullOrDefault(contentType, "application/octet-stream"));
    }

    private String bytesTob64(byte[] bytes) {
        byte[] encoded = Base64.encode(bytes, Base64.DEFAULT | Base64.NO_WRAP);
        return new String(encoded);
    }

    private String bytesToUtf8(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private boolean isGzipCompressed(byte[] bytes)
    {
        if ((bytes == null) || (bytes.length < 2)) {
            return false;
        } else {
            return ((bytes[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8)));
        }
    }
}
