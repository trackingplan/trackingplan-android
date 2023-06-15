package com.trackingplan.client.adapter.core.asm.config;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.transformations.TagManagerDataLayerPushTransformation;

public class TagManagerDataLayerPushIC extends MethodVisitorTransformationConfig {

    private static final String CLASS_NAME = "com/google/android/gms/tagmanager/DataLayer";
    private static final String METHOD_NAME = "push";
    private static final String METHOD_DESC = "(Ljava/util/Map;)V";

    public TagManagerDataLayerPushIC() {
        super(new TagManagerDataLayerPushTransformation.Factory(), CLASS_NAME, METHOD_NAME, METHOD_DESC);
    }
}