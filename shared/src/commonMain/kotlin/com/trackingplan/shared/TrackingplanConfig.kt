// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

/**
 * Immutable shared configuration for Trackingplan SDK.
 * This class contains all common configuration fields shared between platforms.
 *
 * Use [TrackingplanConfigBuilder] to create instances of this class.
 */
@ConsistentCopyVisibility
data class TrackingplanConfig internal constructor(
    val tpId: String,
    val environment: String = "PRODUCTION",
    val sourceAlias: String = defaultSourceAlias(),
    val tags: Map<String, String> = emptyMap(),
    val providerDomains: Map<String, String> = emptyMap(),
    val debug: Boolean = false,
    val testing: Boolean = false,
    val dryRun: Boolean = false,
    val tracksEndpoint: String = DEFAULT_TRACKS_ENDPOINT,
    val configEndpoint: String = DEFAULT_CONFIG_ENDPOINT
) {

    /**
     * Creates a new immutable config with updated tags.
     *
     * @param newTags The tags to add or set.
     * @param replace If true, replaces all existing tags with the new tags.
     *                If false, merges new tags with existing tags (new values overwrite existing ones for same keys).
     * @return A new TrackingplanConfig instance with updated tags
     */
    fun withTags(newTags: Map<String, String>, replace: Boolean = false): TrackingplanConfig {
        val mergedTags = if (replace) {
            newTags
        } else {
            tags + newTags
        }
        return copy(tags = mergedTags)
    }

    /**
     * Generates the sampling rate configuration URL.
     */
    fun sampleRateUrl(): String {
        return "${configEndpoint}config-${tpId}.json"
    }

    companion object {
        const val MAX_REQUEST_BODY_SIZE_IN_BYTES = 100 * 1024
        const val DEFAULT_TRACKS_ENDPOINT = "https://eu-tracks.trackingplan.com/v1/"
        const val DEFAULT_CONFIG_ENDPOINT = "https://config.trackingplan.com/"

        /**
         * Creates an empty/sentinel config. Used for placeholder values.
         */
        // TODO: Find another way to represent empty/sentinel config without bypassing tpId validation
        fun empty(): TrackingplanConfig = TrackingplanConfig(tpId = "")
    }
}

/**
 * Builder for TrackingplanConfig with fluent API.
 * The build() method returns an immutable TrackingplanConfig instance.
 *
 * @throws IllegalArgumentException if tpId is empty when build() is called
 */
class TrackingplanConfigBuilder {

    private var tpId: String = ""
    private var environment: String = "PRODUCTION"
    private var sourceAlias: String = defaultSourceAlias()
    private var tags: Map<String, String> = emptyMap()
    private var providerDomains: Map<String, String> = emptyMap()
    private var debug: Boolean = false
    private var testing: Boolean = false
    private var dryRun: Boolean = false
    private var tracksEndpoint: String = TrackingplanConfig.DEFAULT_TRACKS_ENDPOINT
    private var configEndpoint: String = TrackingplanConfig.DEFAULT_CONFIG_ENDPOINT

    fun tpId(tpId: String) = apply { this.tpId = tpId }
    fun environment(environment: String) = apply { this.environment = environment }
    fun sourceAlias(alias: String) = apply { this.sourceAlias = alias }
    fun tags(tags: Map<String, String>) = apply { this.tags = tags.toMap() }
    fun providerDomains(domains: Map<String, String>) = apply { this.providerDomains = domains.toMap() }
    fun debug(enabled: Boolean) = apply { this.debug = enabled }
    fun testing(enabled: Boolean) = apply { this.testing = enabled }
    fun dryRun(enabled: Boolean) = apply { this.dryRun = enabled }
    fun tracksEndpoint(endpoint: String) = apply {
        this.tracksEndpoint = normalizeEndpoint(endpoint)
    }
    fun configEndpoint(endpoint: String) = apply {
        this.configEndpoint = normalizeEndpoint(endpoint)
    }

    @Throws(IllegalArgumentException::class)
    fun build(): TrackingplanConfig {
        require(tpId.isNotEmpty()) { "Parameter tpId cannot be empty" }
        require(!dryRun || debug) { "DryRun mode requires Debug mode to be enabled" }
        return TrackingplanConfig(
            tpId = tpId,
            environment = environment,
            sourceAlias = sourceAlias,
            tags = tags,
            providerDomains = providerDomains,
            debug = debug,
            testing = testing,
            dryRun = dryRun,
            tracksEndpoint = tracksEndpoint,
            configEndpoint = configEndpoint
        )
    }

    private fun normalizeEndpoint(endpoint: String): String {
        return if (endpoint.endsWith("/")) endpoint else "$endpoint/"
    }
}

/**
 * Platform-specific default source alias.
 */
expect fun defaultSourceAlias(): String
