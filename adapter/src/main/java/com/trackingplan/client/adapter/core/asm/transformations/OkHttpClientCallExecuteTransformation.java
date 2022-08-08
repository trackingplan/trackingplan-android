// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.transformations;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformation;
import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationFactory;

public class OkHttpClientCallExecuteTransformation extends ReplaceMethodTransformation {

    public OkHttpClientCallExecuteTransformation(String owner, String name, String desc) {
        super(owner, name, desc);
    }

    public static class Factory implements MethodVisitorTransformationFactory {
        @Override
        public MethodVisitorTransformation newTransformation(String className, String methodName, String methodDesc) {
            return new OkHttpClientCallExecuteTransformation("com/trackingplan/client/sdk/interception/okhttp/TrackingplanOkHttpClient", "execute", "(Lokhttp3/Call;)Lokhttp3/Response;");
        }
    }
}
