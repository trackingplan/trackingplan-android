// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared.adaptive

import com.trackingplan.shared.urlDecode

import kotlinx.serialization.json.*

/**
 * Context data extracted from request for special key matching operations.
 *
 * @property path URL path only (e.g., "/v1/batch")
 * @property endpoint Full endpoint URL (truncated to MAX_CONTEXT_LENGTH for performance)
 * @property payload Payload string (truncated to MAX_CONTEXT_LENGTH for performance)
 */
internal data class MatchContext(
    val path: String,
    val endpoint: String,
    val payload: String
)

/**
 * Represents different interpretations of payload data for matching.
 *
 * A single request can have multiple payload representations:
 * - QueryString: URL query parameters or query-string-formatted payload
 * - Json: Parsed JSON payload
 * - Merged: Combination of endpoint parameters and JSON payload
 *
 * This enables flexible matching against various data sources within a request.
 */
internal sealed class PayloadVariation {
    /**
     * Query string parameters as key-value pairs.
     */
    data class QueryString(val params: Map<String, String>) : PayloadVariation()

    /**
     * Parsed JSON payload as nested map structure.
     */
    data class Json(val data: Map<String, Any?>) : PayloadVariation()

    /**
     * Merged combination of endpoint parameters and JSON payload data.
     */
    data class Merged(
        val endpointParams: Map<String, String>,
        val payloadData: Map<String, Any?>
    ) : PayloadVariation()
}

/**
 * Utilities for extracting and parsing request data for adaptive sampling matching.
 *
 * Handles various payload formats:
 * - URL query parameters
 * - JSON payloads
 * - Google Analytics batching format (newline-separated query strings)
 */
object RequestDataExtractor {

    private const val MAX_CONTEXT_LENGTH = 65536

    /**
     * Parses a URL to extract the path and query parameters.
     *
     * Example:
     *   Input: "https://api.amplitude.com/v1/batch?api_key=123&param=value"
     *   Output: ("/v1/batch", {"api_key": "123", "param": "value"})
     *
     * @param url The full URL to parse
     * @return Pair of (path, queryParams)
     */
    fun parseUrl(url: String): Pair<String, Map<String, String>> {
        try {
            // Find the query string start
            val queryStart = url.indexOf('?')
            if (queryStart == -1) {
                // No query string, extract path
                val pathStart = url.indexOf('/', url.indexOf("://") + 3)
                val path = if (pathStart != -1) url.substring(pathStart) else "/"
                return Pair(path, emptyMap())
            }

            // Extract path (between host and ?)
            val pathStart = url.indexOf('/', url.indexOf("://") + 3)
            val path = if (pathStart != -1) url.substring(pathStart, queryStart) else "/"

            // Extract and parse query string
            val queryString = url.substring(queryStart + 1)
            val params = parseQueryString(queryString)

            return Pair(path, params)
        } catch (e: Exception) {
            // Fallback for malformed URLs
            return Pair("/", emptyMap())
        }
    }

    /**
     * Parses a query string into key-value pairs.
     *
     * Example:
     *   Input: "key1=value1&key2=value2&key3="
     *   Output: {"key1": "value1", "key2": "value2", "key3": ""}
     *
     * @param queryString The query string to parse (without leading '?')
     * @return Map of parameter name to value
     */
    fun parseQueryString(queryString: String): Map<String, String> {
        if (queryString.isEmpty()) return emptyMap()

        return queryString.split("&").mapNotNull { param ->
            if (param.isEmpty()) return@mapNotNull null

            val parts = param.split("=", limit = 2)
            if (parts.isEmpty()) return@mapNotNull null

            val key = urlDecode(parts[0])
            val value = if (parts.size > 1) urlDecode(parts[1]) else ""
            key to value
        }.toMap()
    }

    /**
     * Attempts to parse payload as JSON and convert to a Map.
     *
     * @param payload The payload string to parse
     * @return Parsed map, or null if parsing fails
     */
    fun parseJsonPayload(payload: String): Map<String, Any?>? {
        return try {
            val json = Json { ignoreUnknownKeys = true; isLenient = true }
            val element = json.parseToJsonElement(payload)
            jsonElementToMap(element)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts a JsonElement to a Map<String, Any?>.
     */
    private fun jsonElementToMap(element: JsonElement): Map<String, Any?>? {
        return when (element) {
            is JsonObject -> element.mapValues { (_, value) -> jsonElementToAny(value) }
            else -> null
        }
    }

    /**
     * Converts a JsonElement to Any? (for nested structures).
     */
    private fun jsonElementToAny(element: JsonElement): Any? {
        return when (element) {
            is JsonPrimitive -> {
                when {
                    element.isString -> element.content
                    element.content == "true" -> "true"  // Convert boolean to string for pattern matching
                    element.content == "false" -> "false"  // Convert boolean to string for pattern matching
                    else -> element.content // Numbers as strings for simplicity
                }
            }
            is JsonArray -> element.map { jsonElementToAny(it) }
            is JsonObject -> element.mapValues { (_, value) -> jsonElementToAny(value) }
            is JsonNull -> null
        }
    }

    /**
     * Extracts all payload variations from a request for matching.
     *
     * Returns multiple interpretations of the request data:
     * 1. Endpoint query parameters
     * 2. JSON payload (if parseable)
     * 3. Query string lines (Google Analytics batching format)
     * 4. Merged endpoint + payload data
     *
     * @param request The request to extract variations from
     * @return List of payload variations
     */
    internal fun extractPayloadVariations(request: Request): List<PayloadVariation> {
        val variations = mutableListOf<PayloadVariation>()

        // Extract endpoint params
        val (_, endpointParams) = parseUrl(request.endpoint)
        if (endpointParams.isNotEmpty()) {
            variations.add(PayloadVariation.QueryString(endpointParams))
        }

        val payload = request.payload
        if (payload.isNullOrEmpty()) {
            return variations
        }

        // Try JSON parsing first
        val jsonData = parseJsonPayload(payload)
        if (jsonData != null) {
            variations.add(PayloadVariation.Json(jsonData))
            // Also add merged version if we have endpoint params
            if (endpointParams.isNotEmpty()) {
                variations.add(PayloadVariation.Merged(endpointParams, jsonData))
            }
            return variations
        }

        // Not JSON - try query string batching (Google Analytics style: newline-separated query strings)
        payload.split("\n").forEach { line ->
            if (line.trim().isNotEmpty()) {
                val lineParams = parseQueryString(line.trim())
                if (lineParams.isNotEmpty()) {
                    val merged = endpointParams + lineParams
                    variations.add(PayloadVariation.QueryString(merged))
                }
            }
        }

        return variations
    }

    /**
     * Creates match context for special key operations.
     *
     * The context contains truncated versions of endpoint and payload for
     * substring matching operations (CONTAINS, etc.).
     *
     * @param request The request to create context from
     * @return MatchContext with path, endpoint, and payload
     */
    internal fun createMatchContext(request: Request): MatchContext {
        val (path, _) = parseUrl(request.endpoint)
        return MatchContext(
            path = path,
            endpoint = request.endpoint.take(MAX_CONTEXT_LENGTH),
            payload = (request.payload ?: "").take(MAX_CONTEXT_LENGTH)
        )
    }
}
