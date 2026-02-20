// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.session

import com.trackingplan.shared.KeyValueStore
import com.trackingplan.shared.Storage

object StorageMigration {

    private const val LEGACY_STORE_NAME = "Trackingplan"

    /**
     * Creates a Storage instance and migrates legacy data if needed.
     * This replaces the old Storage wrapper constructor.
     * @throws Exception if the storage cannot be created
     */
    @JvmStatic
    @Throws(Exception::class)
    fun createWithMigration(tpId: String, environment: String): Storage {
        val storage = Storage.create(tpId, environment)
        migrateLegacyStorageIfNeeded(tpId, environment, storage)
        return storage
    }

    private fun migrateLegacyStorageIfNeeded(
        tpId: String,
        environment: String,
        storage: Storage
    ) {
        val legacyStore = try {
            KeyValueStore.create(LEGACY_STORE_NAME)
        } catch (e: Exception) {
            // If we can't access legacy storage, skip migration silently
            return
        }

        // Skip if legacy store has no data
        if (!legacyStore.contains("tpId")) {
            return
        }

        // Check if tpId/environment match
        val legacyTpId = legacyStore.getString("tpId", null)
        val legacyEnvironment = legacyStore.getString("environment", null)

        val canMigrate = legacyTpId != null && legacyTpId == tpId &&
                legacyEnvironment != null && legacyEnvironment == environment

        // Migrate only if tpId/environment match and new storage doesn't have data yet
        if (canMigrate && storage.isFirstTimeExecution()) {
            // Migrate only important keys: first_time_executed_at and last_dau_event_sent_at
            val firstTimeExecutedAt = legacyStore.getLong("first_time_executed_at", -1)
            if (firstTimeExecutedAt != -1L) {
                storage.saveFirstTimeExecution(firstTimeExecutedAt)
            }

            val lastDauEventSentAt = legacyStore.getLong("last_dau_event_sent_at", -1)
            if (lastDauEventSentAt != -1L) {
                storage.saveLastDauEventSentTimestamp(lastDauEventSentAt)
            }
        }

        // Always clear legacy storage
        legacyStore.clear()
    }
}
