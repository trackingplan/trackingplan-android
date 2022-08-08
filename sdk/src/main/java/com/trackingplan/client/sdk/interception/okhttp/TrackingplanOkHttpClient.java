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
//      https://github.com/firebase/firebase-android-sdk/blob/master/firebase-perf/src/main/java/com/google/firebase/perf/network/FirebasePerfOkHttpClient.java
package com.trackingplan.client.sdk.interception.okhttp;

import static com.trackingplan.client.sdk.TrackingplanConfig.MAX_REQUEST_BODY_SIZE_IN_BYTES;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.TrackingplanInstance;
import com.trackingplan.client.sdk.interception.HttpInstrumentRequestBuilder;
import com.trackingplan.client.sdk.interception.InstrumentRequestBuilder;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * These are the functions that are bytecode instrumented into the apk for
 * OkHttp functions
 */
public class TrackingplanOkHttpClient {

    private TrackingplanOkHttpClient() {
    }

    @Keep
    public static Response execute(final Call call) throws IOException {
        final Response response;

        InstrumentRequestBuilder builder = new HttpInstrumentRequestBuilder(TrackingplanInstance.getInstance(),
                "okhttp");

        try {
            response = call.execute();
            finishInterception(response, builder);
        } catch (IOException e) {
            builder.setException(e);
            Request request = call.request();
            if (request != null) {
                finishInterception(request, builder);
            }
            throw e;
        }

        return response;
    }

    @Keep
    public static void enqueue(final Call call, final Callback callback) {
        call.enqueue(
                new InstrOkHttpEnqueueCallback(
                        callback, new HttpInstrumentRequestBuilder(TrackingplanInstance.getInstance(), "okhttp")));
    }

    static void finishInterception(@NonNull Response response, @NonNull InstrumentRequestBuilder builder)
            throws IOException {

        Request request = response.request();
        if (request == null) {
            return;
        }

        builder.setHttpResponseCode(response.code());
        finishInterception(request, builder);
    }

    static void finishInterception(@NonNull Request request, @NonNull InstrumentRequestBuilder builder)
            throws IOException {

        HttpUrl url = request.url();
        if (url != null) {
            builder.setUrl(url.toString());
        }

        String method = request.method();
        if (method != null) {
            builder.setHttpMethod(method);
        }

        RequestBody requestBody = request.body();
        if (requestBody != null) {
            long requestContentLength = requestBody.contentLength();
            if (requestContentLength != -1) {
                final Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);
                builder.setRequestPayloadNumBytes(requestContentLength);
                builder.setRequestPayload(
                        buffer.readByteArray(Math.min(requestContentLength, MAX_REQUEST_BODY_SIZE_IN_BYTES)));
            }
        }

        builder.build();
    }
}
