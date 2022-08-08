// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.UrlConnectionTransformation;

import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.net.Proxy;
import java.net.URL;

public class UrlConnectionOpenConnectionProxyIC extends MethodVisitorTransformationConfig {

    private static final String CLASS_NAME;
    private static final String METHOD_NAME;
    private static final String METHOD_DESC;

    static {
        try {
            CLASS_NAME = Type.getInternalName(URL.class);
            Method openConnectionMethod = URL.class.getDeclaredMethod("openConnection", Proxy.class);
            METHOD_NAME = openConnectionMethod.getName();
            METHOD_DESC = Type.getType(openConnectionMethod).getDescriptor();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public UrlConnectionOpenConnectionProxyIC() {
        super(new UrlConnectionTransformation.Factory(), CLASS_NAME, METHOD_NAME, METHOD_DESC);
    }
}
