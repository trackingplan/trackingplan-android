// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

/**
 * Base test class for tests that need platform initialization.
 * Extend this class when your test uses platform-dependent features
 * like CacheStorage, KeyValueStore, or other classes that require
 * Android context or iOS-specific APIs.
 */
expect abstract class BaseTest()
