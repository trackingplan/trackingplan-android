// This file contains code copied and adapted from Google Firebase Performance Project, copyrighted
// by Google LLC since 2020 and licensed under the Apache License Version 2.0.
//
// You may obtain a copy of the License at
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Changes to the original work are licensed under the MIT License
//
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
//
// You may see the original Work at
//      https://github.com/firebase/firebase-android-sdk/blob/master/firebase-perf/src/main/java/com/google/firebase/perf/logging/AndroidLogger.java
package com.trackingplan.client.sdk.util;

import androidx.annotation.VisibleForTesting;
import java.util.Locale;

/**
 * Copy of https://github.com/firebase/firebase-android-sdk/blob/master/firebase-perf/src/main/java/com/google/firebase/perf/logging/LogWrapper.java
 */
public class AndroidLogger {

    private static volatile AndroidLogger instance;

    private final LogWrapper logWrapper;

    private boolean isLogcatEnabled = false;

    public static AndroidLogger getInstance() {
        if (instance == null) {
            synchronized (AndroidLogger.class) {
                if (instance == null) {
                    instance = new AndroidLogger();
                }
            }
        }

        return instance;
    }

    @VisibleForTesting
    public AndroidLogger(LogWrapper logWrapper) {
        this.logWrapper = (logWrapper == null) ? LogWrapper.getInstance() : logWrapper;
    }

    private AndroidLogger() {
        this(null);
    }

    public void setLogcatEnabled(boolean logcatEnabled) {
        this.isLogcatEnabled = logcatEnabled;
    }

    /**
     * Returns whether console logging is enabled by the developer or not.
     *
     * @see #setLogcatEnabled(boolean)
     */
    public boolean isLogcatEnabled() {
        return isLogcatEnabled;
    }

    /**
     * Logs a DEBUG message to the console (logcat) if {@link #isLogcatEnabled} is {@code true}.
     *
     * @param msg The string to log.
     */
    public void debug(String msg) {
        if (isLogcatEnabled) {
            logWrapper.d(msg);
        }
    }

    /**
     * Logs a DEBUG message to the console (logcat) if {@link #isLogcatEnabled} is {@code true}.
     *
     * @param format A <a href="../util/Formatter.html#syntax">format string</a>.
     * @param args Arguments referenced by the format specifiers in the format string.
     * @see String#format(Locale, String, Object...)
     */
    public void debug(String format, Object... args) {
        if (isLogcatEnabled) {
            logWrapper.d(String.format(Locale.ENGLISH, format, args));
        }
    }

    /**
     * Logs a VERBOSE message to the console (logcat) if {@link #isLogcatEnabled} is {@code true}.
     *
     * @param msg The string to log.
     */
    public void verbose(String msg) {
        if (isLogcatEnabled) {
            logWrapper.v(msg);
        }
    }

    /**
     * Logs a VERBOSE message to the console (logcat) if {@link #isLogcatEnabled} is {@code true}.
     *
     * @param format A <a href="../util/Formatter.html#syntax">format string</a>.
     * @param args Arguments referenced by the format specifiers in the format string.
     * @see String#format(Locale, String, Object...)
     */
    public void verbose(String format, Object... args) {
        if (isLogcatEnabled) {
            logWrapper.v(String.format(Locale.ENGLISH, format, args));
        }
    }

    /**
     * Logs a INFO message to the console (logcat) if {@link #isLogcatEnabled} is {@code true}.
     *
     * @param msg The string to log.
     */
    public void info(String msg) {
        if (isLogcatEnabled) {
            logWrapper.i(msg);
        }
    }

    /**
     * Logs an INFO message to the console (logcat) if {@link #isLogcatEnabled} is {@code true}.
     *
     * @param format A <a href="../util/Formatter.html#syntax">format string</a>.
     * @param args Arguments referenced by the format specifiers in the format string.
     * @see String#format(Locale, String, Object...)
     */
    public void info(String format, Object... args) {
        if (isLogcatEnabled) {
            logWrapper.i(String.format(Locale.ENGLISH, format, args));
        }
    }

    /**
     * Logs a WARN message to the console (logcat) if {@link #isLogcatEnabled} is {@code true}.
     *
     * @param msg The string to log.
     */
    public void warn(String msg) {
        if (isLogcatEnabled) {
            logWrapper.w(msg);
        }
    }

    /**
     * Logs a WARN message to the console (logcat) if {@link #isLogcatEnabled} is {@code true}.
     *
     * @param format A <a href="../util/Formatter.html#syntax">format string</a>.
     * @param args Arguments referenced by the format specifiers in the format string.
     * @see String#format(Locale, String, Object...)
     */
    public void warn(String format, Object... args) {
        if (isLogcatEnabled) {
            logWrapper.w(String.format(Locale.ENGLISH, format, args));
        }
    }

    /**
     * Logs a ERROR message to the console (logcat) if {@link #isLogcatEnabled} is {@code true}.
     *
     * @param msg The string to log.
     */
    public void error(String msg) {
        if (isLogcatEnabled) {
            logWrapper.e(msg);
        }
    }

    /**
     * Logs an ERROR message to the console (logcat) if {@link #isLogcatEnabled} is {@code true}.
     *
     * @param format A <a href="../util/Formatter.html#syntax">format string</a>.
     * @param args Arguments referenced by the format specifiers in the format string.
     * @see String#format(Locale, String, Object...)
     */
    public void error(String format, Object... args) {
        if (isLogcatEnabled) {
            logWrapper.e(String.format(Locale.ENGLISH, format, args));
        }
    }
}
