// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.delivery;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Base64;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.BuildConfig;
import com.trackingplan.client.sdk.TrackingplanConfig;
import com.trackingplan.client.sdk.interception.HttpRequest;
import com.trackingplan.client.sdk.util.AndroidLog;
import com.trackingplan.shared.TrackingplanSession;
import com.trackingplan.shared.adaptive.SamplingResult;
import com.trackingplan.client.sdk.util.StringUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

final public class TrackBuilder {

    private static final String HEADER_CONTENT_ENCODING = "content-encoding";
    private static final String HEADER_CONTENT_TYPE = "content-type";

    private static final AndroidLog logger = AndroidLog.getInstance();

    private final TrackingplanConfig config;

    public TrackBuilder(@NonNull TrackingplanConfig config, @NonNull final Context context) {
        this.config = config;
    }

    public JSONArray createJsonPayload(List<HttpRequest> requests, @NonNull final TrackingplanSession session) throws JSONException {

        JSONArray payload = new JSONArray();

        for (HttpRequest request : requests) {
            try {
                payload.put(createRawTrack(request, session));
            } catch (JSONException e) {
                logger.error("Cannot convert request to raw track: " + e.getMessage());
                logger.debug("Request information: " + request);
            }
        }

        if (payload.length() == 0) {
            throw new JSONException("JSON conversion failed");
        }

        return payload;
    }

    private JSONObject createRawTrack(HttpRequest request, @NonNull final TrackingplanSession session) throws JSONException {

        var rawTrack = new JSONObject();

        rawTrack.put("tp_id", config.getTpId());
        rawTrack.put("environment", config.getEnvironment());
        rawTrack.put("provider", request.getProvider());
        rawTrack.put("ts", request.getCreatedTimeMs());

        var requestJson = new JSONObject();
        rawTrack.put("request", requestJson);
        requestJson.put("endpoint", request.getUrl());
        requestJson.put("method", request.getMethod());
        parsePayload(request, requestJson);

        if (request.getResponseCode() != -1) {
            requestJson.put("response_code", request.getResponseCode());
        }

        rawTrack.put("source_alias", config.getSourceAlias());

        if (!config.tags().isEmpty()) {
            rawTrack.put("tags", getTagsAsJson(config.tags()));
        }

        var context = new JSONObject();
        rawTrack.put("context", context);

        // Context established by unit tests
        for (var entry : config.customContext().entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }

        for (var entry : request.getContext().entrySet()) {
            context.put(entry.getKey(), entry.getValue());
        }

        SamplingResult.Include samplingResult = request.getSamplingResult();
        if (samplingResult != null) {
            rawTrack.put("sampling_rate", samplingResult.getEffectiveSampleRate());
            rawTrack.put("sampling_mode", samplingResult.getSamplingMode().getValue());
        }
        // Note: If samplingResult is null, sampling_rate is intentionally omitted to signal
        // a bug in the SDK. Ingest will log this case and use a default value of 1.
        rawTrack.put("session_id", session.getSessionId());
        rawTrack.put("sdk", "android");
        rawTrack.put("sdk_version", BuildConfig.SDK_VERSION);

        return rawTrack;
    }

    private static JSONObject getTagsAsJson(Map<String, String> tags) throws JSONException {
        var tagsJson = new JSONObject();

        for (var tag : tags.entrySet()) {
            tagsJson.put(tag.getKey(), tag.getValue());
        }

        return tagsJson;
    }

    private void parsePayload(HttpRequest request, JSONObject requestJson) throws JSONException {

        byte[] payload = request.getPayloadData();

        if (payload.length == 0) {
            requestJson.put("post_payload", JSONObject.NULL);
            return;
        }

        var contentEncoding = request.getHeaders().get(HEADER_CONTENT_ENCODING);
        var contentType = request.getHeaders().get(HEADER_CONTENT_TYPE);

        if (!StringUtils.isEmpty(contentEncoding) || isGzipCompressed(payload)) {
            requestJson.put("post_payload", bytesTob64(payload));
            requestJson.put("post_payload_type", "gzip_base64");
        } else if ("application/octet-stream".equals(contentType)) {
            requestJson.put("post_payload", bytesTob64(payload));
            requestJson.put("post_payload_type", "base64");
        } else {
            requestJson.put("post_payload", bytesToUtf8(payload));
        }
    }

    private String bytesTob64(byte[] bytes) {
        byte[] encoded = Base64.encode(bytes, Base64.DEFAULT | Base64.NO_WRAP);
        return new String(encoded);
    }

    private String bytesToUtf8(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private boolean isGzipCompressed(byte[] bytes) {
        if ((bytes == null) || (bytes.length < 2)) {
            return false;
        } else {
            return ((bytes[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (bytes[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8)));
        }
    }
}
