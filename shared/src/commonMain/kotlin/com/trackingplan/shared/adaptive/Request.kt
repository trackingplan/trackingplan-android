// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared.adaptive

/**
 * Platform-agnostic request representation for adaptive sampling matching.
 *
 * This is a simplified, immutable representation created from platform-specific
 * request objects (Android HttpRequest, iOS TrackingplanTrackRequest) to enable
 * cross-platform matching logic in the shared module.
 *
 * @property provider The analytics provider (e.g., "amplitude", "mixpanel")
 * @property endpoint The full request URL
 * @property payload The request payload (null for GET requests or requests without body)
 */
data class Request(
    val provider: String,
    val endpoint: String,
    val payload: String?
)
