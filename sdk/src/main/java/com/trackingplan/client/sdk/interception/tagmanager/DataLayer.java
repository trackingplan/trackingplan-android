package com.trackingplan.client.sdk.interception.tagmanager;

import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.TrackingplanInstance;
import com.trackingplan.client.sdk.interception.InstrumentRequestBuilder;
import com.trackingplan.client.sdk.util.AndroidLogger;
import com.trackingplan.client.sdk.util.JSONUtils;

import java.util.Map;

final public class DataLayer {

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    private static boolean analyticsEnabled = true;

    @Keep
    public static void push(com.google.android.gms.tagmanager.DataLayer dl, @NonNull Map<String, Object> map) {
        dl.push(map);

        // Ignore empty maps
        if (map.size() == 0) {
            return;
        }

        var methodParams = new Bundle();
        methodParams.putBundle("map", getBundleFromMap(map));
        interceptMethodCall(dl, "push", methodParams);
    }

    private static void interceptMethodCall(@NonNull com.google.android.gms.tagmanager.DataLayer dl,
                                            @NonNull String methodName,
                                            @NonNull Bundle params) {

        // TODO: Read enabled/disabled state from AndroidManifest as well
        if (!analyticsEnabled) {
            return;
        }

        try {
            InstrumentRequestBuilder builder = makeBuilder(dl);
            builder.setHttpMethod("POST");
            byte[] payload = JSONUtils.encodeJsonPayload(JSONUtils.createPayload(methodName, params, 1));
            builder.setRequestPayload(payload);

            builder.setRequestPayloadNumBytes(payload.length);
            builder.build();
        } catch (Exception e) {
            // Silent exceptions. No error should compromise host app
        }
    }

    private static InstrumentRequestBuilder makeBuilder(@NonNull com.google.android.gms.tagmanager.DataLayer dl) {
        InstrumentRequestBuilder builder = new DataLayerInstrumentRequestBuilder(dl, TrackingplanInstance.getInstance());
        builder.setUrl("code://com.google.android.gms.tagmanager.DataLayer");
        return builder;
    }

    private static Bundle getBundleFromMap(@NonNull Map<String, Object> data) {
        Bundle bundle = new Bundle();

        for (Map.Entry<String, Object> entry : data.entrySet()) {

            var key = entry.getKey();
            var value = entry.getValue();

            try {
                if (value instanceof Map) {
                    Map map = (Map) value;
                    bundle.putBundle(key, getBundleFromMap(map));
                } else if (value != null) {
                    bundle.putString(key, value.toString());
                } else {
                    bundle.putString(key, "null");
                }
            } catch (Exception ex) {
                logger.warn("Failed to convert map to bundle: " + ex.getMessage());
            }
        }

        return bundle;
    }
}
