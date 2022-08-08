// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.OkHttpClientCallEnqueueTransformation;

public class OkHttpClientCallEnqueueIC extends MethodVisitorTransformationConfig {
    public OkHttpClientCallEnqueueIC() {
        super(new OkHttpClientCallEnqueueTransformation.Factory(), "okhttp3/Call", "enqueue", "(Lokhttp3/Callback;)V");
    }
}
