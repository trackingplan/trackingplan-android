// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.transformations;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformation;
import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationFactory;

public class UrlConnectionOpenStreamTransformation extends ReplaceMethodTransformation {

    public UrlConnectionOpenStreamTransformation(String owner, String name, String desc) {
        super(owner, name, desc);
    }

    public static class Factory implements MethodVisitorTransformationFactory {
        public MethodVisitorTransformation newTransformation(String className, String methodName, String methodDesc) {
            return new UrlConnectionOpenStreamTransformation("com/trackingplan/client/sdk/interception/urlconnection/TrackingplanUrlConnection", "openStream", "(Ljava/net/URL;)Ljava/io/InputStream;");
        }
    }
}
