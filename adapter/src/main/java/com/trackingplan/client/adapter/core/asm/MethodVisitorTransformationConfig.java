// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm;

public abstract class MethodVisitorTransformationConfig {

    private final MethodVisitorTransformationFactory factory;
    private final String className;
    private final String methodName;
    private final String methodDesc;
    private final String id;

    public MethodVisitorTransformationConfig(MethodVisitorTransformationFactory factory, String className, String methodName, String methodDesc) {
        this.factory = factory;
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.id = getId(className, methodName, methodDesc);
    }

    public MethodVisitorTransformationFactory getFactory() {
        return this.factory;
    }

    public String getClassName() {
        return this.className;
    }

    public String getMethodName() {
        return this.methodName;
    }

    public String getMethodDesc() {
        return this.methodDesc;
    }

    public String getId() {
        return this.id;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof MethodVisitorTransformationConfig)) {
            return false;
        } else {
            MethodVisitorTransformationConfig that = (MethodVisitorTransformationConfig) obj;
            return that.id.equals(this.id);
        }
    }

    public int hashCode() {
        return this.id.hashCode();
    }

    public static String getId(String className, String methodName, String methodDesc) {
        return className + methodName + methodDesc;
    }
}
