package com.trackingplan.client.sdk.interception;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.trackingplan.client.sdk.TrackingplanInstance;

final class FirebaseInstrumentRequestBuilder extends InstrumentRequestBuilder {

    private FirebaseAnalytics fa;

    public FirebaseInstrumentRequestBuilder(FirebaseAnalytics fa, TrackingplanInstance tpInstance) {
        super(tpInstance);
        this.fa = fa;
    }

    @Override
    protected void beforeBuild() {
        String instanceId = fa.getFirebaseInstanceId();
        builder.setProvider("lib-firebase");
        builder.addContextField("firebase_intance_id", instanceId);
    }
}
