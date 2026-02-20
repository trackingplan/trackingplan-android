// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import kotlin.test.*

class KeyValueStoreTest : BaseTest() {

    private lateinit var store: KeyValueStore
    private val testStoreName = "com.trackingplan.test.keyvaluestore.${kotlin.random.Random.nextInt()}"

    @BeforeTest
    fun setup() {
        store = KeyValueStore.create(testStoreName)
        store.clear()
    }

    @AfterTest
    fun cleanup() {
        store.clear()
    }

    // Basic String Operations

    @Test
    fun testSetAndGetString() {
        store.setString("key", "value")
        assertEquals("value", store.getString("key", null))
    }

    @Test
    fun testGetStringReturnsDefaultWhenKeyNotFound() {
        assertEquals("default", store.getString("nonexistent", "default"))
    }

    @Test
    fun testGetStringReturnsNullDefaultWhenKeyNotFound() {
        assertNull(store.getString("nonexistent", null))
    }

    @Test
    fun testSetStringOverwritesExistingValue() {
        store.setString("key", "original")
        store.setString("key", "updated")
        assertEquals("updated", store.getString("key", null))
    }

    @Test
    fun testSetAndGetEmptyString() {
        store.setString("key", "")
        assertEquals("", store.getString("key", "default"))
    }

    // Basic Int Operations

    @Test
    fun testSetAndGetInt() {
        store.setInt("key", 42)
        assertEquals(42, store.getInt("key", 0))
    }

    @Test
    fun testGetIntReturnsDefaultWhenKeyNotFound() {
        assertEquals(99, store.getInt("nonexistent", 99))
    }

    @Test
    fun testSetAndGetNegativeInt() {
        store.setInt("key", -123)
        assertEquals(-123, store.getInt("key", 0))
    }

    // Basic Long Operations

    @Test
    fun testSetAndGetLong() {
        store.setLong("key", 123456789L)
        assertEquals(123456789L, store.getLong("key", 0L))
    }

    @Test
    fun testGetLongReturnsDefaultWhenKeyNotFound() {
        assertEquals(999L, store.getLong("nonexistent", 999L))
    }

    @Test
    fun testSetAndGetNegativeLong() {
        store.setLong("key", -987654321L)
        assertEquals(-987654321L, store.getLong("key", 0L))
    }

    // Basic Float Operations

    @Test
    fun testSetAndGetFloat() {
        store.setFloat("key", 3.14f)
        assertEquals(3.14f, store.getFloat("key", 0f))
    }

    @Test
    fun testGetFloatReturnsDefaultWhenKeyNotFound() {
        assertEquals(1.5f, store.getFloat("nonexistent", 1.5f))
    }

    @Test
    fun testSetAndGetNegativeFloat() {
        store.setFloat("key", -2.5f)
        assertEquals(-2.5f, store.getFloat("key", 0f))
    }

    // Basic Boolean Operations

    @Test
    fun testSetAndGetBooleanTrue() {
        store.setBoolean("key", true)
        assertEquals(true, store.getBoolean("key", false))
    }

    @Test
    fun testSetAndGetBooleanFalse() {
        store.setBoolean("key", false)
        assertEquals(false, store.getBoolean("key", true))
    }

    @Test
    fun testGetBooleanReturnsDefaultWhenKeyNotFound() {
        assertEquals(true, store.getBoolean("nonexistent", true))
        assertEquals(false, store.getBoolean("nonexistent2", false))
    }

    // Utility Operations

    @Test
    fun testContainsReturnsTrueForExistingKey() {
        store.setString("key", "value")
        assertTrue(store.contains("key"))
    }

    @Test
    fun testContainsReturnsFalseForNonexistentKey() {
        assertFalse(store.contains("nonexistent"))
    }

    @Test
    fun testRemoveDeletesKey() {
        store.setString("key", "value")
        store.remove("key")
        assertFalse(store.contains("key"))
        assertNull(store.getString("key", null))
    }

    @Test
    fun testClearRemovesAllKeys() {
        store.setString("key1", "value1")
        store.setInt("key2", 42)
        store.setBoolean("key3", true)
        store.clear()
        assertFalse(store.contains("key1"))
        assertFalse(store.contains("key2"))
        assertFalse(store.contains("key3"))
    }

    // Type Mismatch Tests - verify defaultValue is returned when stored type doesn't match

    @Test
    fun testGetIntReturnsDefaultValueOnTypeMismatch() {
        store.setString("key", "not a number")
        val result = store.getInt("key", 42)
        assertEquals(42, result)
    }

    @Test
    fun testGetLongReturnsDefaultValueOnTypeMismatch() {
        store.setString("key", "not a number")
        val result = store.getLong("key", 123L)
        assertEquals(123L, result)
    }

    @Test
    fun testGetFloatReturnsDefaultValueOnTypeMismatch() {
        store.setString("key", "not a number")
        val result = store.getFloat("key", 3.14f)
        assertEquals(3.14f, result)
    }

    @Test
    fun testGetBooleanReturnsDefaultValueOnTypeMismatch() {
        store.setString("key", "not a boolean")
        val result = store.getBoolean("key", true)
        assertEquals(true, result)
    }

}
