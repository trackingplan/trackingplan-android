// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core;

import com.android.annotations.NonNull;
import com.android.build.api.dsl.BuildType;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;
import com.google.common.base.Ascii;
import com.google.common.collect.ImmutableMap;
import com.trackingplan.client.adapter.TrackingplanExtension;
import com.trackingplan.client.adapter.TrackingplanPlugin;

import org.gradle.api.Project;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final public class AdapterFlagState implements Serializable {

    private static final boolean ADAPTER_ENABLED_DEFAULT = true;

    private final Map<String, Optional<Boolean>> parsedProjectProperties;

    private final transient AppExtension androidExt;

    private final Map<String, Boolean> variantToInstrumentationEnabledMap = new HashMap<>();

    public AdapterFlagState(Project project) {
        androidExt = project.getExtensions().getByType(AppExtension.class);
        parsedProjectProperties = ImmutableMap.of(
                TrackingplanPlugin.TP_ADAPTER_ENABLED_KEY, readProjectPropertyValue(project, TrackingplanPlugin.TP_ADAPTER_ENABLED_KEY)
        );
        androidExt.getApplicationVariants().all(variant -> updateInstrumentationEnabledFor(androidExt, variant));
    }

    public Optional<Boolean> getProjectPropertyValue(@NonNull String propertyKey) {
        return parsedProjectProperties.getOrDefault(propertyKey, Optional.empty());
    }

    public boolean isEnabledFor(String variant) {
        return this.variantToInstrumentationEnabledMap.getOrDefault(variant, ADAPTER_ENABLED_DEFAULT);
    }

    public boolean isEnabledFor(String variant, String buildType) {
        return this.instrumentationEnabledFor(androidExt, variant, buildType);
    }

    public Map<String, Boolean> getVariantToInstrumentationEnabledMap() {
        return this.variantToInstrumentationEnabledMap;
    }

    private void updateInstrumentationEnabledFor(AppExtension extension, ApplicationVariant applicationVariant) {
        String variant = applicationVariant.getName();
        this.variantToInstrumentationEnabledMap.put(variant, this.instrumentationEnabledFor(extension, variant, applicationVariant.getBuildType().getName()));
    }

    private boolean instrumentationEnabledFor(AppExtension extension, String variant, String buildType) {

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

    private static Optional<Boolean> getBuildTypeExtensionValue(AppExtension extension, String buildType) {
        BuildType dslBuildType = extension.getBuildTypes().getByName(buildType);
        TrackingplanExtension buildTypeExt = dslBuildType.getExtensions().getByType(TrackingplanExtension.class);
        return buildTypeExt.getEnabled();
    }

    private static Optional<Boolean> parseBoolean(String s) {
        if (s != null && (Ascii.equalsIgnoreCase(s, "true") || Ascii.equalsIgnoreCase(s, "false"))) {
            return Optional.of(Boolean.parseBoolean(s));
        }
        return Optional.empty();
    }
}
