package com.trackingplan.client.sdk.test;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.trackingplan.client.sdk.util.AndroidLog;
import com.trackingplan.client.sdk.util.Logger;

import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestLogger implements Logger {

    private final List<LogMessage> messages;

    private final List<Expectation> expectations;

    private final List<LogMessage> matches;

    private final int maxSize;

    @VisibleForTesting
    public TestLogger(int maxLogMessages) {
        messages = Collections.synchronizedList(new ArrayList<>());
        maxSize = maxLogMessages;
        expectations = Collections.synchronizedList(new ArrayList<>());
        matches = Collections.synchronizedList(new ArrayList<>());
    }

    @Override
    public void v(@NonNull String msg) {
        append(new LogMessage(AndroidLog.LogLevel.VERBOSE, msg));
    }

    @Override
    public void d(@NonNull String msg) {
        append(new LogMessage(AndroidLog.LogLevel.DEBUG, msg));
    }

    @Override
    public void i(@NonNull String msg) {
        append(new LogMessage(AndroidLog.LogLevel.INFO, msg));
    }

    @Override
    public void w(@NonNull String msg) {
        append(new LogMessage(AndroidLog.LogLevel.WARN, msg));
    }

    @Override
    public void e(@NonNull String msg) {
        append(new LogMessage(AndroidLog.LogLevel.ERROR, msg));
    }

    private void append(@NonNull LogMessage logMessage) {
        if (messages.size() == maxSize) {
            messages.remove(0);
        }

        messages.add(logMessage);

        if (expectations.size() > 0 && expectations.get(0).match(logMessage.message)) {
            matches.add(logMessage);
            expectations.remove(0);
        }
    }

    public void reset() {
        messages.clear();
        expectations.clear();
        matches.clear();
    }

    public boolean containsExactMessage(@NonNull String msg) {
        for (LogMessage logMessage : messages) {
            if (logMessage.message.equals(msg)) {
                return true;
            }
        }
        return false;
    }

    public void expectExactMessage(@NonNull String message) {
        expectations.add(new Expectation(Expectation.MatchOperator.Exact, message));
    }

    public void expectMessageStartsWith(@NonNull String prefix) {
        expectations.add(new Expectation(Expectation.MatchOperator.StartsWith, prefix));
    }

    public void assertExpectationsMatch() {

        if (expectations.isEmpty() && !matches.isEmpty()) {
            matches.clear();
            Assert.assertTrue(true);
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Missing expected log messages:\n");
        for (Expectation expectation : expectations) {
            builder.append("\t- ");
            builder.append(expectation.message);
            builder.append("\n");
        }

        Assert.fail(builder.toString());
    }

    private static class LogMessage {
        public final AndroidLog.LogLevel level;
        public final String message;

        public LogMessage(AndroidLog.LogLevel logLevel, @NonNull String message) {
            this.level = logLevel;
            this.message = message;
        }
    }

    private static class Expectation {

        enum MatchOperator {
            Exact,
            StartsWith
        }

        public final MatchOperator operator;
        public final String message;

        public Expectation(MatchOperator operator, @NonNull String message) {
            this.operator = operator;
            this.message = message;
        }

        public boolean match(@NonNull String message) {
            switch (operator) {
                case Exact:
                    return message.equals(this.message);
                case StartsWith:
                    return message.startsWith(this.message);
                default:
                    return false;
            }
        }
    }
}
