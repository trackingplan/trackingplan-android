// MIT License
//
// Copyright (c) 2021 Trackingplan
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package com.trackingplan.client.adapter.core;

import com.android.build.api.instrumentation.ClassContext;
import com.google.common.collect.ImmutableList;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformationConfig;
import com.trackingplan.client.adapter.visitor_api.ClassDataTransformationConfig;
import com.trackingplan.client.adapter.transform_api.ClassLoaderTransformationConfig;
import com.trackingplan.client.adapter.core.asm.config.UrlConnectionGetContentClassIC;
import com.trackingplan.client.adapter.core.asm.config.UrlConnectionGetContentIC;
import com.trackingplan.client.adapter.core.asm.config.UrlConnectionOpenConnectionIC;
import com.trackingplan.client.adapter.core.asm.config.UrlConnectionOpenConnectionProxyIC;
import com.trackingplan.client.adapter.core.asm.config.UrlConnectionOpenStreamIC;

import java.util.List;

public class TransformationConfigFactory {

    private final List<MethodVisitorTransformationConfig> configs = ImmutableList.of(
            new UrlConnectionOpenConnectionIC(),
            new UrlConnectionOpenConnectionProxyIC(),
            new UrlConnectionOpenStreamIC(),
            new UrlConnectionGetContentIC(),
            new UrlConnectionGetContentClassIC()
    );

    public ClassLoaderTransformationConfig newClassLoaderTransformationConfig(ClassLoader classLoader) {
        return new ClassLoaderTransformationConfig(classLoader, this.configs);
    }

    public ClassDataTransformationConfig newClassDataTransformationConfig(ClassContext classContext) {
        return new ClassDataTransformationConfig(classContext, this.configs);
    }
}
