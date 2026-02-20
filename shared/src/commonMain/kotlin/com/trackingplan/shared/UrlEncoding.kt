// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

/**
 * URL decodes a percent-encoded string.
 *
 * Platform implementations:
 * - Android: Uses java.net.URLDecoder
 * - iOS: Uses NSString.stringByRemovingPercentEncoding
 *
 * @param encoded The percent-encoded string to decode
 * @return The decoded string, or the original if decoding fails
 */
expect fun urlDecode(encoded: String): String
