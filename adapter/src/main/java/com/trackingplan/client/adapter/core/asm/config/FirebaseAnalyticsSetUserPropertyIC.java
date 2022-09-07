// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.FirebaseAnalyticsSetUserPropertyTransformation;

public class FirebaseAnalyticsSetUserPropertyIC extends MethodVisitorTransformationConfig {

    private static final String CLASS_NAME = "com/google/firebase/analytics/FirebaseAnalytics";
    private static final String METHOD_NAME = "setUserProperty";
    private static final String METHOD_DESC = "(Ljava/lang/String;Ljava/lang/String;)V";

    public FirebaseAnalyticsSetUserPropertyIC() {
        super(new FirebaseAnalyticsSetUserPropertyTransformation.Factory(), CLASS_NAME, METHOD_NAME, METHOD_DESC);
    }
}
