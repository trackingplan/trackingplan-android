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

    @Nullable
    public MethodVisitorTransformationFactory getMethodVisitorTransformationFactory(String className, String methodName, String methodDesc) {

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
