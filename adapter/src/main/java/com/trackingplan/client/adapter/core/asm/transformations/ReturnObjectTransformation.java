// Copyright (c) 2021 Trackingplan
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
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "com/trackingplan/client/sdk/interception/urlconnection/TrackingplanUrlConnection", "instrument", "(Ljava/net/URLConnection;)Ljava/net/URLConnection;", false);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, this.returnType);
    }

    @Override
    public boolean replaceMethod(MethodVisitor methodVisitor, int opcode) {
        return false;
    }
}
