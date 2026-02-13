// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core.asm;

import com.trackingplan.client.adapter.TrackingplanPlugin;
import com.trackingplan.client.adapter.core.TransformationConfig;
import com.trackingplan.client.adapter.util.GradleLogger;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.AdviceAdapter;

final public class AdapterClassVisitor extends ClassVisitor {

    private static final GradleLogger logger = TrackingplanPlugin.getLogger();

    private final ClassVisitor nextClassVisitor;
    private final TransformationConfig instrConfig;
    private String currentClassName;

    public AdapterClassVisitor(int asmApiVersion, ClassVisitor nextClassVisitor, TransformationConfig instrConfig) {
        super(asmApiVersion, nextClassVisitor);
        this.nextClassVisitor = nextClassVisitor;
        this.instrConfig = instrConfig;
    }

    public void visit(int version, int access, String className, String signature, String superName, String[] interfaces) {
        logger.debug(String.format("Visit class: %s", className));
        this.currentClassName = className;
        this.nextClassVisitor.visit(version, access, className, signature, superName, interfaces);
    }

    public MethodVisitor visitMethod(int access, String methodName, String methodDesc, String signature, String[] exceptions) {
        MethodVisitor rootMethodVisitor = this.nextClassVisitor.visitMethod(access, methodName, methodDesc, signature, exceptions);
        return new TrackingplanMethodVisitor(this.api, rootMethodVisitor, access, methodName, methodDesc, this.instrConfig, currentClassName);
    }

    public void visitEnd() {
        super.visitEnd();
    }

    static final private class TrackingplanMethodVisitor extends AdviceAdapter {

        private final TransformationConfig instrConfig;
        private final String parentClassName;

        private TrackingplanMethodVisitor(int api, MethodVisitor methodVisitor, int access, String perfMethodName, String perfMethodDesc, TransformationConfig instrConfig, String className) {
            super(api, methodVisitor, access, perfMethodName, perfMethodDesc);
            this.instrConfig = instrConfig;
            this.parentClassName = className;
        }

        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

            MethodVisitorTransformationFactory transformationFactory = this.instrConfig.getMethodVisitorTransformationFactory(parentClassName, owner, name, desc);

            logger.debug(String.format("[%s] Found method call: %s.%s %s", parentClassName, owner, name, desc));

            if (transformationFactory == null) {
                logger.debug(String.format("[%s] Transformation factory not available for %s.%s %s", parentClassName, owner, name, desc));
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                return;
            }

            MethodVisitorTransformation transformation = transformationFactory.newTransformation(owner, name, desc);

            if (transformation == null) {
                logger.debug(String.format("[%s] Transformation not available for %s.%s %s", parentClassName, owner, name));
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                return;
            }

            logger.info(String.format("[%s] Apply transform to call %s.%s -> %s", parentClassName, owner, name, transformation));

            transformation.injectBefore(this.mv);
            if (!transformation.replaceMethod(this.mv, opcode)) {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
            transformation.injectAfter(this.mv);
        }
    }
}
