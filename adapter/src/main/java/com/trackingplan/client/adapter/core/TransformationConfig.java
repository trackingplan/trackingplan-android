// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationFactory;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

public abstract class TransformationConfig {

    protected final List<MethodVisitorTransformationConfig> transformationConfigs;

    public TransformationConfig(List<MethodVisitorTransformationConfig> transformationConfigs) {
        this.transformationConfigs = Collections.unmodifiableList(transformationConfigs);
    }

    @Nullable
    public MethodVisitorTransformationFactory getMethodVisitorTransformationFactory(
            String parentClassName,
            String className,
            String methodName,
            String methodDesc
    ) {
        return null;
    }
}
