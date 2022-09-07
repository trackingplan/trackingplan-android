// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.OkHttpClientCallEnqueueTransformation;

public class OkHttpClientCallEnqueueIC extends MethodVisitorTransformationConfig {

    private static final String CLASS_NAME = "okhttp3/Call";
    private static final String METHOD_NAME = "enqueue";
    private static final String METHOD_DESC = "(Lokhttp3/Callback;)V";

    public OkHttpClientCallEnqueueIC() {
        super(new OkHttpClientCallEnqueueTransformation.Factory(), CLASS_NAME, METHOD_NAME, METHOD_DESC);
    }
}
