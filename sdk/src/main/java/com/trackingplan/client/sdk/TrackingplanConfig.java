// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable class
 */
final public class TrackingplanConfig {

    public static final int MAX_REQUEST_BODY_SIZE_IN_BYTES = 100 * 1024;

    public final static float SAMPLING_RATE_UNINITIALIZED = -1;

    public final static TrackingplanConfig emptyConfig = new TrackingplanConfig();

    private String tpId;
    private String environment;
    private String sourceAlias;
    private final Map<String, String> customDomains = new HashMap<>();

    private boolean ignoreContext;
    private boolean debug;
    private boolean dryRun;

    private TrackingplanConfig() {
        this.tpId = "";
        this.environment = "PRODUCTION";
        this.ignoreContext = false;
        this.sourceAlias = "android";
        this.debug = false;
        this.dryRun = false;
    }

    private TrackingplanConfig(String tpId) {
        this();
        this.tpId = tpId;
    }

    @NonNull
    public String getTpId() {
        return tpId;
    }

    public boolean isDebugEnabled() {
        return debug;
    }

    public boolean isDryRunEnabled() {
        return dryRun;
    }

    public boolean ignoreContext() {
        return ignoreContext;
    }

    public String getSourceAlias() {
        return sourceAlias;
    }

    @NonNull
    public String getEnvironment() {
        return environment;
    }

    @NonNull
    public Map<String, String> customDomains() {
        return Collections.unmodifiableMap(customDomains);
    }

    @Override
    @NonNull
    public String toString() {
        return "TrackingplanConfig{" +
                "tpId='" + tpId + '\'' +
                ", environment='" + environment + '\'' +
                ", sourceAlias='" + sourceAlias + '\'' +
                ", ignoreContext'=" + ignoreContext + '\'' +
                ", dryRun='" + dryRun + '\'' +
                ", debug='" + debug + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackingplanConfig that = (TrackingplanConfig) o;
        return ignoreContext == that.ignoreContext
                && debug == that.debug
                && dryRun == that.dryRun
                && tpId.equals(that.tpId)
                && environment.equals(that.environment)
                && sourceAlias.equals(that.sourceAlias)
                && customDomains.equals(that.customDomains);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tpId, environment, sourceAlias, customDomains, ignoreContext, debug, dryRun);
    }

    static class Builder {

        private TrackingplanConfig config;

        public Builder(String tpId) {
            if (tpId.isEmpty()) {
                throw new IllegalArgumentException("Parameter tpId cannot be empty");
            }
            config = new TrackingplanConfig(tpId);
        }

        public void enableDebug() {
            config.debug = true;
        }

        public void ignoreContext() {
            config.ignoreContext = true;
        }

        public void enableDryRun() {
            config.dryRun = true;
        }

        public void sourceAlias(@NonNull String alias) {
            config.sourceAlias = alias;
        }

        public void environment(@NonNull String environment) {
            config.environment = environment;
        }

        public void customDomains(@NonNull Map<String, String> customDomains) {
            config.customDomains.clear();
            config.customDomains.putAll(customDomains);
        }

        @NonNull
        public TrackingplanConfig build() {

            TrackingplanConfig result = this.config;

            if (result.isDryRunEnabled() && !result.isDebugEnabled()) {
                throw new RuntimeException("Cannot enable DryRun mode. DryRun mode must be used along with " +
                        "Debug mode. Please, enable Debug mode or remove dryRun() call from initialization");
            }

            reset();
            return result;
        }

        private void reset() {
            String tpId = config.getTpId();
            config = new TrackingplanConfig(tpId);
        }
    }
}
