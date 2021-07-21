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

    public boolean equals(Object obj) {
        if (!(obj instanceof MethodVisitorTransformationConfig)) {
            return false;
        } else {
            MethodVisitorTransformationConfig that = (MethodVisitorTransformationConfig)obj;
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
