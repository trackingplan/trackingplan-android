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
//      https://github.com/firebase/firebase-android-sdk/blob/master/firebase-perf/src/main/java/com/google/firebase/perf/network/InstrumentOkHttpEnqueueCallback.java
package com.trackingplan.client.sdk.interception.okhttp;

import com.trackingplan.client.sdk.interception.InstrumentRequestBuilder;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

public class InstrOkHttpEnqueueCallback implements Callback {

    private final Callback callback;
    private final InstrumentRequestBuilder requestBuilder;

    public InstrOkHttpEnqueueCallback(Callback callback, InstrumentRequestBuilder builder) {
        this.callback = callback;
        this.requestBuilder = builder;
    }

    public void onFailure(Call call, IOException e) {
        Request request = call.request();
        if (request != null) {
            HttpUrl url = request.url();
            if (url != null) {
                requestBuilder.setUrl(url.url().toString());
            }

            String method = request.method();
            if (method != null) {
                requestBuilder.setHttpMethod(request.method());
            }
        }

        requestBuilder.setException(e);

        this.callback.onFailure(call, e);
    }

    public void onResponse(Call call, Response response) throws IOException {
        TrackingplanOkHttpClient.finishInterception(response, requestBuilder);
        this.callback.onResponse(call, response);
    }
}
