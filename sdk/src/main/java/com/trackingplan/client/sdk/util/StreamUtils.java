// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class StreamUtils {

    private static final int DEFAULT_BUFFER_SIZE = 8192;

    public static ByteArrayOutputStream readAll(InputStream in) throws IOException {

        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        int length;

        while ((length = in.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }

        return result;
    }

    public static String convertInputStreamToString(InputStream in) throws IOException {
        return readAll(in).toString("utf-8");
    }
}
