// Copyright (c) 2022 Trackingplan
package com.trackingplan.client.adapter.util;

import com.android.annotations.NonNull;
import com.trackingplan.client.adapter.TrackingplanPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GradleLogger {

    private static final String LOG_TAG = TrackingplanPlugin.TP_ADAPTER_TAG;
    private static volatile GradleLogger instance;
    
    private final Logger logWrapper;

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

    public GradleLogger(Logger logger) {
        this.logWrapper = logger == null ? LoggerFactory.getLogger(LOG_TAG) : logger;
    }

    private GradleLogger() {
        this(null);
    }

    public void debug(String message) {
        this.logWrapper.debug(getFormattedMessage(message));
    }

    public void debug(String message, Object var2) {
        this.logWrapper.debug(getFormattedMessage(message), var2);
    }

    public void debug(String message, Object var2, Object var3) {
        this.logWrapper.debug(getFormattedMessage(message), var2, var3);
    }

    public void debug(String message, Object... var2) {
        this.logWrapper.debug(getFormattedMessage(message), var2);
    }

    public void info(String message) {
        this.logWrapper.info(getFormattedMessage(message));
    }

    public void info(String message, Object var2) {
        this.logWrapper.info(getFormattedMessage(message), var2);
    }

    public void warn(String message) {
        this.logWrapper.warn(getFormattedMessage(message));
    }

    public void error(String message) {
        this.logWrapper.error(getFormattedMessage(message));
    }

    public void error(String message, Object var2) {
        this.logWrapper.error(getFormattedMessage(message), var2);
    }

    private String getFormattedMessage(@NonNull String message) {
        if (this.logWrapper.isDebugEnabled()) {
            return message;
        }
        return String.format("[%s] %s", LOG_TAG, message);
    }
}
