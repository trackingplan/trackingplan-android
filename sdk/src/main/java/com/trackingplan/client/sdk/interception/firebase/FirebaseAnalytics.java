// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.interception.firebase;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import com.trackingplan.client.sdk.TrackingplanInstance;
import com.trackingplan.client.sdk.interception.InstrumentRequestBuilder;
import com.trackingplan.client.sdk.util.AndroidLog;
import com.trackingplan.client.sdk.util.JSONUtils;

final public class FirebaseAnalytics {

    private static final AndroidLog logger = AndroidLog.getInstance();

    private static boolean analyticsEnabled = true;

    @Keep
    public static void logEvent(@NonNull com.google.firebase.analytics.FirebaseAnalytics fa, @NonNull String name, @NonNull Bundle params) {
        fa.logEvent(name, params);
        var methodParams = new Bundle();
        methodParams.putString("name", name);
        methodParams.putBundle("params", params);
        interceptMethodCall(fa, "logEvent", methodParams);
    }

    @Keep
    public static void setDefaultEventParameters(@NonNull com.google.firebase.analytics.FirebaseAnalytics fa, @Nullable Bundle parameters) {
        fa.setDefaultEventParameters(parameters);
        var methodParams = new Bundle();
        methodParams.putBundle("parameters", parameters);
        interceptMethodCall(fa, "setDefaultEventParameters", methodParams);
    }

    @MainThread
    @Keep
    public static void setCurrentScreen(@NonNull com.google.firebase.analytics.FirebaseAnalytics fa, @NonNull Activity activity, @Nullable @Size(min = 1L, max = 36L) String screenName, @Nullable @Size(min = 1L, max = 36L) String screenClassOverride) {
        fa.setCurrentScreen(activity, screenName, screenClassOverride);
        var methodParams = new Bundle();
        methodParams.putString("activity", activity.getLocalClassName());
        methodParams.putString("screenName", screenName);
        methodParams.putString("screenClassOverride", screenClassOverride);
        interceptMethodCall(fa, "setCurrentScreen", methodParams);
    }

    @Keep
    public static void setUserId(@NonNull com.google.firebase.analytics.FirebaseAnalytics fa, @NonNull String id) {
        fa.setUserId(id);
        var methodParams = new Bundle();
        methodParams.putString("id", id);
        interceptMethodCall(fa, "setUserId", methodParams);
    }

    @Keep
    public static void setUserProperty(@NonNull com.google.firebase.analytics.FirebaseAnalytics fa, @NonNull String name, String value) {
        fa.setUserProperty(name, value);
        var methodParams = new Bundle();
        methodParams.putString("name", name);
        methodParams.putString("value", value);
        interceptMethodCall(fa, "setUserProperty", methodParams);
    }

    @Keep
    public static void setAnalyticsCollectionEnabled(@NonNull com.google.firebase.analytics.FirebaseAnalytics fa, boolean enabled) {
        analyticsEnabled = enabled;
        fa.setAnalyticsCollectionEnabled(enabled);
        logger.debug(String.format("FirebaseAnalytics is %s", enabled ? "enabled" : "disabled"));
    }

    private static void interceptMethodCall(@NonNull com.google.firebase.analytics.FirebaseAnalytics fa,
                                            @NonNull String methodName,
                                            @NonNull Bundle params) {

        // TODO: Read enabled/disabled state from AndroidManifest as well
        if (!analyticsEnabled) {
            return;
        }

        try {
            InstrumentRequestBuilder builder = makeBuilder(fa);
            builder.setHttpMethod("POST");
            byte[] payload = JSONUtils.encodeJsonPayload(JSONUtils.createPayload(methodName, params, 2));
            builder.setRequestPayload(payload);

            builder.setRequestPayloadNumBytes(payload.length);
            builder.build();
        } catch (Exception e) {
            // Silent exceptions. No error should compromise host app
        }
    }

    private static InstrumentRequestBuilder makeBuilder(@NonNull com.google.firebase.analytics.FirebaseAnalytics fa) {
        InstrumentRequestBuilder builder = new FirebaseInstrumentRequestBuilder(fa, TrackingplanInstance.getInstance());
        builder.setUrl("code://com.google.firebase.analytics.FirebaseAnalytics");
        return builder;
    }
}
