package com.trackingplan.client.sdk.interception;

import androidx.annotation.NonNull;

import com.trackingplan.client.sdk.TrackingplanInstance;
import com.trackingplan.client.sdk.util.AndroidLogger;

import java.net.URI;
import java.net.URISyntaxException;

final class HttpInstrumentRequestBuilder extends InstrumentRequestBuilder {

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    private String url = "";

    public HttpInstrumentRequestBuilder(TrackingplanInstance tpInstance) {
        super(tpInstance);
    }

    @Override
    public void setUrl(@NonNull String url) {
        super.setUrl(url);
        this.url = url;
    }

    @Override
    protected void beforeBuild() {
        String provider = tpInstance.getProviders().get(getDomain(url));
        if (provider != null) {
            builder.setProvider(provider);
        }
        this.url = "";
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

    @NonNull
    private static String getDomain(@NonNull String url) {

        if (url.isEmpty()) {
            return "";
        }

        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (URISyntaxException e) {
            return "";
        }
    }
}
