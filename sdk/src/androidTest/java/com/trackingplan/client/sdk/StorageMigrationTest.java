package com.trackingplan.client.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.trackingplan.client.sdk.session.StorageMigration;
import com.trackingplan.shared.KeyValueStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for Storage migration from legacy store (Trackingplan) to new store (com.trackingplan.sdk).
 * Migration should only transfer first_time_executed_at and last_dau_event_sent_at keys
 * when tpId/environment match.
 */
@RunWith(AndroidJUnit4.class)
public class StorageMigrationTest extends BaseInstrumentedTest {

    private static final String LEGACY_STORE_NAME = "Trackingplan";
    private static final String NEW_STORE_NAME = "com.trackingplan.sdk";

    @Before
    @Override
    public void setUp() {
        super.setUp();
        // Clear both legacy and new stores before each test
        try {
            KeyValueStore.Companion.create(LEGACY_STORE_NAME).clear();
            KeyValueStore.Companion.create(NEW_STORE_NAME).clear();
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear stores", e);
        }
    }

    @After
    @Override
    public void tearDown() {
        // Clean up after tests
        try {
            KeyValueStore.Companion.create(LEGACY_STORE_NAME).clear();
            KeyValueStore.Companion.create(NEW_STORE_NAME).clear();
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear stores", e);
        }
        super.tearDown();
    }

    @Test
    public void test_migration_from_legacy_store_copies_important_keys() throws Exception {
        // Given: Legacy store with data
        KeyValueStore legacyStore = KeyValueStore.Companion.create(LEGACY_STORE_NAME);
        legacyStore.setString("tpId", TEST_TP_ID);
        legacyStore.setString("environment", TEST_ENVIRONMENT);
        legacyStore.setLong("first_time_executed_at", 1000L);
        legacyStore.setLong("last_dau_event_sent_at", 2000L);
        // Also add keys that should NOT be migrated
        legacyStore.setString("session_id", "old-session-id");
        legacyStore.setLong("session_started_at", 3000L);

        // When: Storage is created with matching tpId/environment (triggers migration)
        StorageMigration.createWithMigration(TEST_TP_ID, TEST_ENVIRONMENT);

        // Then: Important keys are migrated
        KeyValueStore newStore = KeyValueStore.Companion.create(NEW_STORE_NAME);
        assertEquals(1000L, newStore.getLong("first_time_executed_at", -1));
        assertEquals(2000L, newStore.getLong("last_dau_event_sent_at", -1));
    }

    @Test
    public void test_migration_clears_legacy_store_after_migration() throws Exception {
        // Given: Legacy store with data
        KeyValueStore legacyStore = KeyValueStore.Companion.create(LEGACY_STORE_NAME);
        legacyStore.setString("tpId", TEST_TP_ID);
        legacyStore.setString("environment", TEST_ENVIRONMENT);
        legacyStore.setLong("first_time_executed_at", 1000L);
        legacyStore.setLong("last_dau_event_sent_at", 2000L);

        // When: Storage is created with matching tpId/environment (triggers migration)
        StorageMigration.createWithMigration(TEST_TP_ID, TEST_ENVIRONMENT);

        // Then: Legacy store is cleared
        KeyValueStore legacyStoreAfter = KeyValueStore.Companion.create(LEGACY_STORE_NAME);
        assertFalse(legacyStoreAfter.contains("tpId"));
        assertFalse(legacyStoreAfter.contains("first_time_executed_at"));
        assertFalse(legacyStoreAfter.contains("last_dau_event_sent_at"));
    }

    @Test
    public void test_migration_skipped_when_new_store_has_data() throws Exception {
        // Given: New store already has data
        KeyValueStore newStore = KeyValueStore.Companion.create(NEW_STORE_NAME);
        newStore.setString("tp_id", TEST_TP_ID);
        newStore.setString("environment", TEST_ENVIRONMENT);
        newStore.setLong("first_time_executed_at", 5000L);

        // And legacy store has different data
        KeyValueStore legacyStore = KeyValueStore.Companion.create(LEGACY_STORE_NAME);
        legacyStore.setString("tpId", TEST_TP_ID);
        legacyStore.setString("environment", TEST_ENVIRONMENT);
        legacyStore.setLong("first_time_executed_at", 1000L);

        // When: Storage is created (triggers migration check)
        StorageMigration.createWithMigration(TEST_TP_ID, TEST_ENVIRONMENT);

        // Then: New store data is preserved (not overwritten)
        KeyValueStore newStoreAfter = KeyValueStore.Companion.create(NEW_STORE_NAME);
        assertEquals(5000L, newStoreAfter.getLong("first_time_executed_at", -1));

        // And legacy store is cleared (cleanup)
        KeyValueStore legacyStoreAfter = KeyValueStore.Companion.create(LEGACY_STORE_NAME);
        assertFalse(legacyStoreAfter.contains("tpId"));
    }

    @Test
    public void test_migration_skipped_when_tpid_mismatch() throws Exception {
        // Given: Legacy store has different tpId
        KeyValueStore legacyStore = KeyValueStore.Companion.create(LEGACY_STORE_NAME);
        legacyStore.setString("tpId", "TP999999"); // Different tpId
        legacyStore.setString("environment", TEST_ENVIRONMENT);
        legacyStore.setLong("first_time_executed_at", 1000L);
        legacyStore.setLong("last_dau_event_sent_at", 2000L);

        // When: Storage is created with different tpId (triggers migration check)
        StorageMigration.createWithMigration(TEST_TP_ID, TEST_ENVIRONMENT);

        // Then: No migration happens (values not copied to new store)
        // but legacy store is still cleared
        KeyValueStore legacyStoreAfter = KeyValueStore.Companion.create(LEGACY_STORE_NAME);
        assertFalse(legacyStoreAfter.contains("tpId"));
    }

    @Test
    public void test_migration_skipped_when_environment_mismatch() throws Exception {
        // Given: Legacy store has different environment
        KeyValueStore legacyStore = KeyValueStore.Companion.create(LEGACY_STORE_NAME);
        legacyStore.setString("tpId", TEST_TP_ID);
        legacyStore.setString("environment", "STAGING"); // Different environment
        legacyStore.setLong("first_time_executed_at", 1000L);
        legacyStore.setLong("last_dau_event_sent_at", 2000L);

        // When: Storage is created with different environment (triggers migration check)
        StorageMigration.createWithMigration(TEST_TP_ID, TEST_ENVIRONMENT);

        // Then: No migration happens (values not copied to new store)
        // but legacy store is still cleared
        KeyValueStore legacyStoreAfter = KeyValueStore.Companion.create(LEGACY_STORE_NAME);
        assertFalse(legacyStoreAfter.contains("environment"));
    }
}
