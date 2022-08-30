// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter;

import com.android.build.gradle.AppExtension;
import com.trackingplan.client.adapter.core.AdapterFlagState;
import com.trackingplan.client.adapter.transform_api.TrackingplanTransform;
import com.trackingplan.client.adapter.util.GradleLogger;
import com.trackingplan.client.adapter.util.SimpleAGPVersion;
import com.trackingplan.client.adapter.visitor_api.TrackingplanClassVisitorFactory;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class TrackingplanPlugin implements Plugin<Project> {

    public static final String TP_ADAPTER_TAG = "TrackingplanAdapterPlugin";
    public static final String TP_EXTENSION_NAME = "trackingplan";
    public static final String TP_ADAPTER_ENABLED_KEY = "trackingplan.enableSdk";
    public static final String TP_ADAPTER_USE_ASM_CLASS_VISITOR_KEY = "trackingplan.useAsmClassVisitor";

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
        var useAsmClassVisitor = adapterFlagState.getProjectPropertyValue(TP_ADAPTER_USE_ASM_CLASS_VISITOR_KEY);

        if (tpGloballyEnabled.isPresent() && Boolean.FALSE.equals(tpGloballyEnabled.get())) {
            logger.info(String.format("%s is disabled globally for the project by specifying '%s=false' flag in the 'gradle.properties' file.", TP_ADAPTER_TAG, TP_ADAPTER_ENABLED_KEY));
            return;
        }

        final var gradlePluginVersion = SimpleAGPVersion.getAndroidGradlePluginVersion();
        logger.info(String.format("Detected AGP Version: %s", gradlePluginVersion));


        if (gradlePluginVersion.compareTo(new SimpleAGPVersion(8, 0, 0)) >= 0
                || (gradlePluginVersion.compareTo(new SimpleAGPVersion(7, 2, 0)) >= 0
                && useAsmClassVisitor.isPresent() && Boolean.TRUE.equals((useAsmClassVisitor.get())))) {

            // Note that the use of the AsmClassVisitor (Variant / Instrumentation API) is opted-in
            // due to the lack of support to transform bytecode from third-party dependencies when using
            // this API. Starting with AGP 8.0 this will be the default method as the Transform API will
            // be removed.

            logger.info("Using new Instrumentation API");
            TrackingplanClassVisitorFactory.registerForProject(project);

        } else {

            // The deprecated Transform API is the preferred method to do bytecode transformation as
            // (AFAIK) this is the only method so far capable of transforming dependencies.

            logger.info("Using Transform API (deprecated)");
            androidExt.registerTransform(new TrackingplanTransform(adapterFlagState, project.provider(androidExt::getBootClasspath)));
        }
    }

    private void registerExtension(AppExtension androidExt) {
        androidExt.getBuildTypes().all(buildType -> buildType.getExtensions().add(TP_EXTENSION_NAME, TrackingplanExtension.class));
    }

    public static GradleLogger getLogger() {
        return logger;
    }
}
