// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.UrlConnectionOpenStreamTransformation;

import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.net.URL;

public class UrlConnectionOpenStreamIC extends MethodVisitorTransformationConfig {

    private static final String CLASS_NAME;
    private static final String METHOD_NAME;
    private static final String METHOD_DESC;

    static {
        try {
            CLASS_NAME = Type.getInternalName(URL.class);
            Method openStreamMethod = URL.class.getDeclaredMethod("openStream");
            METHOD_NAME = openStreamMethod.getName();
            METHOD_DESC = Type.getType(openStreamMethod).getDescriptor();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public UrlConnectionOpenStreamIC() {
        super(new UrlConnectionOpenStreamTransformation.Factory(), CLASS_NAME, METHOD_NAME, METHOD_DESC);
    }
}
