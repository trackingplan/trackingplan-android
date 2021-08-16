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
package com.trackingplan.client.sdk.interception;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.TrackingplanInstance;
import com.trackingplan.client.sdk.util.AndroidLogger;
import com.trackingplan.client.sdk.util.LogWrapper;

class InstrumentRequestBuilder {

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    final protected HttpRequest.Builder builder;
    final protected TrackingplanInstance tpInstance;

    public InstrumentRequestBuilder(TrackingplanInstance tpInstance) {
        this.builder = new HttpRequest.Builder();
        this.tpInstance = tpInstance;
    }

    public void setUrl(@NonNull String url) {
        builder.setUrl(url);
    }

    public void setHttpMethod(@NonNull String method) {
        builder.setHttpMethod(method);
    }

    public void setUserAgent(@NonNull String userAgent) {
        builder.setUserAgent(userAgent);
    }

    public void addHeaderField(String key, String value) {
        builder.addHeaderField(key, value);
    }

    public void setHttpResponseCode(int responseCode) {
        builder.setHttpResponseCode(responseCode);
    }

    public void setRequestPayload(byte[] payload) {
        builder.setRequestPayload(payload);
    }

    public void setRequestPayloadNumBytes(int numBytes) {
        builder.setRequestPayloadNumBytes(numBytes);
    }

    public void setException(@NonNull Exception ex) {
        builder.setException(ex);
    }

    /**
     * This method is called from instruments used for request interception. So it is the
     * entrypoint to the request processor and delivery system. Note that in HTTP instruments
     * this method will be called from network thread of the consumer app whenever a request
     * is intercepted.
     */
    public void build() {
        try {

            if (tpInstance == null) {
                // Use Log directly because at this point logger is not enabled
                Log.w(LogWrapper.LOG_TAG, "Request ignored. Did you forget to initialize Trackingplan?");
                return;
            }

            if (!tpInstance.getConfig().ignoreContext()) {
                String name = getTopActivityName(tpInstance.getContext());
                if (name != null) {
                    builder.addContextField("activity", name);
                }
            }

            tpInstance.runSync(() -> {

                beforeBuild();

                HttpRequest request = builder.build();
                logger.debug("Request intercepted: " + request);

                if (!shouldProcessRequest(request)) {
                    return;
                }

                // Process requests synchronously in Trackingplan thread
                tpInstance.processRequest(request);
            });

        } catch (Exception ex) {
            AndroidLogger.getInstance().warn("Interception failed: " + ex.getMessage());
        }
    }

    protected void beforeBuild() {
        // Empty implementation
    }

    protected boolean shouldProcessRequest(HttpRequest request) {

        if (request.getProvider().isEmpty()) {
            logger.verbose("Request ignored. Doesn't belong to a supported provider");
            return false;
        }

        return true;
    }

    private String getTopActivityName(@NonNull Context context) {

        String name = null;

        try {
            // Note: Works without any extra permission in Android 5.0+
            // Note: This API is deprecated in newer versions of Android
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
            name = cn.getClassName();

        } catch (Exception ignored) {
            // Fail silently
        }

        return name;
    }
}
