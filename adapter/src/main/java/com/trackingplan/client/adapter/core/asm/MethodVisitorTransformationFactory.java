// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm;

public interface MethodVisitorTransformationFactory {
    MethodVisitorTransformation newTransformation(String className, String methodName, String methodDesc);
}
