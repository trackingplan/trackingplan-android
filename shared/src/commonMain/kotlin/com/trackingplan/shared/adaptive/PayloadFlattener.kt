// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared.adaptive

/**
 * Utilities for flattening nested data structures for efficient pattern matching.
 *
 * Flattening converts nested objects/arrays into a flat map of key -> list of string values.
 * This optimization (from the JS SDK) allows fast lookups during pattern matching by
 * avoiding repeated recursive traversals of the same data structure.
 *
 * Example transformation:
 * ```
 * Input: {
 *   "user": {
 *     "name": "John",
 *     "tags": ["premium", "vip"]
 *   },
 *   "event": "purchase"
 * }
 *
 * Output: {
 *   "name" -> ["John"],
 *   "tags" -> ["premium", "vip"],
 *   "event" -> ["purchase"]
 * }
 * ```
 */
object PayloadFlattener {

    /**
     * Flattens an object into a map of key -> list of string values.
     *
     * Recursively processes nested objects and arrays, collecting all leaf string values.
     * This allows fast field lookups during pattern matching without repeated tree traversals.
     *
     * Features:
     * - Collects all string values from nested structures
     * - Deduplicates values per key
     * - Ignores non-string leaf values (numbers, booleans, null)
     * - Preserves array elements as separate values (OR logic)
     *
     * @param data The data to flatten (Map, List, or primitive)
     * @return Map of field name to list of string values
     */
    fun flattenToKeyValues(data: Any?): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        flattenRecursive(data, result)
        return result.mapValues { it.value.toList() }
    }

    /**
     * Recursively flattens data structures, collecting string values by key.
     */
    private fun flattenRecursive(data: Any?, result: MutableMap<String, MutableList<String>>) {
        when (data) {
            null -> return

            is Map<*, *> -> {
                data.forEach { (key, value) ->
                    if (key is String && value is String) {
                        // Direct key-value pair
                        result.getOrPut(key) { mutableListOf() }.apply {
                            if (!contains(value)) add(value)
                        }
                    }
                    // Recursively process nested structures
                    if (value != null && (value is Map<*, *> || value is List<*>)) {
                        flattenRecursive(value, result)
                    }
                }
            }

            is List<*> -> {
                data.forEach { item ->
                    flattenRecursive(item, result)
                }
            }

            // Ignore other types (numbers, booleans, etc.)
            // Only string values are collected for matching
        }
    }

    /**
     * Flattens a payload variation to a key-value map.
     *
     * Handles different payload variation types:
     * - QueryString: Direct key-value mapping
     * - Json: Recursive flattening of nested structure
     * - Merged: Combines endpoint params with flattened JSON
     *
     * @param variation The payload variation to flatten
     * @return Flattened map of key -> list of string values
     */
    internal fun flattenPayloadVariation(variation: PayloadVariation): Map<String, List<String>> {
        return when (variation) {
            is PayloadVariation.QueryString -> {
                // Query strings are already flat key-value pairs
                variation.params.mapValues { listOf(it.value) }
            }

            is PayloadVariation.Json -> {
                // Recursively flatten nested JSON structure
                flattenToKeyValues(variation.data)
            }

            is PayloadVariation.Merged -> {
                // Combine endpoint params with flattened JSON payload
                // Create mutable map with mutable list values to safely merge shared keys
                val flattened = mutableMapOf<String, MutableList<String>>()
                flattenToKeyValues(variation.payloadData).forEach { (k, v) ->
                    flattened[k] = v.toMutableList()
                }

                // Add endpoint params (avoiding duplicates)
                variation.endpointParams.forEach { (key, value) ->
                    val values = flattened.getOrPut(key) { mutableListOf() }
                    if (!values.contains(value)) {
                        values.add(value)
                    }
                }

                flattened.mapValues { it.value.toList() }
            }
        }
    }
}
