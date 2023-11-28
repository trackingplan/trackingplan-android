// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.util;

import android.os.Bundle;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class JSONUtils {

    public static JSONObject createPayload(String name, Bundle bundle, int version) throws JSONException {
        JSONObject params = makeJSONObject(bundle);
        JSONObject payload = new JSONObject();
        payload.put("version", version);
        payload.put("method", name);
        payload.put("params", params);
        return payload;
    }

    public static byte[] encodeJsonPayload(@NonNull JSONObject payload) {
        return payload.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static JSONArray makeJSONArray(@NonNull Object[] array) throws JSONException {

        JSONArray jsonArray = new JSONArray();

        for (Object item : array) {
            if (item == null) {
                jsonArray.put(JSONObject.NULL);
            } else if (item instanceof Bundle) {
                jsonArray.put(makeJSONObject((Bundle) item));
            } else if (item.getClass().isArray()) {
                jsonArray.put(makeJSONArray((Object[]) item));
            } else if (item instanceof Iterable) {
                jsonArray.put(makeJSONArray((Iterable) item));
            } else {
                jsonArray.put(item);
            }
        }

        return jsonArray;
    }

    public static JSONArray makeJSONArray(@NonNull Iterable iterator) throws JSONException {

        JSONArray jsonArray = new JSONArray();

        for (Object item : iterator) {
            if (item == null) {
                jsonArray.put(JSONObject.NULL);
            } else if (item instanceof Bundle) {
                jsonArray.put(makeJSONObject((Bundle) item));
            } else if (item.getClass().isArray()) {
                jsonArray.put(makeJSONArray((Object[]) item));
            } else if (item instanceof Iterable) {
                jsonArray.put(makeJSONArray((Iterable) item));
            } else {
                jsonArray.put(item);
            }
        }

        return jsonArray;
    }

    public static JSONObject makeJSONObject(@NonNull Bundle bundle) throws JSONException {

        JSONObject jsonObject = new JSONObject();

        for (String key : bundle.keySet()) {

            Object value = bundle.get(key);

            if (value == null) {
                jsonObject.put(key, JSONObject.NULL);
            } else if (value instanceof Bundle) {
                jsonObject.put(key, makeJSONObject((Bundle) value));
            } else if (value.getClass().isArray()) {
                jsonObject.put(key, makeJSONArray((Object[]) value));
            } else if (value instanceof Iterable) {
                jsonObject.put(key, makeJSONArray((Iterable) value));
            } else {
                jsonObject.put(key, value);
            }
        }

        return jsonObject;
    }

    public static JSONObject assign(@NonNull JSONObject target, @NonNull JSONObject source) throws JSONException {
        for (Iterator<String> it = source.keys(); it.hasNext(); ) {
            String key = it.next();
            target.put(key, source.get(key));
        }
        return target;
    }
}
