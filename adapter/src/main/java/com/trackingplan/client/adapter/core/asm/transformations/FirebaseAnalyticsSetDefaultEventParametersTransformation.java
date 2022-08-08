// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.transformations;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformation;
import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationFactory;

public class FirebaseAnalyticsSetDefaultEventParametersTransformation extends ReplaceMethodTransformation {

    public FirebaseAnalyticsSetDefaultEventParametersTransformation(String owner, String name, String desc) {
        super(owner, name, desc);
    }

    public static class Factory implements MethodVisitorTransformationFactory {
        public MethodVisitorTransformation newTransformation(String className, String methodName, String methodDesc) {
            return new FirebaseAnalyticsSetDefaultEventParametersTransformation("com/trackingplan/client/sdk/interception/firebase/FirebaseAnalytics", "setDefaultEventParameters", "(Lcom/google/firebase/analytics/FirebaseAnalytics;Landroid/os/Bundle;)V");
        }
    }
}
