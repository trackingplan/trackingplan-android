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
package com.trackingplan.client.adapter.visitor_api;

import com.android.build.api.extension.AndroidComponentsExtension;
import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.android.build.api.instrumentation.FramesComputationMode;
import com.android.build.api.instrumentation.InstrumentationParameters.None;
import com.android.build.api.instrumentation.InstrumentationScope;
import com.android.build.api.variant.ApplicationVariant;
import com.trackingplan.client.adapter.core.AdapterFlagState;
import com.trackingplan.client.adapter.core.asm.AdapterClassVisitor;
import com.trackingplan.client.adapter.core.TransformableChecker;
import com.trackingplan.client.adapter.core.TransformationConfigFactory;

import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassVisitor;

import java.util.stream.Collectors;

import kotlin.Pair;
import kotlin.Unit;

public abstract class TrackingplanClassVisitorFactory implements AsmClassVisitorFactory<None> {

    public static void registerForProject(Project project, AdapterFlagState adapterFlagState) {
        AndroidComponentsExtension androidComponents = project.getExtensions().getByType(AndroidComponentsExtension.class);
        androidComponents.onVariants(androidComponents.selector().all(), (variantProperties) -> {
            ApplicationVariant appVariant = (ApplicationVariant) variantProperties;
            boolean instrumentationEnabled = adapterFlagState.isEnabledFor(appVariant.getName(), appVariant.getBuildType(), appVariant.getProductFlavors().stream().map(Pair::getSecond).collect(Collectors.toList()));
            if (instrumentationEnabled) {
                appVariant.transformClassesWith(TrackingplanClassVisitorFactory.class, InstrumentationScope.ALL, (params) -> Unit.INSTANCE);
                appVariant.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_CLASSES);
            }
        });
    }

    @NotNull
    @Override
    public ClassVisitor createClassVisitor(@NotNull ClassContext classContext, @NotNull ClassVisitor classVisitor) {
        return new AdapterClassVisitor(
                this.getInstrumentationContext().getApiVersion().get(),
                classVisitor,
                (new TransformationConfigFactory()).newClassDataTransformationConfig(classContext)
        );
    }

    @Override
    public boolean isInstrumentable(@NotNull ClassData classData) {
        return TransformableChecker.isClassInstrumentable(classData.getClassName());
    }
}
