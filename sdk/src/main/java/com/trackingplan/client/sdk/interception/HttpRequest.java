// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.interception;

import android.os.SystemClock;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


/**
 * Immutable class
 */
final public class HttpRequest {

    private String url = "";
    private String method = "GET";
    private String userAgent = "";
    private int responseCode = -1;
    private long payloadSizeBytes = 0;
    private byte[] payloadData = new byte[0]; // Payload truncated
    private final Map<String, String> context = new HashMap<>();
    private final Map<String, String> headers = new HashMap<>();
    private boolean hasError = false;
    private long createdTimeMs = 0;
    private String provider = "";
    private String interceptionModule = "";

    private HttpRequest() {
        // Empty constructor
    }

    @NonNull
    public String getUrl() {
        return url;
    }

    @NonNull
    public String getMethod() {
        return method;
    }

    @NonNull
    public String getUserAgent() {
        return userAgent;
    }

    @NonNull
    public String getProvider() {
        return provider;
    }

    public void setProvider(@NonNull String provider) {
        this.provider = provider;
    }

    @NonNull
    public String getInterceptionModule() {
        return interceptionModule;
    }

    /**
     * Gets the status code of the intercepted HTTP request.
     * Return -1 if no code can be discerned. For instance,
     * when the connection failed or the originator of the
     * request didn't read the response.
     *
     * @return the HTTP Status-Code, or -1
     */
    public int getResponseCode() {
        return responseCode;
    }

    public long getPayloadSizeBytes() {
        return payloadSizeBytes;
    }

    public byte[] getPayloadData() {
        return payloadData;
    }

    public boolean isPayloadTruncated() {
        return payloadData.length < payloadSizeBytes;
    }

    public boolean hasConnectionError() {
        return hasError;
    }

    /**
     * Get creation time in milliseconds relative to boot time (including time spent in sleep).
     *
     * @return milliseconds relative to boot time
     */
    public long getCreatedTimeMs() {
        return createdTimeMs;
    }

    @NonNull
    public String getErrorMessage() {
        return StringUtils.getNonNullOrDefault(context.get("request_error"), "");
    }

    @NonNull
    public Map<String, String> getContext() {
        return Collections.unmodifiableMap(context);
    }

    public void addContextField(@NonNull String name, @NonNull String value) {
        context.put(name, value);
    }

    @NonNull
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    @Override
    @NonNull
    public String toString() {

        String payloadStr = "Empty";

        if (payloadSizeBytes > 0) {
            payloadStr = new String(payloadData, StandardCharsets.UTF_8);
        }

        return "HttpRequest{" +
                "method='" + method + '\'' +
                ", failed='" + (hasError ? "Yes" : "No") + '\'' +
                ", responseCode='" + responseCode + '\'' +
                ", provider='" + provider + '\'' +
                ", intercepted by='" + interceptionModule + '\'' +
                ", payloadSize='" + payloadSizeBytes + '\'' +
                ", url='" + url + '\'' +
                ", created_at='" + createdTimeMs + '\'' +
                ", context='" + context.toString() + '\'' +
                ", headers='" + headers.toString() + '\'' +
                ", payload='" + payloadStr + '\'' +
                '}';
    }

    public static class Builder {

        private HttpRequest request = new HttpRequest();

        public Builder setUrl(@NonNull String url) {
            request.url = url;
            return this;
        }

        public Builder setHttpMethod(@NonNull String method) {
            request.method = method;
            return this;
        }

        public Builder setUserAgent(@NonNull String userAgent) {
            request.userAgent = userAgent;
            return this;
        }

        public Builder setHttpResponseCode(int responseCode) {
            request.responseCode = responseCode;
            return this;
        }

        public Builder setRequestPayload(byte[] payload) {
            request.payloadData = payload;
            return this;
        }

        public Builder setRequestPayloadNumBytes(long numBytes) {
            request.payloadSizeBytes = numBytes;
            return this;
        }

        public Builder setException(@NonNull Exception ex) {
            request.hasError = true;
            String message = ex.getMessage();
            if (message != null) {
                request.context.put("request_error", ex.getMessage());
            }
            return this;
        }

        public Builder addContextField(@NonNull String name, @NonNull String value) {
            request.context.put(name, value);
            return this;
        }

        public Builder addHeaderField(String key, String value) {
            String keyStr = key != null ? key.toLowerCase(Locale.ROOT) : null;
            request.headers.put(keyStr, value);
            return this;
        }

        public boolean hasHeaderField(String key) {
            return request.headers.containsKey(key);
        }

        public Builder setProvider(@NonNull String provider) {
            request.provider = provider;
            return this;
        }

        public Builder setInterceptionModule(@NonNull String interceptionModule) {
            request.interceptionModule = interceptionModule;
            return this;
        }

        public HttpRequest build() {
            HttpRequest result = this.request;
            result.createdTimeMs = SystemClock.elapsedRealtime();
            reset();
            return result;
        }

        private void reset() {
            this.request = new HttpRequest();
        }
    }
}
