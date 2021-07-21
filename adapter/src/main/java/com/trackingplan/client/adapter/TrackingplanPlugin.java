// MIT License
//
// Copyright (c) 2021 Trackingplan
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package com.trackingplan.client.adapter;

import com.android.Version;
import com.android.build.gradle.AppExtension;
import com.android.ide.common.repository.GradleVersion;
import com.trackingplan.client.adapter.core.AdapterFlagState;
import com.trackingplan.client.adapter.transform_api.TrackingplanTransform;
import com.trackingplan.client.adapter.visitor_api.TrackingplanClassVisitorFactory;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TrackingplanPlugin implements Plugin<Project> {

    public static final String TRACKINGPLAN_ADAPTER_TAG = "TrackingplanAdapterPlugin";
    public static final String MINIMUM_SUPPORTED_AGP_VERSION_FOR_NEW_TRANSFORM_API = "4.2.0-rc01";
    private static final Logger logger = LoggerFactory.getLogger(TRACKINGPLAN_ADAPTER_TAG);
    public static final String TRACKINGPLAN_ADAPTER_INSTRUMENTATION_ENABLED_KEY = "trackingplanInstrumentationEnabled";

    private boolean foundApplicationPlugin = false;

    @Override
    public void apply(Project project) {
        project.getPluginManager().withPlugin("com.android.application", (androidPlugin) -> {
            this.foundApplicationPlugin = true;
            this.perform(project);
        });
        project.afterEvaluate((project2) -> {
            if (!this.foundApplicationPlugin) {
                throw new IllegalStateException(TRACKINGPLAN_ADAPTER_TAG + " must only be used with Android application projects. Need to apply the 'com.android.application' plugin with this plugin.");
            }
        });
    }

    private void perform(Project project) {

        AppExtension androidExt = project.getExtensions().getByType(AppExtension.class);
        AdapterFlagState adapterFlagState = new AdapterFlagState(project);
        Optional<Boolean> parsedProjectPropertyValue = adapterFlagState.getProjectPropertyValue();

        if (parsedProjectPropertyValue.isPresent() && Boolean.FALSE.equals(parsedProjectPropertyValue.get())) {
            logger.info(String.format("%s is disabled globally for the project by specifying '%s=false' flag in the 'gradle.properties' file.", TRACKINGPLAN_ADAPTER_TAG, TRACKINGPLAN_ADAPTER_INSTRUMENTATION_ENABLED_KEY));
            return;
        }

        if (this.useNewAgpTransformApi()) {
            logger.info(String.format("%s - Using new AGP API", TRACKINGPLAN_ADAPTER_TAG));
            TrackingplanClassVisitorFactory.registerForProject(project, adapterFlagState);
        } else {
            logger.info(String.format("%s - Using RegisterTransform", TRACKINGPLAN_ADAPTER_TAG));
            androidExt.registerTransform(new TrackingplanTransform(adapterFlagState, project.provider(androidExt::getBootClasspath)));
        }
    }

    private boolean useNewAgpTransformApi() {
        try {
            return GradleVersion.parseAndroidGradlePluginVersion(Version.ANDROID_GRADLE_PLUGIN_VERSION)
                    .compareTo(GradleVersion.parseAndroidGradlePluginVersion(MINIMUM_SUPPORTED_AGP_VERSION_FOR_NEW_TRANSFORM_API)) >= 0;
        } catch (NoSuchMethodError | NoClassDefFoundError ex) {
            return false;
        }
    }

    public static Logger getLogger() {
        return logger;
    }
}
