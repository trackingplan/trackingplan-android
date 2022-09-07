// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.FirebaseAnalyticsSetUserIdTransformation;

public class FirebaseAnalyticsSetUserIdIC extends MethodVisitorTransformationConfig {

    private static final String CLASS_NAME = "com/google/firebase/analytics/FirebaseAnalytics";
    private static final String METHOD_NAME = "setUserId";
    private static final String METHOD_DESC = "(Ljava/lang/String;)V";

    public FirebaseAnalyticsSetUserIdIC() {
        super(new FirebaseAnalyticsSetUserIdTransformation.Factory(), CLASS_NAME, METHOD_NAME, METHOD_DESC);
    }
}
