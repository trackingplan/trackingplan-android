// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.session;

import android.content.Context;
import android.content.SharedPreferences;

import com.trackingplan.client.sdk.TrackingplanConfig;

final public class SessionDataStorage {

    private static final String SHARED_PREFERENCES_NAME = "Trackingplan";
    private static final long CACHE_LIFE_TIME = 24 * 3600 * 1000;
    private static final String TP_ID_KEY = "tpId";
    private static final String SAMPLING_RATE_KEY = "sampling_rate";
    private static final String TRACKING_ENABLED_KEY = "tracking_enabled";
    private static final String UPDATED_AT_KEY = "updated_at";

    public static SessionData load(String tpId, Context context) {

        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        String cachedTpId = preferences.getString(TP_ID_KEY, "");
        if (!cachedTpId.equals(tpId)) {
            return null;
        }

        long lastUpdateTimeMillis = preferences.getLong(UPDATED_AT_KEY, -1);
        if (lastUpdateTimeMillis == -1 || lastUpdateTimeMillis + CACHE_LIFE_TIME < System.currentTimeMillis()) {
            return null;
        }

        float samplingRate = preferences.getFloat(SAMPLING_RATE_KEY, TrackingplanConfig.SAMPLING_RATE_UNINITIALIZED);
        boolean trackingEnabled = preferences.getBoolean(TRACKING_ENABLED_KEY, false);

        return new SessionData(tpId, samplingRate, trackingEnabled, lastUpdateTimeMillis);
    }

    public static void save(SessionData sessionData, Context context) {

        SharedPreferences preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        editor.putString(TP_ID_KEY, sessionData.getTpId());
        editor.putFloat(SAMPLING_RATE_KEY, sessionData.getSamplingRate());
        editor.putBoolean(TRACKING_ENABLED_KEY, sessionData.isTrackingEnabled());
        editor.putLong(UPDATED_AT_KEY, sessionData.getCreatedAt());

        editor.apply();
    }

    public static boolean hasExpired(final SessionData sessionData) {
        return sessionData == null || sessionData.getCreatedAt() + CACHE_LIFE_TIME < System.currentTimeMillis();
    }

    public static long remainingTimeTillExpiration(final SessionData sessionData) {
        return Math.max(sessionData.getCreatedAt() + SessionDataStorage.CACHE_LIFE_TIME - System.currentTimeMillis(), 0);
    }
}
