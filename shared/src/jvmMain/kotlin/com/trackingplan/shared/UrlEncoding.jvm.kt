// Copyright (c) 2021 Trackingplan
package com.trackingplan.shared

import java.net.URLDecoder

actual fun urlDecode(encoded: String): String =
    try {
        URLDecoder.decode(encoded, "UTF-8")
    } catch (e: Exception) {
        encoded
    }
