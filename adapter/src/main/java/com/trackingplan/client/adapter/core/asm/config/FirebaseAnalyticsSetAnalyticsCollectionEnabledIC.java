// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.FirebaseAnalyticsSetAnalyticsCollectionEnabledTransformation;

public class FirebaseAnalyticsSetAnalyticsCollectionEnabledIC extends MethodVisitorTransformationConfig {

    private static final String CLASS_NAME = "com/google/firebase/analytics/FirebaseAnalytics";
    private static final String METHOD_NAME = "setAnalyticsCollectionEnabled";
    private static final String METHOD_DESC = "(Z)V";

    public FirebaseAnalyticsSetAnalyticsCollectionEnabledIC() {
        super(new FirebaseAnalyticsSetAnalyticsCollectionEnabledTransformation.Factory(), CLASS_NAME, METHOD_NAME, METHOD_DESC);
    }
}
