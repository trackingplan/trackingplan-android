package com.trackingplan.client.sdk.interception;

import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.trackingplan.client.sdk.TrackingplanInstance;
import com.trackingplan.client.sdk.util.JSONUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

final public class FirebaseAnalytics {

    private static Bundle defaultEventParameters;
    private static String userId;

    @Keep
    public static void logEvent(@NonNull com.google.firebase.analytics.FirebaseAnalytics fa, @NonNull String name, @NonNull Bundle params) {

        // TODO: Take user consent into account
        fa.logEvent(name, params);

        try {
            InstrumentRequestBuilder builder = makeBuilder(fa);
            builder.setHttpMethod("logEvent");
            byte[] payload = encodeJsonPayload(createPayload(name, params));
            builder.setRequestPayload(payload);
            builder.setRequestPayloadNumBytes(payload.length);
            builder.build();
        } catch (Exception e) {
            // Silent exceptions. No error should compromise host app
        }
    }

    @Keep
    public static void setDefaultEventParameters(@NonNull com.google.firebase.analytics.FirebaseAnalytics fa, @Nullable Bundle parameters) {
        defaultEventParameters = parameters;
        fa.setDefaultEventParameters(parameters);
    }

    @Keep
    public static void setUserProperty(@NonNull com.google.firebase.analytics.FirebaseAnalytics fa, @NonNull String name, String value) {
        // TODO: Intercept method setUserProperty
        fa.setUserProperty(name, value);
    }

    @Keep
    public static void setUserId(@NonNull com.google.firebase.analytics.FirebaseAnalytics fa, @NonNull String id) {
        // TODO: Intercept method setUserId
        userId = id;
        fa.setUserId(id);
    }

    private static InstrumentRequestBuilder makeBuilder(@NonNull com.google.firebase.analytics.FirebaseAnalytics fa) {
        InstrumentRequestBuilder builder = new FirebaseInstrumentRequestBuilder(fa, TrackingplanInstance.getInstance());
        builder.setUrl("code://com.google.firebase.analytics.FirebaseAnalytics");
        return builder;
    }

    private static byte[] encodeJsonPayload(JSONObject payload) {
        return payload.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static JSONObject createPayload(String name, Bundle bundle) throws JSONException {

        JSONObject params = JSONUtils.makeJSONObject(bundle);
        if (defaultEventParameters != null) {
            JSONObject defaultParams = JSONUtils.makeJSONObject(defaultEventParameters);
            params = JSONUtils.assign(defaultParams, params);
        }

        JSONObject payload = new JSONObject();
        payload.put("name", name);
        payload.put("ts_millis", System.currentTimeMillis());
        payload.put("params", params);

        return payload;
    }
}
