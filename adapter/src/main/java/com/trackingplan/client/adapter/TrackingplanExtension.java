// Copyright (c) 2022 Trackingplan
package com.trackingplan.client.adapter;

import com.android.tools.r8.Keep;

import java.util.Optional;

public class TrackingplanExtension {

    private Boolean enabled = null;

    public TrackingplanExtension() {
        TrackingplanPlugin.getLogger().debug("Extension loaded");
    }

    public Optional<Boolean> getEnabled() {
        return Optional.ofNullable(enabled);
    }

    @Keep
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "TrackingplanExtension{" +
                "enabled=" + enabled +
                '}';
    }
}
