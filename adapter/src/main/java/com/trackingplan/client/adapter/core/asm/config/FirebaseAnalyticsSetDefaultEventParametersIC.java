// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.FirebaseAnalyticsLogEventTransformation;
import com.trackingplan.client.adapter.core.asm.transformations.FirebaseAnalyticsSetDefaultEventParametersTransformation;

public class FirebaseAnalyticsSetDefaultEventParametersIC extends MethodVisitorTransformationConfig {

    private static final String CLASS_NAME;
    private static final String METHOD_NAME;
    private static final String METHOD_DESC;

    static {
        try {
            // TODO: Add FirebaseAnalytics dependency to get data
            CLASS_NAME = "com/google/firebase/analytics/FirebaseAnalytics";
            METHOD_NAME = "setDefaultEventParameters";
            METHOD_DESC = "(Landroid/os/Bundle;)V";
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public FirebaseAnalyticsSetDefaultEventParametersIC() {
        super(new FirebaseAnalyticsSetDefaultEventParametersTransformation.Factory(), CLASS_NAME, METHOD_NAME, METHOD_DESC);
    }
}
