// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.transformations;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformation;
import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationFactory;

public class FirebaseAnalyticsSetCurrentScreenTransformation extends ReplaceMethodTransformation {

    public FirebaseAnalyticsSetCurrentScreenTransformation(String owner, String name, String desc) {
        super(owner, name, desc);
    }

    public static class Factory implements MethodVisitorTransformationFactory {
        public MethodVisitorTransformation newTransformation(String className, String methodName, String methodDesc) {
            return new FirebaseAnalyticsSetCurrentScreenTransformation("com/trackingplan/client/sdk/interception/firebase/FirebaseAnalytics", "setCurrentScreen", "(Lcom/google/firebase/analytics/FirebaseAnalytics;Landroid/app/Activity;Ljava/lang/String;Ljava/lang/String;)V");
        }
    }
}
