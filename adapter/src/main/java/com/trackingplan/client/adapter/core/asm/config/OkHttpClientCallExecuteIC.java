// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.OkHttpClientCallExecuteTransformation;

public class OkHttpClientCallExecuteIC extends MethodVisitorTransformationConfig {

    private static final String CLASS_NAME = "okhttp3/Call";
    private static final String METHOD_NAME = "execute";
    private static final String METHOD_DESC = "()Lokhttp3/Response;";

    public OkHttpClientCallExecuteIC() {
        super(new OkHttpClientCallExecuteTransformation.Factory(), CLASS_NAME, METHOD_NAME, METHOD_DESC);
    }
}
