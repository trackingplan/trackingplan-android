package com.trackingplan.examples.urlconnection;

public class TestFailedException extends Exception {
    public TestFailedException(String message) {
        super(message);
    }
    public TestFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
