// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.interception.firebase;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.trackingplan.client.sdk.TrackingplanInstance;
import com.trackingplan.client.sdk.interception.InstrumentRequestBuilder;

final class FirebaseInstrumentRequestBuilder extends InstrumentRequestBuilder {

    private FirebaseAnalytics fa;

    public FirebaseInstrumentRequestBuilder(FirebaseAnalytics fa, TrackingplanInstance tpInstance) {
        super(tpInstance, "firebase");
        this.fa = fa;
    }

    @Override
    protected void beforeBuild() {
        String instanceId = fa.getFirebaseInstanceId();
        builder.setProvider("lib-firebase");
        builder.addHeaderField("Content-Type", "application/json");
        builder.addContextField("firebase_intance_id", instanceId);
    }
}
