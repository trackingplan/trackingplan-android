// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.core;

import com.android.annotations.NonNull;
import com.android.build.api.dsl.BuildType;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;
import com.android.builder.model.ProductFlavor;
import com.google.common.base.Ascii;
import com.trackingplan.client.adapter.TrackingplanExtension;
import com.trackingplan.client.adapter.TrackingplanPlugin;

import org.gradle.api.Project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

final public class AdapterFlagState implements Serializable {

    private static final boolean ADAPTER_ENABLED_DEFAULT = true;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Map<String, Optional<Boolean>> parsedProjectProperties;
    private final transient AppExtension androidExt;

    private final Map<String, Boolean> variantToInstrumentationEnabledMap = new HashMap<>();

    public AdapterFlagState(Project project) {
        this.androidExt = project.getExtensions().getByType(AppExtension.class);
        parsedProjectProperties = Map.of(
                TrackingplanPlugin.TP_ADAPTER_ENABLED_KEY, readProjectPropertyValue(project, TrackingplanPlugin.TP_ADAPTER_ENABLED_KEY),
                TrackingplanPlugin.TP_ADAPTER_USE_ASM_CLASS_VISITOR_KEY, readProjectPropertyValue(project, TrackingplanPlugin.TP_ADAPTER_USE_ASM_CLASS_VISITOR_KEY)
        );
        androidExt.getApplicationVariants().all(this::updateInstrumentationEnabledFor);
    }

    public Optional<Boolean> getProjectPropertyValue(@NonNull String propertyKey) {
        return parsedProjectProperties.getOrDefault(propertyKey, Optional.empty());
    }

    public boolean isEnabledFor(String variant) {
        return this.variantToInstrumentationEnabledMap.getOrDefault(variant, ADAPTER_ENABLED_DEFAULT);
    }

    public boolean isEnabledFor(String variant, String buildType, List<String> flavors) {
        return this.instrumentationEnabledFor(variant, buildType, flavors);
    }

    public Map<String, Boolean> getVariantToInstrumentationEnabledMap() {
        return this.variantToInstrumentationEnabledMap;
    }

    private void updateInstrumentationEnabledFor(ApplicationVariant applicationVariant) {
        String variant = applicationVariant.getName();
        List<String> productFlavors = new ArrayList<>();

        for (ProductFlavor flavor : applicationVariant.getProductFlavors()) {
            productFlavors.add(flavor.getName());
        }

        this.variantToInstrumentationEnabledMap.put(variant, this.instrumentationEnabledFor(variant, applicationVariant.getBuildType().getName(), productFlavors));
    }

    private boolean instrumentationEnabledFor(String variant, String buildType, List<String> flavors) {

        var logger = TrackingplanPlugin.getLogger();

        var tpGloballyEnabled = getProjectPropertyValue(TrackingplanPlugin.TP_ADAPTER_ENABLED_KEY);

        if (tpGloballyEnabled.isPresent()) {
            var enabledStr = tpGloballyEnabled.get() ? "enabled" : "disabled";
            logger.info(String.format("Trackingplan Adapter Plugin is %s per the Project Property specified in the 'gradle.properties' file.", enabledStr));
            return tpGloballyEnabled.get();
        }

        Optional<Boolean> parsedBuildTypeVal = getBuildTypeExtensionValue(buildType);

        if (parsedBuildTypeVal.isPresent()) {
            var enabledStr = parsedBuildTypeVal.get() ? "enabled" : "disabled";
            logger.info(String.format("Trackingplan Adapter Plugin is %s for %s variant per the Extension Property specified (for buildType=%s) in the 'build.gradle' file.", enabledStr, variant, buildType));
            return parsedBuildTypeVal.get();
        }

        var enabledStr = ADAPTER_ENABLED_DEFAULT ? "enabled" : "disabled";
        logger.info(String.format("Trackingplan Adapter Plugin is %s by default for %s variant.", enabledStr, variant));

        return ADAPTER_ENABLED_DEFAULT;
    }

    private Optional<Boolean> readProjectPropertyValue(@NonNull Project project, @NonNull String propertyKey) {

        if (!project.hasProperty(propertyKey)) {
            return Optional.empty();
        }

        if (project.property(propertyKey) == null) {
            return Optional.empty();
        }

        var propVal = Objects.requireNonNull(project.property(propertyKey)).toString();
        Optional<Boolean> parsedPropVal = parseBoolean(propVal);
        if (parsedPropVal.isEmpty()) {
            throw new IllegalStateException(String.format("Could not get unknown value '%s' for the project property '%s' defined in the 'gradle.properties' file. Correct format is either '%s=false' or '%s=true'.", propVal, propertyKey, propertyKey, propertyKey));        }

        return parsedPropVal;
    }

    private Optional<Boolean> getBuildTypeExtensionValue(String buildType) {
        BuildType dslBuildType = this.androidExt.getBuildTypes().getByName(buildType);
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
