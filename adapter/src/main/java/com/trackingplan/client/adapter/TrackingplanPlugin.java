// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter;

import com.android.build.gradle.AppExtension;
import com.trackingplan.client.adapter.core.AdapterFlagState;
import com.trackingplan.client.adapter.util.GradleLogger;
import com.trackingplan.client.adapter.util.SimpleAGPVersion;
import com.trackingplan.client.adapter.visitor_api.TrackingplanClassVisitorFactory;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class TrackingplanPlugin implements Plugin<Project> {

    public static final String TP_ADAPTER_TAG = "TrackingplanAdapterPlugin";
    public static final String TP_EXTENSION_NAME = "trackingplan";
    public static final String TP_ADAPTER_ENABLED_KEY = "trackingplan.enableSdk";

    private static final GradleLogger logger = GradleLogger.getInstance();

    private boolean foundApplicationPlugin = false;

    public TrackingplanPlugin() {
        logger.info("Plugin Initialized");
    }

    @Override
    public void apply(Project project) {
        project.getPluginManager().withPlugin("com.android.application", (androidPlugin) -> {
            this.foundApplicationPlugin = true;
            this.perform(project);
        });
        project.afterEvaluate((project2) -> {
            if (!this.foundApplicationPlugin) {
                throw new IllegalStateException(TP_ADAPTER_TAG + " must only be used with Android application projects. Need to apply the 'com.android.application' plugin with this plugin.");
            }
        });
    }

    private void perform(Project project) {
        AppExtension androidExt = project.getExtensions().getByType(AppExtension.class);
        registerExtension(androidExt);

        var adapterFlagState = new AdapterFlagState(project);
        var tpGloballyEnabled = adapterFlagState.getProjectPropertyValue(TP_ADAPTER_ENABLED_KEY);

        if (tpGloballyEnabled.isPresent() && !tpGloballyEnabled.get()) {
            logger.info(String.format("%s is disabled globally for the project by specifying '%s=false' flag in the 'gradle.properties' file.", TP_ADAPTER_TAG, TP_ADAPTER_ENABLED_KEY));
            return;
        }

        final var gradlePluginVersion = SimpleAGPVersion.getAndroidGradlePluginVersion(project);
        logger.info(String.format("Detected AGP Version: %s", gradlePluginVersion));

        // Enforce minimum AGP version requirement
        final var minimumRequiredVersion = new SimpleAGPVersion(8, 0, 2);
        if (gradlePluginVersion.compareTo(minimumRequiredVersion) < 0) {
            throw new IllegalStateException(String.format(
                "%s requires Android Gradle Plugin version 8.0.2 or higher. " +
                "Current version: %s. Please upgrade your Android Gradle Plugin to version 8.0.2 or higher.",
                TP_ADAPTER_TAG, gradlePluginVersion
            ));
        }

        // Since minimum AGP is now 8.0.2+, we always use the Instrumentation/Variant API
        // The old Transform API has been removed starting with AGP 8.0
        logger.info("Using Instrumentation/Variant API");
        TrackingplanClassVisitorFactory.registerForProject(project, adapterFlagState);
    }

    private void registerExtension(AppExtension androidExt) {
        androidExt.getBuildTypes().all(buildType -> buildType.getExtensions().add(TP_EXTENSION_NAME, TrackingplanExtension.class));
    }

    public static GradleLogger getLogger() {
        return logger;
    }
}
