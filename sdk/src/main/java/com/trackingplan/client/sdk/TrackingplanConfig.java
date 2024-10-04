// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable class
 */
final public class TrackingplanConfig {

    public static final int MAX_REQUEST_BODY_SIZE_IN_BYTES = 100 * 1024;

    public final static TrackingplanConfig EMPTY = new TrackingplanConfig();

    private String tpId;
    private String environment;
    private String sourceAlias;
    private final Map<String, String> customDomains = new HashMap<>();
    private final Map<String, String> tags = new HashMap<>();
    private final Map<String, String> customContext = new HashMap<>();

    private boolean debug;
    private boolean dryRun;
    private boolean testing;
    private boolean backgroundObserver;

    private String tracksEndPoint;
    private String configEndPoint;

    @VisibleForTesting
    public static Builder newConfig(@NonNull String tpId) {
        return new Builder(tpId);
    }

    private TrackingplanConfig() {
        this.tpId = "";
        this.environment = "PRODUCTION";
        this.sourceAlias = "android";
        this.debug = false;
        this.dryRun = false;
        this.testing = false;
        this.backgroundObserver = true;
        this.tracksEndPoint = "https://eu-tracks.trackingplan.com/v1/";
        this.configEndPoint = "https://config.trackingplan.com/";
    }

    private TrackingplanConfig(String tpId) {
        this();
        this.tpId = tpId;
    }

    @NonNull
    public String getTpId() {
        return tpId;
    }

    public boolean isBackgroundObserverEnabled() {
        return backgroundObserver;
    }

    public boolean isDebugEnabled() {
        return debug;
    }

    boolean isTestingEnabled() {
        return testing;
    }

    public boolean isDryRunEnabled() {
        return dryRun;
    }

    public String getSourceAlias() {
        return sourceAlias;
    }

    @NonNull
    public String getEnvironment() {
        return environment;
    }

    @NonNull
    public Map<String, String> customContext() {
        return Collections.unmodifiableMap(customContext);
    }

    @NonNull
    public Map<String, String> customDomains() {
        return Collections.unmodifiableMap(customDomains);
    }

    @NonNull
    public Map<String, String> tags() {
        return Collections.unmodifiableMap(tags);
    }

    @NonNull
    public String getTracksEndPoint() {
        return tracksEndPoint;
    }

    @NonNull
    public String getConfigEndPoint() {
        return configEndPoint;
    }

    @Override
    @NonNull
    public String toString() {
        return "TrackingplanConfig{" +
                "tpId='" + tpId + '\'' +
                ", environment='" + environment + '\'' +
                ", sourceAlias='" + sourceAlias + '\'' +
                ", dryRun='" + dryRun + '\'' +
                ", debug='" + debug + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackingplanConfig that = (TrackingplanConfig) o;
        return backgroundObserver == that.backgroundObserver
                && configEndPoint.equals(that.configEndPoint)
                && customContext.equals(that.customContext)
                && customDomains.equals(that.customDomains)
                && debug == that.debug
                && dryRun == that.dryRun
                && environment.equals(that.environment)
                && tags.equals(that.tags)
                && tpId.equals(that.tpId)
                && tracksEndPoint.equals(that.tracksEndPoint)
                && sourceAlias.equals(that.sourceAlias);
    }

    @Override
    public int hashCode() {
        return Objects.hash(backgroundObserver, configEndPoint, customContext, customDomains,
                debug, dryRun, environment, tags, tpId, tracksEndPoint, sourceAlias);
    }

    public static class Builder {

        private TrackingplanConfig config;

        public Builder(String tpId) {
            if (tpId.isEmpty()) {
                throw new IllegalArgumentException("Parameter tpId cannot be empty");
            }
            config = new TrackingplanConfig(tpId);
        }

        public Builder configEndPoint(@NonNull String configEndPoint) {
            config.configEndPoint = endPointInput(configEndPoint);
            return this;
        }

        @VisibleForTesting
        public Builder customContext(@NonNull Map<String, String> context) {
            config.customContext.clear();
            config.customContext.putAll(context);
            return this;
        }

        public Builder customDomains(@NonNull Map<String, String> customDomains) {
            config.customDomains.clear();
            config.customDomains.putAll(customDomains);
            return this;
        }

        public Builder enableDebug() {
            config.debug = true;
            return this;
        }

        public Builder enableTesting() {
            config.testing = true;
            return this;
        }

        public Builder enableDryRun() {
            config.dryRun = true;
            return this;
        }

        public Builder environment(@NonNull String environment) {
            config.environment = environment;
            return this;
        }

        @VisibleForTesting
        public Builder disableBackgroundObserver() {
            config.backgroundObserver = false;
            return this;
        }

        public Builder ignoreContext() {
            // Ignored
            return this;
        }

        public Builder sourceAlias(@NonNull String alias) {
            config.sourceAlias = alias;
            return this;
        }

        public Builder tags(@NonNull Map<String, String> tags) {
            config.tags.clear();
            config.tags.putAll(tags);
            return this;
        }

        public Builder tracksEndPoint(@NonNull String tracksEndPoint) {
            config.tracksEndPoint = endPointInput(tracksEndPoint);
            return this;
        }

        @NonNull
        public TrackingplanConfig build() {

            TrackingplanConfig result = this.config;
            reset();

            if (result.isDryRunEnabled() && !result.isDebugEnabled()) {
                throw new RuntimeException("Cannot enable DryRun mode. DryRun mode must be used along with " +
                        "Debug mode. Please, enable Debug mode or remove dryRun() call from initialization");
            }

            return result;
        }

        private String endPointInput(@NonNull String endPoint) {
            if (!endPoint.endsWith("/")) {
                return endPoint + "/";
            }
            return endPoint;
        }

        private void reset() {
            String tpId = config.getTpId();
            config = new TrackingplanConfig(tpId);
        }
    }
}
