// Copyright (c) 2022 Trackingplan
package com.trackingplan.client.adapter.util;

import com.android.annotations.NonNull;
import com.android.build.api.AndroidPluginVersion;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.trackingplan.client.adapter.core.exceptions.CannotObtainAGPVersionException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.gradle.api.Project;

public class SimpleAGPVersion implements Comparable<SimpleAGPVersion> {

    private final AndroidPluginVersion version;

    public SimpleAGPVersion(int major, int minor, int micro) {
        version = new AndroidPluginVersion(major, minor, micro);
    }

    @Override
    public int compareTo(@NonNull SimpleAGPVersion simpleAGPVersion) {
        return version.compareTo(simpleAGPVersion.version);
    }

    public static SimpleAGPVersion getAndroidGradlePluginVersion(
            Project project
    ) {
        AndroidComponentsExtension androidComponents = project
                .getExtensions()
                .findByType(AndroidComponentsExtension.class);

        if (androidComponents == null) {
            throw new CannotObtainAGPVersionException();
        }

        AndroidPluginVersion pluginVersion = androidComponents.getPluginVersion();
        return new SimpleAGPVersion(
                pluginVersion.getMajor(),
                pluginVersion.getMinor(),
                pluginVersion.getMicro()
        );
    }

    private static List<Integer> splitVersionParts(String version) {
        String[] splits = version.split("\\.");
        return Arrays.stream(splits)
                .map(Integer::valueOf)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return version.toString();
    }
}
