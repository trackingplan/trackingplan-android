// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.interception;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.TrackingplanInstance;
import com.trackingplan.client.sdk.util.AndroidLogger;

import java.net.URI;
import java.net.URISyntaxException;

public final class HttpInstrumentRequestBuilder extends InstrumentRequestBuilder {

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    public HttpInstrumentRequestBuilder(TrackingplanInstance tpInstance, @NonNull String instrument) {
        super(tpInstance, instrument);
    }

    @Override
    protected boolean shouldProcessRequest(HttpRequest request) {

        if (request.hasConnectionError()) {
            logger.verbose("Request ignored. Request failed locally with error: " + request.getErrorMessage());
            return false;
        }

        if (request.isPayloadTruncated()) {
            logger.verbose("Request ignored. Payload was truncated");
            return false;
        }

        return super.shouldProcessRequest(request);
    }
}
