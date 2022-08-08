// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk.exceptions;

import java.io.IOException;

public class TrackingplanSendException extends IOException {
    public TrackingplanSendException(String message) {
        super(message);
    }
    public TrackingplanSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
