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
package com.trackingplan.client.adapter.visitor_api;

import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
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

    @Nullable
    public MethodVisitorTransformationFactory getMethodVisitorTransformationFactory(String className, String methodName, String methodDesc) {

        Type classType = Type.getObjectType(className);
        if (classType.getSort() != 10) {
            return null;
        }

        ClassData classData = this.classContext.loadClassData(className.replace('/', '.'));
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
