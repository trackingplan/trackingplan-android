// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core;

import com.android.annotations.NonNull;
import com.android.build.api.dsl.ApplicationExtension;
import com.android.build.api.dsl.BuildType;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableMap;
import com.trackingplan.client.adapter.TrackingplanExtension;
import com.trackingplan.client.adapter.TrackingplanPlugin;

import org.gradle.api.Project;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

final public class AdapterFlagState implements Serializable {

    private static final boolean ADAPTER_ENABLED_DEFAULT = true;

    private final ImmutableMap<String, Optional<Boolean>> parsedProjectProperties;

    private final transient ApplicationExtension androidExt;

    public AdapterFlagState(Project project) {
        androidExt = project.getExtensions().getByType(ApplicationExtension.class);
        parsedProjectProperties = ImmutableMap.of(
                TrackingplanPlugin.TP_ADAPTER_ENABLED_KEY, readProjectPropertyValue(project, TrackingplanPlugin.TP_ADAPTER_ENABLED_KEY)
        );
    }

    public Optional<Boolean> getProjectPropertyValue(@NonNull String propertyKey) {
        return parsedProjectProperties.getOrDefault(propertyKey, Optional.empty());
    }

    public boolean isEnabledFor(String variant, String buildType) {
        return this.instrumentationEnabledFor(androidExt, variant, buildType);
    }

    private boolean instrumentationEnabledFor(ApplicationExtension extension, String variant, String buildType) {

        var logger = TrackingplanPlugin.getLogger();

        var tpGloballyEnabled = getProjectPropertyValue(TrackingplanPlugin.TP_ADAPTER_ENABLED_KEY);

        if (tpGloballyEnabled.isPresent()) {
            var enabledStr = tpGloballyEnabled.get() ? "enabled" : "disabled";
            logger.info(String.format("Trackingplan Adapter Plugin is %s per the Project Property specified in the 'gradle.properties' file.", enabledStr));
            return tpGloballyEnabled.get();
        }

        Optional<Boolean> parsedBuildTypeVal = getBuildTypeExtensionValue(extension, buildType);

        if (parsedBuildTypeVal.isPresent()) {
            var enabledStr = parsedBuildTypeVal.get() ? "enabled" : "disabled";
            logger.info(String.format("Trackingplan Adapter Plugin is %s for %s variant per the Extension Property specified (for buildType=%s) in the 'build.gradle' file.", enabledStr, variant, buildType));
            return parsedBuildTypeVal.get();
        }

        var enabledStr = ADAPTER_ENABLED_DEFAULT ? "enabled" : "disabled";
        logger.info(String.format("Trackingplan Adapter Plugin is %s by default for %s variant.", enabledStr, variant));

        return ADAPTER_ENABLED_DEFAULT;
    }

    private static Optional<Boolean> readProjectPropertyValue(@NonNull Project project, @NonNull String propertyKey) {

        if (!project.hasProperty(propertyKey)) {
            return Optional.empty();
        }

        if (project.property(propertyKey) == null) {
            return Optional.empty();
        }

        var propVal = Objects.requireNonNull(project.property(propertyKey)).toString();
        Optional<Boolean> parsedPropVal = parseBoolean(propVal);
        if (parsedPropVal.isEmpty()) {
            throw new IllegalStateException(String.format("Could not get unknown value '%s' for the project property '%s' defined in the 'gradle.properties' file. Correct format is either '%s=false' or '%s=true'.", propVal, propertyKey, propertyKey, propertyKey));
        }

        return parsedPropVal;
    }

    private static Optional<Boolean> getBuildTypeExtensionValue(ApplicationExtension extension, String buildType) {
        BuildType dslBuildType = extension.getBuildTypes().findByName(buildType);
        if (dslBuildType == null) {
            return Optional.empty();
        }
        TrackingplanExtension buildTypeExt = dslBuildType.getExtensions().findByType(TrackingplanExtension.class);
        if (buildTypeExt == null) {
            return Optional.empty();
        }
        return buildTypeExt.getEnabled();
    }

    private static Optional<Boolean> parseBoolean(String s) {
        if (s != null && (Ascii.equalsIgnoreCase(s, "true") || Ascii.equalsIgnoreCase(s, "false"))) {
            return Optional.of(Boolean.parseBoolean(s));
        }
        return Optional.empty();
    }
}
