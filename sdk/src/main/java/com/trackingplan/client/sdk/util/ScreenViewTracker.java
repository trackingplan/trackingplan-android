package com.trackingplan.client.sdk.util;

import android.app.Activity;
import android.app.Application;

import android.os.Bundle;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final public class ScreenViewTracker {

    public interface ScreenViewListener  {
        void onScreenViewed(@NonNull final String screenName,
                            @NonNull final String previousScreenName);
    }

    private static final AndroidLog logger = AndroidLog.getInstance();

    private Application application;
    private final MyActivityLifecycleCallbacks callbacks;
    private String lastScreenName = "";

    private final List<ScreenViewListener> listeners = new ArrayList<>();

    public ScreenViewTracker() {
        callbacks = new MyActivityLifecycleCallbacks();
    }

    @MainThread
    public void start(@NonNull final Application application) {
        if (this.application != null) return;
        this.application = application;
        lastScreenName = "";
        application.registerActivityLifecycleCallbacks(callbacks);
    }

    @MainThread
    public void stop() {
        if (application == null) return;
        application.unregisterActivityLifecycleCallbacks(callbacks);
        application = null;
    }

    @NonNull
    public String getLastScreenName() {
        return lastScreenName;
    }

    public void registerScreenViewListener(@NonNull final ScreenViewListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void unregisterScreenViewListener(@NonNull final ScreenViewListener listener) {
        listeners.remove(listener);
    }

    private void doScreenTransition(Class<?> clazz) {

        final var currentScreenName = clazz.getSimpleName();
        final var lastScreenName = this.lastScreenName;

        if (lastScreenName.equals(currentScreenName)) {
            return;
        }

        this.lastScreenName = currentScreenName;

        for (var listener : listeners) {
            try {
                listener.onScreenViewed(currentScreenName, lastScreenName);
            } catch (Exception ex) {
                logger.error("Error when notifying screen transition: " + ex.getMessage());
            }
        }
    }

    private class MyActivityLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

        private final Set<String> excludedClasses = new HashSet<>() {{
            add("zzd"); // Google
            add("NavHostFragment"); // Nav Component
            add("SupportRequestManagerFragment"); // Glide
        }};

        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle bundle) {
            if (activity instanceof FragmentActivity) {
                final var fm = ((FragmentActivity) activity).getSupportFragmentManager();
                fm.registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
                    @Override
                    public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                        super.onFragmentResumed(fm, f);

                        if (excludedClasses.contains(f.getClass().getSimpleName())) {
                            return;
                        }

                        if (f instanceof DialogFragment) {
                            return;
                        }

                        doScreenTransition(f.getClass());
                    }
                }, true);
            }
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            if (activity instanceof FragmentActivity) {
                final var fm = ((FragmentActivity) activity).getSupportFragmentManager();
                if (fm.getFragments().isEmpty()) {
                    // Likely AppCompatActivity or similar where FragmentActivity is inherited but
                    // fragments aren't used
                    doScreenTransition(activity.getClass());
                }
                // else {
                    // Ignore as this will be managed by fragment lifecycle callbacks
                    // logger.info("Fragment Resumed: " + lastScreenName);
                // }
            } else {
                doScreenTransition(activity.getClass());
            }
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {}

        @Override
        public void onActivityStarted(@NonNull Activity activity) {}

        @Override
        public void onActivityPaused(@NonNull Activity activity) {}

        @Override
        public void onActivityStopped(@NonNull Activity activity) {}

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle bundle) {}
    }
}
