// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.transform_api;

import com.trackingplan.client.adapter.TrackingplanPlugin;
import com.trackingplan.client.adapter.util.GradleLogger;

import org.objectweb.asm.ClassWriter;

final class AdapterClassWriter extends ClassWriter {

    private static final GradleLogger logger = TrackingplanPlugin.getLogger();

    private final ClassLoader classLoader;

    public AdapterClassWriter(ClassLoader classLoader, int flags) {
        super(flags);
        this.classLoader = classLoader;
    }

    protected String getCommonSuperClass(String type1, String type2) {

        Class<?> class1;
        Class<?> class2;

        try {
            class1 = Class.forName(type1.replace('/', '.'), false, classLoader);
            class2 = Class.forName(type2.replace('/', '.'), false, classLoader);
        } catch (Throwable ex) {
            logger.warn(ex.toString());
            return "java/lang/Object";
        }

        if (class1.isAssignableFrom(class2)) {
            return type1;
        } else if (class2.isAssignableFrom(class1)) {
            return type2;
        } else if (!class1.isInterface() && !class2.isInterface()) {
            do {
                class1 = class1.getSuperclass();
            } while (!class1.isAssignableFrom(class2));
            return class1.getName().replace('.', '/');
        } else {
            return "java/lang/Object";
        }
    }
}
