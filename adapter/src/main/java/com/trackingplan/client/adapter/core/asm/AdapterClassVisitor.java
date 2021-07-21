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

import com.trackingplan.client.adapter.core.TransformationConfig;
import com.trackingplan.client.adapter.core.exceptions.AlreadyTransformedException;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

final public class AdapterClassVisitor extends ClassVisitor {

    private final ClassVisitor classVisitor;
    private final TransformationConfig instrConfig;
    private boolean ending;

    public AdapterClassVisitor(int asmApiVersion, ClassVisitor classVisitor, TransformationConfig instrConfig) {
        super(asmApiVersion, classVisitor);
        this.classVisitor = classVisitor;
        this.instrConfig = instrConfig;
    }

    public void visit(int version, int access, String className, String signature, String superName, String[] interfaces) {
        this.classVisitor.visit(version, access, className, signature, superName, interfaces);
    }

    public void visitAttribute(Attribute attribute) {
        this.classVisitor.visitAttribute(attribute);
        if (!this.ending && attribute instanceof TransformedAttribute) {
            throw new AlreadyTransformedException();
        }
    }

    public MethodVisitor visitMethod(int access, String methodName, String methodDesc, String signature, String[] exceptions) {
        MethodVisitor rootMethodVisitor = this.classVisitor.visitMethod(access, methodName, methodDesc, signature, exceptions);
        return new TrackingplanMethodVisitor(this.api, rootMethodVisitor, access, methodName, methodDesc, this.instrConfig);
    }

    public void visitEnd() {
        this.ending = true;
        this.visitAttribute(new TransformedAttribute("instrumented"));
        super.visitEnd();
    }

    static final private class TrackingplanMethodVisitor extends AdviceAdapter {

        private final TransformationConfig instrConfig;

        protected TrackingplanMethodVisitor(int api, MethodVisitor methodVisitor, int access, String perfMethodName, String perfMethodDesc, TransformationConfig instrConfig) {
            super(api, methodVisitor, access, perfMethodName, perfMethodDesc);
            this.instrConfig = instrConfig;
        }

        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

            MethodVisitorTransformationFactory transformationFactory = this.instrConfig.getMethodVisitorTransformationFactory(owner, name, desc);

            if (transformationFactory == null) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                return;
            }

            MethodVisitorTransformation transformation = transformationFactory.newTransformation(owner, name, desc);

            if (transformation == null) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                return;
            }

            transformation.injectBefore(this.mv);
            if (!transformation.replaceMethod(this.mv, opcode)) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
            transformation.injectAfter(this.mv);
        }
    }
}
