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
//      https://github.com/firebase/firebase-android-sdk/blob/master/firebase-perf/src/main/java/com/google/firebase/perf/network/InstrHttpsURLConnection.java
package com.trackingplan.client.sdk.interception.urlconnection;

import com.trackingplan.client.sdk.interception.InstrumentRequestBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.security.Permission;
import java.security.Principal;
import java.security.cert.Certificate;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;

/**
 * Collects network data from HttpsURLConnection object. The HttpsURLConnection request from the
 * developer's app is wrapped in these functions.
 */
final class InstrHttpsURLConnection extends HttpsURLConnection {

    // Delegate to share code between HttpURLConnection and HttpsURLConnection
    private final InstrURLConnectionBase delegate;
    private final HttpsURLConnection httpsURLConnection;

    InstrHttpsURLConnection(HttpsURLConnection connection, InstrumentRequestBuilder builder) {
        super(connection.getURL());
        httpsURLConnection = connection;
        delegate = new InstrURLConnectionBase(connection, builder);
    }

    @Override
    public void connect() throws IOException {
        delegate.connect();
    }

    @Override
    public void disconnect() {
        delegate.disconnect();
    }

    @Override
    public Object getContent() throws IOException {
        return delegate.getContent();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getContent(final Class[] classes) throws IOException {
        return delegate.getContent(classes);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return delegate.getInputStream();
    }

    @Override
    public long getLastModified() {
        return delegate.getLastModified();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return delegate.getOutputStream();
    }

    @Override
    public Permission getPermission() throws IOException {
        return delegate.getPermission();
    }

    @Override
    public int getResponseCode() throws IOException {
        return delegate.getResponseCode();
    }

    @Override
    public String getResponseMessage() throws IOException {
        return delegate.getResponseMessage();
    }

    @Override
    public long getExpiration() {
        return delegate.getExpiration();
    }

    @Override
    public String getHeaderField(final int n) {
        return delegate.getHeaderField(n);
    }

    @Override
    public String getHeaderField(final String name) {
        return delegate.getHeaderField(name);
    }

    @Override
    public long getHeaderFieldDate(final String name, final long defaultDate) {
        return delegate.getHeaderFieldDate(name, defaultDate);
    }

    @Override
    public int getHeaderFieldInt(final String name, final int defaultInt) {
        return delegate.getHeaderFieldInt(name, defaultInt);
    }

    @Override
    public long getHeaderFieldLong(final String name, final long defaultLong) {
        return delegate.getHeaderFieldLong(name, defaultLong);
    }

    @Override
    public String getHeaderFieldKey(final int n) {
        return delegate.getHeaderFieldKey(n);
    }

    @Override
    public Map<String, List<String>> getHeaderFields() {
        return delegate.getHeaderFields();
    }

    @Override
    public String getContentEncoding() {
        return delegate.getContentEncoding();
    }

    @Override
    public int getContentLength() {
        return delegate.getContentLength();
    }

    @Override
    public long getContentLengthLong() {
        return delegate.getContentLengthLong();
    }

    @Override
    public String getContentType() {
        return delegate.getContentType();
    }

    @Override
    public long getDate() {
        return delegate.getDate();
    }

    @Override
    public void addRequestProperty(final String key, final String value) {
        delegate.addRequestProperty(key, value);
    }

    @Override
    public boolean equals(final Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public boolean getAllowUserInteraction() {
        return delegate.getAllowUserInteraction();
    }

    @Override
    public int getConnectTimeout() {
        return delegate.getConnectTimeout();
    }

    @Override
    public boolean getDefaultUseCaches() {
        return delegate.getDefaultUseCaches();
    }

    @Override
    public boolean getDoInput() {
        return delegate.getDoInput();
    }

    @Override
    public boolean getDoOutput() {
        return delegate.getDoOutput();
    }

    @Override
    public InputStream getErrorStream() {
        return delegate.getErrorStream();
    }

    @Override
    public long getIfModifiedSince() {
        return delegate.getIfModifiedSince();
    }

    @Override
    public boolean getInstanceFollowRedirects() {
        return delegate.getInstanceFollowRedirects();
    }

    @Override
    public int getReadTimeout() {
        return delegate.getReadTimeout();
    }

    @Override
    public String getRequestMethod() {
        return delegate.getRequestMethod();
    }

    @Override
    public Map<String, List<String>> getRequestProperties() {
        return delegate.getRequestProperties();
    }

    @Override
    public String getRequestProperty(final String key) {
        return delegate.getRequestProperty(key);
    }

    @Override
    public URL getURL() {
        return delegate.getURL();
    }

    @Override
    public boolean getUseCaches() {
        return delegate.getUseCaches();
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public void setAllowUserInteraction(final boolean allowuserinteraction) {
        delegate.setAllowUserInteraction(allowuserinteraction);
    }

    @Override
    public void setChunkedStreamingMode(final int chunklen) {
        delegate.setChunkedStreamingMode(chunklen);
    }

    @Override
    public void setConnectTimeout(final int timeout) {
        delegate.setConnectTimeout(timeout);
    }

    @Override
    public void setDefaultUseCaches(final boolean defaultusecaches) {
        delegate.setDefaultUseCaches(defaultusecaches);
    }

    @Override
    public void setDoInput(final boolean doinput) {
        delegate.setDoInput(doinput);
    }

    @Override
    public void setDoOutput(final boolean dooutput) {
        delegate.setDoOutput(dooutput);
    }

    @Override
    public void setFixedLengthStreamingMode(final int contentLength) {
        delegate.setFixedLengthStreamingMode(contentLength);
    }

    @Override
    public void setFixedLengthStreamingMode(final long contentLength) {
        delegate.setFixedLengthStreamingMode(contentLength);
    }

    @Override
    public void setIfModifiedSince(final long ifmodifiedsince) {
        delegate.setIfModifiedSince(ifmodifiedsince);
    }

    @Override
    public void setInstanceFollowRedirects(final boolean followRedirects) {
        delegate.setInstanceFollowRedirects(followRedirects);
    }

    @Override
    public void setReadTimeout(final int timeout) {
        delegate.setReadTimeout(timeout);
    }

    @Override
    public void setRequestMethod(final String method) throws ProtocolException {
        delegate.setRequestMethod(method);
    }

    @Override
    public void setRequestProperty(final String key, final String value) {
        delegate.setRequestProperty(key, value);
    }

    @Override
    public void setUseCaches(final boolean usecaches) {
        delegate.setUseCaches(usecaches);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean usingProxy() {
        return delegate.usingProxy();
    }

    // Unique to HttpsURLConnection
    @Override
    public String getCipherSuite() {
        return httpsURLConnection.getCipherSuite();
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return httpsURLConnection.getHostnameVerifier();
    }

    @Override
    public Certificate[] getLocalCertificates() {
        return httpsURLConnection.getLocalCertificates();
    }

    @Override
    public Principal getLocalPrincipal() {
        return httpsURLConnection.getLocalPrincipal();
    }

    @Override
    public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
        return httpsURLConnection.getPeerPrincipal();
    }

    @Override
    public Certificate[] getServerCertificates() throws SSLPeerUnverifiedException {
        return httpsURLConnection.getServerCertificates();
    }

    @Override
    public SSLSocketFactory getSSLSocketFactory() {
        return httpsURLConnection.getSSLSocketFactory();
    }

    @Override
    public void setHostnameVerifier(HostnameVerifier verifier) {
        httpsURLConnection.setHostnameVerifier(verifier);
    }

    @Override
    public void setSSLSocketFactory(SSLSocketFactory factory) {
        httpsURLConnection.setSSLSocketFactory(factory);
    }


}
