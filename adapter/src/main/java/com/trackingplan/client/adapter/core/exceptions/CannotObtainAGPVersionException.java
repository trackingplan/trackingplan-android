// Copyright (c) 2022 Trackingplan
package com.trackingplan.client.adapter.core.exceptions;

public class CannotObtainAGPVersionException extends RuntimeException {
    public CannotObtainAGPVersionException() {
        super("Unable to obtain AGP version. It is likely that the AGP version being used is too old.");
    }
}
