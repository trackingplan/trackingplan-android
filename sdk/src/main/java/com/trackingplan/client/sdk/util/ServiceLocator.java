// Copyright (c) 2024 Trackingplan
package com.trackingplan.client.sdk.util;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;

public final class ServiceLocator {

    private final static Map<Class<?>, Object> sharedInstances = new HashMap<>();

    synchronized public static <T> void registerSharedInstance(
            @NonNull Class<T> clazz,
            @NonNull final T instance
    ) {
        registerSharedInstance(clazz, instance, false);
    }

    @VisibleForTesting
    synchronized public static <T> void registerSharedInstance(
            @NonNull Class<T> clazz,
            @NonNull final T instance,
            boolean override
    ) {
        if (!clazz.isAssignableFrom(instance.getClass())) {
            throw new RuntimeException(instance.getClass().getCanonicalName() + " does not implements " + clazz.getCanonicalName());
        }

        if (sharedInstances.containsKey(clazz) && !override) {
            return;
        }

        sharedInstances.put(clazz, instance);
    }

    @NonNull
    public static <T> T getSharedInstance(@NonNull Class<T> clazz) {
        final var instance = sharedInstances.getOrDefault(clazz, null);
        if (instance == null) {
            throw new RuntimeException("No shared instance found for " + clazz.getCanonicalName());
        }
        //noinspection unchecked
        return (T) instance;
    }
}
