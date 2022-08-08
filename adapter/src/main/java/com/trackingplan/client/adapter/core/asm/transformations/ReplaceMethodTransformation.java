// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm.transformations;

import com.trackingplan.client.adapter.core.asm.MethodVisitorTransformation;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class ReplaceMethodTransformation implements MethodVisitorTransformation {

    private final String owner;
    private final String name;
    private final String desc;

    public ReplaceMethodTransformation(String owner, String name, String desc) {
        this.owner = owner;
        this.name = name;
        this.desc = desc;
    }

    @Override
    public void injectBefore(MethodVisitor methodVisitor) {
    }

    @Override
    public void injectAfter(MethodVisitor methodVisitor) {
    }

    @Override
    public boolean replaceMethod(MethodVisitor methodVisitor, int opcode) {
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, this.owner, this.name, this.desc, false);
        return true;
    }

    public String toString() {
        return "[" + this.getClass().getSimpleName() + " : " + this.owner + " : " + this.name + " : " + this.desc + "]";
    }
}
