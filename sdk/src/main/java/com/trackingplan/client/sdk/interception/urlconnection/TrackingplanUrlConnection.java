// This file contains code copied and adapted from Google Firebase Performance Project, copyrighted
// by Google LLC since 2020 and licensed under the Apache License Version 2.0.
//
// You may obtain a copy of the License at
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Changes to the original work are licensed under the MIT License
//
// MIT License
//
// Copyright (c) 2021 Trackingplan
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
// You may see the original Work at
//      https://github.com/firebase/firebase-android-sdk/blob/master/firebase-perf/src/main/java/com/google/firebase/perf/network/FirebasePerfUrlConnection.java
package com.trackingplan.client.sdk.interception.urlconnection;

import androidx.annotation.Keep;

import com.trackingplan.client.sdk.TrackingplanInstance;
import com.trackingplan.client.sdk.interception.HttpInstrumentRequestBuilder;
import com.trackingplan.client.sdk.interception.InstrumentRequestBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

/**
 * These are the functions that are bytecode instrumented into the apk and methods to collect
 * information for the NetworkRequestMetric for UrlConnection.
 */
public class TrackingplanUrlConnection {

    private TrackingplanUrlConnection() {
    }

    // Bytecode instrumented functions

    /**
     * Instrumented function for UrlConnection.getInputStream()
     */
    @Keep
    public static InputStream openStream(final URL url) throws IOException {
        return openStream(url, TrackingplanInstance.getInstance());
    }

    private static InputStream openStream(URL url, TrackingplanInstance tpInstance) throws IOException {

        InstrumentRequestBuilder builder = new HttpInstrumentRequestBuilder(tpInstance, "urlconnection");
        URLConnection connection = url.openConnection();

        if (connection instanceof HttpsURLConnection) {
            return new InstrHttpsURLConnection((HttpsURLConnection) connection, builder)
                    .getInputStream();
        } else if (connection instanceof HttpURLConnection) {
            return new InstrHttpURLConnection((HttpURLConnection) connection, builder)
                    .getInputStream();
        }

        return connection.getInputStream();
    }

    /**
     * Instrumented function for UrlConnection.getContent()
     */
    @Keep
    public static Object getContent(final URL url) throws IOException {
        return getContent(url, TrackingplanInstance.getInstance());
    }

    /**
     * Instrumented function for UrlConnection.getContent(classes)
     */
    @Keep
    public static Object getContent(final URL url, @SuppressWarnings("rawtypes") final Class[] types)
            throws IOException {
        return getContent(url, types, TrackingplanInstance.getInstance());
    }

    private static Object getContent(final URL url, TrackingplanInstance tpInstance) throws IOException {

        InstrumentRequestBuilder builder = new HttpInstrumentRequestBuilder(tpInstance, "urlconnection");
        URLConnection connection = url.openConnection();

        if (connection instanceof HttpsURLConnection) {
            return new InstrHttpsURLConnection((HttpsURLConnection) connection, builder)
                    .getContent();
        } else if (connection instanceof HttpURLConnection) {
            return new InstrHttpURLConnection((HttpURLConnection) connection, builder)
                    .getContent();
        }

        return connection.getContent();
    }

    private static Object getContent(
            final URL url,
            @SuppressWarnings("rawtypes") final Class[] types,
            TrackingplanInstance tpInstance)
            throws IOException {

        InstrumentRequestBuilder builder = new HttpInstrumentRequestBuilder(tpInstance, "urlconnection");
        URLConnection connection = url.openConnection();

        if (connection instanceof HttpsURLConnection) {
            return new InstrHttpsURLConnection((HttpsURLConnection) connection, builder)
                    .getContent(types);
        } else if (connection instanceof HttpURLConnection) {
            return new InstrHttpURLConnection((HttpURLConnection) connection, builder)
                    .getContent(types);
        }

        return connection.getContent(types);
    }

    /**
     * Instrumented function for URL.openConnection()
     */
    @Keep
    public static URLConnection instrument(URLConnection connection) {
        if (connection instanceof HttpsURLConnection) {
            return new InstrHttpsURLConnection(
                    (HttpsURLConnection) connection,
                    new HttpInstrumentRequestBuilder(TrackingplanInstance.getInstance(), "urlconnection"));
        } else if (connection instanceof HttpURLConnection) {
            return new InstrHttpURLConnection(
                    (HttpURLConnection) connection,
                    new HttpInstrumentRequestBuilder(TrackingplanInstance.getInstance(), "urlconnection"));
        }
        return connection;
    }
}
