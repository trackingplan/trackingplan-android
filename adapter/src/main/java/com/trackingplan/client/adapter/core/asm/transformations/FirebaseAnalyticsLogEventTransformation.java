// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.transformations;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformation;
import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationFactory;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.net.URLConnection;

public class FirebaseAnalyticsLogEventTransformation extends ReplaceMethodTransformation {

    public FirebaseAnalyticsLogEventTransformation(String owner, String name, String desc) {
        super(owner, name, desc);
    }

    public static class Factory implements MethodVisitorTransformationFactory {
        public MethodVisitorTransformation newTransformation(String className, String methodName, String methodDesc) {
            return new FirebaseAnalyticsLogEventTransformation("com/trackingplan/client/sdk/interception/firebase/FirebaseAnalytics", "logEvent", "(Lcom/google/firebase/analytics/FirebaseAnalytics;Ljava/lang/String;Landroid/os/Bundle;)V");
        }
    }
}
