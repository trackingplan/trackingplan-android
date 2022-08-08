// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core;

import java.util.Locale;

final public class TransformableChecker {
    public static boolean isTransformable(String name) {
        return !name.equals("android")
                && !name.startsWith("android/")
                && !name.startsWith("com/trackingplan/client")
                && !name.startsWith("com/google/protobuf/")
                && !name.startsWith("com/google/android/apps/common/proguard/")
                && !name.startsWith("com/google/android/datatransport/")
                && !name.startsWith("com/google/common")
                && !name.startsWith("okhttp3/")
                && !name.startsWith("okio/")
                && !name.toLowerCase(Locale.US).startsWith("meta-inf/");
    }

    public static boolean isClassInstrumentable(String className) {
        return isTransformable(className.replace('.', '/'));
    }
}
