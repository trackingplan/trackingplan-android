// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm;

import org.objectweb.asm.MethodVisitor;

public interface MethodVisitorTransformation {
    void injectBefore(MethodVisitor methodVisitor);
    void injectAfter(MethodVisitor methodVisitor);
    boolean replaceMethod(MethodVisitor methodVisitor, int opcode);
}
