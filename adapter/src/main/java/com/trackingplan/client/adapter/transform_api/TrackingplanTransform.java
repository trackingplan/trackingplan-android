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
package com.trackingplan.client.adapter.transform_api;

import com.android.build.api.transform.DirectoryInput;
import com.android.build.api.transform.Format;
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.QualifiedContent.ContentType;
import com.android.build.api.transform.QualifiedContent.Scope;
import com.android.build.api.transform.Status;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider;
import com.android.build.api.variant.VariantInfo;
import com.android.utils.FileUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Files;
import com.trackingplan.client.adapter.TrackingplanPlugin;
import com.trackingplan.client.adapter.core.AdapterFlagState;

import org.apache.commons.io.IOUtils;
import org.gradle.api.provider.Provider;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TrackingplanTransform extends Transform {

    private static final Logger logger = TrackingplanPlugin.getLogger();
    private final Set<ContentType> typeClasses;
    private final Set<Scope> scopes;
    private AsmTransformer trackingplanInstrumentation;
    private boolean applyToVariantUsed = false;
    private final AdapterFlagState adapterFlagState;
    private final Provider<List<File>> bootClasspathProvider;

    public TrackingplanTransform(AdapterFlagState adapterFlagState, Provider<List<File>> bootClasspathProvider) {
        this.adapterFlagState = adapterFlagState;
        this.bootClasspathProvider = bootClasspathProvider;
        this.typeClasses = ImmutableSet.of(QualifiedContent.DefaultContentType.CLASSES);
        this.scopes = ImmutableSet.of(QualifiedContent.Scope.EXTERNAL_LIBRARIES, QualifiedContent.Scope.PROJECT, QualifiedContent.Scope.SUB_PROJECTS);
    }

    @Override
    public String getName() {
        return TrackingplanPlugin.TRACKINGPLAN_ADAPTER_TAG;
    }

    @Override
    public Set<ContentType> getInputTypes() {
        return typeClasses;
    }

    @Override
    public Set<Scope> getScopes() {
        return scopes;
    }

    @Override
    public boolean isIncremental() {
        return true;
    }

    @Override
    public boolean applyToVariant(VariantInfo variant) {
        this.applyToVariantUsed = true;
        boolean enabled = this.adapterFlagState.isEnabledFor(variant.getFullVariantName(), variant.getBuildTypeName(), variant.getFlavorNames());
        logger.info(String.format("applyToVariant(%s): %s", variant, enabled));
        return enabled;
    }

    @Override
    public Map<String, Object> getParameterInputs() {
        return ImmutableMap.copyOf(this.adapterFlagState.getVariantToInstrumentationEnabledMap());
    }

    @Override
    public void transform(TransformInvocation invocation) throws IOException {

        Collection<TransformInput> transformInputs = invocation.getInputs();
        Collection<TransformInput> referencedInputs = invocation.getReferencedInputs();
        TransformOutputProvider outputProvider = invocation.getOutputProvider();
        boolean incremental = invocation.isIncremental();
        String variantName = invocation.getContext().getVariantName();
        boolean instrumentationEnabled = this.applyToVariantUsed || this.adapterFlagState.isEnabledFor(variantName);

        List<URL> runtimeCP = this.buildRuntimeClasspath(transformInputs, referencedInputs);

        try (URLClassLoader cl = new URLClassLoader(runtimeCP.toArray(new URL[0]))) {

            logger.info("Transforming with incremental: {}", incremental);
            if (!incremental) {
                outputProvider.deleteAll();
            }

            this.trackingplanInstrumentation = new AsmTransformer(cl);

            for (TransformInput transformInput : transformInputs) {
                this.transformDirectoryInputs(transformInput, outputProvider, incremental, instrumentationEnabled);
                this.transformJarInputs(transformInput, outputProvider, incremental, instrumentationEnabled);
            }
        }
    }

    private void transformDirectoryInputs(TransformInput transformInput, TransformOutputProvider outputProvider, boolean incremental, boolean instrumentationEnabled) throws IOException {
        for (DirectoryInput directoryInput : transformInput.getDirectoryInputs()) {
            File inputDir = directoryInput.getFile();
            File outputDir = outputProvider.getContentLocation(directoryInput.getName(), directoryInput.getContentTypes(), directoryInput.getScopes(), Format.DIRECTORY);
            logger.debug("transformDirectoryInputs() >> inputDir: '{}', outputDir: '{}'", inputDir, outputDir);
            if (instrumentationEnabled) {
                this.performTransformationForDirectoryInput(directoryInput, inputDir, outputDir, incremental);
            } else {
                this.performDummyTransformationForDirectoryInput(inputDir, outputDir);
            }
        }
    }

    private void performTransformationForDirectoryInput(DirectoryInput directoryInput, File inputDir, File outputDir, boolean incremental) throws IOException {

        if (incremental) {
            for (Map.Entry<File, Status> fileStatusEntry : directoryInput.getChangedFiles().entrySet()) {

                File inputFile = fileStatusEntry.getKey();
                Status incrementalStatus = fileStatusEntry.getValue();
                logger.debug("performTransformationForDirectoryInput() >> inputFile: '{}', incrementalStatus: {}", inputFile, incrementalStatus);

                switch (incrementalStatus) {
                    case NOTCHANGED:
                    default:
                        break;
                    case ADDED:
                    case CHANGED:
                        this.transformFile(inputFile, inputDir, outputDir);
                        break;
                    case REMOVED:
                        File outputFile = toOutputFile(outputDir, inputDir, inputFile);
                        FileUtils.deleteIfExists(outputFile);
                }
            }
        } else {
            for (File inputFile : FileUtils.getAllFiles(inputDir)) {
                this.transformFile(inputFile, inputDir, outputDir);
            }
        }
    }

    private void transformFile(File inputFile, File inputDir, File outputDir) throws IOException {
        if (!inputFile.isDirectory() && inputFile.getName().endsWith(".class")) {
            File outputFile = toOutputFile(outputDir, inputDir, inputFile);
            Files.createParentDirs(outputFile);
            trackingplanInstrumentation.transformClassFile(inputFile, outputFile);
        }
    }

    private void performDummyTransformationForDirectoryInput(File inputDir, File outputDir) throws IOException {
        if (outputDir.mkdirs() || outputDir.isDirectory()) {
            org.apache.commons.io.FileUtils.deleteDirectory(outputDir);
            FileUtils.copyDirectory(inputDir, outputDir);
        }
    }

    private void transformJarInputs(TransformInput transformInput, TransformOutputProvider outputProvider, boolean incremental, boolean instrumentationEnabled) throws IOException {
        for (JarInput jarInput : transformInput.getJarInputs()) {
            File inputJar = jarInput.getFile();
            File outputJar = outputProvider.getContentLocation(jarInput.getName(), jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
            logger.debug("transformJarInputs() >> inputJar: '{}', outputJar: '{}'", inputJar, outputJar);
            if (instrumentationEnabled) {
                this.performTransformationForJarInput(jarInput, inputJar, outputJar, incremental);
            } else {
                this.performDummyTransformationForJarInput(inputJar, outputJar);
            }
        }
    }

    private void performTransformationForJarInput(JarInput jarInput, File inputJar, File outputJar, boolean incremental) throws IOException {
        if (incremental) {
            Status incrementalStatus = jarInput.getStatus();
            logger.debug("performTransformationForJarInput() >> inputJar: '{}', incrementalStatus: {}", inputJar, incrementalStatus);
            switch (incrementalStatus) {
                case NOTCHANGED:
                default:
                    break;
                case ADDED:
                case CHANGED:
                    this.transformJar(inputJar, outputJar);
                    break;
                case REMOVED:
                    FileUtils.deleteIfExists(outputJar);
            }
        } else {
            this.transformJar(inputJar, outputJar);
        }

    }

    private void transformJar(File inputJar, File outputJar) throws IOException {
        Files.createParentDirs(outputJar);
        this.trackingplanInstrumentation.transformClassesInJar(inputJar, outputJar);
    }

    private void performDummyTransformationForJarInput(File inputJar, File outputJar) throws IOException {

        Files.createParentDirs(outputJar);

        try (FileInputStream fis = new FileInputStream(inputJar); FileOutputStream fos = new FileOutputStream(outputJar)) {
            IOUtils.copy(fis, fos);
        }
    }

    private static File toOutputFile(File outputDir, File inputDir, File inputFile) {
        return new File(outputDir, FileUtils.relativePossiblyNonExistingPath(inputFile, inputDir));
    }

    private List<URL> buildRuntimeClasspath(Collection<TransformInput> transformInputs, Collection<TransformInput> referencedInputs) {

        List<File> classPaths = new ArrayList<>(this.bootClasspathProvider.get());

        for (Collection<TransformInput> inputs : Arrays.asList(transformInputs, referencedInputs)) {

            for (TransformInput transformInput : inputs) {

                List<Collection<? extends QualifiedContent>> allQualifiedContents = Arrays.asList(transformInput.getDirectoryInputs(), transformInput.getJarInputs());

                for (Collection<? extends QualifiedContent> allQualifiedContent : allQualifiedContents) {

                    for (QualifiedContent qualifiedContent : allQualifiedContent) {
                        classPaths.add(qualifiedContent.getFile());
                    }
                }
            }
        }

        return classPaths.stream().map((file) -> {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException ex) {
                logger.error("Unable to instrument classes due to file '{}'", file);
                throw new RuntimeException(ex);
            }
        }).collect(Collectors.toList());
    }
}
