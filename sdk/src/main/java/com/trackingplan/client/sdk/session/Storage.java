// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.session;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.util.ServiceLocator;
import com.trackingplan.client.sdk.util.Time;

import java.util.Objects;

final public class Storage {

    private static final String SHARED_PREFERENCES_NAME = "Trackingplan";
    private static final String TP_ID_KEY = "tpId";
    private static final String TP_ENVIRONMENT_KEY = "environment";
    private static final String SESSION_ID_KEY = "session_id";
    private static final String SESSION_STARTED_AT_KEY = "session_started_at";
    private static final String SESSION_LAST_ACTIVITY_TIME_KEY = "last_activity_time";
    private static final String SESSION_SAMPLING_RATE_KEY = "session_sampling_rate";
    private static final String SESSION_TRACKING_ENABLED_KEY = "session_tracking_enabled";
    private static final String SAMPLING_RATE_KEY = "sampling_rate";
    private static final String SAMPLING_RATE_UPDATED_AT_KEY = "updated_at";
    private static final String TRACKING_ENABLED_KEY = "tracking_enabled";
    private static final String FIRST_TIME_EXECUTED_AT_KEY = "first_time_executed_at";
    private static final String LAST_DAU_EVENT_SENT_AT_KEY = "last_dau_event_sent_at";

    private final SharedPreferences preferences;

    public Storage(@NonNull String tpId, @NonNull String environment, @NonNull final Context context) {
        this.preferences = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        if (!isSameTpIdAndEnvironment(tpId, environment)) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.putString(TP_ID_KEY, tpId);
            editor.putString(TP_ENVIRONMENT_KEY, environment);
            editor.apply();
        }
    }

    @NonNull
    public TrackingplanSession loadSession() {
        String sessionId = preferences.getString(SESSION_ID_KEY, "");
        float samplingRate = preferences.getFloat(SESSION_SAMPLING_RATE_KEY, -1);
        long sessionStartedTimeMillis = preferences.getLong(SESSION_STARTED_AT_KEY, -1);
        long sessionLastActivityTimeMillis = preferences.getLong(SESSION_LAST_ACTIVITY_TIME_KEY, -1);
        boolean trackingEnabled = preferences.getBoolean(SESSION_TRACKING_ENABLED_KEY, false);

        if (sessionId == null || sessionId.isEmpty() || samplingRate == -1
                || sessionStartedTimeMillis == -1 || sessionLastActivityTimeMillis == -1) {
            return TrackingplanSession.EMPTY;
        }

        return new TrackingplanSession(
                sessionId,
                samplingRate,
                trackingEnabled,
                sessionStartedTimeMillis,
                sessionLastActivityTimeMillis
        );
    }

    public void saveSession(final TrackingplanSession session) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(SESSION_ID_KEY, session.getSessionId());
        editor.putFloat(SESSION_SAMPLING_RATE_KEY, session.getSamplingRate());
        editor.putBoolean(SESSION_TRACKING_ENABLED_KEY, session.isTrackingEnabled());
        editor.putLong(SESSION_STARTED_AT_KEY, session.getCreatedAt());
        editor.putLong(SESSION_LAST_ACTIVITY_TIME_KEY, session.getLastActivityTime());
        editor.apply();
    }

    @NonNull
    public SamplingRate loadSamplingRate() {
        float samplingRate = preferences.getFloat(SAMPLING_RATE_KEY, -1);
        boolean trackingEnabled = preferences.getBoolean(TRACKING_ENABLED_KEY, false);
        long downloadedAtTimeMs = preferences.getLong(SAMPLING_RATE_UPDATED_AT_KEY, -1);

        if (samplingRate == -1 || downloadedAtTimeMs == -1) {
            return SamplingRate.EMPTY;
        }

        return new SamplingRate(samplingRate, downloadedAtTimeMs, trackingEnabled);
    }

    public void saveSamplingRate(@NonNull final SamplingRate samplingRate) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putFloat(SAMPLING_RATE_KEY, samplingRate.getValue());
        editor.putBoolean(TRACKING_ENABLED_KEY, samplingRate.isTrackingEnabled());
        editor.putLong(SAMPLING_RATE_UPDATED_AT_KEY, samplingRate.getDownloadedAt());
        editor.apply();
    }

    public boolean isFirstTimeExecution() {
        long firstTimeExecutedAt = preferences.getLong(FIRST_TIME_EXECUTED_AT_KEY, -1);
        return firstTimeExecutedAt == -1;
    }

    public void saveFirstTimeExecution() {
        SharedPreferences.Editor editor = preferences.edit();
        final var time = ServiceLocator.getSharedInstance(Time.class);
        editor.putLong(FIRST_TIME_EXECUTED_AT_KEY, time.currentTimeMillis());
        editor.apply();
    }

    public boolean wasLastDauSent24hAgo() {
        final var time = ServiceLocator.getSharedInstance(Time.class);
        long lastDauEventSentAt = preferences.getLong(LAST_DAU_EVENT_SENT_AT_KEY, -1);
        return lastDauEventSentAt == -1 || lastDauEventSentAt + 24 * Time.HOUR < time.currentTimeMillis();
    }

    public void saveLastDauEventSentTime() {
        SharedPreferences.Editor editor = preferences.edit();
        final var time = ServiceLocator.getSharedInstance(Time.class);
        editor.putLong(LAST_DAU_EVENT_SENT_AT_KEY, time.currentTimeMillis());
        editor.apply();
    }

    public void clear() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();
    }

    private boolean isSameTpIdAndEnvironment(@NonNull final String tpId, @NonNull final String environment) {
        String cachedTpId = preferences.getString(TP_ID_KEY, "");
        String cachedEnvironment = preferences.getString(TP_ENVIRONMENT_KEY, "");
        return Objects.equals(cachedTpId, tpId) && Objects.equals(cachedEnvironment, environment);
    }
}
