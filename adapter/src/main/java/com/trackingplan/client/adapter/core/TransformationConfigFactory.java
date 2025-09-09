// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core;

import com.android.build.api.instrumentation.ClassContext;
import com.google.common.collect.ImmutableList;
import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.core.asm.config.FirebaseAnalyticsLogEventIC;
import com.trackingplan.client.adapter.core.asm.config.FirebaseAnalyticsSetAnalyticsCollectionEnabledIC;
import com.trackingplan.client.adapter.core.asm.config.FirebaseAnalyticsSetCurrentScreenIC;
import com.trackingplan.client.adapter.core.asm.config.FirebaseAnalyticsSetDefaultEventParametersIC;
import com.trackingplan.client.adapter.core.asm.config.FirebaseAnalyticsSetUserIdIC;
import com.trackingplan.client.adapter.core.asm.config.FirebaseAnalyticsSetUserPropertyIC;
import com.trackingplan.client.adapter.core.asm.config.OkHttpClientCallEnqueueIC;
import com.trackingplan.client.adapter.core.asm.config.OkHttpClientCallExecuteIC;
import com.trackingplan.client.adapter.core.asm.config.TagManagerDataLayerPushIC;
import com.trackingplan.client.adapter.core.asm.config.UrlConnectionGetContentClassIC;
import com.trackingplan.client.adapter.core.asm.config.UrlConnectionGetContentIC;
import com.trackingplan.client.adapter.core.asm.config.UrlConnectionOpenConnectionIC;
import com.trackingplan.client.adapter.core.asm.config.UrlConnectionOpenConnectionProxyIC;
import com.trackingplan.client.adapter.core.asm.config.UrlConnectionOpenStreamIC;
import com.trackingplan.client.adapter.visitor_api.ClassDataTransformationConfig;

import java.util.List;

public class TransformationConfigFactory {

    private final List<MethodVisitorTransformationConfig> configs = ImmutableList.of(
            new UrlConnectionOpenConnectionIC(),
            new UrlConnectionOpenConnectionProxyIC(),
            new UrlConnectionOpenStreamIC(),
            new UrlConnectionGetContentIC(),
            new UrlConnectionGetContentClassIC(),
            new OkHttpClientCallExecuteIC(),
            new OkHttpClientCallEnqueueIC(),
            new TagManagerDataLayerPushIC(),
            new FirebaseAnalyticsLogEventIC(),
            new FirebaseAnalyticsSetAnalyticsCollectionEnabledIC(),
            new FirebaseAnalyticsSetCurrentScreenIC(),
            new FirebaseAnalyticsSetDefaultEventParametersIC(),
            new FirebaseAnalyticsSetUserIdIC(),
            new FirebaseAnalyticsSetUserPropertyIC()
    );

    public ClassDataTransformationConfig newTransformationConfig(ClassContext classContext) {
        return new ClassDataTransformationConfig(classContext, this.configs);
    }
}
