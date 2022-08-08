// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.FirebaseAnalyticsLogEventTransformation;
import com.trackingplan.client.adapter.core.asm.transformations.FirebaseAnalyticsSetAnalyticsCollectionEnabledTransformation;

public class FirebaseAnalyticsSetAnalyticsCollectionEnabledIC extends MethodVisitorTransformationConfig {

    private static final String CLASS_NAME;
    private static final String METHOD_NAME;
    private static final String METHOD_DESC;

    static {
        try {
            // TODO: Add FirebaseAnalytics dependency to get data
            CLASS_NAME = "com/google/firebase/analytics/FirebaseAnalytics";
            METHOD_NAME = "setAnalyticsCollectionEnabled";
            METHOD_DESC = "(Z)V";
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public FirebaseAnalyticsSetAnalyticsCollectionEnabledIC() {
        super(new FirebaseAnalyticsSetAnalyticsCollectionEnabledTransformation.Factory(), CLASS_NAME, METHOD_NAME, METHOD_DESC);
    }
}
