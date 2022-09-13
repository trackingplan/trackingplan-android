// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.FirebaseAnalyticsSetCurrentScreenTransformation;
import com.trackingplan.client.adapter.core.asm.transformations.FirebaseAnalyticsSetUserPropertyTransformation;

public class FirebaseAnalyticsSetCurrentScreenIC extends MethodVisitorTransformationConfig {

    private static final String CLASS_NAME = "com/google/firebase/analytics/FirebaseAnalytics";
    private static final String METHOD_NAME = "setCurrentScreen";
    private static final String METHOD_DESC = "(Landroid/app/Activity;Ljava/lang/String;Ljava/lang/String;)V";

    public FirebaseAnalyticsSetCurrentScreenIC() {
        super(new FirebaseAnalyticsSetCurrentScreenTransformation.Factory(), CLASS_NAME, METHOD_NAME, METHOD_DESC);
    }
}
