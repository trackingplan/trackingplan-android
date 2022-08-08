// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.transformations;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformation;
import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationFactory;

public class UrlConnectionGetContentTransformation extends ReplaceMethodTransformation {

    public UrlConnectionGetContentTransformation(String owner, String name, String desc) {
        super(owner, name, desc);
    }

    public static class Factory implements MethodVisitorTransformationFactory {
        public MethodVisitorTransformation newTransformation(String className, String methodName, String methodDesc) {
            return new UrlConnectionGetContentTransformation("com/trackingplan/client/sdk/interception/urlconnection/TrackingplanUrlConnection", "getContent", "(Ljava/net/URL;)Ljava/lang/Object;");
        }
    }
}
