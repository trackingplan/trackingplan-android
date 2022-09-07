// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.FirebaseAnalyticsSetDefaultEventParametersTransformation;

public class FirebaseAnalyticsSetDefaultEventParametersIC extends MethodVisitorTransformationConfig {

    private static final String CLASS_NAME = "com/google/firebase/analytics/FirebaseAnalytics";
    private static final String METHOD_NAME = "setDefaultEventParameters";
    private static final String METHOD_DESC = "(Landroid/os/Bundle;)V";

    public FirebaseAnalyticsSetDefaultEventParametersIC() {
        super(new FirebaseAnalyticsSetDefaultEventParametersTransformation.Factory(), CLASS_NAME, METHOD_NAME, METHOD_DESC);
    }
}
