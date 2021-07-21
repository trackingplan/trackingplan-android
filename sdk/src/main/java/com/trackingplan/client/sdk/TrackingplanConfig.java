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
package com.trackingplan.client.sdk;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable class
 */
final public class TrackingplanConfig {

    public final static float SAMPLING_RATE_UNINITIALIZED = -1;

    private final String tpId;
    private String environment;
    private String sourceAlias;
    private final Map<String, String> customDomains = new HashMap<>();

    private boolean ignoreContext;
    private boolean debug;
    private boolean dryRun;

    private TrackingplanConfig(String tpId) {
        this.tpId = tpId;
        this.environment = "PRODUCTION";
        this.ignoreContext = false;
        this.debug = false;
        this.dryRun = false;
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
