// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import platform.Foundation.NSString
import platform.Foundation.stringByRemovingPercentEncoding
import platform.Foundation.stringByReplacingOccurrencesOfString

actual fun urlDecode(encoded: String): String {
    // Replace + with space BEFORE percent decoding to avoid incorrectly converting %2B to space
    // Order matters: + → space (first), then %2B → + (via percent decode)
    val withSpaces = (encoded as NSString).stringByReplacingOccurrencesOfString("+", " ")
    // If decoding fails, still return the string with + replaced
    return (withSpaces as NSString).stringByRemovingPercentEncoding ?: withSpaces
}
