// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.transformations;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformation;
import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationFactory;

import org.objectweb.asm.Type;

import java.net.URLConnection;

public class UrlConnectionTransformation extends ReturnObjectTransformation {

    public UrlConnectionTransformation() {
        super(Type.getType(URLConnection.class).getInternalName());
    }

    public static class Factory implements MethodVisitorTransformationFactory {
        public MethodVisitorTransformation newTransformation(String className, String methodName, String methodDesc) {
            return new UrlConnectionTransformation();
        }
    }
}
