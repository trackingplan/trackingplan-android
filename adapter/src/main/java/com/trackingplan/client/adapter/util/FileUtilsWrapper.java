// Copyright (c) 2022 Trackingplan
package com.trackingplan.client.adapter.util;

import com.android.annotations.NonNull;

import java.io.File;
import java.io.IOException;

import com.android.utils.FileUtils;
import com.google.common.collect.FluentIterable;

public class FileUtilsWrapper {

    @NonNull
    public static FluentIterable<File> getAllFiles(@NonNull File dir) {
        return FileUtils.getAllFiles(dir);
    }

    public static void deleteIfExists(@NonNull File file) throws IOException {
        FileUtils.deleteIfExists(file);
    }

    public static void deleteDirectory(File directory) throws IOException {
        org.apache.commons.io.FileUtils.deleteDirectory(directory);
    }

    public static void copyDirectory(@NonNull File from, @NonNull File to) throws IOException {
        FileUtils.copyDirectory(from, to);
    }

    @NonNull
    public static String relativePossiblyNonExistingPath(@NonNull File file, @NonNull File dir) {
        String path = dir.toURI().relativize(file.toURI()).getPath();
        return toSystemDependentPath(path);
    }

    @NonNull
    private static String toSystemDependentPath(@NonNull String path) {
        if (File.separatorChar != '/') {
            path = path.replace('/', File.separatorChar);
        }
        return path;
    }
}
