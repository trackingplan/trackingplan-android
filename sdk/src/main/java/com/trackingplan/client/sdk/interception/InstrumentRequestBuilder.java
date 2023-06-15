// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.interception;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.TrackingplanInstance;
import com.trackingplan.client.sdk.util.AndroidLogger;

public abstract class InstrumentRequestBuilder {

    private static final AndroidLogger logger = AndroidLogger.getInstance();
    private static boolean disabled = false;

    public static void setDisabled(boolean disabled) {
        InstrumentRequestBuilder.disabled = disabled;
    }

    final protected HttpRequest.Builder builder;
    final protected TrackingplanInstance tpInstance;
    final protected String instrument;

    public InstrumentRequestBuilder(TrackingplanInstance tpInstance, @NonNull String instrument) {
        this.builder = new HttpRequest.Builder();
        this.tpInstance = tpInstance;
        this.instrument = instrument;
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

    public void addHeaderField(String key, String value, boolean overwrite) {
        if (!overwrite && builder.hasHeaderField(key)) return;
        builder.addHeaderField(key, value);
    }

    public void setHttpResponseCode(int responseCode) {
        builder.setHttpResponseCode(responseCode);
    }

    public void setRequestPayload(byte[] payload) {
        builder.setRequestPayload(payload);
    }

    public void setRequestPayloadNumBytes(long numBytes) {
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
    public final void build() {
        try {

            if (disabled) {
                return;
            }

            if (tpInstance == null) {
                // Use Log directly because at this point logger is not enabled
                logger.warn("Request ignored. Looks like Trackingplan SDK is disabled.");
                return;
            }

            builder.setInterceptionModule(instrument);

            final var interceptionContext = createInterceptionContext();

            tpInstance.runSync(() -> {

                beforeBuild();

                HttpRequest request = builder.build();
                logger.verbose("Request intercepted: " + request);

                if (!shouldProcessRequest(request)) {
                    return;
                }

                // Process requests synchronously in Trackingplan thread
                tpInstance.processRequest(request, interceptionContext);
            });

        } catch (Exception ex) {
            AndroidLogger.getInstance().warn("Interception failed: " + ex.getMessage());
        }
    }

    protected void beforeBuild() {
        // Empty implementation
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean shouldProcessRequest(HttpRequest request) {
        return true;
    }

    @NonNull
    private InterceptionContext createInterceptionContext() {

        var context = new InterceptionContext();

        context.instrument = instrument;

        String activityName = getTopActivityName(tpInstance.getContext());
        if (activityName != null) {
            context.activityName = activityName;
        }

        return context;
    }

    private String getTopActivityName(@NonNull Context context) {

        String name = null;

        try {
            // NOTE: Works without any extra permission in Android 5.0+
            // NOTE: This API is deprecated in newer versions of Android
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
            name = cn.getClassName();

        } catch (Exception ignored) {
            // Fail silently
        }

        return name;
    }
}
