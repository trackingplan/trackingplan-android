// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.util;

import androidx.annotation.NonNull;

public class StringUtils {
    public static String getNonNullOrDefault(String value, @NonNull String defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return value;
    }

    public static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }
}
