// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.UrlConnectionGetContentClassTransformation;

import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.net.URL;

public class UrlConnectionGetContentClassIC extends MethodVisitorTransformationConfig {

    private static final String CLASS_NAME;
    private static final String METHOD_NAME;
    private static final String METHOD_DESC;

    static {
        try {
            CLASS_NAME = Type.getInternalName(URL.class);
            Method getContentMethod = URL.class.getDeclaredMethod("getContent", Class[].class);
            METHOD_NAME = getContentMethod.getName();
            METHOD_DESC = Type.getType(getContentMethod).getDescriptor();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public UrlConnectionGetContentClassIC() {
        super(new UrlConnectionGetContentClassTransformation.Factory(), CLASS_NAME, METHOD_NAME, METHOD_DESC);
    }
}
