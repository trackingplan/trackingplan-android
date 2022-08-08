// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.OkHttpClientCallExecuteTransformation;

public class OkHttpClientCallExecuteIC extends MethodVisitorTransformationConfig {
    public OkHttpClientCallExecuteIC() {
        super(new OkHttpClientCallExecuteTransformation.Factory(), "okhttp3/Call", "execute", "()Lokhttp3/Response;");
    }
}
