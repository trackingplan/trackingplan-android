// Copyright (c) 2021 Trackingplan
package com.trackingplan.client.sdk;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.trackingplan.shared.TrackingplanConfigBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable class.
 * Wraps the shared TrackingplanConfig and adds Android-specific configuration.
 */
final public class TrackingplanConfig {

    public static final int MAX_REQUEST_BODY_SIZE_IN_BYTES =
            com.trackingplan.shared.TrackingplanConfig.MAX_REQUEST_BODY_SIZE_IN_BYTES;

    public final static TrackingplanConfig EMPTY = new TrackingplanConfig();

    // Shared configuration (contains common fields)
    private final com.trackingplan.shared.TrackingplanConfig sharedConfig;

    // Android-specific fields
    private final Map<String, String> customContext;
    private final boolean backgroundObserver;

    @VisibleForTesting
    public static Builder newConfig(@NonNull String tpId) {
        return new Builder(tpId);
    }

    private TrackingplanConfig() {
        // Use empty() factory for sentinel/placeholder values
        this.sharedConfig = com.trackingplan.shared.TrackingplanConfig.Companion.empty();
        this.customContext = Collections.emptyMap();
        this.backgroundObserver = true;
    }

    private TrackingplanConfig(
            com.trackingplan.shared.TrackingplanConfig sharedConfig,
            Map<String, String> customContext,
            boolean backgroundObserver
    ) {
        this.sharedConfig = sharedConfig;
        this.customContext = Collections.unmodifiableMap(new HashMap<>(customContext));
        this.backgroundObserver = backgroundObserver;
    }

    @NonNull
    public String getTpId() {
        return sharedConfig.getTpId();
    }

    public boolean isBackgroundObserverEnabled() {
        return backgroundObserver;
    }

    public boolean isDebugEnabled() {
        return sharedConfig.getDebug();
    }

    boolean isTestingEnabled() {
        return sharedConfig.getTesting();
    }

    public boolean isDryRunEnabled() {
        return sharedConfig.getDryRun();
    }

    public String getSourceAlias() {
        return sharedConfig.getSourceAlias();
    }

    @NonNull
    public String getEnvironment() {
        return sharedConfig.getEnvironment();
    }

    @NonNull
    public Map<String, String> customContext() {
        return customContext;
    }

    @NonNull
    public Map<String, String> customDomains() {
        return sharedConfig.getProviderDomains();
    }

    @NonNull
    public Map<String, String> tags() {
        return sharedConfig.getTags();
    }

    @NonNull
    public String getTracksEndPoint() {
        return sharedConfig.getTracksEndpoint();
    }

    @NonNull
    public String getConfigEndPoint() {
        return sharedConfig.getConfigEndpoint();
    }

    /**
     * Creates a new TrackingplanConfig instance with updated tags.
     *
     * @param newTags The tags to add or set. Must not be null.
     * @param replace If true, replaces all existing tags with the new tags.
     *                If false, merges new tags with existing tags (new values overwrite existing ones for same keys).
     * @return A new TrackingplanConfig instance with updated tags
     */
    @NonNull
    public TrackingplanConfig withTags(@NonNull Map<String, String> newTags, boolean replace) {
        com.trackingplan.shared.TrackingplanConfig updatedSharedConfig =
                sharedConfig.withTags(newTags, replace);
        return new TrackingplanConfig(
                updatedSharedConfig,
                this.customContext,
                this.backgroundObserver
        );
    }

    @Override
    @NonNull
    public String toString() {
        return "TrackingplanConfig{" +
                "tpId='" + getTpId() + '\'' +
                ", environment='" + getEnvironment() + '\'' +
                ", sourceAlias='" + getSourceAlias() + '\'' +
                ", dryRun='" + isDryRunEnabled() + '\'' +
                ", debug='" + isDebugEnabled() + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackingplanConfig that = (TrackingplanConfig) o;
        return backgroundObserver == that.backgroundObserver
                && customContext.equals(that.customContext)
                && sharedConfig.equals(that.sharedConfig);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sharedConfig, customContext, backgroundObserver);
    }

    public static class Builder {

        private final TrackingplanConfigBuilder sharedBuilder;
        private final Map<String, String> customContext = new HashMap<>();
        private boolean backgroundObserver = true;

        public Builder(String tpId) {
            if (tpId.isEmpty()) {
                throw new IllegalArgumentException("Parameter tpId cannot be empty");
            }
            sharedBuilder = new TrackingplanConfigBuilder().tpId(tpId);
        }

        public Builder configEndPoint(@NonNull String configEndPoint) {
            sharedBuilder.configEndpoint(configEndPoint);
            return this;
        }

        @VisibleForTesting
        public Builder customContext(@NonNull Map<String, String> context) {
            this.customContext.clear();
            this.customContext.putAll(context);
            return this;
        }

        public Builder customDomains(@NonNull Map<String, String> customDomains) {
            sharedBuilder.providerDomains(customDomains);
            return this;
        }

        public Builder enableDebug() {
            sharedBuilder.debug(true);
            return this;
        }

        public Builder enableTesting() {
            sharedBuilder.testing(true);
            return this;
        }

        public Builder enableDryRun() {
            sharedBuilder.dryRun(true);
            return this;
        }

        public Builder environment(@NonNull String environment) {
            sharedBuilder.environment(environment);
            return this;
        }

        @VisibleForTesting
        public Builder disableBackgroundObserver() {
            this.backgroundObserver = false;
            return this;
        }

        @Deprecated
        public Builder ignoreContext() {
            // Ignored
            return this;
        }

        public Builder sourceAlias(@NonNull String alias) {
            sharedBuilder.sourceAlias(alias);
            return this;
        }

        public Builder tags(@NonNull Map<String, String> tags) {
            sharedBuilder.tags(tags);
            return this;
        }

        public Builder tracksEndPoint(@NonNull String tracksEndPoint) {
            sharedBuilder.tracksEndpoint(tracksEndPoint);
            return this;
        }

        @NonNull
        public TrackingplanConfig build() {
            com.trackingplan.shared.TrackingplanConfig sharedConfig = sharedBuilder.build();
            return new TrackingplanConfig(
                    sharedConfig,
                    customContext,
                    backgroundObserver
            );
        }
    }
}
