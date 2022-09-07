// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.FirebaseAnalyticsLogEventTransformation;

public class FirebaseAnalyticsLogEventIC extends MethodVisitorTransformationConfig {

    private static final String CLASS_NAME = "com/google/firebase/analytics/FirebaseAnalytics";
    private static final String METHOD_NAME = "logEvent";
    private static final String METHOD_DESC = "(Ljava/lang/String;Landroid/os/Bundle;)V";

    public FirebaseAnalyticsLogEventIC() {
        super(new FirebaseAnalyticsLogEventTransformation.Factory(), CLASS_NAME, METHOD_NAME, METHOD_DESC);
    }
}
