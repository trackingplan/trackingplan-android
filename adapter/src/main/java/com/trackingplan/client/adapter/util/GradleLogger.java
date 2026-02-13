// Copyright (c) 2022 Trackingplan
package com.trackingplan.client.adapter.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class GradleLogger {

    private static volatile GradleLogger instance;
    private static volatile String logFilePath;

    public static GradleLogger getInstance() {
        if (instance == null) {
            synchronized (GradleLogger.class) {
                if (instance == null) {
                    instance = new GradleLogger();
                }
            }
        }
        return instance;
    }

    private GradleLogger() {
    }

    public void debug(String message) {
        writeToFile(message);
    }

    public void debug(String message, Object var2) {
        writeToFile(formatMessage(message, var2));
    }

    public void debug(String message, Object var2, Object var3) {
        writeToFile(formatMessage(message, var2, var3));
    }

    public void debug(String message, Object... var2) {
        writeToFile(formatMessage(message, var2));
    }

    public void info(String message) {
        writeToFile(message);
    }

    public void info(String message, Object var2) {
        writeToFile(formatMessage(message, var2));
    }

    public void warn(String message) {
        writeToFile(message);
    }

    public void error(String message) {
        writeToFile(message);
    }

    public void error(String message, Object var2) {
        writeToFile(formatMessage(message, var2));
    }

    private static boolean writeErrorLogged = false;
    private static PrintWriter fileWriter;

    public static synchronized void setLogFile(String path) {
        resetFileWriter();
        logFilePath = path;
    }

    public static synchronized void resetFileWriter() {
        if (fileWriter != null) {
            fileWriter.close();
            fileWriter = null;
        }
        writeErrorLogged = false;
    }

    private static synchronized void writeToFile(String message) {
        if (logFilePath == null) return;
        try {
            if (fileWriter == null) {
                File logFile = new File(logFilePath);
                logFile.getParentFile().mkdirs();
                fileWriter = new PrintWriter(new BufferedWriter(new FileWriter(logFilePath, true)));
            }
            fileWriter.println(LocalDateTime.now() + " " + message);
            fileWriter.flush();
        } catch (IOException e) {
            if (fileWriter != null) {
                fileWriter.close();
                fileWriter = null;
            }
            if (!writeErrorLogged) {
                writeErrorLogged = true;
                System.err.println("Trackingplan: failed to write log file " + logFilePath + ": " + e.getMessage());
            }
        }
    }

    private static String formatMessage(String template, Object... args) {
        if (args == null || args.length == 0) return template;
        StringBuilder sb = new StringBuilder();
        int argIndex = 0;
        int i = 0;
        while (i < template.length()) {
            if (i < template.length() - 1 && template.charAt(i) == '{' && template.charAt(i + 1) == '}' && argIndex < args.length) {
                sb.append(args[argIndex++]);
                i += 2;
            } else {
                sb.append(template.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }
}
