// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.interception;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.TrackingplanInstance;
import com.trackingplan.client.sdk.util.AndroidLog;

public abstract class InstrumentRequestBuilder {

    private static final AndroidLog logger = AndroidLog.getInstance();

    // Instruments are disabled until the SDK is initialized from the App
    private static boolean disabled = true;

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
                logger.warn("Request ignored. Looks like Trackingplan SDK is disabled.");
                return;
            }

            builder.setInterceptionModule(instrument);

            final var interceptionContext = InterceptionContext.createInterceptionContext(tpInstance.getContext());
            interceptionContext.instrument = instrument;

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
            AndroidLog.getInstance().warn("Interception failed: " + ex.getMessage());
        }
    }

    protected void beforeBuild() {
        // Empty implementation
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    protected boolean shouldProcessRequest(HttpRequest request) {
        return true;
    }
}
