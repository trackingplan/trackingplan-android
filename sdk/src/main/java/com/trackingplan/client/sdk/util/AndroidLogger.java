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

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Copy of https://github.com/firebase/firebase-android-sdk/blob/master/firebase-perf/src/main/java/com/google/firebase/perf/logging/AndroidLogger.java
 */
public class AndroidLogger {

    public enum LogLevel {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    private static LogLevel logLevel = LogLevel.INFO;
    private static volatile AndroidLogger instance;

    private final String logTag;

    public static AndroidLogger getInstance() {
        if (instance == null) {
            synchronized (AndroidLogger.class) {
                if (instance == null) {
                    instance = new AndroidLogger("Trackingplan");
                }
            }
        }
        return instance;
    }

    public AndroidLogger(@NonNull String tag) {
        logTag = tag;
    }

    public static void setLogLevel(LogLevel logLevel) {
        AndroidLogger.logLevel = logLevel;
    }

    /**
     * Logs a DEBUG message to the console (logcat).
     *
     * @param msg The string to log.
     */
    public void debug(String msg) {
        if (LogLevel.DEBUG.compareTo(logLevel) >= 0) {
            Log.d(logTag, msg);
        }
    }

    /**
     * Logs a DEBUG message to the console (logcat).
     *
     * @param format A <a href="../util/Formatter.html#syntax">format string</a>.
     * @param args Arguments referenced by the format specifiers in the format string.
     * @see String#format(Locale, String, Object...)
     */
    public void debug(String format, Object... args) {
        if (LogLevel.DEBUG.compareTo(logLevel) >= 0) {
            debug(String.format(Locale.ENGLISH, format, args));
        }
    }

    /**
     * Logs a VERBOSE message to the console (logcat).
     *
     * @param msg The string to log.
     */
    public void verbose(String msg) {
        if (LogLevel.VERBOSE.compareTo(logLevel) >= 0) {
            Log.v(logTag, msg);
        }
    }

    /**
     * Logs a VERBOSE message to the console (logcat).
     *
     * @param format A <a href="../util/Formatter.html#syntax">format string</a>.
     * @param args Arguments referenced by the format specifiers in the format string.
     * @see String#format(Locale, String, Object...)
     */
    public void verbose(String format, Object... args) {
        if (LogLevel.VERBOSE.compareTo(logLevel) >= 0) {
            verbose(String.format(Locale.ENGLISH, format, args));
        }
    }

    /**
     * Logs a INFO message to the console (logcat).
     *
     * @param msg The string to log.
     */
    public void info(String msg) {
        if (LogLevel.INFO.compareTo(logLevel) >= 0) {
            Log.i(logTag, msg);
        }
    }

    /**
     * Logs an INFO message to the console (logcat).
     *
     * @param format A <a href="../util/Formatter.html#syntax">format string</a>.
     * @param args Arguments referenced by the format specifiers in the format string.
     * @see String#format(Locale, String, Object...)
     */
    public void info(String format, Object... args) {
        if (LogLevel.INFO.compareTo(logLevel) >= 0) {
            info(String.format(Locale.ENGLISH, format, args));
        }
    }

    /**
     * Logs a WARN message to the console (logcat).
     *
     * @param msg The string to log.
     */
    public void warn(String msg) {
        if (LogLevel.WARN.compareTo(logLevel) >= 0) {
            Log.w(logTag, msg);
        }
    }

    /**
     * Logs a WARN message to the console (logcat).
     *
     * @param format A <a href="../util/Formatter.html#syntax">format string</a>.
     * @param args Arguments referenced by the format specifiers in the format string.
     * @see String#format(Locale, String, Object...)
     */
    public void warn(String format, Object... args) {
        if (LogLevel.WARN.compareTo(logLevel) >= 0) {
            warn(String.format(Locale.ENGLISH, format, args));
        }
    }

    /**
     * Logs a ERROR message to the console (logcat).
     *
     * @param msg The string to log.
     */
    public void error(String msg) {
        if (LogLevel.ERROR.compareTo(logLevel) >= 0) {
            Log.e(logTag, msg);
        }
    }

    /**
     * Logs an ERROR message to the console (logcat).
     *
     * @param format A <a href="../util/Formatter.html#syntax">format string</a>.
     * @param args Arguments referenced by the format specifiers in the format string.
     * @see String#format(Locale, String, Object...)
     */
    public void error(String format, Object... args) {
        if (LogLevel.ERROR.compareTo(logLevel) >= 0) {
            error(String.format(Locale.ENGLISH, format, args));
        }
    }
}
