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
//      https://github.com/firebase/firebase-android-sdk/blob/master/firebase-perf/src/main/java/com/google/firebase/perf/network/InstrURLConnectionBase.java
package com.trackingplan.client.sdk.interception.urlconnection;

import android.os.Build;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.interception.InstrumentRequestBuilder;
import com.trackingplan.client.sdk.util.AndroidLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.util.List;
import java.util.Map;

/**
 * These methods are the common instrumented methods between HttpURLConnection and
 * HttpsURLConnection.
 */
final class InstrURLConnectionBase {

    private static final AndroidLog logger = AndroidLog.getInstance();
    private static final String USER_AGENT_PROPERTY = "User-Agent";

    private final HttpURLConnection httpUrlConnection;
    private final InstrumentRequestBuilder requestBuilder;
    private boolean interceptionFinished = false;

    public InstrURLConnectionBase(HttpURLConnection connection, InstrumentRequestBuilder builder) {
        httpUrlConnection = connection;
        requestBuilder = builder;
        requestBuilder.setUrl(httpUrlConnection.getURL().toString());
    }

    public void connect() throws IOException {
        try {
            httpUrlConnection.connect();
        } catch (IOException ex) {
            throw finishInterceptionWithError(ex);
        }
    }

    public void disconnect() {
        finishInterception();
        httpUrlConnection.disconnect();
    }

    public Object getContent() throws IOException {
        try {
            requestBuilder.setHttpResponseCode(httpUrlConnection.getResponseCode());
            Object content = httpUrlConnection.getContent();
            finishInterception();
            return content;
        } catch (IOException ex) {
            throw finishInterceptionWithError(ex);
        }
    }

    @SuppressWarnings("rawtypes")
    public Object getContent(final Class[] classes) throws IOException {
        try {
            requestBuilder.setHttpResponseCode(httpUrlConnection.getResponseCode());
            Object content = httpUrlConnection.getContent(classes);
            finishInterception();
            return content;
        } catch (IOException ex) {
            throw finishInterceptionWithError(ex);
        }
    }

    public InputStream getInputStream() throws IOException {
        try {
            requestBuilder.setHttpResponseCode(httpUrlConnection.getResponseCode());
            InputStream in = httpUrlConnection.getInputStream();
            finishInterception();
            return in;
        } catch (IOException ex) {
            throw finishInterceptionWithError(ex);
        }
    }

    public InputStream getErrorStream() {
        try {
            requestBuilder.setHttpResponseCode(httpUrlConnection.getResponseCode());
        } catch (IOException e) {
            logger.debug("IOException thrown trying to obtain the response code");
        }
        finishInterception();
        return httpUrlConnection.getErrorStream();
    }

    public OutputStream getOutputStream() throws IOException {
        try {
            return new InstrHttpOutputStream(httpUrlConnection.getOutputStream(), this, requestBuilder);
        } catch (IOException ex) {
            throw finishInterceptionWithError(ex);
        }
    }

    public long getLastModified() {
        return httpUrlConnection.getLastModified();
    }

    public Permission getPermission() throws IOException {
        try {
            return httpUrlConnection.getPermission();
        } catch (IOException ex) {
            throw finishInterceptionWithError(ex);
        }
    }

    public int getResponseCode() throws IOException {
        try {
            final int code = httpUrlConnection.getResponseCode();
            requestBuilder.setHttpResponseCode(code);
            return code;
        } catch (IOException ex) {
            throw finishInterceptionWithError(ex);
        }
    }

    public String getResponseMessage() throws IOException {
        try {
            final String message = httpUrlConnection.getResponseMessage();
            requestBuilder.setHttpResponseCode(httpUrlConnection.getResponseCode());
            return message;
        } catch (IOException ex) {
            throw finishInterceptionWithError(ex);
        }
    }

    public long getExpiration() {
        return httpUrlConnection.getExpiration();
    }

    public String getHeaderField(final int n) {
        return httpUrlConnection.getHeaderField(n);
    }

    public String getHeaderField(final String name) {
        return httpUrlConnection.getHeaderField(name);
    }

    public long getHeaderFieldDate(final String name, final long defaultDate) {
        return httpUrlConnection.getHeaderFieldDate(name, defaultDate);
    }

    public int getHeaderFieldInt(final String name, final int defaultInt) {
        return httpUrlConnection.getHeaderFieldInt(name, defaultInt);
    }

