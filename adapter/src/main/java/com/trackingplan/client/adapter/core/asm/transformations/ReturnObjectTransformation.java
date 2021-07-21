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
package com.trackingplan.client.adapter.core.asm.transformations;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformation;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ReturnObjectTransformation implements MethodVisitorTransformation {

    private final String returnType;

    public ReturnObjectTransformation(String returnType) {
        this.returnType = returnType;
    }

    @Override
    public void injectBefore(MethodVisitor methodVisitor) {
    }

    @Override
    public void injectAfter(MethodVisitor methodVisitor) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "com/trackingplan/client/sdk/interception/TrackingplanUrlConnection", "instrument", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, this.returnType);
    }

    @Override
    public boolean replaceMethod(MethodVisitor methodVisitor, int opcode) {
        return false;
    }
}
