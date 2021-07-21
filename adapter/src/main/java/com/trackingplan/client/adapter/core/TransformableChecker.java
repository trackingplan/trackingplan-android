package com.trackingplan.client.adapter.core;

final public class TransformableChecker {
    public static boolean isTransformable(String name) {
        return !name.equals("android")
                && !name.startsWith("android/")
                && !name.startsWith("com/trackingplan/client");
                /*
                && !name.startsWith("com/google/protobuf/")
                && !name.startsWith("com/google/android/apps/common/proguard/")
                && !name.startsWith("com/google/android/datatransport/")
                && !name.startsWith("com/google/android/gms/")
                && !name.startsWith("com/google/common")
                && !name.startsWith("okhttp3/")
                && !name.startsWith("okio/")
                */
    }

    public static boolean isClassInstrumentable(String className) {
        return isTransformable(className.replace('.', '/'));
    }
}
