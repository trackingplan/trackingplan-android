// Copyright (c) 2022 Trackingplan
package com.trackingplan.client.adapter.util;

import com.android.Version;
import com.android.annotations.NonNull;
import com.android.ide.common.repository.GradleVersion;
import com.trackingplan.client.adapter.core.exceptions.CannotObtainAGPVersionException;

public class SimpleAGPVersion implements Comparable<SimpleAGPVersion> {

    final private GradleVersion version;

    public SimpleAGPVersion(int major, int minor, int micro) {
        version = new GradleVersion(major, minor, micro);
    }

    @Override
    public int compareTo(@NonNull SimpleAGPVersion simpleAGPVersion) {
        return version.compareTo(simpleAGPVersion.version);
    }

    public static SimpleAGPVersion getAndroidGradlePluginVersion() {
        final var agpVersion = GradleVersion.tryParseAndroidGradlePluginVersion(Version.ANDROID_GRADLE_PLUGIN_VERSION);
        if (agpVersion == null) {
            throw new CannotObtainAGPVersionException();
        }
        return new SimpleAGPVersion(agpVersion.getMajor(), agpVersion.getMinor(), agpVersion.getMicro());
    }

    @Override
    public String toString() {
        return version.toString();
    }
}
