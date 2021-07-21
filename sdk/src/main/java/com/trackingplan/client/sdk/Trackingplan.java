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

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.trackingplan.client.sdk.util.AndroidLogger;
import com.trackingplan.client.sdk.util.LogWrapper;

import java.util.Map;

@SuppressWarnings("unused")
final public class Trackingplan {

    private static final AndroidLogger logger = AndroidLogger.getInstance();

    @SuppressWarnings("unused")
    public static Initializer init(String tpId) {
        return new Initializer(tpId);
    }

    public static class Initializer {

        private final TrackingplanConfig.Builder configBuilder;

        private Initializer(@NonNull String tpId) {
            configBuilder = new TrackingplanConfig.Builder(tpId);
        }

        @SuppressWarnings("unused")
        public Initializer enableDebug() {
            AndroidLogger.getInstance().setLogcatEnabled(true);
            configBuilder.enableDebug();
            return this;
        }

        @SuppressWarnings("unused")
        public Initializer ignoreContext() {
            configBuilder.ignoreContext();
            return this;
        }

        public Initializer dryRun() {
            configBuilder.enableDryRun();
            return this;
        }

        @SuppressWarnings("unused")
        public Initializer sourceAlias(@NonNull String alias) {
            configBuilder.sourceAlias(alias);
            return this;
        }

        @SuppressWarnings("unused")
        public Initializer environment(@NonNull String environment) {
            configBuilder.environment(environment);
            return this;
        }

        @SuppressWarnings("unused")
        public Initializer customDomains(@NonNull Map<String, String> customDomains) {
            configBuilder.customDomains(customDomains);
            return this;
        }

        @SuppressWarnings("unused")
        public void start(Context context) {
            try {

                if (TrackingplanInstance.getInstance() != null) {
                    Log.w(LogWrapper.LOG_TAG, "Trackingplan already initialized");
                    return;
                }

                TrackingplanConfig config = configBuilder.build();

                if (config.isDebugEnabled()) {
                    logger.info("Debug mode enabled");
                }

                if (config.isDryRunEnabled()) {
                    logger.info("DryRun mode enabled");
                }

                TrackingplanInstance instance = new TrackingplanInstance(
                        config,
                        context.getApplicationContext(),
                        ProcessLifecycleOwner.get().getLifecycle()
                );

                TrackingplanInstance.registerInstance(instance);

                Log.i(LogWrapper.LOG_TAG, "Trackingplan v" + BuildConfig.SDK_VERSION + " initialized");

            } catch (Exception e) {
                // Use Log because AndroidLogger may not be enabled
                Log.w(LogWrapper.LOG_TAG, "Trackingplan initialization failed: " + e.getMessage());
            }
        }
    }
}
