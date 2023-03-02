// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.adapter.visitor_api;

import com.android.annotations.NonNull;
import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.android.build.api.instrumentation.FramesComputationMode;
import com.android.build.api.instrumentation.InstrumentationParameters;
import com.android.build.api.instrumentation.InstrumentationScope;
import com.android.build.api.variant.AndroidComponentsExtension;
import com.android.build.api.variant.ApplicationVariant;
import com.trackingplan.client.adapter.core.AdapterFlagState;
import com.trackingplan.client.adapter.core.asm.AdapterClassVisitor;
import com.trackingplan.client.adapter.core.TransformableChecker;
import com.trackingplan.client.adapter.core.TransformationConfigFactory;
import com.trackingplan.client.adapter.util.GradleLogger;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.objectweb.asm.ClassVisitor;

import kotlin.Unit;

public abstract class TrackingplanClassVisitorFactory
        implements AsmClassVisitorFactory<TrackingplanClassVisitorFactory.TrackingplanParameters> {

    private static final GradleLogger logger = GradleLogger.getInstance();

    public abstract static class TrackingplanParameters implements InstrumentationParameters {

        /**
         * AGP will re-instrument dependencies, when the [InstrumentationParameters] changed
         * https://issuetracker.google.com/issues/190082518#comment4. This is just a dummy parameter
         * that is used solely for that purpose.
         */
        @Input
        @Optional
        public abstract Property<Long> getInvalidate();
    }

    @NonNull
    @Override
    public ClassVisitor createClassVisitor(@NonNull ClassContext classContext, @NonNull ClassVisitor nextClassVisitor) {
        var configs = (new TransformationConfigFactory()).newTransformationConfig(classContext);
        return new AdapterClassVisitor(
                this.getInstrumentationContext().getApiVersion().get(),
                nextClassVisitor,
                configs
        );
    }

    @Override
    public boolean isInstrumentable(@NonNull ClassData classData) {
        var instrumentable = TransformableChecker.isClassInstrumentable(classData.getClassName());
        logger.debug(String.format("Maybe instrument %s? %s", classData.getClassName(), instrumentable ? "Yes" : "No"));
        return instrumentable;
    }

    public static void registerForProject(Project project, AdapterFlagState adapterFlagState) {
        var androidComponents = project.getExtensions().getByType(AndroidComponentsExtension.class);
        androidComponents.onVariants(androidComponents.selector().all(), variant -> {
            final var appVariant = (ApplicationVariant) variant;
            boolean enabled = adapterFlagState.isEnabledFor(appVariant.getName(), appVariant.getBuildType());
            if (enabled) {
                registerForVariant(appVariant);
            }
        });
    }

    public static void registerForVariant(ApplicationVariant appVariant) {
        var instrumentation = appVariant.getInstrumentation();
        instrumentation.transformClassesWith(TrackingplanClassVisitorFactory.class, InstrumentationScope.ALL,
                params -> {
                    // Force re-instrumentation of dependencies by avoiding any caches
                    params.getInvalidate().set(System.currentTimeMillis());
                    params.getInvalidate().disallowChanges();
                    return Unit.INSTANCE;
                });
        instrumentation.setAsmFramesComputationMode(FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_CLASSES);
    }
}
