// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val DEFAULT_STORE_NAME = "com.trackingplan.sdk"

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

/**
 * Shared storage for Trackingplan SDK data.
 * Uses platform-specific KeyValueStore for persistence.
 *
 * Use [Storage.create] to instantiate.
 */
class Storage private constructor(
    private val store: KeyValueStore,
    val ingestConfigCache: IngestConfigCache,
    tpId: String,
    environment: String
) {
    init {
        val cachedTpId = store.getString(Keys.TP_ID, null)
        val cachedEnvironment = store.getString(Keys.ENVIRONMENT, null)

        if (cachedTpId != tpId) {
            // tpId changed: clear everything including config cache and timestamps
            store.clear()
            CacheTimestampHelper.clearAll()
            ingestConfigCache.clear()
        } else if (cachedEnvironment != environment) {
            // Only environment changed: clear session data but KEEP config cache
            // Config is unique per tpId (server returns environment_rates map for all environments)
            // Cache timestamps are in separate store, so they're NOT affected by store.clear()
            store.clear()
        }

        store.setString(Keys.TP_ID, tpId)
        store.setString(Keys.ENVIRONMENT, environment)
    }

    companion object {
        /**
         * Creates a Storage instance.
         * @param tpId The Trackingplan ID
         * @param environment The environment name
         * @throws Exception if the underlying KeyValueStore cannot be created
         */
        @Throws(Exception::class)
        fun create(tpId: String, environment: String): Storage {
            val store = KeyValueStore.create(DEFAULT_STORE_NAME)
            val cache = IngestConfigCache(CacheStorage(), tpId)
            return Storage(store, cache, tpId, environment)
        }
    }

    // Session

    fun loadSession(): TrackingplanSession {
        val sessionId = store.getString(Keys.SESSION_ID, null)
        val samplingRate = store.getInt(Keys.SESSION_SAMPLING_RATE, -1)
        val createdAt = store.getLong(Keys.SESSION_STARTED_AT, -1)
        val lastActivityTime = store.getLong(Keys.SESSION_LAST_ACTIVITY_TIME, -1)
        val trackingEnabled = store.getBoolean(Keys.SESSION_TRACKING_ENABLED, false)

        if (sessionId.isNullOrEmpty() || samplingRate == -1 ||
            createdAt == -1L || lastActivityTime == -1L
        ) {
            return TrackingplanSession.EMPTY
        }

        // Load sampling options (defaults to empty for backward compatibility)
        val samplingOptionsJson = store.getString(Keys.SESSION_SAMPLING_OPTIONS, null)
        val samplingOptions = if (samplingOptionsJson.isNullOrEmpty()) {
            SamplingOptions()
        } else {
            try {
                json.decodeFromString<SamplingOptions>(samplingOptionsJson)
            } catch (e: Exception) {
                SamplingOptions()
            }
        }

        return TrackingplanSession.fromStorage(
            sessionId = sessionId,
            samplingRate = samplingRate,
            trackingEnabled = trackingEnabled,
            createdAt = createdAt,
            lastActivityTime = lastActivityTime,
            samplingOptions = samplingOptions
        )
    }

    fun saveSession(session: TrackingplanSession) {
        store.setString(Keys.SESSION_ID, session.sessionId)
        store.setInt(Keys.SESSION_SAMPLING_RATE, session.samplingRate)
        store.setBoolean(Keys.SESSION_TRACKING_ENABLED, session.trackingEnabled)
        store.setLong(Keys.SESSION_STARTED_AT, session.createdAt)
        store.setLong(Keys.SESSION_LAST_ACTIVITY_TIME, session.lastActivityTime)
        store.setString(Keys.SESSION_SAMPLING_OPTIONS, json.encodeToString(session.samplingOptions))
    }

    // Tracking enabled (persisted separately from cached config)

    fun loadTrackingEnabled(): Boolean {
        return store.getBoolean(Keys.TRACKING_ENABLED, false)
    }

    fun saveTrackingEnabled(enabled: Boolean) {
        store.setBoolean(Keys.TRACKING_ENABLED, enabled)
    }

    // First time execution

    fun isFirstTimeExecution(): Boolean {
        return !store.contains(Keys.FIRST_TIME_EXECUTION_TIMESTAMP)
    }

    fun saveFirstTimeExecution(timestamp: Long) {
        store.setLong(Keys.FIRST_TIME_EXECUTION_TIMESTAMP, timestamp)
    }

    fun saveFirstTimeExecutionNow() {
        saveFirstTimeExecution(ServiceLocator.getTimeProvider().currentTimeMillis())
    }

    // DAU

    fun wasLastDauSent24hAgo(): Boolean {
        val lastDauEventSentAt = store.getLong(Keys.LAST_DAU_EVENT_SENT_TIMESTAMP, -1)
        return lastDauEventSentAt == -1L ||
                lastDauEventSentAt + 24 * TimeProvider.HOUR < ServiceLocator.getTimeProvider().currentTimeMillis()
    }

    fun saveLastDauEventSentTimestamp(timestamp: Long) {
        store.setLong(Keys.LAST_DAU_EVENT_SENT_TIMESTAMP, timestamp)
    }

    fun saveLastDauEventSentTimeNow() {
        saveLastDauEventSentTimestamp(ServiceLocator.getTimeProvider().currentTimeMillis())
    }

    // Other

    fun clear() {
        store.clear()
        CacheTimestampHelper.clearAll()
        ingestConfigCache.clear()
    }

    private object Keys {
        const val TP_ID = "tp_id"
        const val ENVIRONMENT = "environment"
        const val SESSION_ID = "session_id"
        const val SESSION_STARTED_AT = "session_started_at"
        const val SESSION_LAST_ACTIVITY_TIME = "last_activity_time"
        const val SESSION_SAMPLING_RATE = "session_sampling_rate"
        const val SESSION_TRACKING_ENABLED = "session_tracking_enabled"
        const val SESSION_SAMPLING_OPTIONS = "session_sampling_options"
        const val TRACKING_ENABLED = "tracking_enabled"
        const val FIRST_TIME_EXECUTION_TIMESTAMP = "first_time_executed_at"
        const val LAST_DAU_EVENT_SENT_TIMESTAMP = "last_dau_event_sent_at"
    }
}
