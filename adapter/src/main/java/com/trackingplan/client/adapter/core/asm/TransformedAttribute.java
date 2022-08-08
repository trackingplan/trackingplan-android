// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ByteVector;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;

final public class TransformedAttribute extends Attribute {

    private final String extra;

    public TransformedAttribute(String extra) {
        super(TransformedAttribute.class.getSimpleName());
        this.extra = extra;
    }

    public boolean isUnknown() {
        return false;
    }

    public boolean isCodeAttribute() {
        return false;
    }

    protected Attribute read(ClassReader cr, int off, int len, char[] buf, int codeOff, Label[] labels) {
        return new TransformedAttribute(cr.readUTF8(off, buf));
    }

    protected ByteVector write(ClassWriter cw, byte[] code, int len, int maxStack, int maxLocals) {
        return (new ByteVector()).putShort(cw.newUTF8(this.extra));
    }
}
