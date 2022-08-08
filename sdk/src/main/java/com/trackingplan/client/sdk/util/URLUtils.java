// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.util;

import androidx.annotation.NonNull;

import java.net.URI;
import java.net.URISyntaxException;

public class URLUtils {
    @NonNull
    public static String getDomain(@NonNull String url) {

        if (url.isEmpty()) {
            return "";
        }

        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException e) {
            return "";
        }
    }
}