    public long getHeaderFieldLong(final String name, final long defaultLong) {
        long value = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            value = httpUrlConnection.getHeaderFieldLong(name, defaultLong);
        }
        return value;
    }

    public String getHeaderFieldKey(final int n) {
        return httpUrlConnection.getHeaderFieldKey(n);
    }

    public Map<String, List<String>> getHeaderFields() {
        return httpUrlConnection.getHeaderFields();
    }

    public String getContentEncoding() {
        return httpUrlConnection.getContentEncoding();
    }

    public int getContentLength() {
        return httpUrlConnection.getContentLength();
    }

    public long getContentLengthLong() {
        long contentLength = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            contentLength = httpUrlConnection.getContentLengthLong();
        }

        return contentLength;
    }

    public String getContentType() {
        return httpUrlConnection.getContentType();
    }

    public long getDate() {
        return httpUrlConnection.getDate();
    }

    public void addRequestProperty(final String key, final String value) {
        requestBuilder.addHeaderField(key, value, false);
        httpUrlConnection.addRequestProperty(key, value);
    }

    @Override
    public boolean equals(final Object obj) {
        return httpUrlConnection.equals(obj);
    }

    public boolean getAllowUserInteraction() {
        return httpUrlConnection.getAllowUserInteraction();
    }

    public void setAllowUserInteraction(boolean allowUserInteraction) {
        httpUrlConnection.setAllowUserInteraction(allowUserInteraction);
    }

    public int getConnectTimeout() {
        return httpUrlConnection.getConnectTimeout();
    }

    public void setConnectTimeout(int timeout) {
        httpUrlConnection.setConnectTimeout(timeout);
    }

    public boolean getDefaultUseCaches() {
        return httpUrlConnection.getDefaultUseCaches();
    }

    public void setDefaultUseCaches(boolean defaultUseCaches) {
        httpUrlConnection.setDefaultUseCaches(defaultUseCaches);
    }

    public boolean getDoInput() {
        return httpUrlConnection.getDoInput();
    }

    public void setDoInput(boolean doInput) {
        httpUrlConnection.setDoInput(doInput);
    }

    public boolean getDoOutput() {
        return httpUrlConnection.getDoOutput();
    }

    public void setDoOutput(boolean doOutput) {
        httpUrlConnection.setDoOutput(doOutput);
    }

    public long getIfModifiedSince() {
        return httpUrlConnection.getIfModifiedSince();
    }

    public void setIfModifiedSince(long ifModifiedSince) {
        httpUrlConnection.setIfModifiedSince(ifModifiedSince);
    }

    public boolean getInstanceFollowRedirects() {
        return httpUrlConnection.getInstanceFollowRedirects();
    }

    public void setInstanceFollowRedirects(boolean followRedirects) {
        httpUrlConnection.setInstanceFollowRedirects(followRedirects);
    }

    public int getReadTimeout() {
        return httpUrlConnection.getReadTimeout();
    }

    public void setReadTimeout(int timeout) {
        httpUrlConnection.setReadTimeout(timeout);
    }

    public String getRequestMethod() {
        return httpUrlConnection.getRequestMethod();
    }

    public void setRequestMethod(String method) throws ProtocolException {
        httpUrlConnection.setRequestMethod(method);
    }

    public Map<String, List<String>> getRequestProperties() {
        return httpUrlConnection.getRequestProperties();
    }

    public String getRequestProperty(String key) {
        return httpUrlConnection.getRequestProperty(key);
    }

    public URL getURL() {
        return httpUrlConnection.getURL();
    }

    public boolean getUseCaches() {
        return httpUrlConnection.getUseCaches();
    }

    public void setUseCaches(boolean useCaches) {
        httpUrlConnection.setUseCaches(useCaches);
    }

    @Override
    public int hashCode() {
        return httpUrlConnection.hashCode();
    }

    public void setChunkedStreamingMode(int chunkLen) {
        httpUrlConnection.setChunkedStreamingMode(chunkLen);
    }

    public void setFixedLengthStreamingMode(int contentLength) {
        httpUrlConnection.setFixedLengthStreamingMode(contentLength);
    }

    public void setFixedLengthStreamingMode(long contentLength) {
        httpUrlConnection.setFixedLengthStreamingMode(contentLength);
    }

    public void setRequestProperty(String key, String value) {
        if (USER_AGENT_PROPERTY.equalsIgnoreCase(key)) {
            requestBuilder.setUserAgent(value);
        }
        requestBuilder.addHeaderField(key, value, true);
        httpUrlConnection.setRequestProperty(key, value);
    }

    @Override
    @NonNull
    public String toString() {
        return httpUrlConnection.toString();
    }

    public boolean usingProxy() {
        return httpUrlConnection.usingProxy();
    }

    private void finishInterception() {
        if (interceptionFinished) return;
        interceptionFinished = true;
        updateRequestMethod();
        requestBuilder.build();
    }

    public IOException finishInterceptionWithError(IOException ex) {
        requestBuilder.setException(ex);
        finishInterception();
        return ex;
    }

    private void updateRequestMethod() {
        final String method = getRequestMethod();
        if (method != null) {
            requestBuilder.setHttpMethod(method);
        } else {
            // Default POST if getDoOutput, GET otherwise.
            if (getDoOutput()) {
                requestBuilder.setHttpMethod("POST");
            } else {
                requestBuilder.setHttpMethod("GET");
            }
        }
    }
}
