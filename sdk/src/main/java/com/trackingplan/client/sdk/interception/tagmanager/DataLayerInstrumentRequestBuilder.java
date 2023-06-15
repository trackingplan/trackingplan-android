package com.trackingplan.client.sdk.interception.tagmanager;

import com.trackingplan.client.sdk.TrackingplanInstance;
import com.trackingplan.client.sdk.interception.InstrumentRequestBuilder;
import com.google.android.gms.tagmanager.DataLayer;

public class DataLayerInstrumentRequestBuilder extends InstrumentRequestBuilder {

    private DataLayer dl;

    public DataLayerInstrumentRequestBuilder(DataLayer dl, TrackingplanInstance tpInstance) {
        super(tpInstance, "google-tagmanager-v4");
        this.dl = dl;
    }

    @Override
    protected void beforeBuild() {
        // TODO: Get containerId from the container
        builder.setProvider("lib-google-tagmanager-v4");
        builder.addHeaderField("Content-Type", "application/json");
    }
}
