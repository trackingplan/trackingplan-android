// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.transform_api;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationFactory;
import com.trackingplan.client.adapter.core.TransformationConfig;

import org.objectweb.asm.Type;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

public class ClassLoaderTransformationConfig extends TransformationConfig {

    private final ClassLoader classLoader;
    private final Set<String> missingClasses = new HashSet<>();

    public ClassLoaderTransformationConfig(ClassLoader classLoader, List<MethodVisitorTransformationConfig> configs) {
        super(configs);
        this.classLoader = classLoader != null ? classLoader : this.getClass().getClassLoader();
    }

    @Override @Nullable
    public MethodVisitorTransformationFactory getMethodVisitorTransformationFactory(String parentClassName, String className, String methodName, String methodDesc) {

        Type classType = Type.getObjectType(className);
        if (classType.getSort() != Type.OBJECT) {
            return null;
        }

        Class<?> clazz;
        try {
            clazz = Class.forName(classType.getClassName(), false, classLoader);
        } catch (Exception | LinkageError ex) {
            return null;
        }

        for (MethodVisitorTransformationConfig config : transformationConfigs) {

            Type configClassType = Type.getObjectType(config.getClassName());
            String configClassName = configClassType.getClassName();

            if (this.missingClasses.contains(configClassName)) {
                continue;
            }

            try {
                Class<?> networkClass = Class.forName(configClassName, false, classLoader);
                boolean isInst = networkClass.isAssignableFrom(clazz);
                boolean methodMatch = config.getMethodName().equals(methodName);
                boolean descMatch = config.getMethodDesc().equals(methodDesc);
                if (isInst && methodMatch && descMatch) {
                    return config.getFactory();
                }
            } catch (Exception | LinkageError ex) {
                this.missingClasses.add(configClassName);
            }
        }

        return null;
    }
}
