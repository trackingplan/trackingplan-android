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

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.trackingplan.client.adapter.TrackingplanPlugin;
import com.trackingplan.client.adapter.core.asm.AdapterClassVisitor;
import com.trackingplan.client.adapter.core.TransformableChecker;
import com.trackingplan.client.adapter.core.TransformationConfig;
import com.trackingplan.client.adapter.core.TransformationConfigFactory;
import com.trackingplan.client.adapter.core.asm.TransformedAttribute;
import com.trackingplan.client.adapter.core.exceptions.AlreadyTransformedException;

import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

final class AsmTransformer {

    protected static final int ASM_API_VERSION = Opcodes.ASM9;
    private static final Logger logger = TrackingplanPlugin.getLogger();
    private final TransformationConfig transformationConfig;
    private final ClassLoader classLoader;

    public AsmTransformer(ClassLoader classLoader) {
        this.transformationConfig = (new TransformationConfigFactory()).newClassLoaderTransformationConfig(classLoader);
        this.classLoader = classLoader;
    }

    public void transformClassFile(File inputFile, File outputFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(inputFile); FileOutputStream fos = new FileOutputStream(outputFile)) {
            logger.debug("instrumentClassFile() >> inputFile: '{}', outputFile: '{}'", inputFile, outputFile);
            fos.write(doTransformation(ByteStreams.toByteArray(fis)));
        } catch (AlreadyTransformedException ex) {
            logger.error("Already instrumented class - {}", ex.getMessage());
        } catch (Exception ex) {
            logger.error("Can't instrument because of {}. Copying as is.", ex.getMessage());
            Files.copy(inputFile, outputFile);
        }
    }

    public void transformClassesInJar(File inputJar, File outputJar) throws IOException {

        try (ZipFile inputZip = new ZipFile(inputJar);
             FileInputStream fis = new FileInputStream(inputJar);
             ZipInputStream zis = new ZipInputStream(fis);
             FileOutputStream fos = new FileOutputStream(outputJar);
             ZipOutputStream zos = new ZipOutputStream(fos)
        ) {
            for (ZipEntry inEntry = zis.getNextEntry(); inEntry != null; inEntry = zis.getNextEntry()) {

                String entryName = inEntry.getName();

                if (!inEntry.isDirectory()) {

                    try (BufferedInputStream bis = new BufferedInputStream(inputZip.getInputStream(inEntry))) {

                        byte[] entryBytes = ByteStreams.toByteArray(bis);
                        boolean isClassFile = entryName.endsWith(".class");
                        boolean isInstrumentable = TransformableChecker.isTransformable(entryName);
                        logger.debug("instrumentClassesInJar() >> entryName: {}, isClassFile: {}, isInstrumentable: {}", entryName, isClassFile, isInstrumentable);

                        if (isClassFile && isInstrumentable) {
                            try {
                                entryBytes = this.doTransformation(entryBytes);
                            } catch (AlreadyTransformedException ex) {
                                logger.error("Already instrumented class - {}", ex.getMessage());
                                continue;
                            } catch (Exception ex) {
                                logger.error("Can't instrument because of {}. Copying as is.", ex.getMessage());
                            }
                        } else {
                            logger.debug("Copying '{}' without instrumenting", entryName);
                        }

                        zos.putNextEntry(new ZipEntry(entryName));
                        zos.write(entryBytes);
                        zos.closeEntry();
                    }
                }
            }
        }
    }

    private byte[] doTransformation(byte[] in) {
        ClassWriter cw = new AdapterClassWriter(this.classLoader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        ClassVisitor cv = new AdapterClassVisitor(ASM_API_VERSION, cw, this.transformationConfig);
        ClassReader cr = new ClassReader(in);
        cr.accept(cv, new Attribute[]{new TransformedAttribute("")}, ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }

    // Dummy Instrumentation: Parse class from bytecode and convert it back to bytecode
    private byte[] doDummyTransformation(byte[] in) {
        ClassWriter cw = new AdapterClassWriter(this.classLoader, 0);
        ClassReader cr = new ClassReader(in);
        cr.accept(cw, 0);
        return cw.toByteArray();
    }
}
