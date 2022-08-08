// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.visitor_api;

import com.android.build.api.instrumentation.ClassContext;
import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationFactory;
import com.trackingplan.client.adapter.core.TransformationConfig;

import org.objectweb.asm.Type;

import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

public class ClassDataTransformationConfig extends TransformationConfig {

    private final ClassContext classContext;

    public ClassDataTransformationConfig(ClassContext classContext, List<MethodVisitorTransformationConfig> configs) {
        super(configs);
        this.classContext = classContext;
    }

    @Override @Nullable
    public MethodVisitorTransformationFactory getMethodVisitorTransformationFactory(String parentClassName, String className, String methodName, String methodDesc) {

        Type classType = Type.getObjectType(className);
        if (classType.getSort() != Type.OBJECT) {
            return null;
        }

        var classData = this.classContext.loadClassData(className.replace('/', '.'));
        if (classData == null) {
            return null;
        }

        Iterator<?> configIterator = this.transformationConfigs.iterator();

        MethodVisitorTransformationConfig transformationConfig;
        boolean isInstance;
        boolean methodMatch;
        boolean descMatch;

        do {
            if (!configIterator.hasNext()) {
                return null;
            }

            transformationConfig = (MethodVisitorTransformationConfig) configIterator.next();
            Type networkClassType = Type.getObjectType(transformationConfig.getClassName());
            String networkClassName = networkClassType.getClassName();
            isInstance = classData.getClassName().equals(networkClassName) || classData.getInterfaces().contains(networkClassName) || classData.getSuperClasses().contains(networkClassName);
            methodMatch = transformationConfig.getMethodName().equals(methodName);
            descMatch = transformationConfig.getMethodDesc().equals(methodDesc);
        } while (!isInstance || !methodMatch || !descMatch);

        return transformationConfig.getFactory();
    }
}
