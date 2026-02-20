// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import kotlinx.serialization.json.Json

/**
 * Parser for TrackingplanIngestConfig using kotlinx.serialization.
 * Provides a common implementation across all platforms.
 */
object TrackingplanIngestConfigParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parses a TrackingplanIngestConfig from a JSON string.
     *
     * @param jsonString The JSON configuration string
     * @return Parsed TrackingplanIngestConfig
     * @throws Exception if parsing fails
     */
    @Throws(Exception::class)
    fun parse(jsonString: String): TrackingplanIngestConfig {
        return json.decodeFromString<TrackingplanIngestConfig>(jsonString)
    }
}
