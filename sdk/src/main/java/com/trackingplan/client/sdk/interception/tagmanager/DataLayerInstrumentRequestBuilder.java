package com.trackingplan.client.sdk.interception.tagmanager;

import com.google.android.gms.tagmanager.DataLayer;
import com.trackingplan.client.sdk.TrackingplanInstance;
import com.trackingplan.client.sdk.interception.InstrumentRequestBuilder;

public class DataLayerInstrumentRequestBuilder extends InstrumentRequestBuilder {

    private DataLayer dl;

    public DataLayerInstrumentRequestBuilder(DataLayer dl, TrackingplanInstance tpInstance) {
        super(tpInstance, "google-tagmanager-v4");
        this.dl = dl;
    }

    @Override
    protected void beforeBuild() {
        // TODO: Get containerId from the container
        // https://developers.google.com/tag-platform/tag-manager/android/v4
        builder.setProvider("lib-google-tagmanager-v4");
        builder.addHeaderField("Content-Type", "application/json");
    }
}
