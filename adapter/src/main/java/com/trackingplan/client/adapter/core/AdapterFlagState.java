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
package com.trackingplan.client.adapter.core;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.api.ApplicationVariant;
import com.android.builder.model.ProductFlavor;

import org.gradle.api.Project;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final public class AdapterFlagState implements Serializable {

    private static final boolean ADAPTER_ENABLED_DEFAULT = true;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private final Optional<Boolean> parsedProjectPropertyValue;

    private final Map<String, Boolean> variantToInstrumentationEnabledMap = new HashMap<>();

    public AdapterFlagState(Project project) {
        AppExtension androidExt = project.getExtensions().getByType(AppExtension.class);
        this.parsedProjectPropertyValue = Optional.empty();
        androidExt.getApplicationVariants().all(this::updateInstrumentationEnabledFor);
    }

    public Optional<Boolean> getProjectPropertyValue() {
        return this.parsedProjectPropertyValue;
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
        // TODO: Add support for variants, buildTypes and flavors
        return ADAPTER_ENABLED_DEFAULT;
    }
}
